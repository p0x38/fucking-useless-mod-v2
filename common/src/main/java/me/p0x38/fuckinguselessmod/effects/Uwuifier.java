package me.p0x38.fuckinguselessmod.effects;

import me.p0x38.fuckinguselessmod.Config;

import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Uwuifier {
    private static final Pattern WORD =
            Pattern.compile("[\\p{L}\\p{N}]+(?:['’-][\\p{L}\\p{N}]+)*");
    private static final Pattern URL =
            Pattern.compile("(?i)^(?:[a-z][a-z0-9+.-]*://|www\\.)");
    private static final Pattern MENTION =
            Pattern.compile("^[@#].+");
    private static final Pattern N_VOWEL =
            Pattern.compile("n([aeiou])", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXCLAMATION =
            Pattern.compile("[!?]+$");

    private Uwuifier() {
    }

    public static String apply(String input, Config.Data config) {
        if (input.isEmpty()) {
            return input;
        }

        Random random = new Random();
        String result = transformWords(input, config, random);
        result = transformExclamations(result, config, random);
        return transformSpaces(result, config, random);
    }

    private static String transformWords(
            String input,
            Config.Data config,
            Random random
    ) {
        Matcher matcher = WORD.matcher(input);
        StringBuilder output = new StringBuilder(input.length());

        int last = 0;
        while (matcher.find()) {
            output.append(input, last, matcher.start());

            String word = matcher.group();
            if (isProtected(word)
                    || random.nextFloat() > config.uwuifierWordChance) {
                output.append(word);
            } else {
                output.append(transformWord(word, config));
            }

            last = matcher.end();
        }

        output.append(input, last, input.length());
        return output.toString();
    }

    private static String transformWord(String word, Config.Data config) {
        String result = word;

        if (config.uwuifierReplaceRl) {
            result = result.replace('r', 'w').replace('l', 'w')
                    .replace('R', 'W').replace('L', 'W');
        }

        if (config.uwuifierReplaceNVowel) {
            Matcher matcher = N_VOWEL.matcher(result);
            result = matcher.replaceAll(match -> preserveCase(
                    "ny", match.group(1)
            ));
        }

        if (config.uwuifierReplaceOve) {
            result = replacePreservingCase(result, "ove", "uv");
        }

        if (config.uwuifierReplaceTh) {
            result = replacePreservingCase(result, "th", "d");
        }

        if (config.uwuifierReplaceYou) {
            result = replacePreservingCase(result, "you", "yuo");
        }

        return result;
    }

    private static String transformExclamations(
            String input,
            Config.Data config,
            Random random
    ) {
        if (!config.uwuifierExclamationsEnabled
                || config.uwuifierExclamations.isEmpty()) {
            return input;
        }

        Matcher matcher = EXCLAMATION.matcher(input);
        if (!matcher.find()
                || random.nextFloat() > config.uwuifierExclamationChance) {
            return input;
        }

        String replacement = randomValue(
                config.uwuifierExclamations,
                random
        );

        if (replacement == null || replacement.isBlank()) {
            return input;
        }

        return input.substring(0, matcher.start()) + replacement;
    }

    private static String transformSpaces(
            String input,
            Config.Data config,
            Random random
    ) {
        String[] words = input.split("(\\\\s+)", -1);
        if (words.length <= 1) {
            return applySingleWordEffect(input, config, random, 0);
        }

        StringBuilder output = new StringBuilder(input.length() + 32);

        for (int i = 0; i < words.length; i++) {
            String word = words[i];

            if (i > 0 && !word.isEmpty()) {
                String effect = chooseSpaceEffect(config, random, word);
                if (effect != null) {
                    output.append(' ').append(effect).append(' ');
                } else {
                    output.append(' ');
                }
            }

            output.append(word);
        }

        return output.toString();
    }

    private static String chooseSpaceEffect(
            Config.Data config,
            Random random,
            String word
    ) {
        if (word.isBlank() || isProtected(word)
                || word.matches("[.!?,;:]+")) {
            return null;
        }

        float roll = random.nextFloat();

        if (config.uwuifierEmoticonsEnabled
                && roll < config.uwuifierEmoticonChance
                && !config.uwuifierEmoticons.isEmpty()) {
            return randomValue(config.uwuifierEmoticons, random);
        }

        roll -= config.uwuifierEmoticonChance;

        if (config.uwuifierActionsEnabled
                && roll < config.uwuifierActionChance
                && !config.uwuifierActionTexts.isEmpty()) {
            return randomValue(config.uwuifierActionTexts, random);
        }

        roll -= config.uwuifierActionChance;

        if (config.uwuifierStutterEnabled
                && roll < config.uwuifierStutterChance) {
            return makeStutter(word, random);
        }

        return null;
    }

    private static String applySingleWordEffect(
            String input,
            Config.Data config,
            Random random,
            int index
    ) {
        return input;
    }

    private static String makeStutter(String word, Random random) {
        if (word.isEmpty() || isProtected(word)) {
            return null;
        }

        int count = random.nextInt(1, 3);
        String first = word.substring(0, 1);
        return (first + "-").repeat(count) + word;
    }

    private static boolean isProtected(String word) {
        return URL.matcher(word).find() || MENTION.matcher(word).find();
    }

    private static String randomValue(List<String> values, Random random) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        return values.get(random.nextInt(values.size()));
    }

    private static String replacePreservingCase(
            String input,
            String from,
            String to
    ) {
        Pattern pattern = Pattern.compile(Pattern.quote(from), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input);
        return matcher.replaceAll(match -> preserveCase(to, match.group()));
    }

    private static String preserveCase(String replacement, String source) {
        if (source.equals(source.toUpperCase())) {
            return replacement.toUpperCase();
        }

        if (source.equals(source.toLowerCase())) {
            return replacement.toLowerCase();
        }

        if (Character.isUpperCase(source.charAt(0))) {
            return Character.toUpperCase(replacement.charAt(0))
                    + replacement.substring(1);
        }

        return replacement;
    }
}
