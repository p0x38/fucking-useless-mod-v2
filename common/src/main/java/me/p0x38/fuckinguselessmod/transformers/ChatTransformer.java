package me.p0x38.fuckinguselessmod.transformers;

import me.p0x38.fuckinguselessmod.Config;
import me.p0x38.fuckinguselessmod.effects.EffectProcessor;
import me.p0x38.fuckinguselessmod.presets.PresetProcessor;

import java.util.EnumMap;
import java.util.Map;

public final class ChatTransformer {
    private static final Map<Config.Mode, TextTransformer> TRANSFORMERS =
            createTransformers();

    private ChatTransformer() {
    }

    public static String transform(String input) {
        Config.Data config = Config.get();

        if (!config.enabled) {
            return input;
        }

        TextTransformer transformer = TRANSFORMERS.get(config.mode);
        if (transformer == null) {
            return input;
        }

        String result = transformer.transform(input, config);
        result = EffectProcessor.apply(result, config);

        return PresetProcessor.apply(result, config);
    }

    private static Map<Config.Mode, TextTransformer> createTransformers() {
        Map<Config.Mode, TextTransformer> transformers =
                new EnumMap<>(Config.Mode.class);

        transformers.put(Config.Mode.INSERT, new InsertTransformer());
        transformers.put(Config.Mode.REPLACE, new ReplaceTransformer());
        transformers.put(Config.Mode.ENCODE, new EncodeTransformer());

        return Map.copyOf(transformers);
    }
}
