package me.p0x38.fabric.client.chatentity;

import me.p0x38.fuckinguselessmod.Config;
import me.p0x38.fuckinguselessmod.util.DebugLogger;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class ChatEntityBrain {
    private enum InteractionKind {
        CONTROL,
        UNSETTLING,
        GREETING,
        IDENTITY,
        QUESTION,
        NORMAL
    }

    private ChatEntityBrain() {
    }

    public static void think(
            ChatEntity entity,
            ClientLevel level
    ) {
        long gameTick = level.getGameTime();

        var memories = entity.memories();

        if (memories.isEmpty()) {
            return;
        }

        Memory latest = entity.latestMemory();

        if (latest == null
        || latest == entity.lastProcessedMemory()) {
            return;
        }

        /*
         * Direct player messages are scheduled immediately, but the actual
         * chat delivery happens after a thinking phase and a typing/composition
         * phase. Ambient reactions still respect the normal speech cooldown.
         */
        if (latest.type() != Memory.Type.PLAYER_INTERACTED
                && !entity.canAct(gameTick)) {
            return;
        }

        DebugLogger.debug(
                "[ChatEntityBrain] think id={} tick={} mood={} latestMemory={} message={}",
                entity.id(),
                gameTick,
                entity.mood(),
                latest.type(),
                entity.currentMessage()
        );

        switch (latest.type()) {
            case CHAT_ENTITY_SPOKE -> handleEntitySpoke(entity, latest, gameTick);
            case WORLD_CHANGED -> handleWorldChanged(entity, latest, gameTick);
            case PLAYER_IDLE -> handlePlayerIdle(entity, latest, gameTick);
            case PLAYER_LOCATION_UPDATED -> handleLocationUpdate(entity, latest);
            case PLAYER_INTERACTED -> handleInteraction(entity, latest, gameTick);
            case PLAYER_SEEN -> handlePlayerSeen(entity, latest, gameTick);
            case PLAYER_RETURNED -> handlePlayerReturned(entity, latest, gameTick);
            case PLAYER_LOOKED_AWAY -> handlePlayerLookedAway(entity, latest, gameTick);
            default -> {
                // No response currently associated with this memory.
            }
        }
    }

    private static void handleEntitySpoke(
            ChatEntity entity,
            Memory memory,
            long gameTick
    ) {
        if ((entity.lastReactionKind() == ChatEntity.ReactionKind.UNSETTLING
        || entity.lastReactionKind() == ChatEntity.ReactionKind.OUT_OF_PLACE) && randomChance(0.35)) {
            say(
                    entity,
                    chooseText("self_aware", 8),
                    gameTick,
                    ChatEntity.ReactionKind.META
            );

            entity.markAction(gameTick);
        }

        entity.markMemoryProcessed(memory);
    }

    private static void handleWorldChanged(
            ChatEntity entity,
            Memory memory,
            long gameTick
    ) {
        if ("hidden".equals(memory.context("phase"))) {
            say(
                    entity,
                    chooseText("world_changed", 8),
                    gameTick,
                    ChatEntity.ReactionKind.META
            );
        }

        markProcessed(entity, memory, gameTick);
    }

    private static void handlePlayerIdle(
            ChatEntity entity,
            Memory memory,
            long gameTick
    ) {
        int stage = parseIdleStage(memory);

        say(
                entity,
                chooseText("idle." + stage, 4),
                gameTick,
                stage >= 5
                ? ChatEntity.ReactionKind.META
                        : ChatEntity.ReactionKind.NORMAL
        );

        markProcessed(entity, memory, gameTick);
    }

    private static int parseIdleStage(Memory memory) {
        try {
            return Math.max(
                    1,
                    Integer.parseInt(memory.context("stage"))
            );
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    private static void handleLocationUpdate(
            ChatEntity entity,
            Memory memory
    ) {
        entity.markMemoryProcessed(memory);
    }

    private static void handleInteraction(
            ChatEntity entity,
            Memory memory,
            long gameTick
    ) {
        String message = memory.context("message");
        String username = memory.context("username");

        say(
                entity,
                chooseInteractionResponse(
                        message,
                        username,
                        entity
                ),
                gameTick
        );

        markProcessed(entity, memory, gameTick);
    }

    private static void handlePlayerSeen(
            ChatEntity entity,
            Memory memory,
            long gameTick
    ) {
        if (randomChance(0.18)) {
            say(
                    entity,
                    chooseText("seen", 6),
                    gameTick,
                    ChatEntity.ReactionKind.NORMAL
            );

            markProcessed(entity, memory, gameTick);
            return;
        }

        entity.markMemoryProcessed(memory);
    }

    private static void handlePlayerReturned(
            ChatEntity entity,
            Memory memory,
            long gameTick
    ) {
        if (randomChance(0.5)) {
            String username = memory.context("username");

            say(
                    entity,
                    choose(
                            text("returned.1", username),
                            text("returned.2", username),
                            text("returned.3", username),
                            text("returned.4", username),
                            text("returned.5"),
                            text("returned.6")
                    ),
                    gameTick,
                    ChatEntity.ReactionKind.NORMAL
            );

            markProcessed(entity, memory, gameTick);
            return;
        }

        entity.markMemoryProcessed(memory);
    }

    private static void handlePlayerLookedAway(
            ChatEntity entity,
            Memory memory,
            long gameTick
    ) {
        if (entity.mood() == ChatEntity.Mood.CURIOUS && randomChance(0.25)) {
            say(
                    entity,
                    chooseText("curious", 8),
                    gameTick
            );

            markProcessed(entity, memory, gameTick);
            return;
        }

        if (entity.mood() == ChatEntity.Mood.ANNOYED && randomChance(0.3)) {
            say(
                    entity,
                    chooseText("annoyed", 8),
                    gameTick
            );

            markProcessed(entity, memory, gameTick);
            return;
        }

        entity.markMemoryProcessed(memory);
    }

    private static void markProcessed(
            ChatEntity entity,
            Memory memory,
            long gameTick
    ) {
        entity.markMemoryProcessed(memory);
        entity.markAction(gameTick);
    }

    private static String chooseInteractionResponse(
            String message,
            String username,
            ChatEntity entity
    ) {
        String normalized = normalize(message);
        int interactionCount = entity.interactionCount();
        int annoyanceCount = entity.annoyanceCount();

        InteractionKind kind = classifyInteraction(normalized);

        if (kind == InteractionKind.CONTROL) {
            return chooseText("control", 6);
        }

        if (kind == InteractionKind.UNSETTLING) {
            return chooseText("unsettling", 8);
        }

        /*
         * The entity occasionally answers with something only loosely
         * connected to the conversation. This becomes more common
         * as it grows familiar with the player.
         */
        if (Config.get().chatEntitySillyMode
                && !isQuestion(normalized)
                && !isGreeting(normalized)
                && randomChance(entity.interactionCount() >= 10 ? 0.18 : 0.08)) {
            return chooseText("out_of_place", 8);
        }

        if (kind == InteractionKind.GREETING) {
            if (interactionCount == 1) {
                return choose(
                        text("greeting.first.1", username),
                        text("greeting.first.2", username),
                        text("greeting.first.3", username),
                        text("greeting.first.4", username)
                );
            }

            if (interactionCount <= 3) {
                return choose(
                        text("greeting.again.1", username),
                        text("greeting.again.2", username),
                        text("greeting.again.3"),
                        text("greeting.again.4")
                );
            }

            return choose(
                    text("greeting.familiar.1", username),
                    text("greeting.familiar.2"),
                    text("greeting.familiar.3"),
                    text("greeting.familiar.4")
            );
        }

        if (kind == InteractionKind.IDENTITY) {
            if (interactionCount >= 5) {
                return choose(
                        text("identity.again.1"),
                        text("identity.again.2"),
                        text("identity.again.3"),
                        text("identity.again.4")
                );
            }

            return choose(
                    text("identity.1"),
                    text("identity.2"),
                    text("identity.3"),
                    text("identity.4"),
                    text("identity.5"),
                    text("identity.6"),
                    text("identity.7"),
                    text("identity.8")
            );
        }

        if (kind == InteractionKind.QUESTION) {
            return chooseText("question", 6);
        }

        if (annoyanceCount >= 6) {
            return choose(
                    text("annoyed.interaction.late.1"),
                    text("annoyed.interaction.late.2"),
                    text("annoyed.interaction.late.3"),
                    text("annoyed.interaction.late.4"),
                    text("annoyed.interaction.late.5")
            );
        }

        if (annoyanceCount >= 3) {
            return choose(
                    text("annoyed.interaction.mid.1"),
                    text("annoyed.interaction.mid.2"),
                    text("annoyed.interaction.mid.3"),
                    text("annoyed.interaction.mid.4"),
                    text("annoyed.interaction.mid.5")
            );
        }

        if (Config.get().chatEntitySillyMode && entity.mood() == ChatEntity.Mood.PLAYFUL) {
            return choose(
                    text("playful.1"),
                    text("playful.2"),
                    text("playful.3"),
                    text("playful.4"),
                    text("playful.5"),
                    text("playful.6")
            );
        }

        if (entity.mood() == ChatEntity.Mood.ANNOYED) {
            return choose(
                    text("annoyed.interaction.1"),
                    text("annoyed.interaction.2"),
                    text("annoyed.interaction.3"),
                    text("annoyed.interaction.4"),
                    text("annoyed.interaction.5"),
                    text("annoyed.interaction.6")
            );
        }

        if (entity.mood() == ChatEntity.Mood.CURIOUS) {
            return choose(
                    text("interacted.curious.1"),
                    text("interacted.curious.2"),
                    text("interacted.curious.3"),
                    text("interacted.curious.4"),
                    text("interacted.curious.5"),
                    text("interacted.curious.6"),
                    text("interacted.curious.7")
            );
        }

        if (interactionCount >= 10) {
            return choose(
                    text("interacted.familiar.1"),
                    text("interacted.familiar.2"),
                    text("interacted.familiar.3"),
                    text("interacted.familiar.4"),
                    text("interacted.familiar.5")
            );
        }

        return choose(
                text("interacted.default.1"),
                text("interacted.default.2"),
                text("interacted.default.3"),
                text("interacted.default.4"),
                text("interacted.default.5"),
                text("interacted.default.6"),
                text("interacted.default.7"),
                text("interacted.default.8")
        );
    }

    private static InteractionKind classifyInteraction(
            String normalized
    ) {
        if (isControlQuestion(normalized)) return InteractionKind.CONTROL;
        if (isUnsettlingQuestion(normalized)) return InteractionKind.UNSETTLING;
        if (isGreeting(normalized)) return InteractionKind.GREETING;
        if (isIdentityQuestion(normalized)) return InteractionKind.IDENTITY;
        if (isQuestion(normalized)) return InteractionKind.QUESTION;

        return InteractionKind.NORMAL;
    }

    private static boolean isUnsettlingQuestion(String message) {
        return message.contains("do you feel pain")
                || message.contains("are you afraid")
                || message.contains("are you scared")
                || message.contains("can you die")
                || message.contains("do chat entities die")
                || message.contains("can i break you")
                || message.contains("what happens if i break you")
                || message.contains("what happens if i destroy you")
                || message.contains("what happens when i break you")
                || message.contains("what happens when i look away")
                || message.contains("where do you go when i look away")
                || message.contains("do you know when i'm not looking")
                || message.contains("are you watching me")
                || message.contains("are you following me")
                || message.contains("can you see me when i'm not looking")
                || message.contains("can you remember me when i'm gone");
    }

    private static boolean isControlQuestion(String message) {
        return message.contains("why do you say weird things")
                || message.contains("why are you saying weird things")
                || message.contains("do you know you're weird")
                || message.contains("do you know you are weird")
                || message.contains("can you control yourself")
                || message.contains("can you control what you say")
                || message.contains("why can't you control yourself")
                || message.contains("why can't you stop")
                || message.contains("can you stop yourself")
                || message.contains("do you choose what you say");
    }

    private static boolean isGreeting(String message) {
        return message.matches(
                "^(hi+|hello+|hey+|hiya|yo+|sup|howdy)[!.,? ]*$"
        );
    }

    private static boolean isIdentityQuestion(String message) {
        return message.equals("who are you")
                || message.equals("who r u")
                || message.equals("what are you")
                || message.equals("what's your name")
                || message.equals("what is your name")
                || message.equals("do you have a name")
                || message.equals("are you alive")
                || message.equals("are you real")
                || message.equals("are you sentient")
                || message.equals("are you conscious");
    }

    private static boolean isQuestion(String message) {
        return message.endsWith("?")
                || message.startsWith("why ")
                || message.startsWith("how ")
                || message.startsWith("what ")
                || message.startsWith("where ")
                || message.startsWith("when ")
                || message.startsWith("can you ")
                || message.startsWith("do you ");
    }

    private static String normalize(String message) {
        return message
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private static void say(
            ChatEntity entity,
            String message,
            long gameTick
    ) {
        say(
                entity,
                message,
                gameTick,
                ChatEntity.ReactionKind.NORMAL
        );
    }

    private static void say(
            ChatEntity entity,
            String message,
            long gameTick,
            ChatEntity.ReactionKind reactionKind
    ) {
        ChatEntityResponseTiming.Timing timing =
                ChatEntityResponseTiming.calculate(
                        entity,
                        message,
                        reactionKind
                );

        DebugLogger.debug(
                "[ChatEntityBrain] scheduling speech id={} tick={} thinkingTicks={} typingTicks={} kind={} message={}",
                entity.id(),
                gameTick,
                timing.thinkingTicks(),
                timing.typingTicks(),
                reactionKind,
                message
        );

        entity.queueResponse(
                message,
                reactionKind,
                gameTick,
                timing.thinkingTicks(),
                timing.typingTicks()
        );
    }

    private static String text(String key) {
        return Component.translatable(
                "text.fuckinguselessmod.chat.entity." + key
        ).getString();
    }

    private static String text(String key, String username) {
        String resolvedUsername =
                username == null || username.isBlank()
                        ? "you"
                        : username;

        return text(key)
                .replace("${username}", resolvedUsername);
    }

    private static String chooseText(
            String prefix,
            int count
    ) {
        return text(
                prefix
                + "."
                + (ThreadLocalRandom.current().nextInt(0, count + 1))
        );
    }

    private static boolean isPerceptionMemory(
            Memory memory
    ) {
        return memory.type() == Memory.Type.PLAYER_RETURNED
                || memory.type() == Memory.Type.PLAYER_LOOKED_AWAY;
    }

    private static boolean randomChance(double chance) {
        return ThreadLocalRandom.current()
                .nextDouble()
                < chance;
    }

    private static String choose(String... options) {
        return options[
                ThreadLocalRandom.current()
                        .nextInt(options.length)
                ];
    }
}
