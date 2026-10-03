package me.p0x38.fabric.client.unknownentity;

import me.p0x38.fuckinguselessmod.Config;
import me.p0x38.fuckinguselessmod.util.DebugLogger;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/** Handles perception-driven behavior, dialogue selection, and response scheduling. */
public final class UnknownEntityBrain {
    private static final String TEXT_PREFIX =
            "text.fuckinguselessmod.chat.entity.";

    private static final long LONG_IDLE_BEFORE_SLEEP_RESPONSE_TICKS =
            60 * 60 * 20L;

    private record InteractionResponse(
            String message,
            ReactionKind reactionKind
    ) {
    }

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
        LOCATION,
        NORMAL,
        SLEEP
    }

    private UnknownEntityBrain() {
    }

    /** Schedules the entity's initial greeting. */
    public static void initialGreeting(
            UnknownEntity entity,
            long gameTick
    ) {
        say(
                entity,
                chooseText("first.greeting", 6),
                gameTick,
                ReactionKind.GREETING
        );
        entity.markAction(gameTick);
    }

    /** Rarely schedules a non-response ambient message after the first chat. */
    public static void firstChatAmbient(
            UnknownEntity entity,
            long gameTick
    ) {
        if (!randomChance(0.05)) {
            return;
        }

        String message =
                chooseText("first.ambient", 12);

        UnknownEntityResponseTiming.Timing timing =
                UnknownEntityResponseTiming.calculate(
                        entity,
                        message,
                        ReactionKind.META
                );

        long delayTicks =
                20
                        + ThreadLocalRandom.current().nextLong(20, 60);

        long baseTick = gameTick + delayTicks;

        entity.queueResponse(
                message,
                ReactionKind.META,
                baseTick,
                timing.thinkingTicks(),
                timing.typingTicks()
        );
        entity.markAction(gameTick);
    }

    /** Processes the next unhandled memory and updates entity behavior. */
    public static void think(
            UnknownEntity entity,
            ClientLevel level
    ) {
        long gameTick = level.getGameTime();

        if (handleSillyMode(entity, gameTick)) return;

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

    private static boolean handleSillyMode(
            UnknownEntity entity,
            long gameTick
    ) {
        if (!entity.isSilly(gameTick)) return false;
        if (!entity.canDoSillyAction(gameTick)) return true;

        entity.scheduleNextSillyAction(gameTick);

        if (ThreadLocalRandom.current().nextInt(4) == 3) {
            // TODO: add some silly actions without speaking
            return true;
        }

        say(
                entity,
                chooseText("silly", 3),
                gameTick,
                ReactionKind.PLAYFUL
        );

        return true;
    }

    private static void handleEntitySpoke(
            UnknownEntity entity,
            Memory memory,
            long gameTick
    ) {
        ReactionKind reaction =
                entity.lastReactionKind();

        if ((reaction == ReactionKind.UNSETTLING
                || reaction == ReactionKind.OUT_OF_PLACE)
                && randomChance(0.35)) {
            say(
                    entity,
                    chooseText("self_aware", 8),
                    gameTick,
                    ReactionKind.META
            );

            entity.markAction(gameTick);
        }

        entity.markMemoryProcessed(memory);
    }

    private static void handleWorldChanged(
            UnknownEntity entity,
            Memory memory,
            long gameTick
    ) {
        if ("hidden".equals(memory.context().get("phase"))) {
            if ("true".equals(memory.context().get("firstEncounter"))) {
                markProcessed(entity, memory, gameTick);
                return;
            }

            if (randomChance(0.25)) {
                say(
                        entity,
                        chooseText("quiet", 3),
                        gameTick,
                        ReactionKind.META
                );
            } else {
                say(
                        entity,
                        chooseText("world_changed", 8),
                        gameTick,
                        ReactionKind.META
                );
            }
        }

        markProcessed(entity, memory, gameTick);
    }

    private static void handlePlayerIdle(
            UnknownEntity entity,
            Memory memory,
            long gameTick
    ) {
        int stage = parseIdleStage(memory);

        if (stage == 1 && randomChance(0.35)) {
            say(
                    entity,
                    chooseText("quiet", 3),
                    gameTick,
                    ReactionKind.NORMAL
            );
        } else {
            say(
                    entity,
                    chooseText("idle." + stage, 4),
                    gameTick,
                    stage >= 5
                            ? ReactionKind.META
                            : ReactionKind.NORMAL
            );
        }

        markProcessed(entity, memory, gameTick);
    }

    private static int parseIdleStage(Memory memory) {
        String value = memory.context().get("stage");

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
            UnknownEntity entity,
            Memory memory
    ) {
        entity.markMemoryProcessed(memory);
    }

    private static void handlePlayerDamaged(
            UnknownEntity entity,
            Memory memory,
            long gameTick
    ) {
        say(
                entity,
                chooseText("event.damage", 5),
                gameTick,
                ReactionKind.NORMAL
        );

        markProcessed(entity, memory, gameTick);
    }

    private static void handlePlayerDied(
            UnknownEntity entity,
            Memory memory,
            long gameTick
    ) {
        if ("creeper".equals(memory.context().get("cause"))) {
            say(
                    entity,
                    chooseText("event.creeper_death", 4),
                    gameTick,
                    ReactionKind.META
            );
        }

        markProcessed(entity, memory, gameTick);
    }

    private static void handleInteraction(
            UnknownEntity entity,
            Memory memory,
            long gameTick
    ) {
        String message = memory.context().get("message");
        String username = memory.context().get("username");

        InteractionKind kind =
                classifyInteraction(
                        normalize(message)
                );

        InteractionResponse response =
                chooseInteractionResponse(
                        message,
                        username,
                        entity,
                        memory
                );

        saySequence(
                entity,
                expandInteractionResponse(
                        kind,
                        response
                ),
                gameTick
        );

        markProcessed(entity, memory, gameTick);
    }

    private static void handlePlayerSeen(
            UnknownEntity entity,
            Memory memory,
            long gameTick
    ) {
        if (randomChance(0.18)) {
            say(
                    entity,
                    chooseText("seen", 6),
                    gameTick,
                    ReactionKind.NORMAL
            );

            markProcessed(entity, memory, gameTick);
            return;
        }

        entity.markMemoryProcessed(memory);
    }

    private static void handlePlayerReturned(
            UnknownEntity entity,
            Memory memory,
            long gameTick
    ) {
        if (randomChance(0.50)) {
            String username = memory.context().get("username");

            say(
                    entity,
                    chooseText(
                            "returned",
                            6,
                            username
                    ),
                    gameTick,
                    ReactionKind.NORMAL
            );

            markProcessed(entity, memory, gameTick);
            return;
        }

        entity.markMemoryProcessed(memory);
    }

    private static void handlePlayerLookedAway(
            UnknownEntity entity,
            Memory memory,
            long gameTick
    ) {
        UnknownEntity.Mood mood = entity.mood();

        if (mood == UnknownEntity.Mood.CURIOUS
                && randomChance(0.25)) {
            say(
                    entity,
                    chooseText("curious", 8),
                    gameTick
            );

            markProcessed(entity, memory, gameTick);
            return;
        }

        if (mood == UnknownEntity.Mood.ANNOYED
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
            UnknownEntity entity,
            Memory memory,
            long gameTick
    ) {
        entity.markMemoryProcessed(memory);
        entity.markAction(gameTick);
    }

    private static InteractionResponse chooseInteractionResponse(
            String message,
            String username,
            UnknownEntity entity,
            Memory memory
    ) {
        String normalized = normalize(message);
        int interactionCount = entity.interactionCount();
        int annoyanceCount = entity.annoyanceCount();
        UnknownEntity.Mood mood = entity.mood();
        boolean sillyMode = Config.get().chatEntitySillyMode;

        InteractionKind kind =
                classifyInteraction(normalized);

        return switch (kind) {
            case CONTROL -> response(
                    chooseText("control", 6),
                    ReactionKind.NORMAL
            );
            case UNSETTLING -> response(
                    chooseText("unsettling", 9),
                    ReactionKind.UNSETTLING
            );
            case GREETING -> response(
                    chooseGreetingResponse(
                            interactionCount,
                            username
                    ),
                    ReactionKind.GREETING
            );
            case IDENTITY -> response(
                    interactionCount >= 5
                            ? chooseText("identity.again", 4)
                            : chooseText("identity", 8),
                    ReactionKind.IDENTITY
            );
            case ACTIVITY -> response(
                    chooseText("activity", 6),
                    ReactionKind.NORMAL
            );
            case WELLBEING -> response(
                    chooseText("wellbeing", 6),
                    ReactionKind.NORMAL
            );
            case LOCATION -> response(
                    chooseText("location", 6),
                    ReactionKind.NORMAL
            );
            case NULL -> response(
                    chooseNullResponse(entity),
                    ReactionKind.NULL
            );
            case CONFUSED -> response(
                    chooseText("confused", 6),
                    ReactionKind.NORMAL
            );
            case INSULT -> response(
                    chooseText("insult", 6),
                    ReactionKind.ANNOYED
            );
            case THANKS -> response(
                    chooseText("thanks", 6),
                    ReactionKind.NORMAL
            );
            case APOLOGY -> response(
                    chooseText("apology", 6),
                    ReactionKind.NORMAL
            );
            case SLEEP -> response(
                    chooseSleepResponse(entity, memory),
                    ReactionKind.SLEEP
            );
            case QUESTION -> response(
                    chooseText("question", 6),
                    ReactionKind.QUESTION
            );
            case NORMAL -> response(
                    chooseNormalResponse(
                            interactionCount,
                            annoyanceCount,
                            mood,
                            sillyMode
                    ),
                    ReactionKind.NORMAL
            );
        };
    }

    private static List<InteractionResponse> expandInteractionResponse(
            InteractionKind kind,
            InteractionResponse response
    ) {
        if (!randomChance(multiResponseChance(kind))) {
            return List.of(response);
        }

        InteractionResponse followUp =
                chooseInteractionFollowUp(
                        kind,
                        response.reactionKind()
                );

        return followUp == null
                ? List.of(response)
                : List.of(response, followUp);
    }

    private static double multiResponseChance(
            InteractionKind kind
    ) {
        return switch (kind) {
            case GREETING -> 0.08;
            case IDENTITY -> 0.12;
            case ACTIVITY, WELLBEING, LOCATION -> 0.15;
            case NULL -> 0.10;
            case CONFUSED -> 0.14;
            case UNSETTLING -> 0.10;
            case QUESTION -> 0.18;
            case SLEEP -> 0.20;
            case CONTROL, INSULT, THANKS, APOLOGY -> 0.07;
            case NORMAL -> 0.15;
        };
    }

    private static InteractionResponse chooseInteractionFollowUp(
            InteractionKind kind,
            ReactionKind reactionKind
    ) {
        String prefix = switch (kind) {
            case CONTROL -> "control.followup";
            case UNSETTLING -> "unsettling.followup";
            case GREETING -> "greeting.followup";
            case IDENTITY -> "identity.followup";
            case ACTIVITY -> "activity.followup";
            case WELLBEING -> "wellbeing.followup";
            case LOCATION -> "location.followup";
            case NULL -> "null.followup";
            case CONFUSED -> "confused.followup";
            case INSULT -> "insult.followup";
            case THANKS -> "thanks.followup";
            case APOLOGY -> "apology.followup";
            case QUESTION -> "question.followup";
            case SLEEP -> "sleep.returned.followup";
            case NORMAL -> "normal.followup";
        };

        int count = switch (kind) {
            case CONTROL, INSULT, THANKS, APOLOGY -> 4;
            default -> 6;
        };

        if (kind == InteractionKind.SLEEP
                && prefix.equals("sleep.returned.followup")) {
            return new InteractionResponse(
                    chooseText(prefix, count),
                    reactionKind
            );
        }

        return new InteractionResponse(
                chooseText(prefix, count),
                reactionKind
        );
    }

    private static void saySequence(
            UnknownEntity entity,
            List<InteractionResponse> responses,
            long gameTick
    ) {
        long nextStartTick = gameTick;

        for (InteractionResponse response : responses) {
            UnknownEntityResponseTiming.Timing timing =
                    UnknownEntityResponseTiming.calculate(
                            entity,
                            response.message(),
                            response.reactionKind()
                    );

            DebugLogger.debug(
                    "[ChatEntityBrain] scheduling speech id={} startTick={} thinkingTicks={} typingTicks={} kind={} message={}",
                    entity.id(),
                    nextStartTick,
                    timing.thinkingTicks(),
                    timing.typingTicks(),
                    response.reactionKind(),
                    response.message()
            );

            entity.queueResponse(
                    response.message(),
                    response.reactionKind(),
                    nextStartTick,
                    timing.thinkingTicks(),
                    timing.typingTicks()
            );

            nextStartTick +=
                    timing.thinkingTicks()
                            + timing.typingTicks()
                            + ThreadLocalRandom.current().nextLong(6, 19);
        }
    }

    private static String chooseSleepResponse(
            UnknownEntity entity,
            Memory memory
    ) {
        long idleTicks =
                parseLongContext(
                        memory
                );

        if (idleTicks >= LONG_IDLE_BEFORE_SLEEP_RESPONSE_TICKS) {
            return chooseText("sleep.returned", 6);
        }

        return chooseText("wellbeing", 6);
    }

    private static long parseLongContext(
            Memory memory
    ) {
        String value = memory.context().get("idleTicksBeforeInteraction");

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
            UnknownEntity entity
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
            UnknownEntity.Mood mood,
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
            return chooseText("out_of_place", 8);
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
                && mood == UnknownEntity.Mood.PLAYFUL) {
            return chooseText("playful", 6);
        }

        if (mood == UnknownEntity.Mood.ANNOYED) {
            return chooseText(
                    "annoyed.interaction",
                    6
            );
        }

        if (mood == UnknownEntity.Mood.CURIOUS) {
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
        if (UnknownEntityTriggerRegistry.matches("control", normalized)) {
            return InteractionKind.CONTROL;
        }

        if (UnknownEntityTriggerRegistry.matches("unsettling", normalized)) {
            return InteractionKind.UNSETTLING;
        }

        if (UnknownEntityTriggerRegistry.matches("greeting", normalized)) {
            return InteractionKind.GREETING;
        }

        if (UnknownEntityTriggerRegistry.matches("identity", normalized)) {
            return InteractionKind.IDENTITY;
        }

        if (UnknownEntityTriggerRegistry.matches("activity", normalized)) {
            return InteractionKind.ACTIVITY;
        }

        if (UnknownEntityTriggerRegistry.matches("wellbeing", normalized)) {
            return InteractionKind.WELLBEING;
        }

        if (UnknownEntityTriggerRegistry.matches("sleep", normalized)) {
            return InteractionKind.SLEEP;
        }

        if (UnknownEntityTriggerRegistry.matches("location", normalized)) {
            return InteractionKind.LOCATION;
        }

        if (UnknownEntityTriggerRegistry.matches("null", normalized)) {
            return InteractionKind.NULL;
        }

        if (UnknownEntityTriggerRegistry.matches("insult", normalized)) {
            return InteractionKind.INSULT;
        }

        if (UnknownEntityTriggerRegistry.matches("thanks", normalized)) {
            return InteractionKind.THANKS;
        }

        if (UnknownEntityTriggerRegistry.matches("apology", normalized)) {
            return InteractionKind.APOLOGY;
        }

        if (UnknownEntityTriggerRegistry.matches("confused", normalized)) {
            return InteractionKind.CONFUSED;
        }

        return UnknownEntityTriggerRegistry.matches("question", normalized)
                ? InteractionKind.QUESTION
                : InteractionKind.NORMAL;
    }

    private static String normalize(String message) {
        return message == null
                ? ""
                : message.strip().toLowerCase(Locale.ROOT);
    }

    private static void say(
            UnknownEntity entity,
            String message,
            long gameTick
    ) {
        say(
                entity,
                message,
                gameTick,
                ReactionKind.NORMAL
        );
    }

    private static void say(
            UnknownEntity entity,
            String message,
            long gameTick,
            ReactionKind reactionKind
    ) {
        UnknownEntityResponseTiming.Timing timing =
                UnknownEntityResponseTiming.calculate(
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

    private static InteractionResponse response(
            String message,
            ReactionKind reactionKind
    ) {
        return new InteractionResponse(
                message,
                reactionKind
        );
    }

    private static boolean randomChance(double chance) {
        return ThreadLocalRandom.current()
                .nextDouble()
                < chance;
    }
}
