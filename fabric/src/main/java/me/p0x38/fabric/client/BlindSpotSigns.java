package me.p0x38.fabric.client;

import me.p0x38.fuckinguselessmod.util.DebugLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates client-only vanilla sign blocks for blind-spot events.
 *
 * The signs are real SignBlockEntity instances, but they only exist
 * in the local ClientLevel and are never sent to the server.
 */
public final class BlindSpotSigns {
    private static final Map<BlockPos, BlockPos> SUPPORT_TO_SIGN =
            new HashMap<>();

    private static final Map<BlockPos, SignBlockEntity> PLACED_SIGNS =
            new HashMap<>();

    private BlindSpotSigns() {
    }

    public static void place(
            ClientLevel level,
            BlockPos supportPosition,
            String message
    ) {
        DebugLogger.debug(
                "[BlindSpotSigns] place requested support={} message={}",
                supportPosition,
                message
        );

        if (SUPPORT_TO_SIGN.containsKey(supportPosition)) {
            DebugLogger.debug(
                    "[BlindSpotSigns] sign already exists; updating support={}",
                    supportPosition
            );

            update(
                    level,
                    supportPosition,
                    message
            );
            return;
        }

        BlockPos signPosition =
                supportPosition.above();

        if (!level.getBlockState(signPosition).isAir()) {
            DebugLogger.debug(
                    "[BlindSpotSigns] placement blocked by block position={} state={}",
                    signPosition,
                    level.getBlockState(signPosition)
            );
            return;
        }

        if (level.getBlockEntity(signPosition) != null) {
            DebugLogger.debug(
                    "[BlindSpotSigns] placement blocked by existing block entity position={}",
                    signPosition
            );
            return;
        }

        int rotation =
                getPlayerFacingRotation();

        BlockState signState =
                Blocks.OAK_SIGN.defaultBlockState()
                        .setValue(
                                BlockStateProperties.ROTATION_16,
                                rotation
                        );

        level.setBlock(
                signPosition,
                signState,
                19
        );

        SignBlockEntity sign =
                new SignBlockEntity(
                        signPosition,
                        signState
                );

        level.setBlockEntity(sign);

        DebugLogger.debug(
                "[BlindSpotSigns] created sign position={} rotation={} levelBound={}",
                signPosition,
                rotation,
                sign.getLevel() != null
        );

        sign.setText(
                createText(message),
                true
        );

        SUPPORT_TO_SIGN.put(
                supportPosition.immutable(),
                signPosition.immutable()
        );

        PLACED_SIGNS.put(
                signPosition.immutable(),
                sign
        );

        DebugLogger.debug(
                "[BlindSpotSigns] sign registered support={} signPosition={} count={}",
                supportPosition,
                signPosition,
                PLACED_SIGNS.size()
        );
    }

    public static void update(
            ClientLevel level,
            BlockPos supportPosition,
            String message
    ) {
        BlockPos signPosition =
                SUPPORT_TO_SIGN.get(supportPosition);

        if (signPosition == null) {
            place(
                    level,
                    supportPosition,
                    message
            );
            return;
        }

        SignBlockEntity sign =
                PLACED_SIGNS.get(signPosition);

        /*
         * Breaking a client-only sign removes its live BlockEntity from
         * the ClientLevel. Keep the sentient state, but recreate the
         * actual BlockEntity before applying new text.
         */
        if (sign == null
                || level.getBlockEntity(signPosition) != sign
                || !level.getBlockState(signPosition).is(Blocks.OAK_SIGN)) {
            BlockState signState =
                    sign != null
                            ? sign.getBlockState()
                            : Blocks.OAK_SIGN.defaultBlockState()
                                    .setValue(
                                            BlockStateProperties.ROTATION_16,
                                            getPlayerFacingRotation()
                                    );

            if (!level.getBlockState(signPosition).is(Blocks.OAK_SIGN)) {
                level.setBlock(
                        signPosition,
                        signState,
                        19
                );
            }

            if (level.getBlockEntity(signPosition) != null) {
                level.removeBlockEntity(signPosition);
            }

            sign =
                    new SignBlockEntity(
                            signPosition,
                            level.getBlockState(signPosition)
                    );

            level.setBlockEntity(sign);

            PLACED_SIGNS.put(
                    signPosition.immutable(),
                    sign
            );

            DebugLogger.debug(
                    "[BlindSpotSigns] recreated sign block entity support={} signPosition={}",
                    supportPosition,
                    signPosition
            );
        }

        DebugLogger.debug(
                "[BlindSpotSigns] updating sign support={} signPosition={} message={}",
                supportPosition,
                signPosition,
                message
        );

        SignText text =
                createText(message);

        sign.setText(
                text,
                true
        );

        level.setBlock(
                signPosition,
                sign.getBlockState(),
                19
        );
    }

    public static boolean isPlaced(
            BlockPos supportPosition
    ) {
        return SUPPORT_TO_SIGN.containsKey(
                supportPosition
        );
    }

    public static SignBlockEntity getSignAt(
            BlockPos signPosition
    ) {
        return PLACED_SIGNS.get(signPosition);
    }

    /**
     * Checks whether the camera is aimed closely enough at the rendered
     * client-only sign to count as looking at it when the vanilla block
     * raycast misses the sign's thin outline.
     */
    public static BlockPos findLookedAtSupport(
            Vec3 cameraPosition,
            Vec3 forward,
            BlockPos preferredSupportPosition
    ) {
        BlockPos signPosition =
                SUPPORT_TO_SIGN.get(preferredSupportPosition);

        if (signPosition == null) {
            return null;
        }

        Vec3 signCenter =
                Vec3.atCenterOf(signPosition)
                        .add(0.0, 0.15, 0.0);

        Vec3 toSign =
                signCenter.subtract(cameraPosition);

        double distanceSqr =
                toSign.lengthSqr();

        if (distanceSqr <= 0.0001
                || distanceSqr > 64.0) {
            return null;
        }

        double dot =
                forward.dot(toSign.normalize());

        return dot >= 0.965
                ? preferredSupportPosition
                : null;
    }

    public static BlockPos getSupportPosition(
            BlockPos signPosition
    ) {
        for (Map.Entry<BlockPos, BlockPos> entry :
                SUPPORT_TO_SIGN.entrySet()) {
            if (entry.getValue().equals(signPosition)) {
                return entry.getKey();
            }
        }

        return null;
    }

    public static BlockPos resolveSupportPosition(
            BlockPos lookedAt
    ) {
        for (Map.Entry<BlockPos, BlockPos> entry :
                SUPPORT_TO_SIGN.entrySet()) {
            if (entry.getValue().equals(lookedAt)) {
                return entry.getKey();
            }
        }

        return lookedAt;
    }

    public static void clear(
            ClientLevel level
    ) {
        for (Map.Entry<BlockPos, SignBlockEntity> entry :
                PLACED_SIGNS.entrySet()) {
            BlockPos signPosition =
                    entry.getKey();

            SignBlockEntity sign =
                    entry.getValue();

            if (level.getBlockEntity(signPosition) == sign) {
                level.removeBlockEntity(
                        signPosition
                );

                if (level.getBlockState(signPosition)
                        .is(Blocks.OAK_SIGN)) {
                    level.setBlock(
                            signPosition,
                            Blocks.AIR.defaultBlockState(),
                            19
                    );
                }
            }
        }

        DebugLogger.debug(
                "[BlindSpotSigns] clearing {} placed signs",
                PLACED_SIGNS.size()
        );

        SUPPORT_TO_SIGN.clear();
        PLACED_SIGNS.clear();
    }

    public static Map<BlockPos, BlockPos> getSigns() {
        return Collections.unmodifiableMap(
                SUPPORT_TO_SIGN
        );
    }

    private static SignText createText(
            String message
    ) {
        String[] lines =
                message
                        .replace("\\r", "")
                        .split("\\n", -1);

        Component[] messages =
                new Component[SignText.LINES];

        for (int index = 0;
             index < messages.length;
             index++) {
            messages[index] =
                    index < lines.length
                            ? Component.literal(lines[index])
                            : Component.empty();
        }

        return new SignText(
                messages,
                messages.clone(),
                DyeColor.RED,
                true
        );
    }

    private static int getPlayerFacingRotation() {
        var player =
                Minecraft.getInstance().player;

        if (player == null) {
            return 0;
        }

        return Math.floorMod(
                (int) Math.floor(
                        (player.getYRot() + 180.0F)
                                * 16.0F
                                / 360.0F
                                + 0.5D
                ),
                16
        );
    }
}
