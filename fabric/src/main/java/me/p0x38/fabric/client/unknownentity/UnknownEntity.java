package me.p0x38.fabric.client.unknownentity;

import me.p0x38.fuckinguselessmod.Config;
import me.p0x38.fuckinguselessmod.util.DebugLogger;

import net.minecraft.core.BlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/** Stores client-side state, memories, perception state, and pending responses. */
public final class UnknownEntity {
    /** Represents the entity's current emotional or behavioral state. */
    public enum Mood {
        /**
         * When the entity is calm.
         * Must be calm tone.
         */
        CALM,

        /**
         * When the entity has curious to something.
         * This can be triggered when player has doing familiar stuff to it.
         */
        CURIOUS,

        /**
         * When the entity has been annoyed.
         * This can be triggered when player has been annoying it by its chat and their behavior taken to it.
         */
        ANNOYED,

        /**
         * When the entity has been silly mode.
         * This can be triggered when silly mode are toggled on in config.
         */
        PLAYFUL,

        /**
         * When the entity has been afraid to something.
         * This is currently unused, but planned to be used future.
         */
        AFRAID
    }

    /** Represents a response waiting to be delivered. */
    public record PendingResponse(
            String message,
            ReactionKind reactionKind,
            long thinkingUntilTick,
            long sendTick
    ) {}

    private static final int MAX_PENDING_RESPONSES = 4;

    private final BlockPos origin;

    private Mood mood = Mood.CALM;

    private int awareness = 0;
    private int suspicion = 0;
    private float curiosity = 0.15f;
    private float trust = 0.5f;
    private float irritation = 0.0f;
    private int interactionCount = 0;
    private int annoyanceCount = 0;
    private int seenCount = 0;
    private int returnCount = 0;
    private int uncontrolledReactionCount = 0;
    private static final float SELF_AWARENESS = 0.95f;
    private static final float SELF_CONTROL = 0.30f;
    private ReactionKind lastReactionKind = ReactionKind.NORMAL;
    private ChatConnectionMode connectionMode = ChatConnectionMode.DISCONNECTED;

    private boolean playerLooking;

    private long lastActionTick = Long.MIN_VALUE;
    private Memory lastProcessedMemory;

    private String currentMessage = "...";
    private boolean active;

    private final Deque<PendingResponse> pendingResponses =
            new ArrayDeque<>();

    private BlockPos lastKnownPlayerBlockPosition;

    private final List<Memory> memories =
            new ArrayList<>();

    /** Creates an inactive entity at the specified origin. */
    public UnknownEntity(BlockPos origin) {
        this.origin = origin;
    }

    public BlockPos origin() { return origin; }

    public String id() {
        return "chat-" + Long.toUnsignedString(origin.asLong(), 36);
    }

    public Mood mood() {
        return mood;
    }

    public String currentMessage() { return currentMessage; }

    /** @return whether the entity is active. */
    public boolean isActive() {
        return active;
    }

    /** Activates the entity. */
    public void activate() {
        if (!active) {
            active = true;

            DebugLogger.debug(
                    "[ChatEntity] activated id={} origin={}",
                    id(),
                    origin
            );
        }
    }

    /** @return the entity's awareness value. */
    public int awareness() { return awareness; }
    /** @return the entity's suspicion value. */
    public int suspicion() { return suspicion; }
    /** @return the entity's curiosity value. */
    public float curiosity() { return curiosity; }
    /** @return the entity's trust value. */
    public float trust() { return trust; }
    /** @return the entity's irritation value. */
    public float irritation() { return irritation; }
    /** @return the number of recorded player interactions. */
    public int interactionCount() { return interactionCount; }
    /** @return the number of recorded annoyance events. */
    public int annoyanceCount() { return annoyanceCount; }
    /** @return the number of times the player has been observed. */
    public int seenCount() { return seenCount; }
    /** @return the number of times the player has returned. */
    public int returnCount() { return returnCount; }
    /** @return the number of uncontrolled unsettling or out-of-place reactions. */
    public int uncontrolledReactionCount() { return uncontrolledReactionCount; }
    /** @return the entity's self-awareness factor. */
    public float selfAwareness() { return SELF_AWARENESS; }
    /** @return the entity's self-control factor. */
    public float selfControl() { return SELF_CONTROL; }
    /** @return the most recently delivered reaction kind. */
    public ReactionKind lastReactionKind() { return lastReactionKind; }
    /** @return the current chat connection mode. */
    public ChatConnectionMode connectionMode() { return connectionMode; }
    public Memory lastProcessedMemory() { return lastProcessedMemory; }

    public List<Memory> memories() {
        return List.copyOf(memories);
    }

    public Memory latestMemory() {
        return memories.isEmpty()
                ? null
                : memories.getLast();
    }

    public boolean hasMemory(Memory.Type type) {
        return memories.stream()
                .anyMatch(memory ->
                        memory.type() == type
                );
    }

    public long countMemories(Memory.Type type) {
        return memories.stream()
                .filter(memory ->
                        memory.type() == type
                )
                .count();
    }

    public Memory latestMemory(Memory.Type type) {
        for (int i = memories.size() - 1; i >= 0; i--) {
            Memory memory = memories.get(i);

            if (memory.type() == type) {
                return memory;
            }
        }

        return null;
    }

    public void observe(
            boolean looking,
            long gameTick,
            ChatConnectionMode connectionMode,
            String username
    ) {
        this.connectionMode = connectionMode;
        boolean wasLooking = playerLooking;

        if (wasLooking != looking) {
            DebugLogger.debug(
                    "[ChatEntity] perception transition id={} looking={} tick={} connection={} username={} seenCount={} annoyanceCount={} returnCount={}",
                    id(),
                    looking,
                    gameTick,
                    connectionMode,
                    username,
                    seenCount,
                    annoyanceCount,
                    returnCount
            );
        }

        playerLooking = looking;

        if (!wasLooking && looking) {
            boolean hasLookedAwayBefore =
                    hasMemory(Memory.Type.PLAYER_LOOKED_AWAY);

            Memory.Type memoryType =
                    hasLookedAwayBefore
                            ? Memory.Type.PLAYER_RETURNED
                            : Memory.Type.PLAYER_SEEN;

            seenCount++;

            if (hasLookedAwayBefore) {
                returnCount++;
            }

            remember(
                    new Memory(
                            memoryType,
                            gameTick,
                            1.0f,
                            origin,
                            Map.of(
                                    "connection",
                                    connectionMode.name(),
                                    "username",
                                    username == null ? "" : username
                            )
                    )
            );

            awareness = Math.min(
                    100,
                    awareness + 1
            );
        }

        if (wasLooking && !looking) {
            annoyanceCount++;

            remember(
                    new Memory(
                            Memory.Type.PLAYER_LOOKED_AWAY,
                            gameTick,
                            1.0f,
                            origin,
                            Map.of(
                                    "username",
                                    username == null ? "" : username
                            )
                    )
            );

            suspicion = Math.min(
                    100,
                    suspicion + 1
            );
        }

        updateMood();
    }

    public void interact(
            String message,
            long gameTick,
            ChatConnectionMode connectionMode,
            String username
    ) {
        interact(
                message,
                gameTick,
                connectionMode,
                username,
                0L
        );
    }

    public void interact(
            String message,
            long gameTick,
            ChatConnectionMode connectionMode,
            String username,
            long idleTicksBeforeInteraction
    ) {
        if (message == null || message.isBlank()) {
            return;
        }

        this.connectionMode = connectionMode;
        interactionCount++;

        DebugLogger.debug(
                "[ChatEntity] interaction id={} count={} tick={} connection={} username={} message={}",
                id(),
                interactionCount,
                gameTick,
                connectionMode,
                username,
                message.trim()
        );

        trust = Math.clamp(trust + 0.04f, 0.0f, 1.0f);
        curiosity = Math.clamp(curiosity + 0.05f, 0.0f, 1.0f);
        irritation = Math.clamp(irritation - 0.03f, 0.0f, 1.0f);

        remember(
                new Memory(
                        Memory.Type.PLAYER_INTERACTED,
                        gameTick,
                        1.0f,
                        origin,
                        Map.of(
                                "message",
                                message.trim(),
                                "connection",
                                connectionMode.name(),
                                "username",
                                username == null ? "" : username,
                                "idleTicksBeforeInteraction",
                                Long.toString(
                                        Math.max(
                                                0L,
                                                idleTicksBeforeInteraction
                                        )
                                )
                        )
                )
        );

        updateMood();
    }

    public boolean canAct(long gameTick) {
        return gameTick - lastActionTick >= 10;
    }

    public void remember(Memory memory) {
        memories.add(memory);

        if (memories.size() > 128) {
            memories.removeFirst();
        }
    }

    public void recordReaction(ReactionKind reactionKind) {
        lastReactionKind = reactionKind;

        if (reactionKind == ReactionKind.UNSETTLING
                || reactionKind == ReactionKind.OUT_OF_PLACE) {
            uncontrolledReactionCount++;
        }
    }

    public void observeLocation(
            long gameTick,
            String dimension,
            BlockPos playerBlockPosition,
            double x,
            double y,
            double z
    ) {
        if (!Config.get().chatEntityLocationAwareness
                || playerBlockPosition == null
                || playerBlockPosition.equals(lastKnownPlayerBlockPosition)) {
            return;
        }

        lastKnownPlayerBlockPosition =
                playerBlockPosition.immutable();

        remember(
                new Memory(
                        Memory.Type.PLAYER_LOCATION_UPDATED,
                        gameTick,
                        1.0f,
                        playerBlockPosition,
                        Map.of(
                                "dimension",
                                dimension,
                                "x",
                                Double.toString(x),
                                "y",
                                Double.toString(y),
                                "z",
                                Double.toString(z)
                        )
                )
        );

        DebugLogger.debug(
                "[ChatEntity] location updated id={} dimension={} block={}",
                id(),
                dimension,
                playerBlockPosition
        );
    }

    public void queueResponse(
            String message,
            ReactionKind reactionKind,
            long currentTick,
            long thinkingTicks,
            long typingTicks
    ) {
        if (message == null || message.isBlank()) {
            return;
        }

        if (pendingResponses.size() >= MAX_PENDING_RESPONSES) {
            pendingResponses.removeFirst();

            DebugLogger.debug(
                    "[ChatEntity] dropped oldest pending response id={} queueSize={}",
                    id(),
                    pendingResponses.size()
            );
        }

        long safeThinkingTicks = Math.max(1, thinkingTicks);
        long safeTypingTicks = Math.max(1, typingTicks);

        long thinkingUntilTick =
                currentTick + safeThinkingTicks;

        long sendTick =
                thinkingUntilTick + safeTypingTicks;

        PendingResponse response =
                new PendingResponse(
                        message,
                        reactionKind,
                        thinkingUntilTick,
                        sendTick
                );

        pendingResponses.addLast(response);

        DebugLogger.debug(
                "[ChatEntity] response queued id={} now={} thinkingUntil={} sendTick={} thinkingTicks={} typingTicks={} kind={} message={}",
                id(),
                currentTick,
                thinkingUntilTick,
                sendTick,
                safeThinkingTicks,
                safeTypingTicks,
                reactionKind,
                message
        );
    }

    public PendingResponse pollDueResponse(long gameTick) {
        PendingResponse dueResponse = null;

        for (PendingResponse response : pendingResponses) {
            if (response.sendTick() <= gameTick
                    && (dueResponse == null
                    || response.sendTick() < dueResponse.sendTick())) {
                dueResponse = response;
            }
        }

        if (dueResponse == null) {
            return null;
        }

        pendingResponses.remove(dueResponse);
        return dueResponse;
    }

    /** Clears all queued responses. */
    public void clearPendingResponses() {
        if (pendingResponses.isEmpty()) {
            return;
        }

        int cleared = pendingResponses.size();
        pendingResponses.clear();

        DebugLogger.debug(
                "[ChatEntity] cleared pending responses id={} count={}",
                id(),
                cleared
        );
    }

    /** @return the number of queued responses. */
    public int pendingResponseCount() {
        return pendingResponses.size();
    }

    public boolean isResponseTyping(long gameTick) {
        PendingResponse response =
                pendingResponses.peekFirst();

        return response != null
                && gameTick >= response.thinkingUntilTick()
                && gameTick < response.sendTick();
    }

    public void deliverResponse(
            PendingResponse response,
            long gameTick
    ) {
        recordReaction(response.reactionKind());
        setCurrentMessage(response.message());

        remember(
                new Memory(
                        Memory.Type.CHAT_ENTITY_SPOKE,
                        gameTick,
                        1.0f,
                        origin,
                        Map.of(
                                "message",
                                response.message()
                        )
                )
        );

        DebugLogger.debug(
                "[ChatEntity] response delivered id={} tick={} kind={} message={}",
                id(),
                gameTick,
                response.reactionKind(),
                response.message()
        );
    }

    public void setCurrentMessage(String message) {
        if (!java.util.Objects.equals(currentMessage, message)) {
            DebugLogger.debug(
                    "[ChatEntity] message changed id={} old={} new={}",
                    id(),
                    currentMessage,
                    message
            );
        }

        currentMessage = message;
    }

    public void markAction(long gameTick) {
        lastActionTick = gameTick;
    }

    public void markMemoryProcessed(Memory memory) {
        lastProcessedMemory = memory;
    }

    private void updateMood() {
        if (irritation >= 0.65f || suspicion >= 70) {
            mood = Mood.ANNOYED;
            return;
        }

        if (trust >= 0.78f && curiosity >= 0.5f) {
            mood = Mood.PLAYFUL;
            return;
        }

        if (curiosity >= 0.45f || awareness >= 30) {
            mood = Mood.CURIOUS;
            return;
        }

        mood = Mood.CALM;
    }
}
