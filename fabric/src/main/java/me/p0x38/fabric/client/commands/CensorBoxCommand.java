package me.p0x38.fabric.client.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.p0x38.fabric.client.renderers.CensorBoxRenderer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class CensorBoxCommand {
    private CensorBoxCommand() {
    }

    public static void initialize() {
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) ->
                        dispatcher.register(
                                ClientCommandManager.literal("censorbox")
                                        .then(
                                                ClientCommandManager.literal("add")
                                                        .then(
                                                                ClientCommandManager.argument(
                                                                                "selector",
                                                                                StringArgumentType.greedyString()
                                                                        )
                                                                        .executes(
                                                                                context ->
                                                                                        add(
                                                                                                context,
                                                                                                StringArgumentType.getString(
                                                                                                        context,
                                                                                                        "selector"
                                                                                                )
                                                                                        )
                                                                        )
                                                        )
                                        )
                                        .then(
                                                ClientCommandManager.literal("remove")
                                                        .then(
                                                                ClientCommandManager.argument(
                                                                                "selector",
                                                                                StringArgumentType.greedyString()
                                                                        )
                                                                        .executes(
                                                                                context ->
                                                                                        remove(
                                                                                                context,
                                                                                                StringArgumentType.getString(
                                                                                                        context,
                                                                                                        "selector"
                                                                                                )
                                                                                        )
                                                                        )
                                                        )
                                        )
                                        .then(
                                                ClientCommandManager.literal("toggle")
                                                        .executes(
                                                                context ->
                                                                        toggle(
                                                                                context,
                                                                                "@s"
                                                                        )
                                                        )
                                                        .then(
                                                                ClientCommandManager.argument(
                                                                                "selector",
                                                                                StringArgumentType.greedyString()
                                                                        )
                                                                        .executes(
                                                                                context ->
                                                                                        toggle(
                                                                                                context,
                                                                                                StringArgumentType.getString(
                                                                                                        context,
                                                                                                        "selector"
                                                                                                )
                                                                                        )
                                                                        )
                                                        )
                                        )
                                        .then(
                                                ClientCommandManager.literal("list")
                                                        .executes(
                                                                CensorBoxCommand::list
                                                        )
                                        )
                                        .then(
                                                ClientCommandManager.literal("clear")
                                                        .executes(
                                                                CensorBoxCommand::clear
                                                        )
                                        )
                        )
        );
    }

    private static int add(
            CommandContext<FabricClientCommandSource> context,
            String selector
    ) {
        Minecraft client =
                Minecraft.getInstance();

        List<Entity> targets =
                resolve(client, selector);

        if (targets.isEmpty()) {
            context.getSource().sendError(
                    Component.literal(
                            "No entities matched: " + selector
                    )
            );

            return 0;
        }

        int added = 0;

        for (Entity entity : targets) {
            UUID uuid = entity.getUUID();

            if (!CensorBoxRenderer.isCensored(uuid)) {
                CensorBoxRenderer.add(uuid);
                added++;
            }
        }

        context.getSource().sendFeedback(
                Component.literal(
                        "Added "
                                + added
                                + " censor box"
                                + (added == 1 ? "." : "es.")
                )
        );

        return added;
    }

    private static int remove(
            CommandContext<FabricClientCommandSource> context,
            String selector
    ) {
        Minecraft client =
                Minecraft.getInstance();

        List<Entity> targets =
                resolve(client, selector);

        if (targets.isEmpty()) {
            context.getSource().sendError(
                    Component.literal(
                            "No entities matched: " + selector
                    )
            );

            return 0;
        }

        int removed = 0;

        for (Entity entity : targets) {
            if (CensorBoxRenderer.remove(
                    entity.getUUID()
            )) {
                removed++;
            }
        }

        context.getSource().sendFeedback(
                Component.literal(
                        "Removed "
                                + removed
                                + " censor box"
                                + (removed == 1 ? "." : "es.")
                )
        );

        return removed;
    }

    private static int toggle(
            CommandContext<FabricClientCommandSource> context,
            String selector
    ) {
        Minecraft client =
                Minecraft.getInstance();

        List<Entity> targets =
                resolve(client, selector);

        if (targets.isEmpty()) {
            context.getSource().sendError(
                    Component.literal(
                            "No entities matched: " + selector
                    )
            );

            return 0;
        }

        int changed = 0;

        for (Entity entity : targets) {
            CensorBoxRenderer.toggle(
                    entity.getUUID()
            );

            changed++;
        }

        context.getSource().sendFeedback(
                Component.literal(
                        "Toggled "
                                + changed
                                + " censor box"
                                + (changed == 1 ? "." : "es.")
                )
        );

        return changed;
    }

    private static int list(
            CommandContext<FabricClientCommandSource> context
    ) {
        Minecraft client =
                Minecraft.getInstance();

        var entities =
                CensorBoxRenderer.getCensoredEntities();

        if (entities.isEmpty()) {
            context.getSource().sendFeedback(
                    Component.literal(
                            "No censor boxes are active."
                    )
            );

            return 0;
        }

        context.getSource().sendFeedback(
                Component.literal(
                        "Active censor boxes ("
                                + entities.size()
                                + "):"
                )
        );

        for (UUID uuid : entities) {
            Entity entity =
                    findByUuid(client, uuid);

            String name =
                    entity == null
                            ? "<unloaded>"
                            : entity.getName().getString();

            context.getSource().sendFeedback(
                    Component.literal(
                            "- "
                                    + name
                                    + " ("
                                    + uuid
                                    + ")"
                    )
            );
        }

        return entities.size();
    }

    private static int clear(
            CommandContext<FabricClientCommandSource> context
    ) {
        int count =
                CensorBoxRenderer
                        .getCensoredEntities()
                        .size();

        CensorBoxRenderer.clear();

        context.getSource().sendFeedback(
                Component.literal(
                        "Removed "
                                + count
                                + " censor box"
                                + (count == 1 ? "." : "es.")
                )
        );

        return count;
    }

    private static List<Entity> resolve(
            Minecraft client,
            String selector
    ) {
        if (client.level == null || client.player == null) {
            return List.of();
        }

        String input = selector.trim();

        if (input.isEmpty()) {
            return List.of();
        }

        switch (input.toLowerCase()) {
            case "@s":
                return List.of(client.player);

            case "@a": {
                List<Entity> result = new ArrayList<>();
                result.addAll(client.level.players());
                return result;
            }

            case "@p":
                return findNearestPlayer(client);

            case "@r":
                return findRandomPlayer(client);

            case "@e":
                return collectAllEntities(client);

            case "@n":
                return findNearestEntity(client);

            default:
                break;
        }

        try {
            UUID uuid = UUID.fromString(input);

            Entity entity = findByUuid(client, uuid);

            if (entity == null) {
                return List.of();
            }

            return List.of(entity);
        } catch (IllegalArgumentException ignored) {
            // Not a UUID.
        }

        return collectAllEntities(client)
                .stream()
                .filter(entity ->
                        entity.getName()
                                .getString()
                                .equalsIgnoreCase(input)
                )
                .toList();
    }

    private static List<Entity> collectAllEntities(
            Minecraft client
    ) {
        if (client.level == null) {
            return List.of();
        }

        List<Entity> entities = new ArrayList<>();

        for (Entity entity : client.level.entitiesForRendering()) {
            entities.add(entity);
        }

        return entities;
    }

    private static Entity findByUuid(
            Minecraft client,
            UUID uuid
    ) {
        if (client.level == null) {
            return null;
        }

        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity.getUUID().equals(uuid)) {
                return entity;
            }
        }

        return null;
    }

    private static List<Entity> findNearestPlayer(
            Minecraft client
    ) {
        if (client.level == null
                || client.player == null) {
            return List.of();
        }

        var nearest = client.level.players()
                .stream()
                .min(
                        Comparator.comparingDouble(
                                player ->
                                        player.distanceToSqr(
                                                client.player
                                        )
                        )
                )
                .orElse(null);

        if (nearest == null) {
            return List.of();
        }

        return List.<Entity>of(nearest);
    }

    private static List<Entity> findRandomPlayer(
            Minecraft client
    ) {
        if (client.level == null) {
            return List.of();
        }

        var players = client.level.players();

        if (players.isEmpty()) {
            return List.of();
        }

        var player = players.get(
                ThreadLocalRandom.current()
                        .nextInt(players.size())
        );

        return List.<Entity>of(player);
    }

    private static List<Entity> findNearestEntity(
            Minecraft client
    ) {
        if (client.level == null
                || client.player == null) {
            return List.of();
        }

        var nearest = collectAllEntities(client)
                .stream()
                .min(
                        Comparator.comparingDouble(
                                entity ->
                                        entity.distanceToSqr(
                                                client.player
                                        )
                        )
                )
                .orElse(null);

        if (nearest == null) {
            return List.of();
        }

        return List.of(nearest);
    }
}