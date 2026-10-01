package me.p0x38.fuckinguselessmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.architectury.platform.Platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Path CONFIG_PATH =
            Platform.getConfigFolder()
                    .resolve("fuckinguselessmod.json");

    private ConfigManager() {
    }

    public static void load() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            if (!Files.exists(CONFIG_PATH)) {
                Config.set(new Config.Data());
                save();
                return;
            }

            Config.Data data = GSON.fromJson(
                    Files.readString(CONFIG_PATH),
                    Config.Data.class
            );

            if (data == null) {
                data = new Config.Data();
            }

            data.clamp();
            Config.set(data);
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
                    GSON.toJson(Config.get())
            );
        } catch (IOException exception) {
            System.err.println(
                    "[Fucking Useless Mod] Failed to save config"
            );
            exception.printStackTrace();
        }
    }
}
