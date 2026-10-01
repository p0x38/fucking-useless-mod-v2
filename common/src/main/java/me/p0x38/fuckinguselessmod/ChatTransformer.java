package me.p0x38.fuckinguselessmod;

import me.p0x38.fuckinguselessmod.presets.PresetProcessor;

import java.util.Random;

public final class ChatTransformer {
    private static final Random RANDOM = new Random();

    private static final char[] ZALGO_UP = {
            '\u030d', '\u030e', '\u0304', '\u0305',
            '\u033f', '\u0311', '\u0306', '\u0310',
            '\u0352', '\u0357', '\u0351'
    };

    private static final char[] ZALGO_DOWN = {
            '\u0316', '\u0317', '\u0318', '\u0319',
            '\u031c', '\u031d', '\u031e', '\u031f',
            '\u0320', '\u0324', '\u0325'
    };

    private static final char[] BLOCK_ELEMENTS = {
            '█', '▓', '▒', '░', '▄', '▀', '■'
    };

    private ChatTransformer() {
    }

    public static String transform(String input) {
        Config.Data config = Config.get();

        if (!config.enabled) {
            return input;
        }

        String result = switch (config.mode) {
            case INSERT -> insertMode(input, config);
            case REPLACE -> replaceMode(input, config);
            case ENCODE -> encodeMode(input, config);
        };

        return PresetProcessor.apply(result, config);
    }

    private static String insertMode(String input, Config.Data config) {
        StringBuilder output = new StringBuilder();

        for (char character : input.toCharArray()) {
            if (output.length() >= config.maxLength) {
                break;
            }

            output.append(character);
            appendCorruption(output, config);
        }

        return truncate(output, config.maxLength);
    }

    private static String replaceMode(String input, Config.Data config) {
        StringBuilder output = new StringBuilder();

        for (char character : input.toCharArray()) {
            if (output.length() >= config.maxLength) {
                break;
            }

            if (Character.isWhitespace(character)) {
                output.append(character);
                continue;
            }

            boolean replaced = false;

            if (config.blocksEnabled
                    && RANDOM.nextFloat() < config.blockChance) {
                output.append(
                        BlockMapper.replace(character, config.blockMode)
                );
                replaced = true;
            }

            if (config.zalgoEnabled
                    && RANDOM.nextFloat() < config.zalgoChance) {
                appendZalgo(output);
                replaced = true;
            }

            if (!replaced) {
                output.append(character);
            }
        }

        return truncate(output, config.maxLength);
    }

    private static String encodeMode(String input, Config.Data config) {
        try {
            return Encoder.encode(input, config);
        } catch (IllegalArgumentException exception) {
            System.err.println(
                    "[Fucking Useless Mod] Encoding failed: "
                            + exception.getMessage()
            );
            return input;
        }
    }

    private static void appendCorruption(
            StringBuilder output,
            Config.Data config
    ) {
        if (config.zalgoEnabled
                && RANDOM.nextFloat() < config.zalgoChance) {
            appendZalgo(output);
        }

        if (config.blocksEnabled
                && RANDOM.nextFloat() < config.blockChance) {
            output.append(
                    BLOCK_ELEMENTS[RANDOM.nextInt(BLOCK_ELEMENTS.length)]
            );
        }
    }

    private static void appendZalgo(StringBuilder output) {
        output.append(
                ZALGO_UP[RANDOM.nextInt(ZALGO_UP.length)]
        );

        if (RANDOM.nextBoolean()) {
            output.append(
                    ZALGO_DOWN[RANDOM.nextInt(ZALGO_DOWN.length)]
            );
        }
    }

    private static String truncate(
            StringBuilder output,
            int maxLength
    ) {
        return output.substring(
                0,
                Math.min(maxLength, output.length())
        );
    }
}
