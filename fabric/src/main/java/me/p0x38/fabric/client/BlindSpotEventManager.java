package me.p0x38.fabric.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

import java.util.concurrent.ThreadLocalRandom;

public final class BlindSpotEventManager {
    private static final int MIN_HIDDEN_TICKS = 10;
    private static final int MAX_HIDDEN_TICKS = 60;

    private static final double LOOK_DOT_THRESHOLD = 0.75;
    private static final double LOOK_DISTANCE = 6.0;

    private static BlockPos observedBlock;
    private static long hiddenSince = Long.MIN_VALUE;
    private static boolean changedWhileHidden;

    private BlindSpotEventManager() {}

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        Player player = client.player;

        if (level == null || player == null) {
            reset();
            return;
        }

        Camera camera = client.gameRenderer.getMainCamera();

        BlockPos target = getLookedAtBlock(level, player, camera);

        /*
         * The player is currently looking at a different block.
         *
         * Keep the currently observed target only when there is
         * actually a target.
         */
        if (target != null) {
            boolean returnedToObservedBlock =
                    observedBlock != null
                        && observedBlock.equals(target);

            observedBlock = target;
            hiddenSince = Long.MIN_VALUE;

            /*
             * We have looked back at it.
             *
             * The world/effect was changed while the player was
             * looking away, so this is the reveal moment.
             */
            if (changedWhileHidden && returnedToObservedBlock) {
                changedWhileHidden = false;

                onReveal(level, target);
            }

            return;
        }

        /*
         * No block is currently being looked at.
         *
         * This means the player has looked away from the previously
         * observed location.
         */
        if (observedBlock == null) {
            return;
        }

        if (hiddenSince == Long.MIN_VALUE) {
            hiddenSince = level.getGameTime();
            return;
        }

        long hiddenTicks =
                level.getGameTime() - hiddenSince;

        if (hiddenTicks < MIN_HIDDEN_TICKS
        || hiddenTicks > MAX_HIDDEN_TICKS
        || changedWhileHidden) {
            return;
        }

        /*
         * Small random change each tick.
         *
         * This makes the effect unpredictable instead of happening
         * at exactly the same delay every time.
         */
        if (ThreadLocalRandom.current().nextDouble() >= 0.03) {
            return;
        }

        changedWhileHidden = true;

        onHiddenChange(level, observedBlock);
    }

    private static BlockPos getLookedAtBlock(
            ClientLevel level,
            Player player,
            Camera camera
    ) {
        Vec3 cameraPosition = camera.position();

        var cameraForward = camera.forwardVector();

        Vec3 forward =
                new Vec3(
                        cameraForward.x(),
                        cameraForward.y(),
                        cameraForward.z()
                );

        /*
         * Check a point several blocks in front of the camera.
         */
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

        BlockPos blockPos = hit.getBlockPos();

        Vec3 blockCenter = Vec3.atCenterOf(blockPos);
        Vec3 toBlock = blockCenter.subtract(cameraPosition);

        if (toBlock.lengthSqr() <= 0.0001) {
            return null;
        }

        toBlock = toBlock.normalize();

        /*
         * Dot product tells us how directly the camera is facing
         * the block.
         */
        double dot =
                forward.dot(toBlock);

        if (dot < LOOK_DOT_THRESHOLD) {
            return null;
        }

        return blockPos;
    }

    private static void onHiddenChange(
            ClientLevel level,
            BlockPos blockPos
    ) {
        /*
         * THIS is the point where the spooky change happens.
         *
         * Example:
         *
         * replaceWithSign(level, blockPos);
         */
    }

    private static void onReveal(
            ClientLevel level,
            BlockPos blockPos
    ) {
        /*
         * Optional reveal effect.
         *
         * You could play a sound, spawn particles, log debug info,
         * etc.
         */
    }

    private static void reset() {
        observedBlock = null;
        hiddenSince = Long.MIN_VALUE;
        changedWhileHidden = false;
    }
}
