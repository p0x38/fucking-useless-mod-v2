package me.p0x38.fuckinguselessmod.sounds;

import me.p0x38.fuckinguselessmod.FuckingUselessMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import java.util.regex.Pattern;

public final class ModSounds {
    public static final int DIALOGUE_SOUND_COUNT = 69;

    private static final Pattern LEGACY_INDEX =
            Pattern.compile("\\d+");

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
        return getDialogueSound(String.valueOf(index));
    }

    public static SoundEvent getDialogueSound(String value) {
        Identifier identifier = normalizeDialogueSoundId(value);

        if (identifier == null) {
            throw new IllegalArgumentException(
                    "Invalid dialogue sound ID: " + value
            );
        }

        return SoundEvent.createVariableRangeEvent(identifier);
    }

    public static Identifier normalizeDialogueSoundId(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        if (trimmed.isEmpty()) {
            return null;
        }

        if (LEGACY_INDEX.matcher(trimmed).matches()) {
            try {
                int index = Integer.parseInt(trimmed);

                if (index < 0 || index >= DIALOGUE_SOUND_COUNT) {
                    return null;
                }

                return Identifier.fromNamespaceAndPath(
                        FuckingUselessMod.MOD_ID,
                        "dialogtxt/t_" + index
                );
            } catch (NumberFormatException exception) {
                return null;
            }
        }

        try {
            int separator = trimmed.indexOf(':');

            if (separator >= 0) {
                if (separator == 0 || separator == trimmed.length() - 1) {
                    return null;
                }

                return Identifier.fromNamespaceAndPath(
                        trimmed.substring(0, separator),
                        trimmed.substring(separator + 1)
                );
            }

            return Identifier.fromNamespaceAndPath(
                    FuckingUselessMod.MOD_ID,
                    trimmed
            );
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public static void initialize() {
        // Triggers static initialization.
    }
}
