package me.p0x38.fuckinguselessmod.effects;

import java.util.Random;

public final class ZalgoEffect {
    private static final char[] UP = {
            '\u030d', '\u030e', '\u0304', '\u0305',
            '\u033f', '\u0311', '\u0306', '\u0310',
            '\u0352', '\u0357', '\u0351'
    };

    private static final char[] DOWN = {
            '\u0316', '\u0317', '\u0318', '\u0319',
            '\u031c', '\u031d', '\u031e', '\u031f',
            '\u0320', '\u0324', '\u0325'
    };

    private ZalgoEffect() {
    }

    public static void append(StringBuilder output, Random random) {
        output.append(UP[random.nextInt(UP.length)]);

        if (random.nextBoolean()) {
            output.append(DOWN[random.nextInt(DOWN.length)]);
        }
    }
}
