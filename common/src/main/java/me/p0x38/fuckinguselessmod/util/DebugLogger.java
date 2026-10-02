package me.p0x38.fuckinguselessmod.util;

import com.mojang.logging.LogUtils;
import me.p0x38.fuckinguselessmod.Config;
import org.slf4j.Logger;

public final class DebugLogger {
    private static final Logger LOGGER = LogUtils.getLogger();

    private DebugLogger() {
    }

    public static void debug(
            String message,
            Object... args
    ) {
        if (!Config.get().debugLoggingEnabled) {
            return;
        }

        LOGGER.info(
                "[Debug] " + message,
                args
        );
    }
}
