package me.p0x38.fuckinguselessmod.effects;

import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class RuleCondition {
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
        String lower = normalized.toLowerCase(Locale.ROOT);

        return switch (lower) {
            case "always", "true" -> true;
            case "changed" -> !current.equals(original);
            case "unchanged" -> current.equals(original);
            case "empty" -> current.isEmpty();
            case "nonempty" -> !current.isEmpty();
            case "has_uppercase" -> current.chars().anyMatch(Character::isUpperCase);
            case "has_lowercase" -> current.chars().anyMatch(Character::isLowerCase);
            case "has_digit" -> current.chars().anyMatch(Character::isDigit);
            case "has_whitespace" -> current.chars().anyMatch(Character::isWhitespace);
            case "has_punctuation" -> current.codePoints().anyMatch(
                    codePoint -> !Character.isLetterOrDigit(codePoint)
                            && !Character.isWhitespace(codePoint)
            );
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
            case "contains" -> find(value, current, caseInsensitive);
            case "not_contains" -> !find(value, current, caseInsensitive);
            case "matches" -> matches(value, current, caseInsensitive);
            case "not_matches" -> !matches(value, current, caseInsensitive);
            case "starts_with" -> startsWith(value, current, caseInsensitive);
            case "ends_with" -> endsWith(value, current, caseInsensitive);
            case "original_contains" -> find(value, original, caseInsensitive);
            case "original_matches" -> matches(value, original, caseInsensitive);
            default -> false;
        };
    }

    private static boolean find(String regex, String input, boolean caseInsensitive) {
        try {
            return compile(regex, caseInsensitive).matcher(input).find();
        } catch (PatternSyntaxException exception) {
            logInvalid(regex);
            return false;
        }
    }

    private static boolean matches(String regex, String input, boolean caseInsensitive) {
        try {
            return compile(regex, caseInsensitive).matcher(input).matches();
        } catch (PatternSyntaxException exception) {
            logInvalid(regex);
            return false;
        }
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
