package me.p0x38.fabric.client.mixins;

import me.p0x38.fabric.client.renderers.CensorBoxRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Inject(
            method = "extractRenderState",
            at = @At("TAIL")
    )
    private void fuckingUselessMod$storeEntityUuid(
            Entity entity,
            EntityRenderState state,
            float tickProgress,
            CallbackInfo callbackInfo
    ) {
        ((FabricRenderState) state).setData(
                CensorBoxRenderer.ENTITY_UUID,
                entity.getUUID()
        );
    }
}
