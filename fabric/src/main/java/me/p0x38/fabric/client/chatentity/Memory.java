package me.p0x38.fabric.client.chatentity;

import net.minecraft.core.BlockPos;

import java.util.Map;

public record Memory(
        Type type,
        long gameTick,
        float confidence,
        BlockPos position,
        Map<String, String> context
) {
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

    public Memory {
        confidence = Math.clamp(confidence, 0.0f, 1.0f);
        context = Map.copyOf(context);
    }

    public String context(String key) {
        return context.get(key);
    }
}
