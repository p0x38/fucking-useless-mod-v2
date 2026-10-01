package me.p0x38.fuckinguselessmod;

import me.p0x38.fuckinguselessmod.config.ConfigToml;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigTomlTest {
    @Test
    void preservesSentenceEndEffectOrder() {
        Config.Data data = new Config.Data();
        data.sentenceEndEffects = List.of(
                Config.SentenceEndEffect.ELLIPSIS,
                Config.SentenceEndEffect.TILDE,
                Config.SentenceEndEffect.EXCLAMATION
        );

        Config.Data loaded = new Config.Data();
        ConfigToml.deserializeInto(ConfigToml.serialize(data), loaded);

        assertEquals(data.sentenceEndEffects, loaded.sentenceEndEffects);
    }

    @Test
    void serializesSentenceEndEffectsAsEnumNames() {
        Config.Data data = new Config.Data();
        data.sentenceEndEffects = List.of(
                Config.SentenceEndEffect.TILDE,
                Config.SentenceEndEffect.ELLIPSIS
        );

        String serialized = ConfigToml.serialize(data);

        assertTrue(serialized.contains(
                "sentenceEndEffects = [\"TILDE\", \"ELLIPSIS\"]"
        ));
    }
}
