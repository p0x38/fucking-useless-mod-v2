package me.p0x38.fuckinguselessmod.entity.ai.investigation;

/**
 * Defines how an entity investigates another entity.
 */
public enum InvestigationMode {
    /**
     * Move directly toward the target until reaching the desired distance.
     */
    APPROACH,

    /**
     * Keep the entity within a preferred distance range from the target.
     */
    KEEP_DISTANCE,

    /**
     * Follow the target while attempting to avoid being noticed.
     */
    HIDE_AND_OBSERVE,

    /**
     * Follow the target while maintaining a reasonable distance.
     */
    FOLLOW,

    /**
     * Remain nearby and observe the target without actively following it.
     */
    WATCH
}
