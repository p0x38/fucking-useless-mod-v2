package me.p0x38.fuckinguselessmod.presets;

import me.p0x38.fuckinguselessmod.Config;

public final class PresetProcessor {
    private PresetProcessor() {
    }

    public static String apply(
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
                System.err.println(
                        "[Fucking Useless Mod] Unknown preset: "
                                + presetId
                );
                continue;
            }

            result = preset.apply(result);
        }

        return result;
    }
}
