package me.p0x38.fuckinguselessmod.effects;

import me.p0x38.fuckinguselessmod.Config;

import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Uwuifier {
    private static final Pattern N_VOWEL =
            Pattern.compile("n([aeiou])", Pattern.CASE_INSENSITIVE);

    private static final Pattern STUTTER_WORD =
            Pattern.compile("(?i)\\b([a-zA-Z])([a-zA-Z]+)");

    private Uwuifier() {
    }

    public static String apply(String input, Config.Data config) {
        String result = switch (config.textCase) {
            case PRESERVE -> input;
            case LOWERCASE -> input.toLowerCase();
            case UPPERCASE -> input.toUpperCase();
        };

        if (config.uwuifierReplaceRl) {
            result = result.replace('r', 'w').replace('l', 'w')
                    .replace('R', 'W').replace('L', 'W');
        }

        if (config.uwuifierReplaceNVowel) {
            Matcher matcher = N_VOWEL.matcher(result);
            result = matcher.replaceAll("ny$1");
        }

        if (config.uwuifierReplaceOve) {
            result = result.replace("ove", "uv")
                    .replace("Ove", "Uv")
                    .replace("OVE", "UV");
        }

        if (config.uwuifierReplaceTh) {
            result = result.replace("th", "d")
                    .replace("Th", "D")
                    .replace("TH", "D");
        }

        if (config.uwuifierReplaceYou) {
            result = result.replace("you", "yuo")
                    .replace("You", "Yuo")
                    .replace("YOU", "YUO");
        }

        Random random = new Random();

        if (config.uwuifierStutterEnabled
                && random.nextFloat() < config.uwuifierStutterChance) {
            result = stutter(result, random);
        }

        if (config.uwuifierActionsEnabled
                && random.nextFloat() < config.uwuifierActionChance) {
            result = appendRandom(result, config.uwuifierActionTexts, random);
        }

        if (config.uwuifierEmoticonsEnabled
                && random.nextFloat() < config.uwuifierEmoticonChance) {
            result = appendRandom(result, config.uwuifierEmoticons, random);
        }

        return result;
    }

    private static String stutter(String input, Random random) {
        Matcher matcher = STUTTER_WORD.matcher(input);

        if (!matcher.find()) {
            return input;
        }

        String prefix = matcher.group(1);
        String replacement = prefix + "-" + matcher.group(0);

        return matcher.replaceFirst(
                Matcher.quoteReplacement(replacement)
        );
    }

    private static String appendRandom(
            String input,
            List<String> values,
            Random random
    ) {
        if (values == null || values.isEmpty()) {
            return input;
        }

        String value = values.get(random.nextInt(values.size()));
        if (value == null || value.isBlank()) {
            return input;
        }

        return input + " " + value;
    }
}
