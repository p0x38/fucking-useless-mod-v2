package me.p0x38.fuckinguselessmod.effects;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class RuleCondition {
    private static final Pattern URL = Pattern.compile(
            "(?i)^(?:[a-z][a-z0-9+.-]*://|www\\.)"
    );
    private static final Pattern WORD = Pattern.compile(
            "[\\p{L}\\p{N}]+(?:['’-][\\p{L}\\p{N}]+)*"
    );
    private static final Pattern LINE_BREAK = Pattern.compile(
            "\\R"
    );

    private RuleCondition() {
    }

    public static boolean evaluate(
            String condition,
            String original,
            String current,
            boolean caseInsensitive
    ) {
        if (condition == null || condition.isBlank()) {
            return true;
        }

        String normalized = condition.trim();

        if (normalized.startsWith("!")) {
            return !evaluate(
                    normalized.substring(1),
                    original,
                    current,
                    caseInsensitive
            );
        }

        String lower = normalized.toLowerCase(Locale.ROOT);

        return switch (lower) {
            case "always", "true" -> true;
            case "never", "false" -> false;
            case "random" -> random(0.5);
            case "changed" -> !current.equals(original);
            case "unchanged" -> current.equals(original);
            case "empty" -> current.isEmpty();
            case "nonempty" -> !current.isEmpty();
            case "has_uppercase" -> current.chars().anyMatch(Character::isUpperCase);
            case "has_lowercase" -> current.chars().anyMatch(Character::isLowerCase);
            case "has_digit" -> current.chars().anyMatch(Character::isDigit);
            case "has_letter" -> current.chars().anyMatch(Character::isLetter);
            case "has_non_ascii" -> current.codePoints().anyMatch(codePoint -> codePoint > 0x7F);
            case "has_whitespace" -> current.chars().anyMatch(Character::isWhitespace);
            case "has_punctuation" -> current.codePoints().anyMatch(
                    codePoint -> !Character.isLetterOrDigit(codePoint)
                            && !Character.isWhitespace(codePoint)
            );
            case "has_question" -> current.indexOf('?') >= 0;
            case "has_exclamation" -> current.indexOf('!') >= 0;
            case "has_line_break" -> LINE_BREAK.matcher(current).find();
            case "is_url" -> URL.matcher(current.trim()).find();
            case "is_mention" -> current.trim().startsWith("@");
            case "is_hashtag" -> current.trim().startsWith("#");
            default -> evaluateArgument(
                    normalized,
                    original,
                    current,
                    caseInsensitive
            );
        };
    }

    private static boolean evaluateArgument(
            String condition,
            String original,
            String current,
            boolean caseInsensitive
    ) {
        int separator = condition.indexOf(':');
        if (separator <= 0) {
            return false;
        }

        String operator = condition.substring(0, separator).trim()
                .toLowerCase(Locale.ROOT);
        String value = condition.substring(separator + 1);

        if (value.isEmpty()) {
            return false;
        }

        return switch (operator) {
            case "random", "chance" -> parseChance(value);
            case "contains", "regex" -> find(value, current, caseInsensitive);
            case "not_contains" -> !find(value, current, caseInsensitive);
            case "matches", "regex_matches" -> matches(value, current, caseInsensitive);
            case "not_matches", "regex_not_matches" -> !matches(value, current, caseInsensitive);
            case "original_contains", "original_regex" ->
                    find(value, original, caseInsensitive);
            case "original_matches", "original_regex_matches" ->
                    matches(value, original, caseInsensitive);
            case "contains_text" -> containsText(value, current, caseInsensitive);
            case "not_contains_text" -> !containsText(value, current, caseInsensitive);
            case "starts_with" -> startsWith(value, current, caseInsensitive);
            case "ends_with" -> endsWith(value, current, caseInsensitive);
            case "length", "chars", "character_count" ->
                    compareNumeric(current.codePointCount(0, current.length()), value);
            case "word_count", "words" ->
                    compareNumeric(countWords(current), value);
            case "line_count", "lines" ->
                    compareNumeric(countLines(current), value);
            case "not" -> !evaluate(
                    value,
                    original,
                    current,
                    caseInsensitive
            );
            default -> false;
        };
    }

    private static boolean parseChance(String value) {
        try {
            double chance = Double.parseDouble(value.trim());
            return random(chance);
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static boolean random(double chance) {
        if (chance <= 0.0) {
            return false;
        }
        if (chance >= 1.0) {
            return true;
        }
        return ThreadLocalRandom.current().nextDouble() < chance;
    }

    private static boolean find(
            String regex,
            String input,
            boolean caseInsensitive
    ) {
        try {
            return compile(regex, caseInsensitive).matcher(input).find();
        } catch (PatternSyntaxException exception) {
            logInvalid(regex);
            return false;
        }
    }

    private static boolean matches(
            String regex,
            String input,
            boolean caseInsensitive
    ) {
        try {
            return compile(regex, caseInsensitive).matcher(input).matches();
        } catch (PatternSyntaxException exception) {
            logInvalid(regex);
            return false;
        }
    }

    private static boolean containsText(
            String value,
            String input,
            boolean caseInsensitive
    ) {
        return caseInsensitive
                ? input.toLowerCase(Locale.ROOT).contains(value.toLowerCase(Locale.ROOT))
                : input.contains(value);
    }

    private static boolean startsWith(
            String value,
            String input,
            boolean caseInsensitive
    ) {
        if (caseInsensitive) {
            return input.regionMatches(true, 0, value, 0, value.length());
        }
        return input.startsWith(value);
    }

    private static boolean endsWith(
            String value,
            String input,
            boolean caseInsensitive
    ) {
        if (value.length() > input.length()) {
            return false;
        }

        int offset = input.length() - value.length();
        if (caseInsensitive) {
            return input.regionMatches(true, offset, value, 0, value.length());
        }
        return input.endsWith(value);
    }

    private static int compareNumeric(int actual, String expression) {
        String value = expression.trim();
        String operator = "=";

        if (value.startsWith(">=") || value.startsWith("<=")) {
            operator = value.substring(0, 2);
            value = value.substring(2).trim();
        } else if (value.startsWith(">") || value.startsWith("<") || value.startsWith("=")) {
            operator = value.substring(0, 1);
            value = value.substring(1).trim();
        }

        try {
            int expected = Integer.parseInt(value);
            return switch (operator) {
                case ">" -> actual > expected;
                case ">=" -> actual >= expected;
                case "<" -> actual < expected;
                case "<=" -> actual <= expected;
                default -> actual == expected;
            };
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static int countWords(String input) {
        var matcher = WORD.matcher(input);
        int count = 0;

        while (matcher.find()) {
            count++;
        }

        return count;
    }

    private static int countLines(String input) {
        if (input.isEmpty()) {
            return 0;
        }

        return input.split("\r", -1).length;
    }

    private static Pattern compile(String regex, boolean caseInsensitive) {
        int flags = caseInsensitive
                ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
                : 0;
        return Pattern.compile(regex, flags);
    }

    private static void logInvalid(String regex) {
        System.err.println(
                "[Fucking Useless Mod] Invalid effect condition regex: "
                        + regex
        );
    }
}
