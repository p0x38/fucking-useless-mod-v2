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
    private static final int FIRST_PERSON_BOX_WIDTH = 72;
    private static final int FIRST_PERSON_BOX_HEIGHT = 64;
    private static final int FIRST_PERSON_POSITION_STEP = 8;

    private static final EnumSet<HumanoidArm> VISIBLE_FIRST_PERSON_HANDS =
            EnumSet.noneOf(HumanoidArm.class);

    private static final int POSITION_STEP = 6;
    private static final int MAX_JITTER = 1;

    private static final int MIN_UPDATE_TICKS = 2;
    private static final int MAX_UPDATE_TICKS = 6;

    private static final Map<UUID, CensorMotionState> MOTION_STATES =
            new HashMap<>();

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

    public static void beginFirstPersonHandTracking() {
        VISIBLE_FIRST_PERSON_HANDS.clear();
    }

    public static void markFirstPersonHand(HumanoidArm arm) {
        VISIBLE_FIRST_PERSON_HANDS.add(arm);
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
        if (VISIBLE_FIRST_PERSON_HANDS.isEmpty()) {
            return;
        }

        CensorMotionState state =
                MOTION_STATES.computeIfAbsent(
                        uuid,
                        key -> {
                            int leftX = firstPersonLeftX(screenWidth);

                            int y = firstPersonY(screenHeight);

                            return new CensorMotionState(
                                    leftX,
                                    y,
                                    FIRST_PERSON_BOX_WIDTH,
                                    FIRST_PERSON_BOX_HEIGHT,
                                    gameTick
                            );
                        }
                );

        if (gameTick >= state.nextUpdateTick) {
            state.x = stepFirstPersonPosition(
                    firstPersonLeftX(screenWidth)
            ) + randomJitter();
            state.y = stepFirstPersonPosition(
                    firstPersonY(screenHeight)
            ) + randomJitter();

            state.width = FIRST_PERSON_BOX_WIDTH;
            state.height = FIRST_PERSON_BOX_HEIGHT;

            state.nextUpdateTick =
                    gameTick + ThreadLocalRandom.current().nextLong(
                            MIN_UPDATE_TICKS,
                            MAX_UPDATE_TICKS + 1L
                    );

            DebugLogger.debug(
                    "[CensorBox] updated first-person uuid={} hands={} size=({}, {})",
                    uuid,
                    visibleFirstPersonHands(),
                    state.width,
                    state.height
            );
        }

        for (HumanoidArm arm : VISIBLE_FIRST_PERSON_HANDS) {
            int handX =
                    arm == HumanoidArm.RIGHT
                        ? firstPersonRightX(screenWidth)
                            : firstPersonLeftX(screenWidth);

            handX =
                    stepFirstPersonPosition(handX)
                            + (state.x - firstPersonLeftX(screenWidth));

            graphics.fill(
                    handX,
                    state.y,
                    handX + state.width,
                    state.y + state.height,
                    0xFF000000
            );
        }
    }

    private static String visibleFirstPersonHands() {
        return VISIBLE_FIRST_PERSON_HANDS.toString();
    }

    private static int firstPersonLeftX(int screenWidth) {
        return (int) (
                screenWidth * 0.08
                );
    }

    private static int firstPersonRightX(int screenWidth) {
        return (int) (
                screenWidth * 0.72
                );
    }

    private static int firstPersonY(int screenHeight) {
        return (int) (
                screenHeight * 0.72
                );
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