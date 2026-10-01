package me.p0x38.fuckinguselessmod.effects;

import me.p0x38.fuckinguselessmod.Config;

import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Uwuifier {
    private static final Pattern WORD =
            Pattern.compile("[\\p{L}\\p{N}]+(?:['’-][\\p{L}\\p{N}]+)*");
    private static final Pattern PROTECTED =
            Pattern.compile(
                    "(?i)(?:"
                            + "\\b[a-z][a-z0-9+.-]*://[^\\s<>]+"
                            + "|\\bwww\\.[^\\s<>]+"
                            + "|\\b[\\p{L}\\p{N}._%+-]+@[\\p{L}\\p{N}.-]+\\.[A-Za-z]{2,}\\b"
                            + "|(?<![\\p{L}\\p{N}])[@#][\\p{L}\\p{N}_.-]+"
                            + ")"
            );
    private static final Pattern N_VOWEL =
            Pattern.compile("n([aeiou])", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXCLAMATION =
            Pattern.compile("[!?]+$");
    private static final Pattern WHITESPACE =
            Pattern.compile("\\s+");

    private Uwuifier() {
    }

    public static String apply(String input, Config.Data config) {
        if (input.isEmpty()) {
            return input;
        }

        Random random = new Random();
        String result = transformWordsAndSpaces(input, config, random);
        return transformExclamations(result, config, random);
    }

    private static String transformWordsAndSpaces(
            String input,
            Config.Data config,
            Random random
    ) {
        Matcher protectedMatcher = PROTECTED.matcher(input);
        StringBuilder output = new StringBuilder(input.length() + 32);

        int lastProtected = 0;

        while (protectedMatcher.find()) {
            appendTransformedSegment(
                    input,
                    lastProtected,
                    protectedMatcher.start(),
                    output,
                    config,
                    random
            );

            output.append(protectedMatcher.group());
            lastProtected = protectedMatcher.end();
        }

        appendTransformedSegment(
                input,
                lastProtected,
                input.length(),
                output,
                config,
                random
        );

        return output.toString();
    }

    private static void appendTransformedSegment(
            String input,
            int start,
            int end,
            StringBuilder output,
            Config.Data config,
            Random random
    ) {
        if (start >= end) {
            return;
        }

        String segment = input.substring(start, end);
        Matcher matcher = WORD.matcher(segment);
        int last = 0;

        while (matcher.find()) {
            String gap = segment.substring(last, matcher.start());
            String word = matcher.group();

            appendGapWithEffect(
                    gap,
                    word,
                    output,
                    config,
                    random
            );

            if (random.nextFloat() >= config.uwuifierWordChance) {
                output.append(word);
            } else {
                output.append(transformWord(word, config));
            }

            last = matcher.end();
        }

        output.append(segment, last, segment.length());
    }

    private static void appendGapWithEffect(
            String gap,
            String word,
            StringBuilder output,
            Config.Data config,
            Random random
    ) {
        if (gap.isEmpty() || !endsWithWhitespace(gap)) {
            output.append(gap);
            return;
        }

        int separatorStart = gap.length();

        while (separatorStart > 0
                && Character.isWhitespace(gap.charAt(separatorStart - 1))) {
            separatorStart--;
        }

        output.append(gap, 0, separatorStart);

        String separator = gap.substring(separatorStart);
        output.append(separator);

        String effect = chooseSpaceEffect(config, random, word);

        if (effect != null) {
            output.append(effect).append(separator);
        }
    }

    private static boolean endsWithWhitespace(String input) {
        return !input.isEmpty()
                && Character.isWhitespace(input.charAt(input.length() - 1));
    }

    private static String transformWord(
            String word,
            Config.Data config
    ) {
        String result = word;

        if (config.uwuifierReplaceRl) {
            result = result.replace('r', 'w').replace('l', 'w')
                    .replace('R', 'W').replace('L', 'W');
        }

        if (config.uwuifierReplaceNVowel) {
            Matcher matcher = N_VOWEL.matcher(result);
            result = matcher.replaceAll(match ->
                    preserveCase(
                            "ny" + match.group(1),
                            match.group()
                    )
            );
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
                || random.nextFloat() >= config.uwuifierExclamationChance) {
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

    private static String chooseSpaceEffect(
            Config.Data config,
            Random random,
            String word
    ) {
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

    private static String makeStutter(String word, Random random) {
        if (word.isEmpty()) {
            return null;
        }

        int count = random.nextInt(1, 3);
        int firstCodePoint = word.codePointAt(0);
        String first = new String(
                Character.toChars(firstCodePoint)
        );

        return (first + "-").repeat(count) + word;
    }

    private static String randomValue(
            List<String> values,
            Random random
    ) {
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
        Pattern pattern = Pattern.compile(
                Pattern.quote(from),
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(input);

        return matcher.replaceAll(
                match -> preserveCase(to, match.group())
        );
    }

    private static String preserveCase(
            String replacement,
            String source
    ) {
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
