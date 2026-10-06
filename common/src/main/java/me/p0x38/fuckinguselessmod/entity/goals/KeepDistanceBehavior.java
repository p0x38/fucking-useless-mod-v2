package me.p0x38.fuckinguselessmod.entity.goals;

/**
 * Maintains a configurable distance range from the current target.
 */
public final class KeepDistanceBehavior implements InvestigationBehavior {

    @Override
    public void tick(InvestigateEntityGoal goal) {
        double distanceSqr = goal.distanceToTargetSqr();

        double minDistance = goal.getSettings().getMinDistance();
        double maxDistance = goal.getSettings().getMaxDistance();

        if (distanceSqr < minDistance * minDistance) {
            goal.moveAwayFromTarget();
            return;
        }

        if (distanceSqr > maxDistance * maxDistance) {
            goal.moveTowardTarget();
            return;
        }

        goal.stopMovement();
        goal.setState(InvestigationState.OBSERVING);
        goal.incrementObservationTicks();
    }
}
