package me.p0x38.fuckinguselessmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.architectury.platform.Platform;
import me.p0x38.fuckinguselessmod.config.ConfigToml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Path CONFIG_PATH =
            Platform.getConfigFolder()
                    .resolve("fuckinguselessmod.toml");

    private static final Path LEGACY_CONFIG_PATH =
            Platform.getConfigFolder()
                    .resolve("fuckinguselessmod.json");

    private ConfigManager() {
    }

    public static void load() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            if (Files.exists(CONFIG_PATH)) {
                Config.Data data = new Config.Data();
                ConfigToml.deserializeInto(
                        Files.readString(CONFIG_PATH),
                        data
                );
                data.clamp();
                Config.set(data);
                return;
            }

            if (Files.exists(LEGACY_CONFIG_PATH)) {
                Config.Data data = GSON.fromJson(
                        Files.readString(LEGACY_CONFIG_PATH),
                        Config.Data.class
                );

                if (data == null) {
                    data = new Config.Data();
                }

                data.clamp();
                Config.set(data);
                save();

                try {
                    Files.deleteIfExists(LEGACY_CONFIG_PATH);
                } catch (IOException exception) {
                    System.err.println(
                            "[Fucking Useless Mod] Failed to remove legacy JSON config: "
                                    + exception.getMessage()
                    );
                }
                return;
            }

            Config.set(new Config.Data());
            save();
        } catch (IOException | RuntimeException exception) {
            System.err.println(
                    "[Fucking Useless Mod] Failed to load config"
            );
            exception.printStackTrace();
            Config.set(new Config.Data());
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            Files.writeString(
                    CONFIG_PATH,
                    ConfigToml.serialize(Config.get())
            );
        } catch (IOException | RuntimeException exception) {
            System.err.println(
                    "[Fucking Useless Mod] Failed to save config"
            );
            exception.printStackTrace();
        }
    }
}
