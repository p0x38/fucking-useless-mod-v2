package me.p0x38.fabric.client.unknownentity;

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
        /**
         * When the entity saw player.
         */
        PLAYER_SEEN,

        /**
         * When the player looked away from the entity.
         */
        PLAYER_LOOKED_AWAY,

        /**
         * When the player returned near the entity.
         */
        PLAYER_RETURNED,

        /**
         * When the player goes away as far as entity can't see them.
         */
        PLAYER_LEFT,

        /**
         * When the player interacted with the entity directly via chat.
         */
        PLAYER_INTERACTED,

        /**
         * When the player moved in minecraft world.
         */
        PLAYER_LOCATION_UPDATED,

        /**
         * When the player idling.
         */
        PLAYER_IDLE,

        /**
         * When the player get damaged.
         */
        PLAYER_DAMAGED,

        /**
         * When the player died.
         */
        PLAYER_DIED,

        /**
         * When the player ignored the entity.
         */
        PLAYER_IGNORED,

        /**
         * When the entity spoke.
         */
        CHAT_ENTITY_SPOKE,

        /**
         * When the entity itself moved.
         */
        CHAT_ENTITY_MOVED,

        /**
         * When the world or dimension was changed.
         */
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
