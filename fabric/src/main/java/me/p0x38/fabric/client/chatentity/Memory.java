package me.p0x38.fabric.client.chatentity;

import net.minecraft.core.BlockPos;

import java.util.Map;

public final class Memory {
    public enum Type {
        PLAYER_SEEN,
        PLAYER_LOOKED_AWAY,
        PLAYER_RETURNED,
        PLAYER_LEFT,
        PLAYER_INTERACTED,
        PLAYER_IGNORED,
        CHAT_ENTITY_SPOKE,
        CHAT_ENTITY_MOVED,
        WORLD_CHANGED
    }

    private final Type type;
    private final long gameTick;
    private final float confidence;
    private final BlockPos position;
    private final Map<String, String> context;

    public Memory(
            Type type,
            long gameTick,
            float confidence,
            BlockPos blockPos,
            Map<String, String> context
    ) {
        this.type = type;
        this.gameTick = gameTick;
        this.confidence = Math.clamp(
                confidence,
                0.0f,
                1.0f
        );
        this.position = blockPos;
        this.context = Map.copyOf(context);
    }

    public Type type() {
        return type;
    }

    public long gameTick() {
        return gameTick;
    }

    public float confidence() {
        return confidence;
    }

    public BlockPos position() {
        return position;
    }

    public Map<String, String> context() {
        return context;
    }

    public String context(String key) {
        return context.get(key);
    }
}
