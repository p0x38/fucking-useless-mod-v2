package me.p0x38.fuckinguselessmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.architectury.platform.Platform;
import me.p0x38.fuckinguselessmod.config.ConfigToml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

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

                if (save()) {
                    Files.deleteIfExists(LEGACY_CONFIG_PATH);
                } else {
                    System.err.println(
                            "[Fucking Useless Mod] Keeping legacy JSON config because TOML migration failed."
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

    public static boolean save() {
        Path temporaryPath = CONFIG_PATH.resolveSibling(
                CONFIG_PATH.getFileName() + ".tmp"
        );

        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            Files.writeString(
                    temporaryPath,
                    ConfigToml.serialize(Config.get())
            );

            try {
                Files.move(
                        temporaryPath,
                        CONFIG_PATH,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                Files.move(
                        temporaryPath,
                        CONFIG_PATH,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            return true;
        } catch (IOException | RuntimeException exception) {
            System.err.println(
                    "[Fucking Useless Mod] Failed to save config"
            );
            exception.printStackTrace();

            try {
                Files.deleteIfExists(temporaryPath);
            } catch (IOException cleanupException) {
                System.err.println(
                        "[Fucking Useless Mod] Failed to clean up temporary config: "
                                + cleanupException.getMessage()
                );
            }

            return false;
        }
    }
}
