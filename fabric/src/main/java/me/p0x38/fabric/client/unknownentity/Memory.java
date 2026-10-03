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

    public String context(String key) {
        return context.get(key);
    }
}
