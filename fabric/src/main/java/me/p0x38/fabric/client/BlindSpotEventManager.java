package me.p0x38.fabric.client;

import me.p0x38.fabric.client.blindspot.Memory;
import me.p0x38.fabric.client.blindspot.SentientSign;
import me.p0x38.fabric.client.blindspot.SentientSignBrain;
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
                getLookedAtBlock(
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
        }

        SentientSign sign =
                SIGNS.computeIfAbsent(
                        observedBlock,
                        SentientSign::new
                );

        boolean lookingAtObservedBlock =
                observedBlock.equals(lookedAtBlock);

        long gameTick =
                level.getGameTime();

        sign.observe(
                lookingAtObservedBlock,
                gameTick
        );

        /*
         * Give the sign's brain the latest perception event.
         */
        SentientSignBrain.think(
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

        onHiddenChange(
                sign,
                level
        );
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

        BlindSpotSigns.place(
                level,
                sign.position(),
                sign.currentMessage()
        );
    }

    private static void onReveal(
            SentientSign sign,
            ClientLevel level
    ) {
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
