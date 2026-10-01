package me.p0x38.fuckinguselessmod.effects;

import me.p0x38.fuckinguselessmod.Config;

public final class GamerSlang {
    private GamerSlang() {
    }

    public static String apply(String input, Config.Data config) {
        if (!config.gamerSlangEnabled
                || Math.random() > config.gamerSlangChance) {
            return input;
        }

        return ReplacementRules.applyWords(
                input,
                config.gamerSlangRules,
                true
        );
    }
}
