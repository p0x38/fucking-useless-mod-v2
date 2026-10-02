package me.p0x38.fabric.client.renderers;

import me.p0x38.fabric.client.mixins.GameRendererMixin;
import me.p0x38.fuckinguselessmod.FuckingUselessMod;
import me.p0x38.fuckinguselessmod.util.DebugLogger;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class CensorBoxRenderer {
    private static final Identifier HUD_ID =
            Identifier.fromNamespaceAndPath(
                    FuckingUselessMod.MOD_ID,
                    "censor_box"
            );

    private static final Set<UUID> CENSORED_ENTITIES =
            new HashSet<>();

    private static final int BOX_PADDING = 4;
    private static final int MIN_BOX_WIDTH = 4;
    private static final int MIN_BOX_HEIGHT = 4;

    /*
     * Box size is quantized separately from its position so
     * perspective changes also appear steppy instead of smooth.
     */
    private static final int SIZE_STEP = 4;

    /*
     * First-person hands are not rendered through the normal
     * entity renderer, so they need their own HUD-space boxes.
     */
    private static final int FIRST_PERSON_POSITION_STEP = 8;

    private static final Map<HumanoidArm, FirstPersonHandState> FIRST_PERSON_HAND_STATES =
            new EnumMap<>(HumanoidArm.class);

    private static float firstPersonPartialTick;

    private static final int POSITION_STEP = 6;
    private static final int MAX_JITTER = 1;
    private static final int MIN_UPDATE_TICKS = 2;
    private static final int MAX_UPDATE_TICKS = 6;
    private static final Map<UUID, CensorMotionState> MOTION_STATES =
            new HashMap<>();
    private static long firstPersonRenderGeneration;

    private CensorBoxRenderer() {
    }

    public static void initialize() {
        HudElementRegistry.addLast(
                HUD_ID,
                CensorBoxRenderer::render
        );

        DebugLogger.debug(
                "[CensorBox] registered HUD element"
        );
    }

    public static void toggle(UUID uuid) {
        if (!CENSORED_ENTITIES.add(uuid)) {
            CENSORED_ENTITIES.remove(uuid);
            MOTION_STATES.remove(uuid);
        }

        DebugLogger.debug(
                "[CensorBox] uuid={} enabled={}",
                uuid,
                CENSORED_ENTITIES.contains(uuid)
        );
    }

    public static void add(UUID uuid) {
        CENSORED_ENTITIES.add(uuid);

        DebugLogger.debug(
                "[CensorBox] added uuid={}",
                uuid
        );
    }

    public static boolean remove(UUID uuid) {
        boolean removed =
                CENSORED_ENTITIES.remove(uuid);

        if (removed) {
            MOTION_STATES.remove(uuid);

            DebugLogger.debug(
                    "[CensorBox] removed uuid={}",
                    uuid
            );
        }

        return removed;
    }

    public static void clear() {
        CENSORED_ENTITIES.clear();
        MOTION_STATES.clear();

        DebugLogger.debug(
                "[CensorBox] cleared"
        );
    }

    public static boolean isCensored(UUID uuid) {
        return CENSORED_ENTITIES.contains(uuid);
    }

    public static Set<UUID> getCensoredEntities() {
        return Set.copyOf(CENSORED_ENTITIES);
    }

    /*
     * Called immediately before vanilla starts rendering the
     * first-person hands for the current frame.
     *
     * We mark every hand as absent first, then renderPlayerArm()
     * marks the ones vanilla actually draws.
     */
    public static void beginFirstPersonHandTracking(float partialTick) {
        firstPersonPartialTick = partialTick;
        firstPersonRenderGeneration++;

        for (FirstPersonHandState state :
                FIRST_PERSON_HAND_STATES.values()) {
            state.visible = false;
        }
    }

    public static void setFirstPersonPartialTick(float partialTick) {
        firstPersonPartialTick = partialTick;
    }

    public static float getFirstPersonPartialTick() {
        return firstPersonPartialTick;
    }

    /*
     * Called from ItemInHandRenderer.renderPlayerArm().
     *
     * This means the HUD box follows the actual vanilla arm
     * render path instead of guessing from held-item state.
     */
    public static void markFirstPersonHand(
            HumanoidArm arm,
            float ndcMinX,
            float ndcMinY,
            float ndcMaxX,
            float ndcMaxY
    ) {
        FirstPersonHandState state =
                FIRST_PERSON_HAND_STATES.computeIfAbsent(
                        arm,
                        key -> new FirstPersonHandState()
                );

        state.visible = true;
        state.renderGeneration = firstPersonRenderGeneration;
        state.ndcMinX = ndcMinX;
        state.ndcMinY = ndcMinY;
        state.ndcMaxX = ndcMaxX;
        state.ndcMaxY = ndcMaxY;
    }

    private static void render(
            GuiGraphics graphics,
            DeltaTracker deltaTracker
    ) {
        if (CENSORED_ENTITIES.isEmpty()) {
            return;
        }

        Minecraft client =
                Minecraft.getInstance();

        var level = client.level;

        if (level == null) {
            return;
        }

        Camera camera =
                client.gameRenderer.getMainCamera();

        Vec3 cameraPosition =
                camera.position();

        var forward =
                camera.forwardVector();

        var up =
                camera.upVector();

        var left =
                camera.leftVector();

        float partialTick =
                deltaTracker.getGameTimeDeltaPartialTick(
                        false
                );

        int screenWidth =
                graphics.guiWidth();

        int screenHeight =
                graphics.guiHeight();

        long gameTick =
                level.getGameTime();

        boolean firstPerson =
                client.options.getCameraType() == CameraType.FIRST_PERSON;

        if (firstPerson
                && client.player != null
                && CENSORED_ENTITIES.contains(
                client.player.getUUID()
        )) {
            renderFirstPerson(
                    graphics,
                    screenWidth,
                    screenHeight,
                    gameTick,
                    client.player.getUUID()
            );
        }

        /*
         * Use the actual FOV calculated by vanilla for this
         * frame instead of reading the raw FOV option.
         *
         * This includes vanilla FOV modifiers such as
         * sprinting and their interpolation.
         *
         * The censor box itself remains fixed-shape 2D GUI
         * geometry, so camera rotation cannot distort it.
         */
        float fov =
                ((GameRendererMixin) client.gameRenderer)
                        .fuckingUselessMod$getFov(
                                camera,
                                partialTick,
                                true
                        );

        double fovRadians = Math.toRadians(fov);

        double focalLength =
                (screenHeight * 0.5)
                        / Math.tan(fovRadians * 0.5);

        for (UUID uuid : CENSORED_ENTITIES) {
            /*
             * The local player is represented by first-person
             * hands instead of the normal world entity.
             *
             * Do not project it from the camera position or the
             * censor box can become enormous because the camera
             * is effectively inside the entity.
             */
            if (firstPerson
                    && client.player != null
                    && uuid.equals(client.player.getUUID())) {
                continue;
            }

            Entity entity =
                    findEntity(
                            level,
                            uuid
                    );

            if (entity == null) {
                continue;
            }

            /*
             * The HUD is rendered after the world, so depth testing
             * cannot tell us whether the entity is actually visible.
             *
             * Perform several camera-to-entity raycasts instead.
             * The box is shown when at least one sampled point can
             * be seen. If every sampled point is behind a block,
             * the censor box disappears.
             */
            if (!isVisible(
                    level,
                    cameraPosition,
                    entity,
                    partialTick
            )) {
                continue;
            }

            Vec3 position =
                    entity.getPosition(partialTick);

            /*
             * Put the anchor around the entity's center.
             */
            position = position.add(
                    0.0,
                    entity.getBbHeight() * 0.5,
                    0.0
            );

            double relativeX =
                    position.x - cameraPosition.x;

            double relativeY =
                    position.y - cameraPosition.y;

            double relativeZ =
                    position.z - cameraPosition.z;

            /*
             * Transform world-space position into camera-space
             * using the camera basis vectors.
             */
            double depth =
                    relativeX * forward.x()
                            + relativeY * forward.y()
                            + relativeZ * forward.z();

            /*
             * Behind the camera.
             */
            if (depth <= 0.01) {
                continue;
            }

            /*
             * Positive = camera-left.
             */
            double horizontal =
                    relativeX * left.x()
                            + relativeY * left.y()
                            + relativeZ * left.z();

            /*
             * Positive = camera-up.
             */
            double vertical =
                    relativeX * up.x()
                            + relativeY * up.y()
                            + relativeZ * up.z();

            double screenX =
                    screenWidth * 0.5
                            - horizontal
                            * focalLength
                            / depth;

            double screenY =
                    screenHeight * 0.5
                            - vertical
                            * focalLength
                            / depth;

            /*
             * Estimate the entity's viewport size from its
             * bounding box and the camera basis.
             *
             * This changes only the size of the 2D HUD rectangle.
             * No 3D rotation or world-space scaling is used.
             */
            double halfEntityWidth =
                    entity.getBbWidth() * 0.5;

            double halfEntityHeight =
                    entity.getBbHeight() * 0.5;

            double projectedHalfWidth = (Math.abs(left.x()) * halfEntityWidth + Math.abs(left.y()) * halfEntityHeight + Math.abs(left.z()) * halfEntityWidth) * focalLength / depth;
            double projectedHalfHeight = (Math.abs(up.x()) * halfEntityWidth + Math.abs(up.y()) * halfEntityHeight + Math.abs(up.z()) * halfEntityWidth) * focalLength / depth;

            int boxWidth =
                    Math.max(
                            MIN_BOX_WIDTH,
                            stepSize(
                                    (int) Math.ceil(
                                            projectedHalfWidth * 2.0
                                                    + BOX_PADDING * 2.0
                                    )
                            )
                    );

            int boxHeight =
                    Math.max(
                            MIN_BOX_HEIGHT,
                            stepSize(
                                    (int) Math.ceil(
                                            projectedHalfHeight * 2.0
                                                    + BOX_PADDING * 2.0
                                    )
                            )
                    );

            int x =
                    (int) Math.round(
                            screenX
                                    - boxWidth * 0.5
                    );

            int y =
                    (int) Math.round(
                            screenY
                                    - boxHeight * 0.5
                    );

            /*
             * Skip entities completely outside the screen.
             */
            if (x + boxWidth < 0
                    || x > screenWidth
                    || y + boxHeight < 0
                    || y > screenHeight) {
                continue;
            }

            /*
             * Make the censor box intentionally "wrong":
             *
             * - It does not smoothly follow the entity.
             * - Position is quantized into discrete steps.
             * - Small random offsets are introduced on each jump.
             * - It only updates every few game ticks.
             *
             * This gives it a deliberately broken / steppy
             * tracking effect while keeping the box itself
             * completely flat and fixed-size.
             */
            CensorMotionState state =
                    MOTION_STATES.computeIfAbsent(
                            uuid,
                            key -> new CensorMotionState(
                                    stepPosition(x),
                                    stepPosition(y),
                                    boxWidth,
                                    boxHeight,
                                    gameTick
                            )
                    );

            if (gameTick >= state.nextUpdateTick) {
                int nextX = stepPosition(x) + randomJitter();
                int nextY = stepPosition(y) + randomJitter();

                state.x = nextX;
                state.y = nextY;
                state.width = boxWidth;
                state.height = boxHeight;

                state.nextUpdateTick = gameTick + ThreadLocalRandom.current().nextLong(MIN_UPDATE_TICKS, MAX_UPDATE_TICKS + 1L);

                DebugLogger.debug(
                        "[CensorBox] updated uuid={} screen=({}, {}) size=({}, {}) depth={}",
                        uuid,
                        state.x,
                        state.y,
                        state.width,
                        state.height,
                        depth
                );
            }

            graphics.fill(
                    state.x,
                    state.y,
                    state.x + state.width,
                    state.y + state.height,
                    0xFF000000
            );
        }
    }

    private static void renderFirstPerson(
            GuiGraphics graphics,
            int screenWidth,
            int screenHeight,
            long gameTick,
            UUID uuid
    ) {
        if (FIRST_PERSON_HAND_STATES.isEmpty()) {
            return;
        }

        for (Map.Entry<HumanoidArm, FirstPersonHandState> entry :
                FIRST_PERSON_HAND_STATES.entrySet()) {
            FirstPersonHandState handState =
                    entry.getValue();

            /*
             * Vanilla didn't render this arm during the current
             * renderHandsWithItems() invocation.
             *
             * Do not trust the previous frame's visibility state.
             */
            if (!handState.visible
                    || handState.renderGeneration
                    != firstPersonRenderGeneration) {
                continue;
            }

            /*
             * Keep jitter stable for the whole game tick.
             *
             * The actual arm position is still updated every
             * rendered frame from vanilla's PoseStack.
             */
            if (handState.lastJitterTick != gameTick) {
                handState.jitterX = randomJitter();
                handState.jitterY = randomJitter();
                handState.lastJitterTick = gameTick;
            }

            /*
             * Convert the actual transformed arm bounds from NDC
             * into GUI coordinates.
             */
            int minX =
                    Math.round(
                            (handState.ndcMinX + 1.0F)
                                * 0.5f
                            * screenWidth
                    );

            int minY =
                    Math.round(
                            (1.0f - handState.ndcMaxY)
                                    * 0.5f
                                    * screenHeight
                    );

            int maxX =
                    Math.round(
                            (handState.ndcMaxX + 1.0f)
                                    * 0.5f
                                    * screenWidth
                    );

            int maxY =
                    Math.round(
                            (handState.ndcMinY)
                                    * 0.5F
                                    * screenHeight
                    );

            /*
             * The actual projected arm size is used directly.
             * This means the box follows FOV, bobbing, equipment
             * motion, and vanilla swing transforms.
             */
            int boxWidth =
                    Math.max(
                            MIN_BOX_WIDTH,
                            stepSize(
                                    maxX - minX + BOX_PADDING * 2
                            )
                    );

            int boxHeight =
                    Math.max(
                            MIN_BOX_HEIGHT,
                            stepSize(
                                    maxY - minY + BOX_PADDING * 2
                            )
                    );

            int centerX = (minX + maxX) / 2;
            int centerY = (minY + maxY) / 2;

            /*
             * Step the center, not the actual vanilla animation.
             * The animation therefore remains recognizable while
             * the censor itself still has the intentionally broken
             * movement.
             */
            centerX = stepFirstPersonPosition(centerX) + handState.jitterX;
            centerY = stepFirstPersonPosition(centerY) + handState.jitterY;

            int boxX = centerX - boxWidth / 2;
            int boxY = centerY - boxHeight / 2;

            graphics.fill(
                    boxX,
                    boxY,
                    boxX + boxWidth,
                    boxY + boxHeight,
                    0xFF000000
            );
        }
    }

    private static int stepFirstPersonPosition(int value) {
        return Math.round(
                (float) value / FIRST_PERSON_POSITION_STEP
        ) * FIRST_PERSON_POSITION_STEP;
    }

    private static int stepPosition(int value) {
        return Math.round(
                (float) value / POSITION_STEP
        ) * POSITION_STEP;
    }

    private static int stepSize(int value) {
        return Math.max(
                MIN_BOX_WIDTH,
                Math.round(
                        (float) value / SIZE_STEP
                ) * SIZE_STEP
        );
    }

    private static int randomJitter() {
        return ThreadLocalRandom.current().nextInt(-MAX_JITTER, MAX_JITTER + 1);
    }

    private static boolean isVisible(
            ClientLevel level,
            Vec3 cameraPosition,
            Entity entity,
            float partialTick
    ) {
        /*
         * Shift the bounding box to the interpolated position so
         * the visibility test uses the same frame position as the
         * HUD projection.
         */
        Vec3 interpolatedPosition =
                entity.getPosition(partialTick);

        Vec3 currentPosition =
                entity.position();

        AABB box =
                entity.getBoundingBox().move(
                        interpolatedPosition.subtract(
                                currentPosition
                        )
                );

        double centerX = (box.minX + box.maxX) * .5;
        double centerY = (box.minY + box.maxY) * .5;
        double centerZ = (box.minZ + box.maxZ) * .5;

        /*
         * Center + top/bottom + four side points + four corners.
         *
         * This handles partial obstruction much better than a
         * single center-point raycast.
         */
        Vec3[] samples = {
                new Vec3(centerX, centerY, centerZ),

                new Vec3(centerX, box.maxY, centerZ),
                new Vec3(centerX, box.minY, centerZ),

                new Vec3(box.minX, centerY, centerZ),
                new Vec3(box.maxX, centerY, centerZ),

                new Vec3(centerX, centerY, box.minZ),
                new Vec3(centerX, centerY, box.maxZ),

                new Vec3(box.minX, box.minY, box.minZ),
                new Vec3(box.maxX, box.maxY, box.maxZ),
                new Vec3(box.minX, box.maxY, box.maxZ),
                new Vec3(box.maxX, box.minY, box.minZ)
        };

        for (Vec3 target : samples) {
            if (canSee(
                    level,
                    cameraPosition,
                    target,
                    entity
            )) {
                return true;
            }
        }

        return false;
    }

    private static boolean canSee(
            ClientLevel level,
            Vec3 cameraPosition,
            Vec3 target,
            Entity entity
    ) {
        BlockHitResult hit =
                level.clip(
                        new ClipContext(
                                cameraPosition,
                                target,
                                ClipContext.Block.OUTLINE,
                                ClipContext.Fluid.NONE,
                                entity
                        )
                );

        return hit.getType() == HitResult.Type.MISS;
    }

    private static Entity findEntity(
            ClientLevel level,
            UUID uuid
    ) {
        for (Entity entity :
                level.entitiesForRendering()) {
            if (entity.getUUID().equals(uuid)) {
                return entity;
            }
        }

        return null;
    }

    private static final class FirstPersonHandState {
        private boolean visible;
        private long renderGeneration = Long.MIN_VALUE;

        private long lastJitterTick = Long.MIN_VALUE;
        private int jitterX;
        private int jitterY;

        private float ndcMinX;
        private float ndcMinY;
        private float ndcMaxX;
        private float ndcMaxY;
    }

    private static final class CensorMotionState {
        private int x;
        private int y;
        private int width;
        private int height;

        private long nextUpdateTick;

        private CensorMotionState(
                int initialX,
                int initialY,
                int width,
                int height,
                long nextUpdateTick
        ) {
            this.x = initialX;
            this.y = initialY;
            this.width = width;
            this.height = height;
            this.nextUpdateTick = nextUpdateTick;
        }
    }
}