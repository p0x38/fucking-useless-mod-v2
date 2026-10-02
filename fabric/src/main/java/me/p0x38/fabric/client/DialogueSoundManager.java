package me.p0x38.fabric.client;

import com.mojang.authlib.GameProfile;
import me.p0x38.fuckinguselessmod.sounds.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.UUID;

public final class DialogueSoundManager {
    private static final int DEFAULT_INTERVAL_TICKS = 2;
    private static final float DEFAULT_VOLUME = 0.5F;
    private static final float MIN_PITCH = 0.9F;
    private static final float MAX_PITCH = 1.1F;

    private static final Queue<DialogueMessage> QUEUE =
            new ArrayDeque<>();

    private static DialogueMessage current;
    private static int tickCounter;

    private DialogueSoundManager() {
    }

    public static void queue(
            GameProfile sender,
            String message
    ) {
        if (sender == null || message == null || message.isBlank()) {
            return;
        }

        QUEUE.add(
                new DialogueMessage(
                        sender.id(),
                        message
                )
        );
    }

    public static void tick() {
        if (current == null) {
            current = QUEUE.poll();

            if (current == null) {
                return;
            }

            tickCounter = DEFAULT_INTERVAL_TICKS;
        }

        if (tickCounter > 0) {
            tickCounter--;
            return;
        }

        if (current.nextCodePoint()) {
            playCurrentSound();
            tickCounter = DEFAULT_INTERVAL_TICKS;
            return;
        }

        current = null;
    }

    private static void playCurrentSound() {
        int soundIndex = current.chooseSoundIndex();
        float pitch = current.choosePitch();

        Minecraft.getInstance()
                .getSoundManager()
                .play(
                        SimpleSoundInstance.forUI(
                                ModSounds.getDialogueSound(soundIndex),
                                DEFAULT_VOLUME,
                                pitch
                        )
                );
    }

    private static final class DialogueMessage {
        private final UUID senderUuid;
        private final String message;

        private int charIndex;
        private int characterNumber;

        private DialogueMessage(
                UUID senderUuid,
                String message
        ) {
            this.senderUuid = senderUuid;
            this.message = message;
        }

        private boolean nextCodePoint() {
            while (charIndex < message.length()) {
                int codePoint =
                        message.codePointAt(charIndex);

                charIndex += Character.charCount(codePoint);

                if (Character.isLetterOrDigit(codePoint)) {
                    characterNumber++;
                    return true;
                }
            }

            return false;
        }

        private int chooseSoundIndex() {
            long seed = createSeed(0x1234L);

            return (int) Long.remainderUnsigned(
                    mix64(seed),
                    ModSounds.DIALOGUE_SOUND_COUNT
            );
        }

        private float choosePitch() {
            long seed = createSeed(0x5678L);
            long mixed = mix64(seed);

            double normalized =
                    (double) Long.remainderUnsigned(
                            mixed,
                            1_000_000L
                    ) / 1_000_000.0;

            return (float) (
                    MIN_PITCH
                            + normalized
                            * (MAX_PITCH - MIN_PITCH)
            );
        }

        private long createSeed(long salt) {
            long seed =
                    senderUuid.getMostSignificantBits()
                            ^ Long.rotateLeft(
                            senderUuid.getLeastSignificantBits(),
                            32
                    );

            seed ^= characterNumber
                    * 0x9E3779B97F4A7C15L;

            seed ^= salt;

            return seed;
        }

        private static long mix64(long value) {
            value ^= value >>> 30;
            value *= 0xBF58476D1CE4E5B9L;
            value ^= value >>> 27;
            value *= 0x94D049BB133111EBL;
            value ^= value >>> 31;
            return value;
        }
    }
}