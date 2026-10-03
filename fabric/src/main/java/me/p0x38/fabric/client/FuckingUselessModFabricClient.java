package me.p0x38.fabric.client;

import me.p0x38.fabric.client.commands.CensorBoxCommand;
import me.p0x38.fabric.client.renderers.UnknownEntityRenderer;
import me.p0x38.fabric.client.unknownentity.UnknownEntityTriggerRegistry;
import me.p0x38.fabric.client.commands.ChatEntityCommand;
import me.p0x38.fabric.client.renderers.CensorBoxRenderer;
import me.p0x38.fuckinguselessmod.FuckingUselessMod;
import me.p0x38.fuckinguselessmod.detectors.KonamiCodeDetector;
import me.p0x38.fuckinguselessmod.util.DebugLogger;
import me.p0x38.fuckinguselessmod.entity.ModEntities;
import me.p0x38.fuckinguselessmod.transformers.ChatTransformer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.packs.PackType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.renderer.entity.EntityRenderers;

public final class FuckingUselessModFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FuckingUselessMod.init();

        ResourceLoader.get(
                PackType.CLIENT_RESOURCES
        ).registerReloader(
                UnknownEntityTriggerRegistry.RESOURCE_ID,
                UnknownEntityTriggerRegistry.INSTANCE
        );

        EntityRenderers.register(ModEntities.CHAT_ENTITY, UnknownEntityRenderer::new);

        CensorBoxRenderer.initialize();
        CensorBoxCommand.initialize();
        ChatEntityCommand.initialize();

        ClientSendMessageEvents.MODIFY_CHAT.register(
                ChatTransformer::transform
        );

        ClientSendMessageEvents.ALLOW_CHAT.register(
                message -> {
                    boolean handled =
                            UnknownEntityManager.talkToChatEntity(
                                    message
                            );

                    DebugLogger.debug(
                            "[BlindSpot] outgoing chat intercepted={} message={}",
                            handled,
                            message
                    );

                    return !handled;
                }
        );

        ClientReceiveMessageEvents.CHAT.register(
                (message, signedMessage, sender, params, receptionTimestamp) -> {
                    DebugLogger.debug(
                            "[Chat] CHAT callback sender={} uuid={} text={} signed={} timestamp={}",
                            sender != null ? sender.name() : "<null>",
                            sender != null ? sender.id() : "<null>",
                            message.getString(),
                            signedMessage != null,
                            receptionTimestamp
                    );

                    if (sender != null && signedMessage != null) {
                        DialogueSoundManager.queue(
                                sender,
                                signedMessage.signedContent(),
                                receptionTimestamp,
                                signedMessage
                        );
                    }
                }
        );

        ClientTickEvents.END_CLIENT_TICK.register(
                client -> {
                    CensorBoxCommand.applyConfiguredDefaults(client);
                    DialogueSoundManager.tick();
                }
        );

        ClientTickEvents.END_CLIENT_TICK.register(
                client -> {
                    UnknownEntityManager.tick();

                    if (KonamiCodeDetector.tick(client)) {
                        UnknownEntityManager.onSecretMode();
                    }
                }
        );
    }
}
