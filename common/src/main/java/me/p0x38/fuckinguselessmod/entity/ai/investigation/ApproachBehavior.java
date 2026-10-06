package me.p0x38.fuckinguselessmod.entity.ai.investigation;

import me.p0x38.fuckinguselessmod.entity.ai.goals.InvestigateEntityGoal;

/**
 * Directly approaches the current investigation target.
 */
public final class ApproachBehavior implements InvestigationBehavior {

    @Override
    public void tick(InvestigateEntityGoal goal) {
        double preferredDistance = goal.getSettings().getPreferredDistance();

        if (goal.distanceToTargetSqr() > preferredDistance * preferredDistance) {
            goal.moveTowardTarget();
            goal.setState(InvestigationState.APPROACHING);
        } else {
            goal.stopMovement();
            goal.setState(InvestigationState.OBSERVING);
            goal.incrementObservationTicks();
        }
    }
}
