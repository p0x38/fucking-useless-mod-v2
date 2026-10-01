package me.p0x38.fuckinguselessmod.effects;

import me.p0x38.fuckinguselessmod.Config;

public final class LeetSpeak {
    private LeetSpeak() {}

    public static String apply(String input, Config.Data config) {
        if (!config.leetSpeakEnabled || Math.random() > config.leetSpeakChance) {
            return input;
        }

        return input
                .replace('a', '4').replace('A', '4')
                .replace('e', '3').replace('E', '3')
                .replace('i', '1').replace('I', '1')
                .replace('o', '0').replace('O', '0')
                .replace('s', '5').replace('S', '5')
                .replace('t', '7').replace('T', '7');
    }
}
