package me.p0x38.fuckinguselessmod.entity.goals;

/**
 * Represents the current state of an entity investigation.
 */
public enum InvestigationState {
    /**
     * No investigation is currently active.
     */
    INACTIVE,

    /**
     * Moving toward the investigation target.
     */
    APPROACHING,

    /**
     * Moving into a suitable observation position.
     */
    POSITIONING,

    /**
     * Following the investigation target.
     */
    FOLLOWING,

    /**
     * Observing the investigation target.
     */
    OBSERVING,

    /**
     * The investigation target has temporarily been lost.
     */
    LOST,

    /**
     * The investigation has finished.
     */
    FINISHED
}
