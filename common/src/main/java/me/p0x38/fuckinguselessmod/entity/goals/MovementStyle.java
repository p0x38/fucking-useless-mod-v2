package me.p0x38.fuckinguselessmod.entity.goals;

/**
 * Defines the preferred movement style used by an AI goal.
 */
public enum MovementStyle {
    /**
     * Normal movement.
     */
    WALK,

    /**
     * Sneaking-style movement, when supported by the entity.
     */
    SNEAK,

    /**
     * Fast movement, when supported by the entity.
     */
    SPRINT
}
