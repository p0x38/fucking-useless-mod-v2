package me.p0x38.fuckinguselessmod.effects;

import me.p0x38.fuckinguselessmod.Config;

import java.util.LinkedHashMap;
import java.util.Map;

public final class GamerSlang {
    private static final Map<String, String> REPLACEMENTS = new LinkedHashMap<>();

    static {
        REPLACEMENTS.put("you", "u");
        REPLACEMENTS.put("your", "ur");
        REPLACEMENTS.put("are", "r");
        REPLACEMENTS.put("why", "y");
        REPLACEMENTS.put("people", "ppl");
        REPLACEMENTS.put("please", "pls");
        REPLACEMENTS.put("thanks", "thx");
        REPLACEMENTS.put("thank", "thx");
        REPLACEMENTS.put("because", "cuz");
        REPLACEMENTS.put("before", "b4");
        REPLACEMENTS.put("really", "rly");
        REPLACEMENTS.put("probably", "prolly");
    }

    private GamerSlang() {}

    public static String apply(String input, Config.Data config) {
        if (!config.gamerSlangEnabled || Math.random() > config.gamerSlangChance) {
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
