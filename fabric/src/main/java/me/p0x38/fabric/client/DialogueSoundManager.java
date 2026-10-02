package me.p0x38.fabric.client;

import com.mojang.authlib.GameProfile;
import me.p0x38.fuckinguselessmod.Config;
import me.p0x38.fuckinguselessmod.sounds.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;

public final class DialogueSoundManager {
    private static final long UUID_MIX = 0x9E3779B97F4A7C15L;

    private static final Queue<DialogueMessage> QUEUE = new ArrayDeque<>();
    private static final Map<UUID, Long> MESSAGE_COUNTERS = new HashMap<>();

    private static DialogueMessage current;
    private static int interval;
    private static int soundsPlayed;

    private DialogueSoundManager() {
    }

    public static void queue(GameProfile sender, String message) {
        if (sender == null || message == null || message.isBlank()) {
            return;
        }

        Config.Data config = Config.get();

        if (!config.dialogueSoundsEnabled) {
            return;
        }

        long messageCounter = MESSAGE_COUNTERS.merge(
                sender.id(),
                1L,
                Long::sum
        );

        DialogueMessage next = new DialogueMessage(
                sender.id(),
                message,
                messageCounter,
                config
        );

        if (config.dialogueQueueMessages) {
            QUEUE.add(next);
            return;
        }

        QUEUE.clear();
        current = next;
        interval = 0;
        soundsPlayed = 0;
    }

    public static void tick() {
        Config.Data config = Config.get();

        if (!config.dialogueSoundsEnabled) {
            QUEUE.clear();
            current = null;
            return;
        }

        if (current == null) {
            current = QUEUE.poll();

            if (current == null) {
                return;
            }

            interval = 0;
            soundsPlayed = 0;
        }

        if (interval > 0) {
            interval--;
            return;
        }

        if (config.dialogueMaxSoundsPerMessage > 0
                && soundsPlayed >= config.dialogueMaxSoundsPerMessage) {
            current = null;
            return;
        }

        if (!current.advance(config)) {
            current = null;
            return;
        }

        if (current.shouldPlay(config)) {
            playCurrentSound(config);
            soundsPlayed++;
        }

        interval = chooseInterval(config, current.random);
    }

    private static void playCurrentSound(Config.Data config) {
        int soundIndex = current.chooseSound(config);
        float pitch = current.choosePitch(config);

        Minecraft.getInstance()
                .getSoundManager()
                .play(
                        SimpleSoundInstance.forUI(
                                ModSounds.getDialogueSound(soundIndex),
                                config.dialogueVolume,
                                pitch
                        )
                );
    }

    private static int chooseInterval(
            Config.Data config,
            Random random
    ) {
        if (config.dialogueIntervalMin == config.dialogueIntervalMax) {
            return config.dialogueIntervalMin;
        }

        return random.nextInt(
                config.dialogueIntervalMin,
                config.dialogueIntervalMax + 1
        );
    }

    private static final class DialogueMessage {
        private final UUID senderUuid;
        private final String message;
        private final long messageCounter;
        private final Random random;
        private final List<Integer> soundPool;

        private int index;
        private boolean insideWord;
        private boolean emittedMessage;
        private int lastSound = -1;

        private DialogueMessage(
                UUID senderUuid,
                String message,
                long messageCounter,
                Config.Data config
        ) {
            this.senderUuid = senderUuid;
            this.message = message;
            this.messageCounter = messageCounter;
            this.random = new Random(
                    createSeed(config.dialogueSeedMode)
            );
            this.soundPool = createSoundPool(config.dialogueSoundPool);
        }

        private boolean advance(Config.Data config) {
            return switch (config.dialoguePlaybackMode) {
                case MESSAGE -> advanceMessage();
                case WORD -> advanceWord(config);
                case CHARACTER -> advanceCharacter(config);
            };
        }

        private boolean advanceMessage() {
            if (emittedMessage) {
                return false;
            }

            emittedMessage = true;
            return true;
        }

        private boolean advanceWord(Config.Data config) {
            while (index < message.length()) {
                int codePoint = message.codePointAt(index);
                index += Character.charCount(codePoint);

                boolean wordCharacter =
                        Character.isLetterOrDigit(codePoint)
                                && (!Character.isDigit(codePoint)
                                || !config.dialogueSkipNumbers);

                if (wordCharacter) {
                    if (!insideWord) {
                        insideWord = true;
                        return true;
                    }
                } else {
                    insideWord = false;
                }
            }

            return false;
        }

        private boolean advanceCharacter(Config.Data config) {
            while (index < message.length()) {
                int codePoint = message.codePointAt(index);
                index += Character.charCount(codePoint);

                if (isEligible(codePoint, config)) {
                    return true;
                }
            }

            return false;
        }

        private boolean isEligible(
                int codePoint,
                Config.Data config
        ) {
            if (Character.isWhitespace(codePoint)) {
                return !config.dialogueSkipWhitespace;
            }

            if (Character.isDigit(codePoint)) {
                return !config.dialogueSkipNumbers;
            }

            if (isPunctuation(codePoint)) {
                return !config.dialogueSkipPunctuation;
            }

            return Character.isLetter(codePoint);
        }

        private boolean shouldPlay(Config.Data config) {
            return random.nextFloat() < config.dialogueSoundChance;
        }

        private int chooseSound(Config.Data config) {
            if (soundPool.size() == 1
                    || !config.dialogueAvoidRepeats) {
                int selected = soundPool.get(
                        random.nextInt(soundPool.size())
                );
                lastSound = selected;
                return selected;
            }

            int selected = lastSound;
            int attempts = 0;

            while (selected == lastSound && attempts++ < 16) {
                selected = soundPool.get(
                        random.nextInt(soundPool.size())
                );
            }

            lastSound = selected;
            return selected;
        }

        private float choosePitch(Config.Data config) {
            if (!config.dialogueRandomizePitch
                    || config.dialoguePitchMin == config.dialoguePitchMax) {
                return config.dialoguePitchMin;
            }

            return config.dialoguePitchMin
                    + random.nextFloat()
                    * (config.dialoguePitchMax - config.dialoguePitchMin);
        }

        private long createSeed(Config.DialogueSeedMode mode) {
            long seed =
                    senderUuid.getMostSignificantBits()
                            ^ Long.rotateLeft(
                            senderUuid.getLeastSignificantBits(),
                            32
                    );

            return switch (mode) {
                case UUID -> mix64(seed);
                case UUID_MESSAGE -> mix64(seed ^ message.hashCode());
                case UUID_MESSAGE_COUNTER -> mix64(
                        seed
                                ^ message.hashCode()
                                ^ messageCounter * UUID_MIX
                );
            };
        }

        private static List<Integer> createSoundPool(
                List<String> configured
        ) {
            List<Integer> result = new ArrayList<>();

            if (configured != null) {
                for (String value : configured) {
                    if (value == null) {
                        continue;
                    }

                    try {
                        int index = Integer.parseInt(value.trim());

                        if (index >= 0
                                && index < ModSounds.DIALOGUE_SOUND_COUNT) {
                            result.add(index);
                        }
                    } catch (NumberFormatException ignored) {
                        // Ignore invalid sound indices.
                    }
                }
            }

            if (result.isEmpty()) {
                for (
                        int index = 0;
                        index < ModSounds.DIALOGUE_SOUND_COUNT;
                        index++
                ) {
                    result.add(index);
                }
            }

            return List.copyOf(result);
        }

        private static boolean isPunctuation(int codePoint) {
            int type = Character.getType(codePoint);

            return type == Character.CONNECTOR_PUNCTUATION
                    || type == Character.DASH_PUNCTUATION
                    || type == Character.START_PUNCTUATION
                    || type == Character.END_PUNCTUATION
                    || type == Character.INITIAL_QUOTE_PUNCTUATION
                    || type == Character.FINAL_QUOTE_PUNCTUATION
                    || type == Character.OTHER_PUNCTUATION;
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
