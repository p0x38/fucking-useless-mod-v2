package me.p0x38.fabric.client.blindspot;

import me.p0x38.fuckinguselessmod.util.DebugLogger;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;

import java.util.Locale;
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
            String username = latest.context("username");

            String response = chooseInteractionResponse(
                    message,
                    username,
                    sign
            );

            say(
                    sign,
                    response,
                    gameTick
            );

            sign.markAction(gameTick);
            return;
        }

        /*
         * React to the moment the player first notices the sign.
         */
        if (latest.type() == Memory.Type.PLAYER_SEEN
                && randomChance(0.18)) {
            say(
                    sign,
                    choose(
                            text("seen.1"),
                            text("seen.2"),
                            text("seen.3"),
                            text("seen.4"),
                            text("seen.5"),
                            text("seen.6")
                    ),
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
                && randomChance(0.50)) {
            String username = latest.context("username");

            say(
                    sign,
                    choose(
                            text("returned.1", username),
                            text("returned.2", username),
                            text("returned.3", username),
                            text("returned.4", username),
                            text("returned.5"),
                            text("returned.6")
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
                && randomChance(0.25)) {
            say(
                    sign,
                    choose(
                            text("curious.1"),
                            text("curious.2"),
                            text("curious.3"),
                            text("curious.4"),
                            text("curious.5"),
                            text("curious.6"),
                            text("curious.7"),
                            text("curious.8")
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
                            text("annoyed.1"),
                            text("annoyed.2"),
                            text("annoyed.3"),
                            text("annoyed.4"),
                            text("annoyed.5"),
                            text("annoyed.6"),
                            text("annoyed.7"),
                            text("annoyed.8")
                    ),
                    gameTick
            );

            sign.markAction(gameTick);
        }
    }

    private static String chooseInteractionResponse(
            String message,
            String username,
            SentientSign sign
    ) {
        String normalized = normalize(message);
        int interactionCount = sign.interactionCount();
        int annoyanceCount = sign.annoyanceCount();

        /*
         * Some questions are so specific that the sign reacts to
         * the subject itself instead of treating them as ordinary
         * conversation.
         */
        if (isUnsettlingQuestion(normalized)) {
            return choose(
                    text("unsettling.1"),
                    text("unsettling.2"),
                    text("unsettling.3"),
                    text("unsettling.4"),
                    text("unsettling.5"),
                    text("unsettling.6"),
                    text("unsettling.7"),
                    text("unsettling.8")
            );
        }

        /*
         * The sign occasionally answers with something only loosely
         * connected to the conversation. This becomes more common
         * as it grows familiar with the player.
         */
        if (!isQuestion(normalized)
                && !isGreeting(normalized)
                && (randomChance(sign.interactionCount() >= 10 ? 0.18 : 0.08))) {
            return choose(
                    text("out_of_place.1"),
                    text("out_of_place.2"),
                    text("out_of_place.3"),
                    text("out_of_place.4"),
                    text("out_of_place.5"),
                    text("out_of_place.6"),
                    text("out_of_place.7"),
                    text("out_of_place.8")
            );
        }

        if (isGreeting(normalized)) {
            if (interactionCount == 1) {
                return choose(
                        text("greeting.first.1", username),
                        text("greeting.first.2", username),
                        text("greeting.first.3", username),
                        text("greeting.first.4", username)
                );
            }

            if (interactionCount <= 3) {
                return choose(
                        text("greeting.again.1", username),
                        text("greeting.again.2", username),
                        text("greeting.again.3"),
                        text("greeting.again.4")
                );
            }

            return choose(
                    text("greeting.familiar.1", username),
                    text("greeting.familiar.2"),
                    text("greeting.familiar.3"),
                    text("greeting.familiar.4")
            );
        }

        if (isIdentityQuestion(normalized)) {
            if (interactionCount >= 5) {
                return choose(
                        text("identity.again.1"),
                        text("identity.again.2"),
                        text("identity.again.3"),
                        text("identity.again.4")
                );
            }

            return choose(
                    text("identity.1"),
                    text("identity.2"),
                    text("identity.3"),
                    text("identity.4"),
                    text("identity.5"),
                    text("identity.6"),
                    text("identity.7"),
                    text("identity.8")
            );
        }

        if (isQuestion(normalized)) {
            return choose(
                    text("question.1"),
                    text("question.2"),
                    text("question.3"),
                    text("question.4"),
                    text("question.5"),
                    text("question.6")
            );
        }

        if (annoyanceCount >= 6) {
            return choose(
                    text("annoyed.interaction.late.1"),
                    text("annoyed.interaction.late.2"),
                    text("annoyed.interaction.late.3"),
                    text("annoyed.interaction.late.4"),
                    text("annoyed.interaction.late.5")
            );
        }

        if (annoyanceCount >= 3) {
            return choose(
                    text("annoyed.interaction.mid.1"),
                    text("annoyed.interaction.mid.2"),
                    text("annoyed.interaction.mid.3"),
                    text("annoyed.interaction.mid.4"),
                    text("annoyed.interaction.mid.5")
            );
        }

        if (sign.mood() == SentientSign.Mood.PLAYFUL) {
            return choose(
                    text("playful.1"),
                    text("playful.2"),
                    text("playful.3"),
                    text("playful.4"),
                    text("playful.5"),
                    text("playful.6")
            );
        }

        if (sign.mood() == SentientSign.Mood.ANNOYED) {
            return choose(
                    text("annoyed.interaction.1"),
                    text("annoyed.interaction.2"),
                    text("annoyed.interaction.3"),
                    text("annoyed.interaction.4"),
                    text("annoyed.interaction.5"),
                    text("annoyed.interaction.6")
            );
        }

        if (sign.mood() == SentientSign.Mood.CURIOUS) {
            return choose(
                    text("interacted.curious.1"),
                    text("interacted.curious.2"),
                    text("interacted.curious.3"),
                    text("interacted.curious.4"),
                    text("interacted.curious.5"),
                    text("interacted.curious.6"),
                    text("interacted.curious.7")
            );
        }

        if (interactionCount >= 10) {
            return choose(
                    text("interacted.familiar.1"),
                    text("interacted.familiar.2"),
                    text("interacted.familiar.3"),
                    text("interacted.familiar.4"),
                    text("interacted.familiar.5")
            );
        }

        return choose(
                text("interacted.default.1"),
                text("interacted.default.2"),
                text("interacted.default.3"),
                text("interacted.default.4"),
                text("interacted.default.5"),
                text("interacted.default.6"),
                text("interacted.default.7"),
                text("interacted.default.8")
        );
    }

    private static boolean isUnsettlingQuestion(String message) {
        return message.contains("do you feel pain")
                || message.contains("are you afraid")
                || message.contains("are you scared")
                || message.contains("can you die")
                || message.contains("do signs die")
                || message.contains("can i break you")
                || message.contains("what happens if i break you")
                || message.contains("what happens if i destroy you")
                || message.contains("what happens when i break you")
                || message.contains("what happens when i look away")
                || message.contains("where do you go when i look away")
                || message.contains("do you know when i'm not looking")
                || message.contains("are you watching me")
                || message.contains("are you following me")
                || message.contains("can you see me when i'm not looking")
                || message.contains("can you remember me when i'm gone");
    }

    private static boolean isGreeting(String message) {
        return message.matches(
                "^(hi+|hello+|hey+|hiya|yo+|sup|howdy)[!.,? ]*$"
        );
    }

    private static boolean isIdentityQuestion(String message) {
        return message.equals("who are you")
                || message.equals("who r u")
                || message.equals("what are you")
                || message.equals("what's your name")
                || message.equals("what is your name")
                || message.equals("do you have a name")
                || message.equals("are you alive")
                || message.equals("are you real")
                || message.equals("are you sentient")
                || message.equals("are you conscious");
    }

    private static boolean isQuestion(String message) {
        return message.endsWith("?")
                || message.startsWith("why ")
                || message.startsWith("how ")
                || message.startsWith("what ")
                || message.startsWith("where ")
                || message.startsWith("when ")
                || message.startsWith("can you ")
                || message.startsWith("do you ");
    }

    private static String normalize(String message) {
        return message
                .trim()
                .toLowerCase(Locale.ROOT);
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

    private static String text(String key, String username) {
        String resolvedUsername =
                username == null || username.isBlank()
                        ? "you"
                        : username;

        return text(key)
                .replace("${username}", resolvedUsername);
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
