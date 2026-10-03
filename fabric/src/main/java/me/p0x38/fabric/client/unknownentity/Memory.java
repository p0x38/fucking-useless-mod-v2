package me.p0x38.fabric.client.unknownentity;

import net.minecraft.core.BlockPos;

import java.util.Map;

/**
 * Represents an observation, interaction, or event remembered by the
 * Unknown Entity.
 *
 * @param type the type of event
 * @param gameTick the game tick at which the event occurred
 * @param confidence the entity's confidence in the memory
 * @param position the relevant world position
 * @param context additional information associated with the memory
 */
public record Memory(
        Type type,
        long gameTick,
        float confidence,
        BlockPos position,
        Map<String, String> context
) {

    /**
     * Represents the type of event stored in a memory.
     */
    public enum Type {

        /**
         * The entity sees the player.
         */
        PLAYER_SEEN,

        /**
         * The player looks away from the entity.
         */
        PLAYER_LOOKED_AWAY,

        /**
         * The player returns after previously leaving.
         */
        PLAYER_RETURNED,

        /**
         * The player leaves the entity's observed area.
         */
        PLAYER_LEFT,

        /**
         * The player directly interacts with the entity.
         */
        PLAYER_INTERACTED,

        /**
         * The player's location is updated.
         */
        PLAYER_LOCATION_UPDATED,

        /**
         * The player remains inactive for long enough to trigger
         * an idle event.
         */
        PLAYER_IDLE,

        /**
         * The player takes damage.
         */
        PLAYER_DAMAGED,

        /**
         * The player dies.
         */
        PLAYER_DIED,

        /**
         * The player ignores the entity.
         */
        PLAYER_IGNORED,

        /**
         * The entity speaks.
         */
        CHAT_ENTITY_SPOKE,

        /**
         * The entity moves.
         */
        CHAT_ENTITY_MOVED,

        /**
         * The world or dimension changes.
         */
        WORLD_CHANGED
    }
}