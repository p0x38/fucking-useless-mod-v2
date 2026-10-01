package me.p0x38.fuckinguselessmod.transformers;

import me.p0x38.fuckinguselessmod.Config;

public interface TextTransformer {
    String transform(String input, Config.Data config);
}
