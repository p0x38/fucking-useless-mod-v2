package me.p0x38.fuckinguselessmod;

import java.util.Map;
import java.util.Random;

public final class BlockMapper {
    private static final Random RANDOM = new Random();

    private static final Map<Character, String> DYNAMIC_MAP = Map.ofEntries(
            Map.entry('i', "·"),
            Map.entry('l', "▪"),
            Map.entry('I', "▪"),
            Map.entry('t', "▪"),
            Map.entry('f', "▪"),
            Map.entry('j', "▪"),
            Map.entry('r', "▪"),
            Map.entry('e', "▪"),
            Map.entry('a', "▪"),
            Map.entry('c', "▪"),
            Map.entry('o', "▪"),
            Map.entry('s', "▪"),
            Map.entry('u', "▪"),
            Map.entry('v', "▪"),
            Map.entry('x', "▪"),
            Map.entry('z', "▪"),
            Map.entry('m', "▓"),
            Map.entry('w', "▓"),
            Map.entry('b', "▒"),
            Map.entry('d', "▒"),
            Map.entry('g', "▒"),
            Map.entry('h', "▒"),
            Map.entry('k', "▒"),
            Map.entry('n', "▒"),
            Map.entry('p', "▒"),
            Map.entry('q', "▒"),
            Map.entry('y', "▒"),
            Map.entry('M', "█"),
            Map.entry('W', "█")
    );

    private static final char[] RANDOM_BLOCKS = {
            '█', '▓', '▒', '░', '▄', '▀', '■', '▪', '▫', '·'
    };

    private BlockMapper() {
    }

    public static String replace(char character, Config.BlockMode mode) {
        return switch (mode) {
            case DYNAMIC -> DYNAMIC_MAP.getOrDefault(character, "█");
            case FULL_WIDTH -> "█";
            case RANDOM -> String.valueOf(
                    RANDOM_BLOCKS[RANDOM.nextInt(RANDOM_BLOCKS.length)]
            );
        };
    }
}
