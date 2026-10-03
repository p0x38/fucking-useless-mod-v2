package me.p0x38.fuckinguselessmod.presets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.architectury.platform.Platform;
import me.p0x38.fuckinguselessmod.util.DebugLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PresetRegistry {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Map<String, TextPreset> PRESETS =
            new LinkedHashMap<>();

    private static final Path PRESET_DIRECTORY =
            Platform.getConfigFolder()
                    .resolve("fuckinguselessmod")
                    .resolve("presets");

    private PresetRegistry() {
    }

    public static void register(TextPreset preset) {
        PRESETS.put(preset.id(), preset);
    }

    public static TextPreset get(String id) {
        return PRESETS.get(id);
    }

    public static Collection<TextPreset> all() {
        return PRESETS.values();
    }

    public static void loadExternal() {
        try {
            Files.createDirectories(PRESET_DIRECTORY);
        } catch (IOException exception) {
            DebugLogger.error(
                    "[Fucking Useless Mod] Failed to create preset directory.",
                    exception
            );
            return;
        }

        try (var files = Files.list(PRESET_DIRECTORY)) {
            files.filter(path ->
                    path.toString()
                            .toLowerCase()
                            .endsWith(".json")
            ).forEach(PresetRegistry::loadFile);
        } catch (IOException exception) {
            DebugLogger.error(
                    "[Fucking Useless Mod] Failed to scan preset directory.",
                    exception
            );
        }
    }

    private static void loadFile(Path path) {
        try {
            PresetFile file = GSON.fromJson(
                    Files.readString(path),
                    PresetFile.class
            );

            if (file == null
                    || file.id() == null
                    || file.id().isBlank()
                    || file.name() == null
                    || file.name().isBlank()
                    || file.rules() == null) {
                System.err.println(
                        "[Fucking Useless Mod] Invalid preset: " + path
                );
                return;
            }

            TextPreset preset = new RegexPreset(
                    file.id(),
                    file.name(),
                    file.rules()
            );

            register(preset);

            System.out.println(
                    "[Fucking Useless Mod] Loaded preset: "
                            + file.id()
            );
        } catch (Exception exception) {
            DebugLogger.error(
                    "[Fucking Useless Mod] Failed to load preset: " + path,
                    exception
            );
        }
    }
}
