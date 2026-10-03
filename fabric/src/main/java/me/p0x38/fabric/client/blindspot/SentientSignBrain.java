package me.p0x38.fabric.client.blindspot;

import net.minecraft.client.multiplayer.ClientLevel;

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

        /*
         * Direct communication has priority over ambient thoughts.
         */
        if (latest.type() == Memory.Type.PLAYER_INTERACTED) {
            String message = latest.context("message");

            say(
                    sign,
                    sign.mood() == SentientSign.Mood.CURIOUS
                            ? choose(
                                    "tell me more.",
                                    "why?",
                                    "i'm listening."
                            )
                            : choose(
                                    "i heard you.",
                                    "interesting.",
                                    "keep talking."
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
                    "...",
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
                            "you came back",
                            "i remember you"
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
                            "why did you leave?",
                            "i saw that",
                            "you keep looking away",
                            "are you watching me?"
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
                            "you remember incorrectly",
                            "there has always been a sign here"
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
