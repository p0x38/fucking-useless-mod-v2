package me.p0x38.fuckinguselessmod.presets;

import java.util.List;

public record PresetFile(
        String id,
        String name,
        List<PresetRule> rules
) {
}
