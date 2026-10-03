package me.p0x38.fabric.client.chatentity;

import me.p0x38.fuckinguselessmod.Config;
import me.p0x38.fuckinguselessmod.util.DebugLogger;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class ChatEntityBrain {
    private static final String TEXT_PREFIX =
            "text.fuckinguselessmod.chat.entity.";

    private static final long LONG_IDLE_BEFORE_SLEEP_RESPONSE_TICKS =
            60 * 60 * 20L;

    private enum InteractionKind {
        CONTROL,
        UNSETTLING,
        GREETING,
        IDENTITY,
        ACTIVITY,
        WELLBEING,
        NULL,
        CONFUSED,
        INSULT,
        THANKS,
        APOLOGY,
        QUESTION,
        NORMAL,
        SLEEP
    }

    private ChatEntityBrain() {
    }

    public static void initialGreeting(
            ChatEntity entity,
            long gameTick
    ) {
        say(
                entity,
                chooseText("first.greeting", 6),
                gameTick,
                ChatEntity.ReactionKind.GREETING
        );
        entity.markAction(gameTick);
    }

    public static void firstChatAmbient(
            ChatEntity entity,
            long gameTick
    ) {
        if (!randomChance(0.05)) {
            return;
        }

        say(
                entity,
                chooseText("first.ambient", 8),
                gameTick,
                ChatEntity.ReactionKind.META
        );
        entity.markAction(gameTick);
    }

    public static void think(
            ChatEntity entity,
            ClientLevel level
    ) {
        long gameTick = level.getGameTime();
        Memory latest = entity.latestMemory();

        if (latest == null
                || latest == entity.lastProcessedMemory()) {
            return;
        }

        /*
         * Direct player messages bypass the ambient action cooldown because
         * they are conversational events. Everything else respects it.
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
            case CHAT_ENTITY_SPOKE ->
                    handleEntitySpoke(entity, latest, gameTick);
            case WORLD_CHANGED ->
                    handleWorldChanged(entity, latest, gameTick);
            case PLAYER_IDLE ->
                    handlePlayerIdle(entity, latest, gameTick);
            case PLAYER_DAMAGED ->
                    handlePlayerDamaged(entity, latest, gameTick);
            case PLAYER_DIED ->
                    handlePlayerDied(entity, latest, gameTick);
            case PLAYER_LOCATION_UPDATED ->
                    handleLocationUpdate(entity, latest);
            case PLAYER_INTERACTED ->
                    handleInteraction(entity, latest, gameTick);
            case PLAYER_SEEN ->
                    handlePlayerSeen(entity, latest, gameTick);
            case PLAYER_RETURNED ->
                    handlePlayerReturned(entity, latest, gameTick);
            case PLAYER_LOOKED_AWAY ->
                    handlePlayerLookedAway(entity, latest, gameTick);
            default -> entity.markMemoryProcessed(latest);
        }
    }

    private static void handleEntitySpoke(
            ChatEntity entity,
            Memory memory,
            long gameTick
    ) {
        ChatEntity.ReactionKind reaction =
                entity.lastReactionKind();

        if ((reaction == ChatEntity.ReactionKind.UNSETTLING
                || reaction == ChatEntity.ReactionKind.OUT_OF_PLACE)
                && randomChance(0.35)) {
            say(
                    entity,
                    chooseText("self_aware", 20),
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
            if ("true".equals(memory.context("firstEncounter"))) {
                markProcessed(entity, memory, gameTick);
                return;
            }

            if (randomChance(0.25)) {
                say(
                        entity,
                        chooseText("quiet", 3),
                        gameTick,
                        ChatEntity.ReactionKind.META
                );
            } else {
                say(
                        entity,
                        chooseText("world_changed", 20),
                        gameTick,
                        ChatEntity.ReactionKind.META
                );
            }
        }

        markProcessed(entity, memory, gameTick);
    }

    private static void handlePlayerIdle(
            ChatEntity entity,
            Memory memory,
            long gameTick
    ) {
        int stage = parseIdleStage(memory);

        if (stage == 1 && randomChance(0.35)) {
            say(
                    entity,
                    chooseText("quiet", 3),
                    gameTick,
                    ChatEntity.ReactionKind.NORMAL
            );
        } else {
            say(
                    entity,
                    chooseText("idle." + stage, 4),
                    gameTick,
                    stage >= 5
                            ? ChatEntity.ReactionKind.META
                            : ChatEntity.ReactionKind.NORMAL
            );
        }

        markProcessed(entity, memory, gameTick);
    }

    private static int parseIdleStage(Memory memory) {
        String value = memory.context("stage");

        if (value == null) {
            return 1;
        }

        try {
            return Math.max(
                    1,
                    Integer.parseInt(value)
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

    private static void handlePlayerDamaged(
            ChatEntity entity,
            Memory memory,
            long gameTick
    ) {
        say(
                entity,
                chooseText("event.damage", 5),
                gameTick,
                ChatEntity.ReactionKind.NORMAL
        );

        markProcessed(entity, memory, gameTick);
    }

    private static void handlePlayerDied(
            ChatEntity entity,
            Memory memory,
            long gameTick
    ) {
        if ("creeper".equals(memory.context("cause"))) {
            say(
                    entity,
                    chooseText("event.creeper_death", 4),
                    gameTick,
                    ChatEntity.ReactionKind.META
            );
        }

        markProcessed(entity, memory, gameTick);
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
                        entity,
                        memory
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
        if (randomChance(0.50)) {
            String username = memory.context("username");

            say(
                    entity,
                    chooseText(
                            "returned",
                            6,
                            username
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
        ChatEntity.Mood mood = entity.mood();

        if (mood == ChatEntity.Mood.CURIOUS
                && randomChance(0.25)) {
            say(
                    entity,
                    chooseText("curious", 18),
                    gameTick
            );

            markProcessed(entity, memory, gameTick);
            return;
        }

        if (mood == ChatEntity.Mood.ANNOYED
                && randomChance(0.30)) {
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
            ChatEntity entity,
            Memory memory
    ) {
        String normalized = normalize(message);
        int interactionCount = entity.interactionCount();
        int annoyanceCount = entity.annoyanceCount();
        ChatEntity.Mood mood = entity.mood();
        boolean sillyMode = Config.get().chatEntitySillyMode;

        InteractionKind kind =
                classifyInteraction(normalized);

        return switch (kind) {
            case CONTROL -> chooseText("control", 14);
            case UNSETTLING -> chooseText("unsettling", 16);
            case ACTIVITY -> chooseText("activity", 10);
            case WELLBEING -> chooseText("wellbeing", 10);
            case NULL -> chooseNullResponse(entity);
            case CONFUSED -> chooseText("confused", 14);
            case INSULT -> chooseText("insult", 14);
            case THANKS -> chooseText("thanks", 14);
            case APOLOGY -> chooseText("apology", 14);
            case SLEEP ->
                    chooseSleepResponse(entity, memory);
            case GREETING ->
                    chooseGreetingResponse(
                            interactionCount,
                            username
                    );
            case IDENTITY ->
                    interactionCount >= 5
                            ? chooseText("identity.again", 4)
                            : chooseText("identity", 8);
            case QUESTION -> chooseText("question", 16);
            case NORMAL -> chooseNormalResponse(
                    interactionCount,
                    annoyanceCount,
                    mood,
                    sillyMode
            );
        };
    }

    private static String chooseSleepResponse(
            ChatEntity entity,
            Memory memory
    ) {
        long idleTicks =
                parseLongContext(
                        memory
                );

        if (idleTicks >= LONG_IDLE_BEFORE_SLEEP_RESPONSE_TICKS) {
            return chooseText("sleep.returned", 6);
        }

        return chooseText("wellbeing", 10);
    }

    private static long parseLongContext(
            Memory memory
    ) {
        String value = memory.context("idleTicksBeforeInteraction");

        if (value == null) {
            return 0L;
        }

        try {
            return Math.max(
                    0L,
                    Long.parseLong(value)
            );
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    private static String chooseNullResponse(
            ChatEntity entity
    ) {
        int interactionCount = entity.interactionCount();

        double formalChance =
                interactionCount >= 8
                        ? 0.85
                        : interactionCount >= 4
                                ? 0.70
                                : 0.55;

        if (randomChance(0.005)) {
            return chooseText("null.special", 1);
        }

        if (randomChance(formalChance)) {
            return chooseText("null.formal", 10);
        }

        return chooseText("null", 19);
    }

    private static String chooseNormalResponse(
            int interactionCount,
            int annoyanceCount,
            ChatEntity.Mood mood,
            boolean sillyMode
    ) {
        /*
         * Silly responses are only possible for otherwise ordinary messages.
         * Classification already ruled out greetings and questions.
         */
        if (randomChance(0.0035)) {
            if (randomChance(0.25)) {
                return chooseText(
                        randomChance(0.50)
                                ? "arg"
                                : "horror",
                        6
                );
            }

            return chooseText("rare", 33);
        }

        if (sillyMode
                && randomChance(
                        interactionCount >= 10
                                ? 0.18
                                : 0.08
                )) {
            return chooseText("out_of_place", 18);
        }

        if (annoyanceCount >= 6) {
            return chooseText(
                    "annoyed.interaction.late",
                    5
            );
        }

        if (annoyanceCount >= 3) {
            return chooseText(
                    "annoyed.interaction.mid",
                    5
            );
        }

        if (sillyMode
                && mood == ChatEntity.Mood.PLAYFUL) {
            return chooseText("playful", 16);
        }

        if (mood == ChatEntity.Mood.ANNOYED) {
            return chooseText(
                    "annoyed.interaction",
                    6
            );
        }

        if (mood == ChatEntity.Mood.CURIOUS) {
            return chooseText(
                    "interacted.curious",
                    7
            );
        }

        if (interactionCount >= 10) {
            return chooseText(
                    "interacted.familiar",
                    5
            );
        }

        return chooseText(
                "interacted.default",
                8
        );
    }

    private static String chooseGreetingResponse(
            int interactionCount,
            String username
    ) {
        if (interactionCount == 1) {
            return chooseText(
                    "greeting.first",
                    4,
                    username
            );
        }

        if (interactionCount <= 3) {
            int index = randomIndex(4);

            return index <= 2
                    ? text(
                            "greeting.again." + index,
                            username
                    )
                    : text(
                            "greeting.again." + index
                    );
        }

        int index = randomIndex(4);

        return index == 1
                ? text(
                        "greeting.familiar.1",
                        username
                )
                : text(
                        "greeting.familiar." + index
                );
    }

    private static InteractionKind classifyInteraction(
            String normalized
    ) {
        if (ChatEntityTriggerRegistry.matches("control", normalized)) {
            return InteractionKind.CONTROL;
        }

        if (ChatEntityTriggerRegistry.matches("unsettling", normalized)) {
            return InteractionKind.UNSETTLING;
        }

        if (ChatEntityTriggerRegistry.matches("greeting", normalized)) {
            return InteractionKind.GREETING;
        }

        if (ChatEntityTriggerRegistry.matches("identity", normalized)) {
            return InteractionKind.IDENTITY;
        }

        if (ChatEntityTriggerRegistry.matches("activity", normalized)) {
            return InteractionKind.ACTIVITY;
        }

        if (ChatEntityTriggerRegistry.matches("wellbeing", normalized)) {
            return InteractionKind.WELLBEING;
        }

        if (ChatEntityTriggerRegistry.matches("sleep", normalized)) {
            return InteractionKind.SLEEP;
        }

        if (ChatEntityTriggerRegistry.matches("null", normalized)) {
            return InteractionKind.NULL;
        }

        if (ChatEntityTriggerRegistry.matches("insult", normalized)) {
            return InteractionKind.INSULT;
        }

        if (ChatEntityTriggerRegistry.matches("thanks", normalized)) {
            return InteractionKind.THANKS;
        }

        if (ChatEntityTriggerRegistry.matches("apology", normalized)) {
            return InteractionKind.APOLOGY;
        }

        if (ChatEntityTriggerRegistry.matches("confused", normalized)) {
            return InteractionKind.CONFUSED;
        }

        return ChatEntityTriggerRegistry.matches("question", normalized)
                ? InteractionKind.QUESTION
                : InteractionKind.NORMAL;
    }

    private static String normalize(String message) {
        return message == null
                ? ""
                : message.strip().toLowerCase(Locale.ROOT);
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
                TEXT_PREFIX + key
        ).getString();
    }

    private static String text(
            String key,
            String username
    ) {
        String resolvedUsername =
                username == null || username.isBlank()
                        ? "you"
                        : username;

        return text(key)
                .replace(
                        "${username}",
                        resolvedUsername
                );
    }

    private static String chooseText(
            String prefix,
            int count
    ) {
        return text(
                prefix + "." + randomIndex(count)
        );
    }

    private static String chooseText(
            String prefix,
            int count,
            String username
    ) {
        return text(
                prefix + "." + randomIndex(count),
                username
        );
    }

    private static int randomIndex(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException(
                    "Response pool must contain at least one entry"
            );
        }

        return ThreadLocalRandom.current()
                .nextInt(count) + 1;
    }

    private static boolean randomChance(double chance) {
        return ThreadLocalRandom.current()
                .nextDouble()
                < chance;
    }
}
