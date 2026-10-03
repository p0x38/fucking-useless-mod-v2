package me.p0x38.fabric.client.blindspot;

import me.p0x38.fuckinguselessmod.util.DebugLogger;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class SentientSignBrain {
    private SentientSignBrain() {
    }

    public static void think(
            SentientSign sign,
            ClientLevel level
    ) {
        long gameTick = level.getGameTime();

        if (!sign.canAct(gameTick)) {
            return;
        }

        var memories = sign.memories();

        if (memories.isEmpty()) {
            return;
        }

        Memory latest = memories.getLast();

        DebugLogger.debug(
                "[SentientSignBrain] think id={} tick={} mood={} latestMemory={} message={}",
                sign.id(),
                gameTick,
                sign.mood(),
                latest.type(),
                sign.currentMessage()
        );

        /*
         * Direct communication has priority over ambient thoughts.
         */
        if (latest.type() == Memory.Type.PLAYER_INTERACTED) {
            String message = latest.context("message");

            say(
                    sign,
                    sign.mood() == SentientSign.Mood.CURIOUS
                            ? choose(
                                    text("interacted.curious.1"),
                                    text("interacted.curious.2"),
                                    "i'm listening."
                            )
                            : choose(
                                    text("interacted.default.1"),
                                    text("interacted.default.2"),
                                    text("interacted.default.3")
                            ),
                    gameTick
            );

            sign.markAction(gameTick);
            return;
        }

        /*
         * React to the moment the player first notices the sign.
         */
        if (latest.type() == Memory.Type.PLAYER_SEEN
                && randomChance(0.10)) {
            say(
                    sign,
                    text("seen"),
                    gameTick
            );

            sign.markAction(gameTick);
            return;
        }

        /*
         * Returning is much more interesting to the sign than
         * simply continuing to stare at it.
         */
        if (latest.type() == Memory.Type.PLAYER_RETURNED
                && randomChance(0.35)) {
            say(
                    sign,
                    choose(
                            "you're back",
                            "i knew you'd return",
                            text("returned.3"),
                            text("returned.4")
                    ),
                    gameTick
            );

            sign.markAction(gameTick);
            return;
        }

        /*
         * A curious sign can comment on the player's behavior
         * after a fresh observation.
         */
        if (sign.mood() == SentientSign.Mood.CURIOUS
                && (latest.type() == Memory.Type.PLAYER_RETURNED
                || latest.type() == Memory.Type.PLAYER_LOOKED_AWAY)
                && randomChance(0.20)) {
            say(
                    sign,
                    choose(
                            text("curious.1"),
                            text("curious.2"),
                            text("curious.3"),
                            text("curious.4")
                    ),
                    gameTick
            );

            sign.markAction(gameTick);
            return;
        }

        /*
         * An annoyed sign can respond after a fresh return or
         * another moment where the player looks away.
         */
        if (sign.mood() == SentientSign.Mood.ANNOYED
                && (latest.type() == Memory.Type.PLAYER_RETURNED
                || latest.type() == Memory.Type.PLAYER_LOOKED_AWAY)
                && randomChance(0.30)) {
            say(
                    sign,
                    choose(
                            "i wasn't here before",
                            text("annoyed.2"),
                            text("annoyed.3")
                    ),
                    gameTick
            );
        }
    }

    private static void say(
            SentientSign sign,
            String message,
            long gameTick
    ) {
        DebugLogger.debug(
                "[SentientSignBrain] speaking id={} tick={} message={}",
                sign.id(),
                gameTick,
                message
        );

        sign.setCurrentMessage(message);

        sign.remember(
                new Memory(
                        Memory.Type.SIGN_SPOKE,
                        gameTick,
                        1.0f,
                        sign.position(),
                        Map.of(
                                "message",
                                message
                        )
                )
        );
    }

    private static String text(String key) {
        return Component.translatable(
                "text.fuckinguselessmod.blindspot.sign." + key
        ).getString();
    }

    private static boolean randomChance(double chance) {
        return ThreadLocalRandom.current()
                .nextDouble()
                < chance;
    }

    private static String choose(String... options) {
        return options[
                ThreadLocalRandom.current()
                        .nextInt(options.length)
        ];
    }
}
