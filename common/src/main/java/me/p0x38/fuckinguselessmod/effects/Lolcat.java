package me.p0x38.fuckinguselessmod.effects;

import me.p0x38.fuckinguselessmod.Config;

import java.util.regex.Pattern;

public final class Lolcat {
    private static final Pattern WORD = Pattern.compile("[A-Za-z]+");

    private Lolcat() {}

    public static String apply(String input, Config.Data config) {
        if (!config.lolcatEnabled || Math.random() > config.lolcatChance) {
            return input;
        }

        String result = input
                .replaceAll("(?i)\\bthe\\b", "teh")
                .replaceAll("(?i)\\bthis\\b", "dis")
                .replaceAll("(?i)\\bthat\\b", "dat")
                .replaceAll("(?i)\\bwhat\\b", "wat")
                .replaceAll("(?i)\\bwith\\b", "wit")
                .replaceAll("(?i)\\bis\\b", "iz")
                .replaceAll("(?i)\\bmy\\b", "mah")
                .replaceAll("(?i)\\bI am\\b", "I can haz")
                .replaceAll("(?i)\\bhas\\b", "haz")
                .replaceAll("(?i)\\bhave\\b", "hav");

        if (result.length() > 0 && !result.endsWith("!!!")) {
            result += "!!!";
        }

        return result;
    }
}
