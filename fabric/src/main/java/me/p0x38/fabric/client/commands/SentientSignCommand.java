package me.p0x38.fabric.client.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import me.p0x38.fabric.client.BlindSpotEventManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

public final class SentientSignCommand {
    private SentientSignCommand() {}

    public static void initialize() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("sign")
                        .then(ClientCommandManager.literal("say")
                                .then(ClientCommandManager.argument("message", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            boolean ok = BlindSpotEventManager.talkToObservedSign(StringArgumentType.getString(context, "message"));
                                            if (ok) context.getSource().sendFeedback(Component.literal("The sign heard you."));
                                            else context.getSource().sendError(Component.literal("No sentient sign is being watched."));
                                            return ok ? 1 : 0;
                                        })))
                        .then(ClientCommandManager.literal("id")
                                .executes(context -> {
                                    String id = BlindSpotEventManager.getObservedSignId();
                                    if (id == null) { context.getSource().sendError(Component.literal("No sentient sign is being watched.")); return 0; }
                                    context.getSource().sendFeedback(Component.literal("Sign ID: " + id));
                                    return 1;
                                }))
                        .then(ClientCommandManager.literal("info")
                                .executes(context -> {
                                    String info = BlindSpotEventManager.getObservedSignInfo();
                                    if (info == null) { context.getSource().sendError(Component.literal("No sentient sign is being watched.")); return 0; }
                                    context.getSource().sendFeedback(Component.literal(info));
                                    return 1;
                                }))));
    }
}
