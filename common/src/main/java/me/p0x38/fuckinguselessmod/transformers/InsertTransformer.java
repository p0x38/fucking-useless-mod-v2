package me.p0x38.fuckinguselessmod.transformers;

import me.p0x38.fuckinguselessmod.Config;
import me.p0x38.fuckinguselessmod.effects.BlockEffect;
import me.p0x38.fuckinguselessmod.effects.ZalgoEffect;

import java.util.Random;

public final class InsertTransformer implements TextTransformer {
    private final Random random = new Random();

    @Override
    public String transform(String input, Config.Data config) {
        StringBuilder output = new StringBuilder();

        for (char character : input.toCharArray()) {
            if (output.length() >= config.maxLength) {
                break;
            }

            output.append(character);

            if (config.zalgoEnabled
                    && random.nextFloat() < config.zalgoChance) {
                ZalgoEffect.append(output, random);
            }

            if (config.blocksEnabled
                    && random.nextFloat() < config.blockChance) {
                BlockEffect.appendRandom(output, random);
            }
        }

        return truncate(output, config.maxLength);
    }

    private static String truncate(StringBuilder output, int maxLength) {
        return output.substring(
                0,
                Math.min(maxLength, output.length())
        );
    }
}
