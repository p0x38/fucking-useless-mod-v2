package me.p0x38.fuckinguselessmod.entity.goals;

/**
 * Remains stationary while observing the current target.
 */
public final class WatchBehavior implements InvestigationBehavior {

    @Override
    public void tick(InvestigateEntityGoal goal) {
        goal.stopMovement();
        goal.setState(InvestigationState.OBSERVING);
        goal.incrementObservationTicks();
    }
}
