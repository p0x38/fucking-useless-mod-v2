package me.p0x38.fabric.client.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.world.entity.EntityType;
import me.p0x38.fabric.client.renderers.CensorBoxRenderer;
import me.p0x38.fuckinguselessmod.Config;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class CensorBoxCommand {
    private static Object lastDefaultLevel;

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
                resolve(context, selector);

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
                resolve(context, selector);

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
                resolve(context, selector);

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

    public static void applyConfiguredDefaults(Minecraft client) {
        if (client.level == null) {
            lastDefaultLevel = null;
            return;
        }

        if (lastDefaultLevel == client.level) {
            return;
        }

        lastDefaultLevel = client.level;
        Config.Data config = Config.get();

        for (String selector : config.censorBoxDefaultSelectors) {
            for (Entity entity : resolve(null, selector)) {
                CensorBoxRenderer.add(entity.getUUID());
            }
        }
    }

    private static List<Entity> resolve(
            CommandContext<FabricClientCommandSource> context,
            String selector
    ) {
        Minecraft client =
                Minecraft.getInstance();

        if (client.level == null || client.player == null) {
            return List.of();
        }

        String input = selector.trim();

        if (input.isEmpty()) {
            return List.of();
        }

        if (input.charAt(0) != '@') {
            try {
                UUID uuid = UUID.fromString(input);

                Entity entity = findByUuid(client, uuid);

                return entity == null
                        ? List.of()
                        : List.of(entity);
            } catch (IllegalArgumentException ignored) {
                // Fall through to an entity-name lookup.
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

        try {
            ParsedSelector parsed =
                    ParsedSelector.parse(input);

            List<Entity> entities = new ArrayList<>(
                    switch (parsed.selectorType) {
                        case SELF -> List.of(client.player);
                        case ALL_PLAYERS, NEAREST_PLAYER, RANDOM_PLAYER -> client.level.players();
                        case ALL_ENTITIES, NEAREST_ENTITY -> collectAllEntities(client);
                    }
            );

            Vec3 origin =
                    new Vec3(
                            parsed.x != null ? parsed.x : client.player.getX(),
                            parsed.y != null ? parsed.y : client.player.getY(),
                            parsed.z != null ? parsed.z : client.player.getZ()
                    );

            entities.removeIf(entity ->
                    !parsed.matches(entity, origin)
            );

            Comparator<Entity> comparator =
                    switch (parsed.sort) {
                        case NEAREST ->
                                Comparator.comparingDouble(
                                        entity -> entity.distanceToSqr(origin)
                                );
                        case FURTHEST ->
                                Comparator.comparingDouble(
                                        (Entity entity) -> entity.distanceToSqr(origin)
                                ).reversed();
                        case RANDOM, ARBITRARY ->
                                null;
                    };

            if (comparator != null) {
                entities.sort(comparator);
            } else if (parsed.sort == SelectorSort.RANDOM) {
                java.util.Collections.shuffle(entities);
            }

            if (parsed.selectorType == SelectorType.NEAREST_PLAYER) {
                parsed.limit = Math.min(parsed.limit, 1);
                entities.sort(
                        Comparator.comparingDouble(
                                entity -> entity.distanceToSqr(origin)
                        )
                );
            } else if (parsed.selectorType == SelectorType.RANDOM_PLAYER
                    && parsed.limit == Integer.MAX_VALUE) {
                parsed.limit = 1;
            } else if (parsed.selectorType == SelectorType.NEAREST_ENTITY) {
                parsed.limit = Math.min(parsed.limit, 1);
                entities.sort(
                        Comparator.comparingDouble(
                                entity -> entity.distanceToSqr(origin)
                        )
                );
            }

            if (entities.size() > parsed.limit) {
                entities = new ArrayList<>(entities.subList(0, parsed.limit));
            }

            return entities;
        } catch (IllegalArgumentException exception) {
            if (context != null) {
                context.getSource().sendError(
                        Component.literal(
                                "Invalid entity selector: " + exception.getMessage()
                        )
                );
            }
            return List.of();
        }
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

    private static final class ParsedSelector {
        private final SelectorType selectorType;
        private String type;
        private boolean typeInverted;
        private String name;
        private boolean nameInverted;
        private String tag;
        private boolean tagInverted;
        private Double minDistance;
        private Double maxDistance;
        private Double x;
        private Double y;
        private Double z;
        private Double dx;
        private Double dy;
        private Double dz;
        private int limit = Integer.MAX_VALUE;
        private SelectorSort sort;

        private ParsedSelector(
                SelectorType selectorType,
                SelectorSort defaultSort
        ) {
            this.selectorType = selectorType;
            this.sort = defaultSort;
        }

        private static ParsedSelector parse(String input) {
            SelectorType selectorType =
                    parseSelectorType(input);

            SelectorSort defaultSort =
                    defaultSortFor(selectorType);

            ParsedSelector result =
                    new ParsedSelector(selectorType, defaultSort);

            if (input.length() == 2) {
                return result;
            }

            String options =
                    extractOptions(input);

            if (options.isBlank()) {
                return result;
            }

            parseOptions(options, result);
            return result;
        }

        private static SelectorType parseSelectorType(String input) {
            if (input.length() < 2 || input.charAt(0) != '@') {
                throw new IllegalArgumentException(
                        "selector must start with '@'"
                );
            }

            return switch (input.charAt(1)) {
                case 's' -> SelectorType.SELF;
                case 'a' -> SelectorType.ALL_PLAYERS;
                case 'p' -> SelectorType.NEAREST_PLAYER;
                case 'r' -> SelectorType.RANDOM_PLAYER;
                case 'e' -> SelectorType.ALL_ENTITIES;
                case 'n' -> SelectorType.NEAREST_ENTITY;
                default -> throw new IllegalArgumentException(
                        "unknown selector '@" + input.charAt(1) + "'"
                );
            };
        }

        private static SelectorSort defaultSortFor(
                SelectorType selectorType
        ) {
            return switch (selectorType) {
                case NEAREST_PLAYER, NEAREST_ENTITY ->
                        SelectorSort.NEAREST;
                case RANDOM_PLAYER ->
                        SelectorSort.RANDOM;
                default ->
                        SelectorSort.ARBITRARY;
            };
        }

        private static String extractOptions(String input) {
            if (input.charAt(2) != '['
                    || input.charAt(input.length() - 1) != ']') {
                throw new IllegalArgumentException(
                        "expected selector options like @e[type=minecraft:zombie]"
                );
            }

            return input.substring(3, input.length() - 1);
        }

        private static void parseOptions(
                String options,
                ParsedSelector result
        ) {
            for (String option : splitOptions(options)) {
                int equals = option.indexOf('=');

                if (equals <= 0 || equals == option.length() - 1) {
                    throw new IllegalArgumentException(
                            "invalid selector option '" + option + "'"
                    );
                }

                String key = option.substring(0, equals).trim();
                String value = option.substring(equals + 1).trim();

                parseOption(result, key, value);
            }
        }

        private static void parseOption(
                ParsedSelector result,
                String key,
                String value
        ) {
            switch (key) {
                case "type" -> {
                    result.typeInverted = value.startsWith("!");
                    result.type = stripNegation(value);

                    if (EntityType.byString(result.type).isEmpty()) {
                        throw new IllegalArgumentException(
                                "unknown entity type '" + result.type + "'"
                        );
                    }
                }
                case "name" -> {
                    result.nameInverted = value.startsWith("!");
                    result.name = stripNegation(value);
                }
                case "tag" -> {
                    result.tagInverted = value.startsWith("!");
                    result.tag = stripNegation(value);
                }
                case "distance" -> {
                    DoubleRange range =
                            DoubleRange.parse(value);
                    result.minDistance = range.min;
                    result.maxDistance = range.max;
                }
                case "x" -> result.x = parseDouble(value, "x");
                case "y" -> result.y = parseDouble(value, "y");
                case "z" -> result.z = parseDouble(value, "z");
                case "dx" -> result.dx = parseDouble(value, "dx");
                case "dy" -> result.dy = parseDouble(value, "dy");
                case "dz" -> result.dz = parseDouble(value, "dz");
                case "limit" -> result.limit = parseLimit(value);
                case "sort" -> result.sort = SelectorSort.parse(value);
                default -> throw new IllegalArgumentException(
                        "unsupported selector option '" + key + "'"
                );
            }
        }

        private static int parseLimit(String value) {
            try {
                int limit = Integer.parseInt(value);

                if (limit < 1) {
                    throw new IllegalArgumentException(
                            "limit must be at least 1"
                    );
                }

                return limit;
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        "invalid limit '" + value + "'"
                );
            }
        }

        private boolean matches(Entity entity, Vec3 origin) {
            if (type != null) {
                boolean sameType =
                        EntityType.byString(type)
                                .map(entityType -> entity.getType() == entityType)
                                .orElse(false);

                if (typeInverted == sameType) {
                    return false;
                }
            }

            if (name != null) {
                boolean sameName =
                        entity.getName()
                                .getString()
                                .equals(name);

                if (nameInverted == sameName) {
                    return false;
                }
            }

            if (tag != null) {
                boolean hasTag = entity.getTags().contains(tag);

                if (tagInverted == hasTag) {
                    return false;
                }
            }

            double distance =
                    Math.sqrt(entity.distanceToSqr(origin));

            if (minDistance != null && distance < minDistance) {
                return false;
            }

            if (maxDistance != null && distance > maxDistance) {
                return false;
            }

            if (dx != null || dy != null || dz != null) {
                double minX = origin.x;
                double minY = origin.y;
                double minZ = origin.z;
                double maxX = origin.x + (dx != null ? dx : 0.0);
                double maxY = origin.y + (dy != null ? dy : 0.0);
                double maxZ = origin.z + (dz != null ? dz : 0.0);

                double lowX = Math.min(minX, maxX);
                double lowY = Math.min(minY, maxY);
                double lowZ = Math.min(minZ, maxZ);
                double highX = Math.max(minX, maxX);
                double highY = Math.max(minY, maxY);
                double highZ = Math.max(minZ, maxZ);

                return entity.getBoundingBox().intersects(
                        lowX, lowY, lowZ,
                        highX, highY, highZ
                );
            }

            return true;
        }

        private static List<String> splitOptions(String options) {
            List<String> result = new ArrayList<>();
            int start = 0;
            boolean quoted = false;

            for (int i = 0; i < options.length(); i++) {
                char character = options.charAt(i);

                if (character == '"') {
                    quoted = !quoted;
                } else if (character == ',' && !quoted) {
                    result.add(options.substring(start, i));
                    start = i + 1;
                }
            }

            if (quoted) {
                throw new IllegalArgumentException("unterminated quote in selector");
            }

            result.add(options.substring(start));
            return result;
        }

        private static String stripNegation(String value) {
            return value.startsWith("!")
                    ? value.substring(1)
                    : value;
        }

        private static double parseDouble(
                String value,
                String key
        ) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        "invalid " + key + " value '" + value + "'"
                );
            }
        }
    }

    private enum SelectorType {
        SELF,
        ALL_PLAYERS,
        NEAREST_PLAYER,
        RANDOM_PLAYER,
        ALL_ENTITIES,
        NEAREST_ENTITY
    }

    private enum SelectorSort {
        NEAREST,
        FURTHEST,
        RANDOM,
        ARBITRARY;

        private static SelectorSort parse(String value) {
            return switch (value.toLowerCase()) {
                case "nearest" -> NEAREST;
                case "furthest" -> FURTHEST;
                case "random" -> RANDOM;
                case "arbitrary" -> ARBITRARY;
                default -> throw new IllegalArgumentException(
                        "unknown sort '" + value + "'"
                );
            };
        }
    }

    private record DoubleRange(Double min, Double max) {

        private static DoubleRange parse(
                    String value
            ) {
                try {
                    int separator = value.indexOf("..");

                    if (separator < 0) {
                        double exact = Double.parseDouble(value);
                        return new DoubleRange(exact, exact);
                    }

                    String left = value.substring(0, separator);
                    String right = value.substring(separator + 2);

                    Double min = left.isEmpty()
                            ? null
                            : Double.parseDouble(left);
                    Double max = right.isEmpty()
                            ? null
                            : Double.parseDouble(right);

                    if (min == null && max == null) {
                        throw new NumberFormatException();
                    }

                    return new DoubleRange(min, max);
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException(
                            "invalid " + "distance" + " range '" + value + "'"
                    );
                }
            }
        }

    private static Entity findNearestPlayer(
            Minecraft client
    ) {
        if (client.level == null
                || client.player == null) {
            return null;
        }

        return client.level.players()
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

        return List.of(
                players.get(
                        ThreadLocalRandom.current()
                                .nextInt(players.size())
                )
        );
    }

}