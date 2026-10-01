package me.p0x38.fuckinguselessmod.effects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleConditionTest {
    @Test
    void comparesCharacterCount() {
        assertTrue(condition("length:>4", "hello"));
        assertTrue(condition("length:>=5", "hello"));
        assertTrue(condition("length:<6", "hello"));
        assertTrue(condition("length:5", "hello"));
        assertFalse(condition("length:<5", "hello"));
    }

    @Test
    void countsWords() {
        assertTrue(condition("word_count:3", "hello little world"));
        assertTrue(condition("words:>=2", "hello world"));
        assertFalse(condition("word_count:>3", "hello world"));
    }

    @Test
    void countsCarriageReturnLines() {
        assertTrue(condition("line_count:2", "hello\rworld"));
        assertTrue(condition("lines:>1", "hello\rworld"));
        assertFalse(condition("line_count:3", "hello\rworld"));
    }

    private static boolean condition(String expression, String input) {
        return RuleCondition.evaluate(
                expression,
                input,
                input,
                false
        );
    }
}
