package me.p0x38.fuckinguselessmod.entity.goals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/**
 * Positions the mob behind a moving target and observes it while attempting
 * to remain outside the target's attention.
 *
 * <p>The behavior uses the target's current look direction to prefer positions
 * behind the target. Once positioned, it checks whether the target can see
 * and is looking at the investigator and repositions when necessary.</p>
 */
public final class HideAndObserveBehavior implements InvestigationBehavior {

    private BlockPos observationPosition;
    private int repositionTicks;

    @Override
    public void start(InvestigateEntityGoal goal) {
        this.observationPosition = findObservationPosition(goal);
        this.repositionTicks = 0;
        goal.setState(InvestigationState.POSITIONING);
    }

    @Override
    public void tick(InvestigateEntityGoal goal) {
        Entity target = goal.getTarget();

        if (target == null || this.observationPosition == null) {
            goal.setState(InvestigationState.LOST);
            return;
        }

        this.repositionTicks++;

        if (this.repositionTicks >= goal.getSettings().getHideRepositionIntervalTicks()
                && goal.getState() != InvestigationState.OBSERVING) {
            this.observationPosition = findObservationPosition(goal);
            this.repositionTicks = 0;
        }

        if (this.observationPosition == null) {
            goal.setState(InvestigationState.LOST);
            return;
        }

        if (!goal.isAtPosition(this.observationPosition, 1.75D)) {
            goal.moveToPosition(this.observationPosition);
            goal.setState(InvestigationState.POSITIONING);
            return;
        }

        goal.stopMovement();

        boolean canObserve = goal.hasLineOfSightToTarget();
        boolean noticed = false;

        if (target instanceof LivingEntity livingTarget) {
            noticed = livingTarget.hasLineOfSight(goal.getMob())
                    && livingTarget.isLookingAtMe(
                    goal.getMob(),
                    0.5D,
                    false,
                    false
            );
        }

        if (!canObserve || noticed) {
            this.observationPosition = findObservationPosition(goal);
            this.repositionTicks = 0;
            goal.setState(InvestigationState.POSITIONING);
            return;
        }

        goal.setState(InvestigationState.OBSERVING);
        goal.incrementObservationTicks();
    }

    @Override
    public void stop(InvestigateEntityGoal goal) {
        this.observationPosition = null;
        this.repositionTicks = 0;
    }

    private BlockPos findObservationPosition(InvestigateEntityGoal goal) {
        Entity target = goal.getTarget();

        if (target == null) {
            return null;
        }

        Vec3 look = target.getLookAngle();

        double horizontalLength = Math.sqrt(
                look.x * look.x + look.z * look.z
        );

        if (horizontalLength < 1.0E-4D) {
            look = new Vec3(0.0D, 0.0D, 1.0D);
            horizontalLength = 1.0D;
        }

        double backX = -look.x / horizontalLength;
        double backZ = -look.z / horizontalLength;
        double distance = goal.getSettings().getHideDistance();

        BlockPos best = null;
        double bestPathDistance = Double.MAX_VALUE;

        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2.0D * i / 8.0D;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            double offsetX = backX * cos - backZ * sin;
            double offsetZ = backX * sin + backZ * cos;

            BlockPos candidate = BlockPos.containing(
                    target.getX() + offsetX * distance,
                    target.getY(),
                    target.getZ() + offsetZ * distance
            );

            if (!goal.getNavigation().isStableDestination(candidate)) {
                continue;
            }

            Path path = goal.getNavigation().createPath(candidate, 1);

            if (path == null) {
                continue;
            }

            double pathDistance = goal.getMob().distanceToSqr(
                    candidate.getCenter()
            );

            if (pathDistance < bestPathDistance) {
                best = candidate;
                bestPathDistance = pathDistance;
            }
        }

        return best;
    }
}
