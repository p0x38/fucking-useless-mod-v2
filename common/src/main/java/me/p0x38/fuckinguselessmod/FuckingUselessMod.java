package me.p0x38.fuckinguselessmod;

import me.p0x38.fuckinguselessmod.entity.ModEntities;
import me.p0x38.fuckinguselessmod.presets.PresetRegistry;
import me.p0x38.fuckinguselessmod.sounds.ModSounds;

public final class FuckingUselessMod {
    public static final String MOD_ID = "fuckinguselessmod";

    private FuckingUselessMod() {
    }

    public static void init() {
        PresetRegistry.loadExternal();
        ConfigManager.load();

        ModEntities.USELESS_ENTITY.toString();
        ModSounds.initialize();
    }
}
