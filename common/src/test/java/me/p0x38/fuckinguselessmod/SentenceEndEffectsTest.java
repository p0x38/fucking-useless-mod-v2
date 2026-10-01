package me.p0x38.fuckinguselessmod;

import me.p0x38.fuckinguselessmod.effects.SentenceEndEffects;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SentenceEndEffectsTest {
    @Test
    void appliesEffectsInConfiguredOrder() {
        Config.Data config = new Config.Data();
        config.sentenceEndEffectsEnabled = true;
        config.sentenceEndEffects = List.of(
                Config.SentenceEndEffect.TILDE,
                Config.SentenceEndEffect.ELLIPSIS,
                Config.SentenceEndEffect.EXCLAMATION
        );

        assertEquals(
                "Hello.~...! World?~...!",
                SentenceEndEffects.apply("Hello. World?", config)
        );
    }

    @Test
    void supportsDifferentOrder() {
        Config.Data config = new Config.Data();
        config.sentenceEndEffectsEnabled = true;
        config.sentenceEndEffects = List.of(
                Config.SentenceEndEffect.ELLIPSIS,
                Config.SentenceEndEffect.TILDE,
                Config.SentenceEndEffect.EXCLAMATION
        );

        assertEquals(
                "Hello....~! World?...~!",
                SentenceEndEffects.apply("Hello. World?", config)
        );
    }

    @Test
    void canDisableAllEffects() {
        Config.Data config = new Config.Data();
        config.sentenceEndEffectsEnabled = false;

        assertEquals(
                "Hello. World?",
                SentenceEndEffects.apply("Hello. World?", config)
        );
    }

    @Test
    void leavesTextWithoutSentenceEndUntouched() {
        Config.Data config = new Config.Data();

        assertEquals(
                "Hello",
                SentenceEndEffects.apply("Hello", config)
        );
    }
}
