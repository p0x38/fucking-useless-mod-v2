package me.p0x38.fuckinguselessmod.effects;

import me.p0x38.fuckinguselessmod.Config;

public final class EffectProcessor {
    private static final TextEffect UWUIFIER = Uwuifier::apply;

    private EffectProcessor() {
    }

    public static String apply(String input, Config.Data config) {
        String result = input;

        if (config.uwuifierEnabled) {
            result = UWUIFIER.apply(result, config);
        }

        return result;
    }
}
