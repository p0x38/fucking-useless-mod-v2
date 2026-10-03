package me.p0x38.fabric.client.chatentity;

import me.p0x38.fuckinguselessmod.util.DebugLogger;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ChatEntity {
    public enum Mood {
        CALM,
        CURIOUS,
        ANNOYED,
        PLAYFUL,
        AFRAID
    }

    public enum ReactionKind {
        NORMAL,
        GREETING,
        IDENTITY,
        QUESTION,
        UNSETTLING,
        OUT_OF_PLACE,
        PLAYFUL,
        ANNOYED,
        META
    }

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
    private float selfAwareness = 0.95f;
    private float selfControl = 0.30f;
    private ReactionKind lastReactionKind = ReactionKind.NORMAL;
    private ChatConnectionMode connectionMode = ChatConnectionMode.DISCONNECTED;

    private boolean hasBeenSeen;
    private boolean playerLooking;
    private boolean playerLookingAway;

    private long lastSeenTick = Long.MIN_VALUE;
    private long lastActionTick = Long.MIN_VALUE;
    private Memory lastProcessedMemory;

    private String currentMessage = "...";

    private final List<Memory> memories =
            new ArrayList<>();

    public ChatEntity(BlockPos origin) {
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
    public int awareness() { return awareness; }
    public int suspicion() { return suspicion; }
    public float curiosity() { return curiosity; }
    public float trust() { return trust; }
    public float irritation() { return irritation; }
    public int interactionCount() { return interactionCount; }
    public int annoyanceCount() { return annoyanceCount; }
    public int seenCount() { return seenCount; }
    public int returnCount() { return returnCount; }
    public int uncontrolledReactionCount() { return uncontrolledReactionCount; }
    public float selfAwareness() { return selfAwareness; }
    public float selfControl() { return selfControl; }
    public ReactionKind lastReactionKind() { return lastReactionKind; }
    public ChatConnectionMode connectionMode() { return connectionMode; }
    public Memory lastProcessedMemory() { return lastProcessedMemory; }

    public List<Memory> memories() {
        return List.copyOf(memories);
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
        playerLookingAway = !looking;

        if (!wasLooking && looking) {
            hasBeenSeen = true;
            lastSeenTick = gameTick;

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
                                username == null ? "" : username
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
