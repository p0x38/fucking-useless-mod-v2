package me.p0x38.fabric.client.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import me.p0x38.fabric.client.renderers.CensorBoxRenderer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.HumanoidArm;
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
            method = "renderPlayerArm",
            at = @At("HEAD")
    )
    private void fuckingUselessMod$trackPlayerArm(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int packedLight,
            float equipProgress,
            float swingProgress,
            HumanoidArm arm,
            CallbackInfo callbackInfo
    ) {
        CensorBoxRenderer.markFirstPersonHand(
                arm,
                swingProgress
        );
    }
}
