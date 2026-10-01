package me.p0x38.fuckinguselessmod;

import me.p0x38.fuckinguselessmod.presets.PresetRegistry;

public final class FuckingUselessMod {
    public static final String MOD_ID = "fuckinguselessmod";

    private FuckingUselessMod() {
    }

    public static void init() {
        PresetRegistry.loadExternal();
        ConfigManager.load();
    }
}
