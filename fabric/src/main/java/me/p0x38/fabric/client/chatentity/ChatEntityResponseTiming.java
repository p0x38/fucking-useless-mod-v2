package me.p0x38.fabric.client.chatentity;

import java.util.concurrent.ThreadLocalRandom;

public final class ChatEntityResponseTiming {
    private ChatEntityResponseTiming() {}

    public record Timing(
            long thinkingTicks,
            long typingTicks
    ) {}

    public static Timing calculate(
            ChatEntity entity,
            String message,
            ChatEntity.ReactionKind reactionKind
    ) {
        if (message == null || message.isBlank()) {
            return new Timing(1, 1);
        }

        double thinkingBaseSeconds =
                switch (reactionKind) {
                    case GREETING -> 0.18;
                    case IDENTITY -> 0.55;
                    case QUESTION -> 0.65;
                    case UNSETTLING -> 1.15;
                    case OUT_OF_PLACE -> 0.50;
                    case PLAYFUL -> 0.20;
                    case ANNOYED -> 0.10;
                    case META -> 0.90;
                    case NORMAL -> 0.40;
                };

        double moodMultiplier =
                switch (entity.mood()) {
                    case CALM -> 1.0;
                    case CURIOUS -> 0.90;
                    case PLAYFUL -> 0.75;
                    case ANNOYED -> 0.65;
                    case AFRAID -> 1.50;
                };

        double thinkingVariation =
                0.85
                        + ThreadLocalRandom.current().nextDouble()
                        * 0.35;

        double thinkingSeconds =
                Math.max(
                        0.20,
                        thinkingBaseSeconds
                                * moodMultiplier
                                * thinkingVariation
                );

        double typingSeconds = getTypingSeconds(entity, message);

        double punctuationPause = 0.0;

        for (int i = 0; i < message.length(); i++) {
            punctuationPause += switch (message.charAt(i)) {
                case ',' -> 0.06;
                case '.' -> 0.08;
                case '?' -> 0.14;
                case '!' -> 0.13;
                case ':' -> 0.07;
                case ';' -> 0.09;
                case '…' -> 0.22;
                default -> 0.0;
            };
        }

        double hesitationSeconds =
                ThreadLocalRandom.current().nextDouble(
                        0.0,
                        0.35
                );

        double typingVariation =
                0.90
                        + ThreadLocalRandom.current().nextDouble()
                        * 0.20;

        typingSeconds =
                Math.max(
                        0.15,
                        (typingSeconds
                                + punctuationPause
                                + hesitationSeconds)
                                * typingVariation
                );

        if (reactionKind == ChatEntity.ReactionKind.UNSETTLING
                || reactionKind == ChatEntity.ReactionKind.META) {
            typingSeconds +=
                    ThreadLocalRandom.current().nextDouble(
                            0.0,
                            0.45
                    );
        }

        long thinkingTicks =
                Math.max(
                        1,
                        Math.round(thinkingSeconds * 20.0)
                );

        long typingTicks =
                Math.max(
                        1,
                        Math.round(typingSeconds * 20.0)
                );

        return new Timing(
                thinkingTicks,
                typingTicks
        );
    }

    private static double getTypingSeconds(ChatEntity entity, String message) {
        long characterCount =
                message.codePointCount(
                        0,
                        message.length()
                );

        double charactersPerSecond =
                switch (entity.mood()) {
                    case CALM -> 10.5;
                    case CURIOUS -> 9.5;
                    case PLAYFUL -> 14.0;
                    case ANNOYED -> 18.0;
                    case AFRAID -> 6.5;
                };

        return Math.min(
                4.5,
                characterCount / charactersPerSecond
        );
    }
}
