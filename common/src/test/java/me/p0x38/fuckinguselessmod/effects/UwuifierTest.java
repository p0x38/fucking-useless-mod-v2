package me.p0x38.fuckinguselessmod.effects;

import me.p0x38.fuckinguselessmod.Config;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UwuifierTest {
    @Test
    void nVowelKeepsTheVowel() {
        Config.Data config = config();
        config.uwuifierReplaceNVowel = true;

        assertEquals(
                "nyice nyame nyo",
                Uwuifier.apply("nice name no", config)
        );
    }

    @Test
    void protectedTokensStayUntouched() {
        Config.Data config = config();
        config.uwuifierReplaceRl = true;

        assertEquals(
                "wove https://example.com/rabbit @really #really bob@example.com weawwy",
                Uwuifier.apply(
                        "love https://example.com/rabbit @really #really bob@example.com really",
                        config
                )
        );
    }

    @Test
    void zeroWordChanceNeverChangesWords() {
        Config.Data config = config();
        config.uwuifierReplaceRl = true;
        config.uwuifierReplaceNVowel = true;
        config.uwuifierReplaceOve = true;
        config.uwuifierReplaceTh = true;
        config.uwuifierReplaceYou = true;
        config.uwuifierWordChance = 0.0f;

        String input = "really nice love thing you";
        assertEquals(input, Uwuifier.apply(input, config));
    }

    @Test
    void stutterKeepsOriginalWhitespaceAndWord() {
        Config.Data config = config();
        config.uwuifierStutterEnabled = true;
        config.uwuifierStutterChance = 1.0f;

        String result = Uwuifier.apply("hello stuff", config);

        assertTrue(
                result.matches("hello s-(?:s-)?stuff"),
                result
        );
    }

    @Test
    void stutterPreservesNonBmpFirstCodePoint() {
        Config.Data config = config();
        config.uwuifierStutterEnabled = true;
        config.uwuifierStutterChance = 1.0f;

        String result = Uwuifier.apply("hello 𝒶bc", config);

        assertTrue(
                result.matches("hello 𝒶-(?:𝒶-)?𝒶bc"),
                result
        );
    }

    @Test
    void punctuationAndWhitespaceArePreserved() {
        Config.Data config = config();
        config.uwuifierStutterEnabled = true;
        config.uwuifierStutterChance = 1.0f;

        assertTrue(
                Uwuifier.apply("hello, world!", config)
                        .matches("hello, w-(?:w-)?world!"),
                "Unexpected transformation"
        );
    }

    @Test
    void blacklistWordsAreNotTransformed() {
        Config.Data config = config();
        config.uwuifierBlacklist = List.of("rawr", "lol", "lmao");
        config.uwuifierReplaceRl = true;

        assertEquals(
                "rawr lol lmao",
                Uwuifier.apply("rawr lol lmao", config)
        );
    }

    @Test
    void nVowelTransformationPreservesTheVowel() {
        Config.Data config = config();
        config.uwuifierReplaceNVowel = true;

        assertEquals(
                "nyah",
                Uwuifier.apply("nah", config)
        );
    }

    @Test
    void stutterIsAppliedToTransformedWord() {
        Config.Data config = config();
        config.uwuifierReplaceNVowel = true;
        config.uwuifierStutterEnabled = true;
        config.uwuifierStutterChance = 1.0f;

        String result = Uwuifier.apply("nah", config);

        assertTrue(
                result.matches("n-(?:n-){0,3}nyah"),
                result
        );
    }

    private static Config.Data config() {
        Config.Data config = new Config.Data();
        config.uwuifierReplaceRl = false;
        config.uwuifierReplaceNVowel = false;
        config.uwuifierReplaceOve = false;
        config.uwuifierReplaceTh = false;
        config.uwuifierReplaceYou = false;
        config.uwuifierWordChance = 1.0f;
        config.uwuifierExclamationsEnabled = false;
        config.uwuifierEmoticonsEnabled = false;
        config.uwuifierActionsEnabled = false;
        config.uwuifierStutterEnabled = false;
        config.uwuifierEmoticons = List.of();
        config.uwuifierActionTexts = List.of();
        config.uwuifierExclamations = List.of();
        return config;
    }
}
