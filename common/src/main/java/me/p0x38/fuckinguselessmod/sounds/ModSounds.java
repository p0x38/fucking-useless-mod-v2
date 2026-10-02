package me.p0x38.fuckinguselessmod.sounds;

import me.p0x38.fuckinguselessmod.FuckingUselessMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
    public static final int DIALOGUE_SOUND_COUNT = 69;

    private static final SoundEvent[] DIALOGUE_SOUNDS =
            registerDialogueSounds();

    private ModSounds() {
    }

    private static SoundEvent[] registerDialogueSounds() {
        SoundEvent[] sounds = new SoundEvent[DIALOGUE_SOUND_COUNT];

        for (int index = 0; index < DIALOGUE_SOUND_COUNT; index++) {
            Identifier identifier = Identifier.fromNamespaceAndPath(
                    FuckingUselessMod.MOD_ID,
                    "dialogtxt/t_" + index
            );

            sounds[index] = Registry.register(
                    BuiltInRegistries.SOUND_EVENT,
                    identifier,
                    SoundEvent.createVariableRangeEvent(identifier)
            );
        }

        return sounds;
    }

    public static SoundEvent getDialogueSound(int index) {
        if (index < 0 || index >= DIALOGUE_SOUND_COUNT) {
            throw new IndexOutOfBoundsException(
                    "Invalid dialogue sound index: " + index
            );
        }

        return DIALOGUE_SOUNDS[index];
    }

    public static void initialize() {
        // Triggers static initialization.
    }
}