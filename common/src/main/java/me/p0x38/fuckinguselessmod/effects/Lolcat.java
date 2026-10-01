package me.p0x38.fuckinguselessmod.effects;

import me.p0x38.fuckinguselessmod.Config;

public final class Lolcat {
    private Lolcat() {
    }

    public static String apply(String input, Config.Data config) {
        if (!config.lolcatEnabled
                || Math.random() > config.lolcatChance) {
            return input;
        }

        String result = ReplacementRules.applyWords(
                input,
                config.lolcatRules,
                true
        );

        if (!result.isEmpty() && !result.endsWith("!!!")) {
            result += "!!!";
        }

        return result;
    }
}
