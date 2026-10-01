package me.p0x38.fuckinguselessmod.effects;

import me.p0x38.fuckinguselessmod.Config;

public final class TextSpeak {
    private TextSpeak() {
    }

    public static String apply(String input, Config.Data config) {
        if (!config.textSpeakEnabled
                || Math.random() > config.textSpeakChance) {
            return input;
        }

        return ReplacementRules.applyRegex(
                input,
                config.textSpeakRules,
                false
        );
    }
}
