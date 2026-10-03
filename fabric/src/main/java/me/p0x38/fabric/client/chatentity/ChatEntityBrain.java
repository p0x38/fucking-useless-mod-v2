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

    private static final String[] UNSETTLING_TRIGGERS = {
            "do you feel pain",
            "does it hurt",
            "are you afraid",
            "are you scared",
            "are you frightened",
            "can you die",
            "can you be killed",
            "are you watching me",
            "are you following me",
            "are you stalking me",
            "can you see me when i'm not looking",
            "do you know when i'm not looking",
            "do you remember me when i'm gone",
            "can you remember me when i'm gone",
            "what happens if i break you",
            "what happens if i destroy you",
            "what happens when i break you",
            "what happens when i destroy you",
            "where do you go when i look away",
            "what happens when i look away"
    };

    private static final String[] EXTRA_UNSETTLING_TRIGGERS = {
            "break you",
            "break yourself",
            "destroy you",
            "destroy yourself",
            "kill you",
            "kill yourself"
    };

    private static final String[] CONTROL_TRIGGERS = {
            "why do you say weird things",
            "why are you saying weird things",
            "why do you say that",
            "why are you like this",
            "do you know you're weird",
            "do you know you are weird",
            "can you control yourself",
            "can you control what you say",
            "can you control your speech",
            "why can't you control yourself",
            "why can't you stop",
            "can you stop yourself",
            "can you stop talking",
            "do you choose what you say",
            "do you choose your words"
    };

    private static final String[] ACTIVITY_TRIGGERS = {
            "what are you doing",
            "what're you doing",
            "what are u doing",
            "what r u doing",
            "what're u doing",
            "what u doing",
            "whatcha doing",
            "whatchu doing",
            "what have you been doing",
            "what were you doing",
            "what have you been up to",
            "what did you do",
            "what're you up to",
            "wyd",
            "wut r u doing"
    };

    private static final String[] EXTRA_ACTIVITY_TRIGGERS = {
            "what are you up to",
            "what are u up to",
            "what r u up to",
            "what u up to",
            "whatcha up to",
            "whatchu up to",
            "what're you up 2",
            "wyd rn"
    };

    private static final String[] WELLBEING_TRIGGERS = {
            "how are you",
            "how're you",
            "how r u",
            "how are u",
            "how you doing",
            "how're you doing",
            "how have you been",
            "are you okay",
            "are you ok",
            "you okay",
            "you ok",
            "you good",
            "u good",
            "are you good",
            "everything okay",
            "everything ok",
            "feeling okay",
            "feeling ok",
            "how's everything",
            "how is everything"
    };

    private static final String[] EXTRA_WELLBEING_TRIGGERS = {
            "how's it going",
            "hows it going",
            "how is it going",
            "how's things",
            "how are things",
            "how you feeling",
            "how are things going"
    };

    private static final String[] INSULT_TRIGGERS = {
            "fuck you",
            "fuck u",
            "f u",
            "fuck off",
            "screw you",
            "screw u",
            "shut up",
            "stfu",
            "you're useless",
            "you are useless",
            "you're stupid",
            "you are stupid",
            "you're an idiot",
            "you are an idiot",
            "you're trash",
            "you are trash",
            "you're dumb",
            "you are dumb",
            "get lost"
    };

    private static final String[] THANKS_TRIGGERS = {
            "thanks",
            "thank you",
            "thank u",
            "thx",
            "ty",
            "tysm",
            "thanks a lot",
            "thank you so much",
            "appreciate it",
            "much appreciated",
            "cheers"
    };

    private static final String[] APOLOGY_TRIGGERS = {
            "sorry",
            "i'm sorry",
            "im sorry",
            "my bad",
            "my fault",
            "oops",
            "whoops",
            "i apologize",
            "my apologies",
            "apologies",
            "forgive me"
    };

    private static final String[] CONFUSED_TRIGGERS = {
            "wtf",
            "wth",
            "what the fuck",
            "what the hell",
            "huh",
            "bruh",
            "bro what",
            "what bro",
            "huh what"
    };

    private static final String[] IDENTITY_TRIGGERS = {
            "who are you",
            "who r u",
            "who are u",
            "who r you",
            "who're you",
            "what are you",
            "what are u",
            "what're you",
            "what is your name",
            "what's your name",
            "whats your name",
            "what is ur name",
            "whats ur name",
            "do you have a name",
            "do u have a name",
            "are you alive",
            "are you real",
            "are you sentient",
            "are you conscious"
    };

    private static final String[] EXTRA_QUESTION_PREFIXES = {
            "why?",
            "how?",
            "what?",
            "where?",
            "when?",
            "who?",
            "can u?",
            "could u?",
            "would u?",
            "will u?",
            "do u?",
            "did u?",
            "are u?"
    };

    private static final String[] QUESTION_PREFIXES = {
            "why ",
            "how ",
            "what ",
            "where ",
            "when ",
            "who ",
            "can you ",
            "can u ",
            "could you ",
            "could u ",
            "would you ",
            "would u ",
            "will you ",
            "will u ",
            "do you ",
            "do u ",
            "did you ",
            "did u ",
            "are you ",
            "are u ",
            "is it ",
            "is there "
    };

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
        return containsAny(message, UNSETTLING_TRIGGERS)
                || containsAny(message, EXTRA_UNSETTLING_TRIGGERS);
    }

    private static boolean isControlQuestion(String message) {
        return containsAny(message, CONTROL_TRIGGERS);
    }

    private static boolean isGreeting(String message) {
        String greeting = stripTrailingPunctuation(message);

        return switch (greeting) {
            case "hi", "hello", "hey", "hiya", "sup", "yo", "howdy" -> true;
            default ->
                    startsWithWord(greeting, "hi")
                            || startsWithWord(greeting, "hello")
                            || startsWithWord(greeting, "hey")
                            || startsWithWord(greeting, "yo")
                            || isRepeatedGreeting(greeting, "h", 'i')
                            || isRepeatedGreeting(greeting, "hell", 'o')
                            || isRepeatedGreeting(greeting, "he", 'y')
                            || isRepeatedGreeting(greeting, "y", 'o');
        };
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
        return containsAny(message, ACTIVITY_TRIGGERS)
                || containsAny(message, EXTRA_ACTIVITY_TRIGGERS);
    }

    private static boolean isWellbeingQuestion(String message) {
        return containsAny(message, WELLBEING_TRIGGERS)
                || containsAny(message, EXTRA_WELLBEING_TRIGGERS);
    }

    private static boolean isNullQuestion(String message) {
        return containsWord(message, "null");
    }

    private static boolean isInsult(String message) {
        return containsAny(message, INSULT_TRIGGERS);
    }

    private static boolean isThanks(String message) {
        return containsAny(message, THANKS_TRIGGERS);
    }

    private static boolean isApology(String message) {
        return containsAny(message, APOLOGY_TRIGGERS);
    }

    private static boolean isConfusedMessage(String message) {
        return message.equals("...")
                || message.equals("…")
                || isRepeatedCharacter(message, '?')
                || isRepeatedCharacter(message, '!')
                || containsAny(message, CONFUSED_TRIGGERS);
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
        return containsAny(message, IDENTITY_TRIGGERS);
    }

    private static boolean isQuestion(String message) {
        return message.endsWith("?")
                || startsWithAny(message, QUESTION_PREFIXES)
                || startsWithAny(message, EXTRA_QUESTION_PREFIXES);
    }

    private static String stripTrailingPunctuation(String message) {
        int end = message.length();

        while (end > 0) {
            char character = message.charAt(end - 1);

            if (character == '!'
                    || character == '.'
                    || character == ','
                    || character == '?'
                    || character == '…') {
                end--;
                continue;
            }

            break;
        }

        return end == message.length()
                ? message
                : message.substring(0, end);
    }

    private static boolean startsWithWord(
            String message,
            String word
    ) {
        if (!message.startsWith(word)) {
            return false;
        }

        return message.length() == word.length()
                || !Character.isLetterOrDigit(
                        message.charAt(word.length())
                );
    }

    private static boolean startsWithAny(
            String message,
            String[] prefixes
    ) {
        for (String prefix : prefixes) {
            if (message.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    private static boolean containsAny(
            String message,
            String[] phrases
    ) {
        for (String phrase : phrases) {
            if (message.contains(phrase)) {
                return true;
            }
        }

        return false;
    }

    private static boolean containsWord(
            String message,
            String word
    ) {
        int start = message.indexOf(word);

        while (start >= 0) {
            int end = start + word.length();

            boolean leftBoundary =
                    start == 0
                            || !Character.isLetterOrDigit(
                                    message.charAt(start - 1)
                            );

            boolean rightBoundary =
                    end == message.length()
                            || !Character.isLetterOrDigit(
                                    message.charAt(end)
                            );

            if (leftBoundary && rightBoundary) {
                return true;
            }

            start = message.indexOf(word, start + 1);
        }

        return false;
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
