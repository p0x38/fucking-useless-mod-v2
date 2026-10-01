package me.p0x38.fuckinguselessmod.effects;

import me.p0x38.fuckinguselessmod.Config;

public final class LeetSpeak {
    private LeetSpeak() {
    }

    public static String apply(String input, Config.Data config) {
        if (!config.leetSpeakEnabled
                || Math.random() > config.leetSpeakChance) {
            return input;
        }

        return ReplacementRules.applyLiteral(
                input,
                config.leetSpeakRules,
                false
        );
    }
}
