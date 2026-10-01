package me.p0x38.fuckinguselessmod.presets;

public interface TextPreset {
    String id();

    String displayName();

    String apply(String input);
}
