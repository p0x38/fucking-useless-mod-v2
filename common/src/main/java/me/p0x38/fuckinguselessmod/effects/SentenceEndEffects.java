package me.p0x38.fuckinguselessmod.effects;

import me.p0x38.fuckinguselessmod.Config;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SentenceEndEffects {
    private static final Pattern SENTENCE_END =
            Pattern.compile("[.!?]+(?=\\s|$)");

    private SentenceEndEffects() {
    }

    public static String apply(String input, Config.Data config) {
        if (input.isEmpty()
                || !config.sentenceEndEffectsEnabled
                || config.sentenceEndEffects == null
                || config.sentenceEndEffects.isEmpty()) {
            return input;
        }

        Matcher matcher = SENTENCE_END.matcher(input);
        StringBuilder output = new StringBuilder(
                input.length() + config.sentenceEndEffects.size() * 4
        );

        int last = 0;

        while (matcher.find()) {
            output.append(input, last, matcher.end());

            for (Config.SentenceEndEffect effect : config.sentenceEndEffects) {
                output.append(marker(effect));
            }

            last = matcher.end();
        }

        output.append(input, last, input.length());
        return output.toString();
    }

    private static String marker(Config.SentenceEndEffect effect) {
        return switch (effect) {
            case TILDE -> "~";
            case ELLIPSIS -> "...";
            case EXCLAMATION -> "!";
        };
    }
}
