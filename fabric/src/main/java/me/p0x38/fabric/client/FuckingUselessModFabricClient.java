package me.p0x38.fabric.client;

import me.p0x38.fuckinguselessmod.FuckingUselessMod;
import me.p0x38.fuckinguselessmod.util.DebugLogger;
import me.p0x38.fuckinguselessmod.entity.ModEntities;
import me.p0x38.fuckinguselessmod.transformers.ChatTransformer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.renderer.entity.EntityRenderers;

public final class FuckingUselessModFabricClient implements ClientModInitializer {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitializeClient() {
        FuckingUselessMod.init();

        EntityRenderers.register(ModEntities.USELESS_ENTITY, UselessEntityRenderer::new);

        ClientSendMessageEvents.MODIFY_CHAT.register(
                ChatTransformer::transform
        );

        ClientReceiveMessageEvents.CHAT.register(
                (message, signedMessage, sender, params, receptionTimestamp) -> {
                    LOGGER.info(
                            "[DialogueDebug] CHAT callback sender={} uuid={} text={} signed={} timestamp={}",
                            sender != null ? sender.name() : "<null>",
                            sender != null ? sender.id() : "<null>",
                            message.getString(),
                            signedMessage != null,
                            receptionTimestamp
                    );

                    if (sender != null) {
                        DialogueSoundManager.queue(
                                sender,
                                message.getString(),
                                receptionTimestamp,
                                signedMessage
                        );
                    }
                }
        );

        ClientTickEvents.END_CLIENT_TICK.register(
                client -> DialogueSoundManager.tick()
        );
    }
}
