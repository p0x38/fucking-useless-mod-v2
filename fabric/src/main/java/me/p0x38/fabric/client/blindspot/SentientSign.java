package me.p0x38.fabric.client.blindspot;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SentientSign {
    public enum Mood {
        CALM,
        CURIOUS,
        ANNOYED,
        PLAYFUL,
        AFRAID
    }

    private final BlockPos position;

    private Mood mood = Mood.CALM;

    private int awareness = 0;
    private int suspicion = 0;
    private float curiosity = 0.15f;
    private float trust = 0.5f;
    private float irritation = 0.0f;
    private int interactionCount = 0;
    private SignConnectionMode connectionMode = SignConnectionMode.DISCONNECTED;

    private boolean hasBeenSeen;
    private boolean playerLooking;
    private boolean playerLookingAway;

    private long lastSeenTick = Long.MIN_VALUE;
    private long lastActionTick = Long.MIN_VALUE;

    private String currentMessage = "...";

    private final List<Memory> memories =
            new ArrayList<>();

    public SentientSign(BlockPos position) {
        this.position = position;
    }

    public BlockPos position() { return position; }

    public String id() { return "sign-" + Long.toUnsignedString(position.asLong(), 36); }

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
    public SignConnectionMode connectionMode() { return connectionMode; }

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
            SignConnectionMode connectionMode
    ) {
        this.connectionMode = connectionMode;
        boolean wasLooking = playerLooking;

        playerLooking = looking;
        playerLookingAway = !looking;

        if (!wasLooking && looking) {
            hasBeenSeen = true;
            lastSeenTick = gameTick;

            Memory.Type memoryType =
                    hasMemory(Memory.Type.PLAYER_LOOKED_AWAY)
                            ? Memory.Type.PLAYER_RETURNED
                            : Memory.Type.PLAYER_SEEN;

            remember(
                    new Memory(
                            memoryType,
                            gameTick,
                            1.0f,
                            position,
                            Map.of("connection", connectionMode.name())
                    )
            );

            awareness = Math.min(
                    100,
                    awareness + 1
            );
        }

        if (wasLooking && !looking) {
            remember(
                    new Memory(
                            Memory.Type.PLAYER_LOOKED_AWAY,
                            gameTick,
                            1.0f,
                            position,
                            Map.of()
                    )
            );

            suspicion = Math.min(
                    100,
                    suspicion + 1
            );
        }

        updateMood();
    }

    public void interact(String message, long gameTick, SignConnectionMode connectionMode) {
        if (message == null || message.isBlank()) return;
        this.connectionMode = connectionMode;
        interactionCount++;
        trust = Math.clamp(trust + 0.04f, 0.0f, 1.0f);
        curiosity = Math.clamp(curiosity + 0.05f, 0.0f, 1.0f);
        irritation = Math.clamp(irritation - 0.03f, 0.0f, 1.0f);
        remember(new Memory(Memory.Type.PLAYER_INTERACTED, gameTick, 1.0f, position,
                Map.of("message", message.trim(), "connection", connectionMode.name())));
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

    public void setCurrentMessage(String message) {
        currentMessage = message;
    }

    public void markAction(long gameTick) {
        lastActionTick = gameTick;
    }

    private void updateMood() {
        if (irritation >= 0.65f || suspicion >= 70) {
            mood = Mood.ANNOYED;
            return;
        }

        if (trust >= 0.78f && curiosity >= 0.5f) {\n            mood = Mood.PLAYFUL;\n            return;\n        }\n\n        if (curiosity >= 0.45f || awareness >= 30) {
            mood = Mood.CURIOUS;
            return;
        }

        mood = Mood.CALM;
    }
}
