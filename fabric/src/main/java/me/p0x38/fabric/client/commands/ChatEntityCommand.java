package me.p0x38.fabric.client.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import me.p0x38.fabric.client.ChatEntityManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;

public final class ChatEntityCommand {
    private ChatEntityCommand() {}

    public static void initialize() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("chatentity")
                        .then(ClientCommandManager.literal("say")
                                .then(ClientCommandManager.argument("message", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            boolean ok = ChatEntityManager.talkToChatEntity(StringArgumentType.getString(context, "message"));
                                            if (ok) context.getSource().sendFeedback(Component.literal("The chat entity heard you."));
                                            else context.getSource().sendError(Component.literal("No active chat entity is available."));
                                            return ok ? 1 : 0;
                                        })))
                        .then(ClientCommandManager.literal("id")
                                .executes(context -> {
                                    String id = ChatEntityManager.getObservedChatEntityId();
                                    if (id == null) { context.getSource().sendError(Component.literal("No active chat entity is available.")); return 0; }
                                    context.getSource().sendFeedback(Component.literal("Chat Entity ID: " + id));
                                    return 1;
                                }))
                        .then(ClientCommandManager.literal("info")
                                .executes(context -> {
                                    String info = ChatEntityManager.getObservedChatEntityInfo();
                                    if (info == null) { context.getSource().sendError(Component.literal("No active chat entity is available.")); return 0; }
                                    context.getSource().sendFeedback(Component.literal(info));
                                    return 1;
                                }))));
    }
}
