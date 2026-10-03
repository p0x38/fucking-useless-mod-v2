package me.p0x38.fabric.client.blindspot;

import net.minecraft.client.multiplayer.ClientLevel;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class SentientSignBrain {
    private SentientSignBrain() {}

    public static void think(
            SentientSign sign,
            ClientLevel level
    ) {
        long gameTick = level.getGameTime();

        if (!sign.canAct(gameTick)) return;

        /*
         * A sign which has never been noticed behaves quietly.
         */
        if (!sign.hasMemory(Memory.Type.PLAYER_SEEN) && randomChance(0.01)) {
            say(sign, "...", gameTick);

            sign.markAction(gameTick);
            return;
        }

        /*
         * Once the player knows the sign exists,
         * it becomes increasingly interested in them.
         */
        if (sign.mood() == SentientSign.Mood.CURIOUS && randomChance(0.02)) {
            say(sign, choose("hello", "you're back", "i saw that", "why did you leave?" ), gameTick);

            sign.markAction(gameTick);
            return;
        }

        /*
         * An annoyed sign can become deliberately deceptive.
         */
        if (sign.mood() == SentientSign.Mood.ANNOYED && randomChance(0.03)) {
            say(sign, choose(
                    "i wasn't here before",
                    "you remember incorrectly",
                    "there has always been a sign here"
            ), gameTick);

            sign.markAction(gameTick);
        }
    }

    private static void say(
            SentientSign sign,
            String message,
            long gameTick
    ) {
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
        return ThreadLocalRandom.current().nextDouble() < chance;
    }

    private static String choose(String... options) {
        return options[ThreadLocalRandom.current().nextInt(options.length)];
    }
}
