package me.p0x38.fabric.client.detectors;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/**
 * Detects the Konami Code from client keyboard input.
 */
public final class KonamiCodeDetector {
    private static final int[] SEQUENCE = {
            GLFW.GLFW_KEY_UP,
            GLFW.GLFW_KEY_UP,
            GLFW.GLFW_KEY_DOWN,
            GLFW.GLFW_KEY_DOWN,
            GLFW.GLFW_KEY_LEFT,
            GLFW.GLFW_KEY_RIGHT,
            GLFW.GLFW_KEY_LEFT,
            GLFW.GLFW_KEY_RIGHT,
            GLFW.GLFW_KEY_B,
            GLFW.GLFW_KEY_A
    };

    private static final Map<Integer, Boolean> PREVIOUS_STATE =
            new HashMap<>();

    private static int progress;

    private KonamiCodeDetector() {}

    /**
     * Updates the detector and returns {@code true} when the full Konami Code
     * has been entered.
     *
     * @param client the Minecraft client
     * @return {@code true} exactly when the sequence has just been completed
     */
    public static boolean tick(Minecraft client) {
        if (client.level == null || client.player == null) {
            reset();
            return false;
        }

        long window = client.getWindow().handle();

        for (int key : SEQUENCE) {
            boolean down =
                    GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;

            boolean wasDown =
                    PREVIOUS_STATE.getOrDefault(key, false);

            PREVIOUS_STATE.put(key, down);

            if (!down || wasDown) {
                continue;
            }

            if (key == SEQUENCE[progress]) {
                progress++;

                if (progress >= SEQUENCE.length) {
                    reset();
                    return true;
                }

                continue;
            }

            progress = key == SEQUENCE[0] ? 1 : 0;
        }

        return false;
    }

    /** Resets the currently entered Konami Code sequence. */
    public static void reset() {
        progress = 0;
        PREVIOUS_STATE.clear();
    }
}
