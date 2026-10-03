package me.p0x38.fuckinguselessmod;

import me.p0x38.fuckinguselessmod.effects.SentenceEndEffects;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SentenceEndEffectsTest {
    @Test
    void randomlySelectsAndCombinesConfiguredEffects() {
        Config.Data config = new Config.Data();
        config.sentenceEndEffectsEnabled = true;
        config.sentenceEndEffects = List.of(
                Config.SentenceEndEffect.TILDE,
                Config.SentenceEndEffect.ELLIPSIS,
                Config.SentenceEndEffect.EXCLAMATION
        );

        for (int i = 0; i < 100; i++) {
            String result = SentenceEndEffects.apply("Hello.", config);

            assertTrue(
                    result.matches("Hello(?:~|\\.{3}|!|~\\.{3}|~!|\\.{3}~|\\.{3}!|!~|!\\.{3}|~\\.{3}!|~!\\.{3}|\\.{3}~!|\\.{3}!~|!~\\.{3}|!\\.{3}~)?"),
                    result
            );
        }
    }

    @Test
    void replacesExistingSentencePunctuation() {
        Config.Data config = new Config.Data();
        config.sentenceEndEffectsEnabled = true;
        config.sentenceEndEffects = List.of(
                Config.SentenceEndEffect.TILDE,
                Config.SentenceEndEffect.ELLIPSIS
        );

        for (int i = 0; i < 50; i++) {
            String result = SentenceEndEffects.apply("Hello. World?", config);

            assertTrue(
                    result.matches("Hello(?:~|\\.{3}|~\\.{3}|\\.{3}~)? World(?:~|\\.{3}|~\\.{3}|\\.{3}~)?"),
                    result
            );
        }
    }

    @Test
    void canDisableAllEffects() {
        Config.Data config = new Config.Data();
        config.sentenceEndEffectsEnabled = false;

        assertEquals("Hello. World?", SentenceEndEffects.apply("Hello. World?", config));
    }

    @Test
    void leavesTextWithoutSentenceEndUntouched() {
        Config.Data config = new Config.Data();

        assertEquals("Hello", SentenceEndEffects.apply("Hello", config));
    }
}
