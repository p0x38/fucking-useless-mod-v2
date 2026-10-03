package me.p0x38.fabric.client;

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
        if (SUPPORT_TO_SIGN.containsKey(supportPosition)) {
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
            return;
        }

        if (level.getBlockEntity(signPosition) != null) {
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

        sign.setText(
                createText(message),
                true
        );

        level.addBlockEntity(sign);

        SUPPORT_TO_SIGN.put(
                supportPosition.immutable(),
                signPosition.immutable()
        );

        PLACED_SIGNS.put(
                signPosition.immutable(),
                sign
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

        if (sign == null) {
            return;
        }

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

        SignText text =
                new SignText();

        for (int index = 0;
             index < Math.min(4, lines.length);
             index++) {
            text = text.withMessage(
                    index,
                    Component.literal(
                            lines[index]
                    )
            );
        }

        return text.withColor(
                DyeColor.BLACK
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
