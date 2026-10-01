package me.p0x38.fuckinguselessmod.presets;

import java.util.regex.Pattern;

public record PresetRule(String regex, String replacement) {
    public Pattern pattern() {
        return Pattern.compile(regex);
    }

    public void validate() {
        pattern()
                .matcher("")
                .replaceAll(replacement);
    }
}
