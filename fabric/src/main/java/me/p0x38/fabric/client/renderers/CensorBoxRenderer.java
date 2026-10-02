package me.p0x38.fabric.client.renderers;

import me.p0x38.fabric.client.mixins.GameRendererMixin;
import me.p0x38.fuckinguselessmod.Config;
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
import net.minecraft.world.entity.LivingEntity;
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

    private static final int MIN_BOX_WIDTH = 4;
    private static final int MIN_BOX_HEIGHT = 4;

    /*
     * Box size is quantized separately from its position so
     * perspective changes also appear steppy instead of smooth.
     */

    /*
     * First-person hands are not rendered through the normal
     * entity renderer, so they need their own HUD-space boxes.
     */
    private static final Map<HumanoidArm, FirstPersonHandState> FIRST_PERSON_HAND_STATES =
            new EnumMap<>(HumanoidArm.class);

    private static float firstPersonPartialTick;

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

        Config.Data config = Config.get();

        if (!config.censorBoxEnabled) {
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
                    partialTick,
                    config
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
            Vec3 interpolatedPosition = entity.getPosition(partialTick);
            Vec3 currentPosition = entity.position();
            AABB visibilityBox = entity.getBoundingBox().move(
                    interpolatedPosition.subtract(currentPosition)
            );

            double visibilityCenterX =
                    (visibilityBox.minX + visibilityBox.maxX) * 0.5;
            double visibilityCenterY =
                    (visibilityBox.minY + visibilityBox.maxY) * 0.5;
            double visibilityCenterZ =
                    (visibilityBox.minZ + visibilityBox.maxZ) * 0.5;

            boolean visible =
                    canSee(
                            level,
                            cameraPosition,
                            new Vec3(
                                    visibilityCenterX,
                                    visibilityCenterY,
                                    visibilityCenterZ
                            ),
                            entity
                    )
                            || canSee(
                            level,
                            cameraPosition,
                            new Vec3(
                                    visibilityCenterX,
                                    visibilityBox.maxY,
                                    visibilityCenterZ
                            ),
                            entity
                    )
                            || canSee(
                            level,
                            cameraPosition,
                            new Vec3(
                                    visibilityCenterX,
                                    visibilityBox.minY,
                                    visibilityCenterZ
                            ),
                            entity
                    )
                            || canSee(
                            level,
                            cameraPosition,
                            new Vec3(
                                    visibilityBox.minX,
                                    visibilityCenterY,
                                    visibilityCenterZ
                            ),
                            entity
                    )
                            || canSee(
                            level,
                            cameraPosition,
                            new Vec3(
                                    visibilityBox.maxX,
                                    visibilityCenterY,
                                    visibilityCenterZ
                            ),
                            entity
                    )
                            || canSee(
                            level,
                            cameraPosition,
                            new Vec3(
                                    visibilityCenterX,
                                    visibilityCenterY,
                                    visibilityBox.minZ
                            ),
                            entity
                    )
                            || canSee(
                            level,
                            cameraPosition,
                            new Vec3(
                                    visibilityCenterX,
                                    visibilityCenterY,
                                    visibilityBox.maxZ
                            ),
                            entity
                    )
                            || canSee(
                            level,
                            cameraPosition,
                            new Vec3(
                                    visibilityBox.minX,
                                    visibilityBox.minY,
                                    visibilityBox.minZ
                            ),
                            entity
                    )
                            || canSee(
                            level,
                            cameraPosition,
                            new Vec3(
                                    visibilityBox.maxX,
                                    visibilityBox.maxY,
                                    visibilityBox.maxZ
                            ),
                            entity
                    )
                            || canSee(
                            level,
                            cameraPosition,
                            new Vec3(
                                    visibilityBox.minX,
                                    visibilityBox.maxY,
                                    visibilityBox.maxZ
                            ),
                            entity
                    )
                            || canSee(
                            level,
                            cameraPosition,
                            new Vec3(
                                    visibilityBox.maxX,
                                    visibilityBox.minY,
                                    visibilityBox.minZ
                            ),
                            entity
                    );

            if (!visible) {
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
                                                    + config.censorBoxPadding * 2.0
                                    ),
                                    config.censorBoxSizeStep
                            )
                    );

            int boxHeight =
                    Math.max(
                            MIN_BOX_HEIGHT,
                            stepSize(
                                    (int) Math.ceil(
                                            projectedHalfHeight * 2.0
                                                    + config.censorBoxPadding * 2.0
                                    ),
                                    config.censorBoxSizeStep
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
                                    stepPosition(
                                            x,
                                            config.censorBoxPositionStep
                                    ),
                                    stepPosition(
                                            y,
                                            config.censorBoxPositionStep
                                    ),
                                    boxWidth,
                                    boxHeight,
                                    gameTick
                            )
                    );

            double deltaSeconds = 1.0;
            if (state.initialized && gameTick > state.lastSampleTick) {
                deltaSeconds = gameTick - state.lastSampleTick;
            }

            double velocityX = state.initialized
                    ? (x - state.lastRawX) / deltaSeconds
                    : 0.0;
            double velocityY = state.initialized
                    ? (y - state.lastRawY) / deltaSeconds
                    : 0.0;

            double accelerationX = state.initialized
                    ? velocityX - state.velocityX
                    : 0.0;
            double accelerationY = state.initialized
                    ? velocityY - state.velocityY
                    : 0.0;

            double speed = Math.hypot(velocityX, velocityY);
            float prediction = predictionAmount(config, speed);

            state.lastRawX = x;
            state.lastRawY = y;
            state.velocityX = velocityX;
            state.velocityY = velocityY;
            state.lastSampleTick = gameTick;

            if (!state.initialized
                    || !hasEffect(config, Config.CensorBoxEffect.STEPPY)
                    || gameTick >= state.nextUpdateTick) {
                double predictedX = x + velocityX * prediction;
                double predictedY = y + velocityY * prediction;

                if (config.censorBoxPredictionAcceleration) {
                    predictedX += accelerationX * prediction * prediction * 0.5;
                    predictedY += accelerationY * prediction * prediction * 0.5;
                }

                state.x =
                        stepPosition(
                                (int) Math.round(predictedX),
                                config.censorBoxPositionStep
                        ) + (
                                hasEffect(
                                        config,
                                        Config.CensorBoxEffect.JITTER
                                )
                                        ? randomJitter(
                                                config.censorBoxMaxJitter
                                        )
                                        : 0
                        );

                state.y =
                        stepPosition(
                                (int) Math.round(predictedY),
                                config.censorBoxPositionStep
                        ) + (
                                hasEffect(
                                        config,
                                        Config.CensorBoxEffect.JITTER
                                )
                                        ? randomJitter(
                                                config.censorBoxMaxJitter
                                        )
                                        : 0
                        );

                state.width = boxWidth;
                state.height = boxHeight;
                state.nextUpdateTick =
                        gameTick
                                + chooseUpdateTicks(
                                        config,
                                        speed
                                );
                state.initialized = true;

                DebugLogger.debug(
                        "[CensorBox] updated uuid={} screen=({}, {}) size=({}, {}) speed={} prediction={}",
                        uuid,
                        state.x,
                        state.y,
                        state.width,
                        state.height,
                        speed,
                        prediction
                );
            }

            if (hasEffect(config, Config.CensorBoxEffect.FLASH)
                    && ((gameTick / config.censorBoxFlashPeriod) & 1L) != 0L) {
                continue;
            }

            float pulse = pulseScale(config, gameTick, partialTick);
            drawCensorBox(
                    graphics,
                    state.x + state.width / 2,
                    state.y + state.height / 2,
                    Math.round(state.width * pulse),
                    Math.round(state.height * pulse),
                    censorColor(config, gameTick, partialTick),
                    config
            );
        }
    }

    private static void renderFirstPerson(
            GuiGraphics graphics,
            int screenWidth,
            int screenHeight,
            long gameTick,
            float partialTick,
            Config.Data config
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
             * Convert the actual transformed arm bounds from NDC
             * into GUI coordinates. These are the live vanilla arm
             * bounds, but they are only sampled into the censor's
             * motion state every few game ticks.
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
                            (1.0F - handState.ndcMinY)
                                    * 0.5F
                                    * screenHeight
                    );

            int liveBoxWidth =
                    Math.max(
                            MIN_BOX_WIDTH,
                            stepSize(
                                    maxX - minX + config.censorBoxPadding * 2,
                                    config.censorBoxSizeStep
                            )
                    );

            int liveBoxHeight =
                    Math.max(
                            MIN_BOX_HEIGHT,
                            stepSize(
                                    maxY - minY + config.censorBoxPadding * 2,
                                    config.censorBoxSizeStep
                            )
                    );

            int liveCenterX = (minX + maxX) / 2;
            int liveCenterY = (minY + maxY) / 2;

            float frameVelocityX =
                    handState.hasMotionState
                            ? liveCenterX - handState.previousCenterX
                            : 0.0f;
            float frameVelocityY =
                    handState.hasMotionState
                            ? liveCenterY - handState.previousCenterY
                            : 0.0f;

            handState.velocityX = frameVelocityX;
            handState.velocityY = frameVelocityY;
            handState.speed =
                    (float) Math.hypot(
                            frameVelocityX,
                            frameVelocityY
                    );
            handState.previousCenterX = liveCenterX;
            handState.previousCenterY = liveCenterY;

            /*
             * Match the world/entity censor behavior:
             *
             * - position is quantized
             * - size is quantized
             * - a small random offset is added
             * - the cached box remains frozen for 2-6 ticks
             * - the box suddenly snaps to the next sample
             *
             * The real arm animation keeps moving underneath it.
             */
            if (!handState.hasMotionState
                    || !hasEffect(config, Config.CensorBoxEffect.STEPPY)
                    || gameTick >= handState.nextUpdateTick) {
                handState.x =
                        stepPosition(
                                (int) Math.round(
                                        liveCenterX
                                                + handState.velocityX
                                                * predictionAmount(
                                                        config,
                                                        handState.speed
                                                )
                                ),
                                config.censorBoxPositionStep
                        ) + (
                                hasEffect(
                                        config,
                                        Config.CensorBoxEffect.JITTER
                                )
                                        ? randomJitter(
                                                config.censorBoxMaxJitter
                                        )
                                        : 0
                        );

                handState.y =
                        stepPosition(
                                (int) Math.round(
                                        liveCenterY
                                                + handState.velocityY
                                                * predictionAmount(
                                                        config,
                                                        handState.speed
                                                )
                                ),
                                config.censorBoxPositionStep
                        ) + (
                                hasEffect(
                                        config,
                                        Config.CensorBoxEffect.JITTER
                                )
                                        ? randomJitter(
                                                config.censorBoxMaxJitter
                                        )
                                        : 0
                        );

                handState.width = liveBoxWidth;
                handState.height = liveBoxHeight;
                handState.nextUpdateTick =
                        gameTick
                                + chooseUpdateTicks(
                                        config,
                                        handState.speed
                                );
                handState.hasMotionState = true;

                DebugLogger.debug(
                        "[CensorBox] updated first-person arm={} screen=({}, {}) size=({}, {})",
                        entry.getKey(),
                        handState.x,
                        handState.y,
                        handState.width,
                        handState.height
                );
            }

            if (hasEffect(config, Config.CensorBoxEffect.FLASH)
                    && ((gameTick / config.censorBoxFlashPeriod) & 1L) != 0L) {
                continue;
            }

            float pulse = pulseScale(config, gameTick, partialTick);
            drawCensorBox(
                    graphics,
                    handState.x,
                    handState.y,
                    Math.round(handState.width * pulse),
                    Math.round(handState.height * pulse),
                    censorColor(config, gameTick, partialTick),
                    config
            );
        }
    }

    private static int chooseUpdateTicks(
            Config.Data config,
            double movementSpeed
    ) {
        if (!hasEffect(config, Config.CensorBoxEffect.STEPPY)) {
            return 1;
        }

        int min = config.censorBoxMinUpdateTicks;
        int max = config.censorBoxMaxUpdateTicks;

        if (!config.censorBoxDynamicUpdateInterval) {
            return chooseRandomUpdateTicks(min, max);
        }

        double normalized =
                Math.clamp(
                        movementSpeed / config.censorBoxDynamicUpdateSpeed,
                        0.0,
                        1.0
                );

        int dynamicMax =
                (int) Math.round(
                        max - (max - min) * normalized
                );

        return chooseRandomUpdateTicks(
                min,
                Math.max(min, dynamicMax)
        );
    }

    private static int chooseRandomUpdateTicks(
            int min,
            int max
    ) {
        if (min >= max) {
            return min;
        }

        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private static float predictionAmount(
            Config.Data config,
            double speed
    ) {
        if (!config.censorBoxPredictionEnabled) {
            return 0.0f;
        }

        double normalized =
                Math.clamp(
                        speed / config.censorBoxDynamicUpdateSpeed,
                        0.0,
                        1.0
                );

        return config.censorBoxPredictionStrength
                * (float) (0.35 + normalized * 0.65);
    }

    private static int stepPosition(
            int value,
            int step
    ) {
        int safeStep = Math.max(1, step);

        return Math.round(
                (float) value / safeStep
        ) * safeStep;
    }

    private static int stepSize(
            int value,
            int step
    ) {
        int safeStep = Math.max(1, step);

        return Math.max(
                MIN_BOX_WIDTH,
                Math.round(
                        (float) value / safeStep
                ) * safeStep
        );
    }

    private static int randomJitter(
            int maxJitter
    ) {
        int safeMax = Math.max(0, maxJitter);

        return ThreadLocalRandom.current().nextInt(
                -safeMax,
                safeMax + 1
        );
    }

    private static boolean hasEffect(
            Config.Data config,
            Config.CensorBoxEffect effect
    ) {
        return config.censorBoxEffects != null
                && config.censorBoxEffects.contains(effect);
    }

    private static int parseColor(String value) {
        if (value == null) {
            return 0xFF000000;
        }

        String hex = value.trim();

        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        } else if (hex.startsWith("0x")
                || hex.startsWith("0X")) {
            hex = hex.substring(2);
        }

        try {
            long parsed = Long.parseLong(hex, 16);

            if (hex.length() == 6) {
                return 0xFF000000 | (int) parsed;
            }

            if (hex.length() == 8) {
                return (int) parsed;
            }
        } catch (NumberFormatException ignored) {
            // Fall back to opaque black.
        }

        return 0xFF000000;
    }

    private static int hsvToRgb(float hue) {
        float scaled = hue * 6.0F;
        int sector =
                ((int) Math.floor(scaled)) % 6;
        float fraction =
                scaled - (float) Math.floor(scaled);

        float q = 1.0F - fraction;

        return switch (sector) {
            case 0 ->
                    0x00FF0000
                            | (Math.round(fraction * 255.0F) << 8);
            case 1 ->
                    (Math.round(q * 255.0F) << 16)
                            | 0x0000FF00;
            case 2 ->
                    (Math.round(q * 255.0F) << 8)
                            | 0x000000FF;
            case 3 ->
                    0x0000FF00
                            | Math.round(fraction * 255.0F);
            case 4 ->
                    (Math.round(fraction * 255.0F) << 16)
                            | 0x000000FF;
            default ->
                    (255 << 16)
                            | Math.round(q * 255.0F);
        };
    }

    private static int censorColor(
            Config.Data config,
            long gameTick,
            float partialTick
    ) {
        int base = parseColor(config.censorBoxColor);

        if (!hasEffect(
                config,
                Config.CensorBoxEffect.RAINBOW
        )) {
            return base;
        }

        float hue =
                (gameTick + partialTick)
                        * config.censorBoxRainbowSpeed
                        * 0.01F;

        hue -= (float) Math.floor(hue);

        return (base & 0xFF000000)
                | hsvToRgb(hue);
    }

    private static float pulseScale(
            Config.Data config,
            long gameTick,
            float partialTick
    ) {
        if (!hasEffect(
                config,
                Config.CensorBoxEffect.PULSE
        )
                || config.censorBoxPulseAmount <= 0.0F) {
            return 1.0F;
        }

        return 1.0F
                + (float) Math.sin(
                (gameTick + partialTick) * 0.25F
        ) * config.censorBoxPulseAmount;
    }

    private static void drawCensorBox(
            GuiGraphics graphics,
            int centerX,
            int centerY,
            int width,
            int height,
            int color,
            Config.Data config
    ) {
        int drawWidth =
                Math.max(MIN_BOX_WIDTH, width);
        int drawHeight =
                Math.max(MIN_BOX_HEIGHT, height);
        int x =
                centerX - drawWidth / 2;
        int y =
                centerY - drawHeight / 2;

        graphics.fill(
                x,
                y,
                x + drawWidth,
                y + drawHeight,
                color
        );

        if (hasEffect(
                config,
                Config.CensorBoxEffect.DOUBLE
        )
                && config.censorBoxDoubleOffset > 0) {
            int offset =
                    config.censorBoxDoubleOffset;

            graphics.fill(
                    x - offset,
                    y + offset,
                    x - offset + drawWidth,
                    y + offset + drawHeight,
                    color
            );

            graphics.fill(
                    x + offset,
                    y - offset,
                    x + offset + drawWidth,
                    y - offset + drawHeight,
                    color
            );
        }
    }

    public static void rememberThirdPersonRenderState(
            LivingEntity entity,
            net.minecraft.client.renderer.entity.state.LivingEntityRenderState state
    ) {
        if (CENSORED_ENTITIES.contains(entity.getUUID())) {
            THIRD_PERSON_RENDER_STATES.put(
                    state,
                    entity.getUUID()
            );
        } else {
            THIRD_PERSON_RENDER_STATES.remove(state);
        }
    }

    public static void captureThirdPersonArms(
            net.minecraft.client.renderer.entity.state.LivingEntityRenderState state,
            net.minecraft.client.model.HumanoidModel<?> model,
            com.mojang.blaze3d.vertex.PoseStack poseStack
    ) {
        UUID uuid = THIRD_PERSON_RENDER_STATES.get(state);

        if (uuid == null) {
            return;
        }

        ThirdPersonArmBounds bounds =
                new ThirdPersonArmBounds();

        poseStack.pushPose();

        model.body.translateAndRotate(poseStack);

        captureArmExtents(
                model.rightArm,
                poseStack,
                bounds
        );
        captureArmExtents(
                model.leftArm,
                poseStack,
                bounds
        );

        poseStack.popPose();

        THIRD_PERSON_ARM_BOUNDS.put(uuid, bounds);
    }

    private static void captureArmExtents(
            net.minecraft.client.model.geom.ModelPart arm,
            com.mojang.blaze3d.vertex.PoseStack poseStack,
            ThirdPersonArmBounds bounds
    ) {
        if (!arm.visible || arm.skipDraw) {
            return;
        }

        arm.getExtentsForGui(
                poseStack,
                position -> bounds.points.add(
                        new Vec3(
                                position.x(),
                                position.y(),
                                position.z()
                        )
                )
        );
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

        private float ndcMinX;
        private float ndcMinY;
        private float ndcMaxX;
        private float ndcMaxY;

        /*
         * The captured arm bounds update every rendered frame,
         * but the censor box intentionally does not. This mirrors
         * the deliberately broken motion used by world entities.
         */
        private int x;
        private int y;
        private int width;
        private int height;
        private long nextUpdateTick = Long.MIN_VALUE;
        private boolean hasMotionState;

        private float previousCenterX;
        private float previousCenterY;
        private float velocityX;
        private float velocityY;
        private float speed;
    }

    private static final Map<
            net.minecraft.client.renderer.entity.state.LivingEntityRenderState,
            UUID
            > THIRD_PERSON_RENDER_STATES =
            new IdentityHashMap<>();

    private static final Map<UUID, ThirdPersonArmBounds>
            THIRD_PERSON_ARM_BOUNDS =
            new HashMap<>();

    private static final class ThirdPersonArmBounds {
        private final List<Vec3> points =
                new ArrayList<>();
    }

    private static final class CensorMotionState {
        private int x;
        private int y;
        private int width;
        private int height;

        private long nextUpdateTick;
        private boolean initialized;

        private double lastRawX;
        private double lastRawY;
        private double velocityX;
        private double velocityY;
        private long lastSampleTick = Long.MIN_VALUE;

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
