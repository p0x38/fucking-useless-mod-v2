package me.p0x38.fabric.client.mixins;

import me.p0x38.fabric.client.renderers.CensorBoxRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Accessor("model")
    protected abstract EntityModel<?> fuckingUselessMod$getModel();

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void fuckingUselessMod$rememberRenderState(
            LivingEntity entity,
            LivingEntityRenderState state,
            float partialTick,
            CallbackInfo ci
    ) {
        CensorBoxRenderer.rememberThirdPersonRenderState(entity, state);
    }

    @Inject(
            method = "submit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void fuckingUselessMod$captureArms(
            LivingEntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera,
            CallbackInfo ci
    ) {
        EntityModel<?> model = fuckingUselessMod$getModel();

        if (model instanceof HumanoidModel<?> humanoidModel) {
            CensorBoxRenderer.captureThirdPersonArms(
                    state,
                    humanoidModel,
                    poseStack
            );
        }
    }
}
