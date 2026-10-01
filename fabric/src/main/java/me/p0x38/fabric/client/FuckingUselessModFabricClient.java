package me.p0x38.fabric.client;

import me.p0x38.fuckinguselessmod.transformers.ChatTransformer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;

public final class FuckingUselessModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientSendMessageEvents.MODIFY_CHAT.register(
                ChatTransformer::transform
        );
    }
}
