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

    public BlockPos position() {
        return position;
    }

    public Mood mood() {
        return mood;
    }

    public String currentMessage() {
        return currentMessage;
    }

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
            long gameTick
    ) {
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
                            Map.of()
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
        if (suspicion >= 70) {
            mood = Mood.ANNOYED;
            return;
        }

        if (awareness >= 50) {
            mood = Mood.CURIOUS;
            return;
        }

        mood = Mood.CALM;
    }
}
