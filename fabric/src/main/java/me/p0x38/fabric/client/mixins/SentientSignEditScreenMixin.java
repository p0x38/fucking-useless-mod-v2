package me.p0x38.fabric.client.mixins;

import me.p0x38.fabric.client.BlindSpotEventManager;
import me.p0x38.fabric.client.BlindSpotSigns;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSignEditScreen.class)
public abstract class SentientSignEditScreenMixin {
    @Shadow @Final
    private String[] messages;

    @Shadow @Final
    protected SignBlockEntity sign;

    @Inject(method = "finishEditing", at = @At("HEAD"))
    private void fuckingUselessMod$captureSentientSignMessage(
            CallbackInfo callbackInfo
    ) {
        var supportPosition =
                BlindSpotSigns.getSupportPosition(
                        sign.getBlockPos()
                );

        if (supportPosition == null) {
            return;
        }

        BlindSpotEventManager.handleSignInput(
                supportPosition,
                String.join("\n", messages)
        );
    }
}
