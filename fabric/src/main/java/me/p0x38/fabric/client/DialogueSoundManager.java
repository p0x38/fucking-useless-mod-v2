package me.p0x38.fabric.client;

import com.mojang.authlib.GameProfile;
import me.p0x38.fuckinguselessmod.Config;
import me.p0x38.fuckinguselessmod.sounds.ModSounds;
import me.p0x38.fuckinguselessmod.util.DebugLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.PlayerChatMessage;
import java.time.Instant;
import java.util.*;

public final class DialogueSoundManager {

    private static final Queue<DialogueMessage> QUEUE =
            new ArrayDeque<>();

    /*
     * One player = one voice.
     *
     * The selected sound is calculated from the player's UUID
     * and cached here so every letter/message uses the same sound.
     */
    private static final Map<UUID, String> PLAYER_VOICES =
            new HashMap<>();

    private static String playerVoicePoolKey;

    /*
     * The signature uniquely identifies a signed chat message.
     *
     * This prevents the same received message from being queued
     * multiple times while still allowing the same player to send
     * identical text again as a new message.
     */
    private static UUID lastSenderUuid;
    private static String lastMessage;
    private static Instant lastReceptionTimestamp;
    private static MessageSignature lastMessageSignature;

    private static DialogueMessage current;

    /*
     * The currently playing dialogue sound.
     *
     * Every new dialogue sound stops this instance first so
     * dialogue sounds never overlap.
     */
    private static SoundInstance currentSound;

    private static int interval;
    private static int soundsPlayed;

    private DialogueSoundManager() {
    }

    public static void queue(
            GameProfile sender,
            String message,
            Instant receptionTimestamp,
            PlayerChatMessage signedMessage
    ) {
        DebugLogger.debug(
                "[Dialogue] queue() sender={} uuid={} message={} timestamp={} queueSize={}",
                sender != null ? sender.name() : "<null>",
                sender != null ? sender.id() : "<null>",
                message,
                receptionTimestamp,
                QUEUE.size()
        );

        if (sender == null
                || message == null
                || message.isBlank()) {
            DebugLogger.debug(
                    "[Dialogue] queue() ignored: invalid input"
            );
            return;
        }

        Config.Data config = Config.get();

        if (!config.dialogueSoundsEnabled) {
            DebugLogger.debug(
                    "[Dialogue] queue() ignored: dialogue sounds disabled"
            );
            return;
        }

        MessageSignature signature =
                signedMessage != null
                    ? signedMessage.signature()
                        : null;

        if (signature != null) {
            if (signature.equals(lastMessageSignature)) {
                return;
            }

            lastMessageSignature = signature;
        } else {
            /*
             * Unsigned message handling
             */
            if (sender.id().equals(lastSenderUuid)
                && message.equals(lastMessage)
                && receptionTimestamp != null
                && receptionTimestamp.equals(lastReceptionTimestamp)) {
                return;
            }

            lastSenderUuid = sender.id();
            lastMessage = message;
            lastReceptionTimestamp = receptionTimestamp;
        }

        DialogueMessage next = new DialogueMessage(
                message,
                getPlayerVoice(sender.id(), config)
        );

        DebugLogger.debug(
                "[Dialogue] created DialogueMessage uuid={} voiceId={} textLength={}",
                sender.id(),
                next.voiceId,
                message.codePointCount(0, message.length())
        );

        if (config.dialogueQueueMessages) {
            QUEUE.add(next);

            DebugLogger.debug(
                    "[Dialogue] message queued queueSize={}",
                    QUEUE.size()
            );
            return;
        }

        QUEUE.clear();
        stopCurrentSound();
        current = next;
        interval = 0;
        soundsPlayed = 0;

        DebugLogger.debug(
                "[Dialogue] message became current voiceId={}",
                current.voiceId
        );
    }

    public static void tick() {
        Config.Data config = Config.get();

        if (!config.dialogueSoundsEnabled) {
            QUEUE.clear();
            stopCurrentSound();
            current = null;
            return;
        }

        if (current == null) {
            current = QUEUE.poll();

            if (current == null) {
                return;
            }

            DebugLogger.debug(
                    "[Dialogue] dequeued message voiceId={} queueSize={}",
                    current.voiceId,
                    QUEUE.size()
            );

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
            DebugLogger.debug(
                    "[Dialogue] message finished soundsPlayed={}",
                    soundsPlayed
            );
            current = null;
            return;
        }
        DebugLogger.debug(
                "[Dialogue] advance() succeeded index={} soundsPlayed={}",
                current.index,
                soundsPlayed
        );

        if (current.shouldPlay(config)) {
            playCurrentSound(config);
            soundsPlayed++;

            DebugLogger.debug(
                    "[Dialogue] sound played count={} voiceId={}",
                    soundsPlayed,
                    current.voiceId
            );
        } else {
            DebugLogger.debug(
                    "[Dialogue] sound skipped by chance"
            );
        }

        interval = chooseInterval(
                config,
                current.random
        );
    }

    private static void playCurrentSound(
            Config.Data config
    ) {
        /*
         * IMPORTANT:
         * current.voiceId never changes during this message.
         */
        float pitch = current.choosePitch(config);

        DebugLogger.debug(
                "[Dialogue] playCurrentSound voiceId={} pitch={} volume={}",
                current.voiceId,
                pitch,
                config.dialogueVolume
        );

        stopCurrentSound();

        currentSound = SimpleSoundInstance.forUI(
                ModSounds.getDialogueSound(
                        current.voiceId
                ),
                pitch,
                config.dialogueVolume
        );

        Minecraft.getInstance().getSoundManager().play(currentSound);
    }

    private static void stopCurrentSound() {
        if (currentSound == null) {
            return;
        }

        Minecraft.getInstance().getSoundManager().stop(currentSound);

        currentSound = null;
    }

    private static String getPlayerVoice(
            UUID uuid,
            Config.Data config
    ) {
        List<String> pool = createSoundPool(config.dialogueSoundPool);
        String poolKey = String.join("\u0000", pool);

        if (!poolKey.equals(playerVoicePoolKey)) {
            PLAYER_VOICES.clear();
            playerVoicePoolKey = poolKey;
        }

        return PLAYER_VOICES.computeIfAbsent(
                uuid,
                ignored -> choosePlayerVoice(uuid, pool)
        );
    }

    private static String choosePlayerVoice(
            UUID uuid,
            List<String> configuredPool
    ) {

        long seed =
                uuid.getMostSignificantBits()
                        ^ Long.rotateLeft(
                        uuid.getLeastSignificantBits(),
                        32
                );

        Random random =
                new Random(mix64(seed));

        return configuredPool.get(
                random.nextInt(configuredPool.size())
        );
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private static int chooseInterval(
            Config.Data config,
            Random random
    ) {
        if (config.dialogueIntervalMin
                == config.dialogueIntervalMax) {
            return config.dialogueIntervalMin;
        }

        return random.nextInt(
                config.dialogueIntervalMin,
                config.dialogueIntervalMax + 1
        );
    }

    private static List<String> createSoundPool(
            List<String> configured
    ) {
        List<String> result = new ArrayList<>();

        if (configured != null) {
            for (String value : configured) {
                var identifier = ModSounds.normalizeDialogueSoundId(value);

                if (identifier != null) {
                    /*
                     * Duplicate entries intentionally act as weight.
                     */
                    result.add(identifier.toString());
                }
            }
        }

        if (result.isEmpty()) {
            for (int index = 0; index < ModSounds.DIALOGUE_SOUND_COUNT; index++) {
                result.add(
                        ModSounds.normalizeDialogueSoundId(
                                String.valueOf(index)
                        ).toString()
                );
            }
        }

        return List.copyOf(result);
    }

    private static final class DialogueMessage {
        private final String message;

        /*
         * Fixed for the entire message.
         */
        private final String voiceId;

        private final Random random;

        private int index;
        private boolean insideWord;
        private boolean emittedMessage;

        private DialogueMessage(
                String message,
                String voiceId
        ) {
            this.message = message;
            this.voiceId = voiceId;

            /*
             *
             * The player's UUID is intentionally NOT used here.
             * UUID -> voiceId is handled separately by
             * getPlayerVoice().
             */
            this.random = new Random();
        }

        private boolean advance(
                Config.Data config
        ) {
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

        private boolean advanceWord(
                Config.Data config
        ) {
            while (index < message.length()) {
                int codePoint =
                        message.codePointAt(index);

                index += Character.charCount(codePoint);

                boolean wordCharacter =
                        Character.isLetterOrDigit(codePoint)
                                && (
                                !Character.isDigit(codePoint)
                                        || !config.dialogueSkipNumbers
                        );

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

        private boolean advanceCharacter(
                Config.Data config
        ) {
            while (index < message.length()) {
                int codePoint =
                        message.codePointAt(index);

                index += Character.charCount(codePoint);

                if (isEligible(codePoint, config)) {
                    /*
                     * Exactly ONE successful advance means
                     * exactly ONE letter/unit can produce ONE sound.
                     */
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

        private boolean shouldPlay(
                Config.Data config
        ) {
            return random.nextFloat()
                    < config.dialogueSoundChance;
        }

        private float choosePitch(Config.Data config) {
            if (!config.dialogueRandomizePitch
                    || config.dialoguePitchVariation <= 0.0f) {
                return config.dialoguePitch;
            }

            float variation =
                    (random.nextFloat() * 2.0f - 1.0f)
                            * config.dialoguePitchVariation;

            return Math.clamp(
                    config.dialoguePitch + variation,
                    0.5f,
                    2.0f
            );
        }

        private static boolean isPunctuation(
                int codePoint
        ) {
            int type =
                    Character.getType(codePoint);

            return type
                    == Character.CONNECTOR_PUNCTUATION
                    || type
                    == Character.DASH_PUNCTUATION
                    || type
                    == Character.START_PUNCTUATION
                    || type
                    == Character.END_PUNCTUATION
                    || type
                    == Character.INITIAL_QUOTE_PUNCTUATION
                    || type
                    == Character.FINAL_QUOTE_PUNCTUATION
                    || type
                    == Character.OTHER_PUNCTUATION;
        }
    }
}