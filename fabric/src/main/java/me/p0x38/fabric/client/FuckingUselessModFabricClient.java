package me.p0x38.fabric.client;

import me.p0x38.fuckinguselessmod.FuckingUselessMod;
import me.p0x38.fuckinguselessmod.entity.ModEntities;
import me.p0x38.fuckinguselessmod.transformers.ChatTransformer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.renderer.entity.EntityRenderers;

public final class FuckingUselessModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FuckingUselessMod.init();

        EntityRenderers.register(ModEntities.USELESS_ENTITY, UselessEntityRenderer::new);

        ClientSendMessageEvents.MODIFY_CHAT.register(
                ChatTransformer::transform
        );
    }
}
