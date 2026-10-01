package me.p0x38.fuckinguselessmod.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class ReplacementRules {
    private ReplacementRules() {
    }

    public static List<String> applyLiteral(
            String input,
            List<String> rules,
            boolean caseInsensitive
    ) {
        String result = input;

        if (rules == null) {
            return result;
        }

        for (String rule : rules) {
            int separator = rule == null ? -1 : rule.indexOf('=');

            if (separator <= 0) {
                continue;
            }

            String from = rule.substring(0, separator);
            String to = rule.substring(separator + 1);

            if (from.isEmpty()) {
                continue;
            }

            Pattern pattern = Pattern.compile(
                    Pattern.quote(from),
                    caseInsensitive ? Pattern.CASE_INSENSITIVE_UNICODE : 0
            );
            result = pattern.matcher(result).replaceAll(
                    java.util.regex.Matcher.quoteReplacement(to)
            );
        }

        return result;
    }

    public static String applyWords(
            String input,
            List<String> rules,
            boolean caseInsensitive
    ) {
        String result = input;

        if (rules == null) {
            return result;
        }

        for (String rule : rules) {
            int separator = rule == null ? -1 : rule.indexOf('=');

            if (separator <= 0) {
                continue;
            }

            String from = rule.substring(0, separator).trim();
            String to = rule.substring(separator + 1);

            if (from.isEmpty()) {
                continue;
            }

            String flags = caseInsensitive
                    ? "(?iu)"
                    : "";

            Pattern pattern = Pattern.compile(
                    flags + "\\b" + Pattern.quote(from) + "\\b"
            );
            result = pattern.matcher(result).replaceAll(
                    java.util.regex.Matcher.quoteReplacement(to)
            );
        }

        return result;
    }

    public static List<String> copyOrDefault(
            List<String> rules,
            List<String> defaults
    ) {
        if (rules == null || rules.isEmpty()) {
            return new ArrayList<>(defaults);
        }

        return new ArrayList<>(rules);
    }
}
