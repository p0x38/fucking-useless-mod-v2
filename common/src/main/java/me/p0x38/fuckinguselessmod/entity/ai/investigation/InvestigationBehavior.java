package me.p0x38.fuckinguselessmod.entity.ai.investigation;

import me.p0x38.fuckinguselessmod.entity.ai.goals.InvestigateEntityGoal;

/**
 * A strategy used by {@link InvestigateEntityGoal} to perform one investigation mode.
 */
public interface InvestigationBehavior {

    /**
     * Called when this behavior becomes active.
     *
     * @param goal owning investigation goal
     */
    default void start(InvestigateEntityGoal goal) {}

    /**
     * Updates this behavior.
     *
     * @param goal owning investigation goal
     */
    void tick(InvestigateEntityGoal goal);

    /**
     * Called when this behavior stops being active.
     *
     * @param goal owning investigation goal
     */
    default void stop(InvestigateEntityGoal goal) {}
}
