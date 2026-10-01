package me.p0x38.fuckinguselessmod.effects;

import me.p0x38.fuckinguselessmod.Config;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TextSpeak {
    private static final Map<String, String> REPLACEMENTS = new LinkedHashMap<>();

    static {
        REPLACEMENTS.put("see you", "c u");
        REPLACEMENTS.put("see", "c");
        REPLACEMENTS.put("you", "u");
        REPLACEMENTS.put("are", "r");
        REPLACEMENTS.put("why", "y");
        REPLACEMENTS.put("be right back", "brb");
        REPLACEMENTS.put("as soon as possible", "asap");
        REPLACEMENTS.put("laughing out loud", "lol");
        REPLACEMENTS.put("by the way", "btw");
        REPLACEMENTS.put("in my opinion", "imo");
        REPLACEMENTS.put("for your information", "fyi");
        REPLACEMENTS.put("I don't know", "idk");
        REPLACEMENTS.put("oh my god", "omg");
        REPLACEMENTS.put("right now", "rn");
        REPLACEMENTS.put("to be honest", "tbh");
        REPLACEMENTS.put("because", "bc");
        REPLACEMENTS.put("without", "w/o");
    }

    private TextSpeak() {}

    public static String apply(String input, Config.Data config) {
        if (!config.textSpeakEnabled || Math.random() > config.textSpeakChance) {
            return input;
        }

        String result = input;
        for (var entry : REPLACEMENTS.entrySet()) {
            result = result.replaceAll(
                    "(?i)\\b" + java.util.regex.Pattern.quote(entry.getKey()) + "\\b",
                    entry.getValue()
            );
        }
        return result;
    }
}
