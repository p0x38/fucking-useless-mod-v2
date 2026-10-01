package me.p0x38.fuckinguselessmod.effects;

import me.p0x38.fuckinguselessmod.Config;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LolcatTest {
    @Test
    void appliesRulesWithoutAddingExclamationMarks() {
        Config.Data config = new Config.Data();
        config.lolcatEnabled = true;
        config.lolcatChance = 1.0f;
        config.lolcatRules = List.of("\\bhello\\b=>hullo");

        assertEquals("hullo", Lolcat.apply("hello", config));
    }

    @Test
    void doesNotChangeTextWhenNoRuleMatches() {
        Config.Data config = new Config.Data();
        config.lolcatEnabled = true;
        config.lolcatChance = 1.0f;
        config.lolcatRules = List.of("\\bcat\\b=>kitteh");

        assertEquals("hello", Lolcat.apply("hello", config));
    }
}
