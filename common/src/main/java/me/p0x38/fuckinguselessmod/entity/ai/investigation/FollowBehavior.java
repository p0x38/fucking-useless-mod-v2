package me.p0x38.fuckinguselessmod.entity.ai.investigation;

import me.p0x38.fuckinguselessmod.entity.ai.goals.InvestigateEntityGoal;

/**
 * Follows the current target while trying to maintain its preferred distance.
 */
public final class FollowBehavior implements InvestigationBehavior {

    @Override
    public void tick(InvestigateEntityGoal goal) {
        double preferredDistance = goal.getSettings().getPreferredDistance();

        if (goal.distanceToTargetSqr() > preferredDistance * preferredDistance) {
            goal.moveTowardTarget();
            goal.setState(InvestigationState.FOLLOWING);
        } else {
            goal.stopMovement();
            goal.setState(InvestigationState.OBSERVING);
            goal.incrementObservationTicks();
        }
    }
}
