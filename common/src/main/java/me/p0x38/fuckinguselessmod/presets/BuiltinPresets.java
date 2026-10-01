package me.p0x38.fuckinguselessmod.presets;

import java.util.List;

public final class BuiltinPresets {
    public static final TextPreset UWUIFY = new RegexPreset(
            "uwuify",
            "Uwuify",
            List.of(
                    new PresetRule("[rl]", "w"),
                    new PresetRule("[RL]", "W"),
                    new PresetRule("n([aeiou])", "ny$1"),
                    new PresetRule("N([aeiou])", "Ny$1"),
                    new PresetRule("ove", "uv"),
                    new PresetRule("Ove", "Uv"),
                    new PresetRule("OVE", "UV")
            )
    );

    private BuiltinPresets() {
    }
}
