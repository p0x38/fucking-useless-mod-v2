package me.p0x38.fabric.client.unknownentity;

import net.minecraft.core.BlockPos;

import java.util.Map;

/** When the entity moves from its previous position. */

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

    /**
     * Returns a value stored in this memory's context map.
     *
     * @param key context key to look up
     * @return the associated value, or {@code null} when absent
     */
    public String context(String key) {
        return context.get(key);
    }
}
