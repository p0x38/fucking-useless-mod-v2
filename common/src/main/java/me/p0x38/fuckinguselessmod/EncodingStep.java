package me.p0x38.fuckinguselessmod;

import java.util.Locale;

public enum EncodingStep {
    BINARY,
    BIT_ROTATE,
    BIT_XOR,
    BIT_REVERSAL,
    BASE64,
    ALPHA_ROTATION,
    URL_ENCODE;

    public static EncodingStep parse(String value) {
        return valueOf(
                value.trim().toUpperCase(Locale.ROOT)
        );
    }
}
