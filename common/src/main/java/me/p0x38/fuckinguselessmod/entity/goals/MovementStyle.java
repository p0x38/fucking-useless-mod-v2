package me.p0x38.fuckinguselessmod.entity.goals;

/**
 * Defines the preferred movement style used by an AI goal.
 *
 * <p>The corresponding movement state is applied by the goal's movement
 * controller. Not every mob needs to implement a unique movement system;
 * the standard entity movement flags are used where available.</p>
 */
public enum MovementStyle {

    /**
     * Normal movement.
     */
    WALK,

    /**
     * Sneaking-style movement.
     */
    SNEAK,

    /**
     * Fast, sprinting-style movement.
     */
    SPRINT
}
