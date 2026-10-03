package me.p0x38.fabric.client;

import me.p0x38.fabric.client.blindspot.Memory;
import me.p0x38.fabric.client.blindspot.SentientSign;
import me.p0x38.fabric.client.blindspot.SentientSignBrain;
import me.p0x38.fabric.client.blindspot.SignConnectionMode;
import me.p0x38.fuckinguselessmod.util.DebugLogger;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class BlindSpotEventManager {
    private static final int MIN_HIDDEN_TICKS = 10;
    private static final int MAX_HIDDEN_TICKS = 60;

    private static final double LOOK_DOT_THRESHOLD = 0.75;
    private static final double LOOK_DISTANCE = 6.0;

    private static final Map<BlockPos, SentientSign> SIGNS =
            new HashMap<>();

    /*
     * This is the block currently being treated as the persistent
     * sign target. Looking at another block makes this target hidden
     * instead of replacing it.
     */
    private static ClientLevel observedLevel;
    private static BlockPos observedBlock;

    private static long hiddenSince = Long.MIN_VALUE;
    private static boolean changedWhileHidden;

    private BlindSpotEventManager() {
    }

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        Player player = client.player;

        if (level == null || player == null) {
            reset();
            return;
        }

        if (observedLevel != null && observedLevel != level) {
            DebugLogger.debug(
                    "[BlindSpot] client world changed; clearing previous state oldLevel={} newLevel={}",
                    observedLevel.dimension(),
                    level.dimension()
            );

            BlindSpotSigns.clear(observedLevel);
            SIGNS.clear();
            observedBlock = null;
            hiddenSince = Long.MIN_VALUE;
            changedWhileHidden = false;
        }

        observedLevel = level;

        Camera camera =
                client.gameRenderer.getMainCamera();

        BlockPos lookedAtBlock =
                resolveLookedAtSupport(
                        level,
                        player,
                        camera
                );

        /*
         * Start observing the first block the player looks at.
         * The selected target remains fixed until the client world
         * is reset.
         */
        if (observedBlock == null) {
            if (lookedAtBlock == null) {
                return;
            }

            observedBlock = lookedAtBlock;
            hiddenSince = Long.MIN_VALUE;
            changedWhileHidden = false;

            DebugLogger.debug(
                    "[BlindSpot] observing new target block={}",
                    observedBlock
            );
        }

        SentientSign sign =
                SIGNS.computeIfAbsent(
                        observedBlock,
                        position -> {
                            DebugLogger.debug(
                                    "[BlindSpot] created sentient sign position={} id={}",
                                    position,
                                    "sign-" + Long.toUnsignedString(position.asLong(), 36)
                            );
                            return new SentientSign(position);
                        }
                );

        boolean lookingAtObservedBlock =
                observedBlock.equals(lookedAtBlock);

        long gameTick =
                level.getGameTime();

        sign.observe(
                lookingAtObservedBlock,
                gameTick,
                SignConnectionMode.detect(client),
                player.getGameProfile().name()
        );

        /*
         * Give the sign's brain the latest perception event.
         */
        thinkAndSync(
                sign,
                level
        );

        if (lookingAtObservedBlock) {
            hiddenSince = Long.MIN_VALUE;

            if (changedWhileHidden) {
                changedWhileHidden = false;

                onReveal(
                        sign,
                        level
                );
            }

            return;
        }

        /*
         * The player is no longer looking at the observed sign.
         */
        if (hiddenSince == Long.MIN_VALUE) {
            hiddenSince = gameTick;

            DebugLogger.debug(
                    "[BlindSpot] target became hidden position={} tick={} message={}",
                    sign.position(),
                    gameTick,
                    sign.currentMessage()
            );
            return;
        }

        long hiddenTicks =
                gameTick - hiddenSince;

        if (hiddenTicks < MIN_HIDDEN_TICKS
                || hiddenTicks > MAX_HIDDEN_TICKS
                || changedWhileHidden) {
            return;
        }

        /*
         * Small random change each tick prevents the event from
         * feeling synchronized to a predictable fixed delay.
         */
        if (ThreadLocalRandom.current().nextDouble() >= 0.03) {
            return;
        }

        changedWhileHidden = true;

        DebugLogger.debug(
                "[BlindSpot] hidden world-change triggered position={} hiddenTicks={} tick={}",
                sign.position(),
                hiddenTicks,
                gameTick
        );

        onHiddenChange(
                sign,
                level
        );
    }

    public static boolean talkToObservedSign(
            String message
    ) {
        Minecraft client = Minecraft.getInstance();

        if (client.level == null
                || client.player == null
                || observedLevel != client.level
                || observedBlock == null
                || message == null
                || message.isBlank()) {
            return false;
        }

        BlockPos lookedAtBlock =
                resolveLookedAtSupport(
                        client.level,
                        client.player,
                        client.gameRenderer.getMainCamera()
                );

        if (!observedBlock.equals(lookedAtBlock)) {
            DebugLogger.debug(
                    "[BlindSpot] chat ignored: player is not looking at observed sign target observed={} lookedAt={}",
                    observedBlock,
                    lookedAtBlock
            );
            return false;
        }

        SentientSign sign =
                SIGNS.computeIfAbsent(
                        observedBlock,
                        SentientSign::new
                );

        long gameTick =
                client.level.getGameTime();

        SignConnectionMode connectionMode =
                SignConnectionMode.detect(client);

        DebugLogger.debug(
                "[BlindSpot] player communicated with sign={} tick={} connection={} message={}",
                sign.id(),
                gameTick,
                connectionMode,
                message.trim()
        );

        sign.interact(
                message,
                gameTick,
                connectionMode,
                client.player.getGameProfile().name()
        );

        thinkAndSync(
                sign,
                client.level
        );

        if (BlindSpotSigns.isPlaced(sign.position())) {
            BlindSpotSigns.update(
                    client.level,
                    sign.position(),
                    sign.currentMessage()
            );
        }

        return true;
    }

    public static String getObservedSignId() {
        SentientSign sign =
                getObservedSign();

        return sign == null
                ? null
                : sign.id();
    }

    public static String getObservedSignInfo() {
        SentientSign sign =
                getObservedSign();

        if (sign == null) {
            return null;
        }

        return "Sign ID: "
                + sign.id()
                + " | mode: "
                + sign.connectionMode().name().toLowerCase()
                + " | mood: "
                + sign.mood().name().toLowerCase()
                + " | awareness: "
                + sign.awareness()
                + " | suspicion: "
                + sign.suspicion()
                + " | curiosity: "
                + sign.curiosity()
                + " | trust: "
                + sign.trust()
                + " | irritation: "
                + sign.irritation()
                + " | memories: "
                + sign.memories().size();
    }

    private static SentientSign getObservedSign() {
        if (observedLevel == null
                || observedBlock == null) {
            return null;
        }

        return SIGNS.get(observedBlock);
    }

    public static void handleSignInput(
            BlockPos supportPosition,
            String message
    ) {
        Minecraft client = Minecraft.getInstance();

        if (client.level == null
                || client.player == null
                || message == null
                || message.isBlank()) {
            return;
        }

        SentientSign sign =
                SIGNS.computeIfAbsent(
                        supportPosition,
                        SentientSign::new
                );

        long gameTick =
                client.level.getGameTime();

        sign.interact(
                message,
                gameTick,
                SignConnectionMode.detect(client),
                client.player.getGameProfile().name()
        );

        thinkAndSync(
                sign,
                client.level
        );

        if (BlindSpotSigns.isPlaced(sign.position())) {
            BlindSpotSigns.update(
                    client.level,
                    sign.position(),
                    sign.currentMessage()
            );
        }
    }

    private static BlockPos resolveLookedAtSupport(
            ClientLevel level,
            Player player,
            Camera camera
    ) {
        BlockPos lookedAtBlock =
                getLookedAtBlock(
                        level,
                        player,
                        camera
                );

        if (lookedAtBlock != null) {
            lookedAtBlock =
                    BlindSpotSigns.resolveSupportPosition(
                            lookedAtBlock
                    );
        }

        if (observedBlock != null) {
            BlockPos signSupport =
                    BlindSpotSigns.findLookedAtSupport(
                            camera.position(),
                            camera.forwardVector(),
                            observedBlock
                    );

            if (signSupport != null) {
                DebugLogger.debug(
                        "[BlindSpot] sign gaze fallback observed={} raycast={}",
                        observedBlock,
                        lookedAtBlock
                );
                return signSupport;
            }
        }

        return lookedAtBlock;
    }

    private static BlockPos getLookedAtBlock(
            ClientLevel level,
            Player player,
            Camera camera
    ) {
        Vec3 cameraPosition =
                camera.position();

        var cameraForward =
                camera.forwardVector();

        Vec3 forward =
                new Vec3(
                        cameraForward.x(),
                        cameraForward.y(),
                        cameraForward.z()
                );

        Vec3 targetPosition =
                cameraPosition.add(
                        forward.scale(LOOK_DISTANCE)
                );

        BlockHitResult hit =
                level.clip(
                        new ClipContext(
                                cameraPosition,
                                targetPosition,
                                ClipContext.Block.OUTLINE,
                                ClipContext.Fluid.NONE,
                                player
                        )
                );

        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        BlockPos blockPos =
                hit.getBlockPos();

        Vec3 blockCenter =
                Vec3.atCenterOf(blockPos);

        Vec3 toBlock =
                blockCenter.subtract(
                        cameraPosition
                );

        if (toBlock.lengthSqr() <= 0.0001) {
            return null;
        }

        toBlock = toBlock.normalize();

        double dot =
                forward.dot(toBlock);

        if (dot < LOOK_DOT_THRESHOLD) {
            return null;
        }

        return blockPos;
    }

    private static void thinkAndSync(
            SentientSign sign,
            ClientLevel level
    ) {
        String before =
                sign.currentMessage();

        SentientSignBrain.think(
                sign,
                level
        );

        /*
         * The brain can act later, after its cooldown expires. Keep the
         * actual client-side sign synchronized whenever that happens.
         */
        if (BlindSpotSigns.isPlaced(sign.position())
                && !java.util.Objects.equals(
                        before,
                        sign.currentMessage()
                )) {
            DebugLogger.debug(
                    "[BlindSpot] syncing brain message to rendered sign id={} old={} new={}",
                    sign.id(),
                    before,
                    sign.currentMessage()
            );

            BlindSpotSigns.update(
                    level,
                    sign.position(),
                    sign.currentMessage()
            );
        }
    }

    private static void onHiddenChange(
            SentientSign sign,
            ClientLevel level
    ) {
        sign.remember(
                new Memory(
                        Memory.Type.WORLD_CHANGED,
                        level.getGameTime(),
                        0.85f,
                        sign.position(),
                        Map.of(
                                "phase",
                                "hidden",
                                "block",
                                level.getBlockState(
                                        sign.position()
                                ).toString()
                        )
                )
        );

        DebugLogger.debug(
                "[BlindSpot] placing sign after hidden change position={} message={} state={}",
                sign.position(),
                sign.currentMessage(),
                level.getBlockState(sign.position())
        );

        BlindSpotSigns.place(
                level,
                sign.position(),
                sign.currentMessage()
        );

        thinkAndSync(
                sign,
                level
        );
    }

    private static void onReveal(
            SentientSign sign,
            ClientLevel level
    ) {
        DebugLogger.debug(
                "[BlindSpot] player returned to sign position={} message={}",
                sign.position(),
                sign.currentMessage()
        );

        BlindSpotSigns.update(
                level,
                sign.position(),
                sign.currentMessage()
        );

        sign.remember(
                new Memory(
                        Memory.Type.WORLD_CHANGED,
                        level.getGameTime(),
                        0.95f,
                        sign.position(),
                        Map.of(
                                "phase",
                                "revealed"
                        )
                )
        );
    }

    private static void reset() {
        Minecraft client = Minecraft.getInstance();

        if (client.level != null) {
            BlindSpotSigns.clear(client.level);
        }

        SIGNS.clear();
        observedLevel = null;
        observedBlock = null;
        hiddenSince = Long.MIN_VALUE;
        changedWhileHidden = false;
    }
}
