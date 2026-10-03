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
        NORMAL
    }

    private ChatEntityBrain() {
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
                    chooseText("curious", 8),
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
            ChatEntity entity
    ) {
        String normalized = normalize(message);
        int interactionCount = entity.interactionCount();
        int annoyanceCount = entity.annoyanceCount();
        ChatEntity.Mood mood = entity.mood();
        boolean sillyMode = Config.get().chatEntitySillyMode;

        InteractionKind kind =
                classifyInteraction(normalized);

        return switch (kind) {
            case CONTROL -> chooseText("control", 6);
            case UNSETTLING -> chooseText("unsettling", 8);
            case ACTIVITY -> chooseText("activity", 6);
            case WELLBEING -> chooseText("wellbeing", 6);
            case NULL -> chooseText("null", 8);
            case CONFUSED -> chooseText("confused", 6);
            case INSULT -> chooseText("insult", 6);
            case THANKS -> chooseText("thanks", 6);
            case APOLOGY -> chooseText("apology", 6);
            case GREETING ->
                    chooseGreetingResponse(
                            interactionCount,
                            username
                    );
            case IDENTITY ->
                    interactionCount >= 5
                            ? chooseText("identity.again", 4)
                            : chooseText("identity", 8);
            case QUESTION -> chooseText("question", 6);
            case NORMAL -> chooseNormalResponse(
                    interactionCount,
                    annoyanceCount,
                    mood,
                    sillyMode
            );
        };
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
                && mood == ChatEntity.Mood.PLAYFUL) {
            return chooseText("playful", 6);
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
        if (isControlQuestion(normalized)) {
            return InteractionKind.CONTROL;
        }

        if (isUnsettlingQuestion(normalized)) {
            return InteractionKind.UNSETTLING;
        }

        if (isGreeting(normalized)) {
            return InteractionKind.GREETING;
        }

        if (isIdentityQuestion(normalized)) {
            return InteractionKind.IDENTITY;
        }

        if (isActivityQuestion(normalized)) {
            return InteractionKind.ACTIVITY;
        }

        if (isWellbeingQuestion(normalized)) {
            return InteractionKind.WELLBEING;
        }

        if (isNullQuestion(normalized)) {
            return InteractionKind.NULL;
        }

        if (isInsult(normalized)) {
            return InteractionKind.INSULT;
        }

        if (isThanks(normalized)) {
            return InteractionKind.THANKS;
        }

        if (isApology(normalized)) {
            return InteractionKind.APOLOGY;
        }

        if (isConfusedMessage(normalized)) {
            return InteractionKind.CONFUSED;
        }

        if (isQuestion(normalized)) {
            return InteractionKind.QUESTION;
        }

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
        int end = message.length();

        while (end > 0) {
            char character =
                    message.charAt(end - 1);

            if (character == '!'
                    || character == '.'
                    || character == ','
                    || character == '?') {
                end--;
                continue;
            }

            break;
        }

        String greeting =
                message.substring(0, end);

        return isRepeatedGreeting(
                greeting,
                "h",
                'i'
        ) || isRepeatedGreeting(
                greeting,
                "hell",
                'o'
        ) || isRepeatedGreeting(
                greeting,
                "he",
                'y'
        ) || isRepeatedGreeting(
                greeting,
                "y",
                'o'
        ) || greeting.equals("hiya")
                || greeting.equals("sup")
                || greeting.equals("howdy");
    }

    private static boolean isRepeatedGreeting(
            String value,
            String prefix,
            char repeatedCharacter
    ) {
        if (!value.startsWith(prefix)
                || value.length() <= prefix.length()) {
            return false;
        }

        for (int i = prefix.length(); i < value.length(); i++) {
            if (value.charAt(i) != repeatedCharacter) {
                return false;
            }
        }

        return true;
    }

    private static boolean isActivityQuestion(String message) {
        return message.equals("what are you doing")
                || message.equals("whatcha doing")
                || message.equals("what're you doing")
                || message.equals("what are u doing")
                || message.equals("wyd");
    }

    private static boolean isWellbeingQuestion(String message) {
        return message.equals("how are you")
                || message.equals("how are you doing")
                || message.equals("how are u")
                || message.equals("are you okay")
                || message.equals("are you ok")
                || message.equals("you okay")
                || message.equals("you ok");
    }

    private static boolean isNullQuestion(String message) {
        return message.equals("do you know null")
                || message.equals("do you know about null")
                || message.equals("have you heard of null")
                || message.equals("what do you know about null")
                || message.equals("who is null");
    }

    private static boolean isInsult(String message) {
        return message.equals("fuck you")
                || message.startsWith("fuck you ")
                || message.equals("fuck off")
                || message.startsWith("fuck off ")
                || message.equals("screw you")
                || message.startsWith("screw you ")
                || message.equals("shut up")
                || message.startsWith("shut up ")
                || message.equals("you're an idiot")
                || message.equals("you are an idiot");
    }

    private static boolean isThanks(String message) {
        return message.equals("thanks")
                || message.equals("thank you")
                || message.equals("thx")
                || message.equals("ty")
                || message.equals("thank u")
                || message.equals("thanks a lot");
    }

    private static boolean isApology(String message) {
        return message.equals("sorry")
                || message.equals("i'm sorry")
                || message.equals("im sorry")
                || message.equals("my bad")
                || message.equals("oops")
                || message.equals("whoops");
    }

    private static boolean isConfusedMessage(String message) {
        return message.equals("...")
                || isRepeatedCharacter(message, '?')
                || isRepeatedCharacter(message, '!')
                || message.equals("wtf")
                || message.equals("bruh")
                || message.equals("huh")
                || message.equals("what");
    }

    private static boolean isRepeatedCharacter(
            String value,
            char character
    ) {
        if (value.length() < 2) {
            return false;
        }

        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) != character) {
                return false;
            }
        }

        return true;
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
        return message == null
                ? ""
                : message.trim().toLowerCase(Locale.ROOT);
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
