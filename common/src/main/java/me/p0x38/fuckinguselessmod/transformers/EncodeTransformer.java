package me.p0x38.fuckinguselessmod.transformers;

import me.p0x38.fuckinguselessmod.Config;
import me.p0x38.fuckinguselessmod.Encoder;

public final class EncodeTransformer implements TextTransformer {
    @Override
    public String transform(String input, Config.Data config) {
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
}
