package me.p0x38.fabric.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import me.p0x38.fabric.client.commands.CensorBoxCommand;
import me.p0x38.fabric.client.commands.ChatEntityCommand;
import me.p0x38.fabric.client.renderers.CensorBoxRenderer;
import me.p0x38.fuckinguselessmod.FuckingUselessMod;
import me.p0x38.fuckinguselessmod.util.DebugLogger;
import me.p0x38.fuckinguselessmod.entity.ModEntities;
import me.p0x38.fuckinguselessmod.transformers.ChatTransformer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

public final class FuckingUselessModFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FuckingUselessMod.init();

        EntityRenderers.register(ModEntities.USELESS_ENTITY, UselessEntityRenderer::new);

        CensorBoxRenderer.initialize();
        CensorBoxCommand.initialize();
        ChatEntityCommand.initialize();

        ClientSendMessageEvents.MODIFY_CHAT.register(
                ChatTransformer::transform
        );

        ClientSendMessageEvents.ALLOW_CHAT.register(
                message -> {
                    boolean handled =
                            ChatEntityManager.talkToChatEntity(
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
                client -> ChatEntityManager.tick()
        );
    }
}
