package me.p0x38.fabric.client.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import me.p0x38.fabric.client.renderers.CensorBoxRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Inject(method = "renderHandsWithItems", at = @At("HEAD"))
    private void fuckingUselessMod$beginHandTracking(
            float f,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            LocalPlayer localPlayer,
            int i,
            CallbackInfo callbackInfo
    ) {
        CensorBoxRenderer.beginFirstPersonHandTracking();
    }

    @Inject(
            method = "renderArmWithItem",
            at = @At("HEAD")
    )
    private void fuckingUselessMod$trackRenderedHand(
            AbstractClientPlayer player,
            float partialTick,
            float pitch,
            InteractionHand hand,
            float attack,
            ItemStack itemStack,
            float equipProgress,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int packedLight,
            CallbackInfo callbackInfo
    ) {
        if (player.isScoping()) {
            return;
        }

        boolean mainHand =
                hand == InteractionHand.MAIN_HAND;

        HumanoidArm arm =
                mainHand
                ? player.getMainArm()
                        : player.getMainArm().getOpposite();

        if (itemStack.isEmpty()) {
            if (mainHand && !player.isInvisible()) {
                CensorBoxRenderer.markFirstPersonHand(arm);
            }

            return;
        }

        /*
         * A main-hand map with an empty offhand renders both
         * player arms as part of the two-handed map animation.
         */
        if (itemStack.has(
                DataComponents.MAP_ID
        ) && mainHand && player.getOffhandItem().isEmpty()) {
            CensorBoxRenderer.markFirstPersonHand(HumanoidArm.RIGHT);
            CensorBoxRenderer.markFirstPersonHand(HumanoidArm.LEFT);
            return;
        }

        CensorBoxRenderer.markFirstPersonHand(arm);
    }
}
