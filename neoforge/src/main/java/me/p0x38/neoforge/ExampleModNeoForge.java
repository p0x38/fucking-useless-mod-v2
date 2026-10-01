package me.p0x38.neoforge;

import net.neoforged.fml.common.Mod;

import me.p0x38.ExampleMod;

@Mod(ExampleMod.MOD_ID)
public final class ExampleModNeoForge {
    public ExampleModNeoForge() {
        // Run our common setup.
        ExampleMod.init();
    }
}
