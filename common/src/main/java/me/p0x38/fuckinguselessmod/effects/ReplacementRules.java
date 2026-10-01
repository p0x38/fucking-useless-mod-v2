package me.p0x38.fuckinguselessmod.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class ReplacementRules {
    private ReplacementRules() {
    }

    public static String applyRegex(
            String input,
            List<String> rules,
            boolean caseInsensitive
    ) {
        String result = input;

        if (rules == null) {
            return result;
        }

        for (String rule : rules) {
            ParsedRule parsed = parse(rule);

            if (parsed == null || !RuleCondition.evaluate(
                    parsed.condition(),
                    input,
                    result,
                    caseInsensitive
            )) {
                continue;
            }

            try {
                int flags = caseInsensitive
                        ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
                        : 0;

                Pattern pattern = Pattern.compile(
                        parsed.regex(),
                        flags
                );

                result = pattern.matcher(result).replaceAll(
                        parsed.replacement()
                );
            } catch (PatternSyntaxException exception) {
                System.err.println(
                        "[Fucking Useless Mod] Invalid effect regex: "
                                + parsed.regex()
                );
            } catch (IllegalArgumentException exception) {
                System.err.println(
                        "[Fucking Useless Mod] Invalid effect replacement: "
                                + exception.getMessage()
                );
            }
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

    private static ParsedRule parse(String rule) {
        if (rule == null || rule.isBlank()) {
            return null;
        }

        String expression = rule.trim();
        String condition = null;

        int conditionSeparator = expression.indexOf("::");
        if (conditionSeparator > 0) {
            condition = expression.substring(0, conditionSeparator).trim();
            expression = expression.substring(conditionSeparator + 2);
        }

        int separator = expression.indexOf("=>");

        if (separator < 0) {
            separator = expression.indexOf('=');
        }

        if (separator <= 0) {
            return null;
        }

        String regex = expression.substring(0, separator).trim();
        String replacement = expression.substring(
                separator + (expression.startsWith("=>", separator) ? 2 : 1)
        );

        if (regex.isEmpty()) {
            return null;
        }

        return new ParsedRule(condition, regex, replacement);
    }

    private record ParsedRule(
            String condition,
            String regex,
            String replacement
    ) {
    }
}
