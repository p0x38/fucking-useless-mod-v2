package me.p0x38.fuckinguselessmod.util;

import com.mojang.logging.LogUtils;
import me.p0x38.fuckinguselessmod.Config;
import org.slf4j.Logger;
import org.slf4j.helpers.MessageFormatter;

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

        String formatted =
                MessageFormatter.arrayFormat(message, args).getMessage();

        LOGGER.info(
                "[DEBUG] {}",
                formatted
        );
    }

    public static void error(
            String message,
            Throwable throwable
    ) {
        LOGGER.error("[Error] {}", message, throwable);
    }
}
