package me.p0x38.fuckinguselessmod.effects;

import me.p0x38.fuckinguselessmod.Config;
import me.p0x38.fuckinguselessmod.presets.PresetRegistry;
import me.p0x38.fuckinguselessmod.presets.TextPreset;

public final class Effects {
    private Effects() {
    }

    public static String applyAll(
            String input,
            Config.Data config
    ) {
        String result = input;

        for (String presetId : config.presets) {
            if (presetId == null || presetId.isBlank()) {
                continue;
            }

            TextPreset preset = PresetRegistry.get(presetId);

            if (preset == null) {
                continue;
            }

            result = preset.apply(result);
        }

        return result;
    }
}
