package me.p0x38.fabric.client.chatentity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.p0x38.fuckinguselessmod.util.DebugLogger;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ChatEntityPersistence {
    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().create();

    private static final String FILE_NAME =
            "fuckinguselessmod_chat_entity.json";

    private static boolean loaded;
    private static boolean everInteracted;

    private ChatEntityPersistence() {
    }

    public static void load() {
        if (loaded) {
            return;
        }

        loaded = true;

        Path path = getPath();

        try {
            if (!Files.exists(path)) {
                return;
            }

            State state =
                    GSON.fromJson(
                            Files.readString(path),
                            State.class
                    );

            if (state != null) {
                everInteracted = state.everInteracted();
            }
        } catch (IOException | RuntimeException exception) {
            DebugLogger.debug(
                    "[ChatEntityPersistence] failed to load {}: {}",
                    path,
                    exception.getMessage()
            );
        }
    }

    public static boolean hasEverInteracted() {
        load();
        return everInteracted;
    }

    public static void markEverInteracted() {
        load();

        if (everInteracted) {
            return;
        }

        everInteracted = true;
        save();
    }

    private static void save() {
        Path path = getPath();

        try {
            Files.createDirectories(path.getParent());

            Files.writeString(
                    path,
                    GSON.toJson(new State(everInteracted))
            );
        } catch (IOException | RuntimeException exception) {
            DebugLogger.debug(
                    "[ChatEntityPersistence] failed to save {}: {}",
                    path,
                    exception.getMessage()
            );
        }
    }

    private static Path getPath() {
        return Minecraft.getInstance()
                .gameDirectory
                .toPath()
                .resolve("config")
                .resolve(FILE_NAME);
    }

    private record State(boolean everInteracted) {
    }
}
