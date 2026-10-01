package me.p0x38.fuckinguselessmod.effects;

import me.p0x38.fuckinguselessmod.Config;

public final class EffectProcessor {
    private EffectProcessor() {
    }

    public static String apply(String input, Config.Data config) {
        String result = input;

        if (config.leetSpeakEnabled) {
            result = LeetSpeak.apply(result, config);
        }
        if (config.gamerSlangEnabled) {
            result = GamerSlang.apply(result, config);
        }
        if (config.textSpeakEnabled) {
            result = TextSpeak.apply(result, config);
        }
        if (config.lolcatEnabled) {
            result = Lolcat.apply(result, config);
        }
        if (config.uwuifierEnabled) {
            result = Uwuifier.apply(result, config);
        }

        return result;
    }
}
