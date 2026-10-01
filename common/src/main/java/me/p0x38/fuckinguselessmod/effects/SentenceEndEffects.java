package me.p0x38.fuckinguselessmod.effects;

import me.p0x38.fuckinguselessmod.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
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
        Random random = new Random();
        int last = 0;

        while (matcher.find()) {
            output.append(input, last, matcher.start());
            output.append(buildEffects(config.sentenceEndEffects, random));
            last = matcher.end();
        }

        output.append(input, last, input.length());
        return output.toString();
    }

    private static String buildEffects(
            List<Config.SentenceEndEffect> configured,
            Random random
    ) {
        List<Config.SentenceEndEffect> selected = new ArrayList<>();

        for (Config.SentenceEndEffect effect : configured) {
            if (random.nextBoolean()) {
                selected.add(effect);
            }
        }

        if (selected.isEmpty()) {
            return "";
        }

        Collections.shuffle(selected, random);

        StringBuilder result = new StringBuilder();

        for (Config.SentenceEndEffect effect : selected) {
            switch (effect) {
                case TILDE -> result.append("~");
                case ELLIPSIS -> result.append("...");
                case EXCLAMATION -> result.append("!");
            }
        }

        return result.toString();
    }
}
