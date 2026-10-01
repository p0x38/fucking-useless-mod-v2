package me.p0x38.fuckinguselessmod.effects;

import me.p0x38.fuckinguselessmod.Config;

import java.util.Locale;

public final class TextCaseEffect {
    private TextCaseEffect() {
    }

    public static String apply(String input, Config.Data config) {
        return switch (config.textCase) {
            case PRESERVE -> input;
            case LOWERCASE -> input.toLowerCase(Locale.ROOT);
            case UPPERCASE -> input.toUpperCase(Locale.ROOT);
        };
    }
}
