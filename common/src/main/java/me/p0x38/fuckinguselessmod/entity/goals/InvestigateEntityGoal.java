package me.p0x38.fuckinguselessmod.entity.goals;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.function.Predicate;

/**
 * A reusable goal that causes a pathfinding mob to investigate a selected entity.
 *
 * <p>The target is selected using a predicate, while the actual behavior
 * is controlled by a mutable {@link InvestigationSettings} instance.</p>
 *
 * <p>The settings may be changed while this goal is running, allowing
 * an entity's own AI or another system to dynamically alter its behavior.</p>
 */
public final class InvestigateEntityGoal extends MobGoal<PathfinderMob> {
    private final Predicate<Entity> targetPredicate;
    private final InvestigationSettings settings;

    private Entity target;

    private int elapsedTicks;
    private int observationTicks;

    /**
     * Creates an investigation goal using default settings.
     *
     * @param mob mob running the goal
     * @param targetPredicate predicate used to determine valid targets
     */
    public InvestigateEntityGoal(
            PathfinderMob mob,
            Predicate<Entity> targetPredicate
    ) {
        this(mob,
                targetPredicate,
                new InvestigationSettings());
    }

    /**
     * Creates an investigation goal using custom settings.
     *
     * @param mob mob running the goal
     * @param targetPredicate predicate used to determine valid targets
     * @param settings mutable investigation settings
     */
    public InvestigateEntityGoal(
            PathfinderMob mob,
            Predicate<Entity> targetPredicate,
            InvestigationSettings settings
    ) {
        super(mob);

        this.targetPredicate = targetPredicate;
        this.settings = settings;

        this.setFlags(EnumSet.of(
                Flag.MOVE,
                Flag.LOOK
        ));
    }

    @Override
    public boolean canUse() {
        Entity target = findTarget();

        return this.target != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.target == null) return false;
        if (!this.target.isAlive()) return false;
        if (this.elapsedTicks >= this.settings.getMaxDuration()) return false;

        double maxDistance = this.settings.getMaxDistance();

        return this.mob.distanceToSqr(this.target)
                <= maxDistance * maxDistance;
    }

    @Override
    public void start() {
        this.elapsedTicks = 0;
        this.observationTicks = 0;

        updateBehavior();
    }

    @Override
    public void tick() {
        this.elapsedTicks++;

        if (this.target == null || !this.target.isAlive()) return;

        this.mob.getLookControl().setLookAt(this.target);

        updateBehavior();
    }

    @Override
    public void stop() {
        this.mob.getNavigation().stop();

        this.target = null;
        this.elapsedTicks = 0;
        this.observationTicks = 0;
    }

    /**
     * Returns the mutable settings used by this goal.
     */
    public InvestigationSettings getSettings() {
        return this.settings;
    }

    /**
     * Returns the entity currently being investigated.
     */
    public Entity getTarget() {
        return this.target;
    }

    /**
     * Changes the investigation mode at runtime.
     *
     * @param mode new mode
     */
    public void setInvestigationMode(InvestigationMode mode) {
        this.settings.setMode(mode);
    }

    /**
     * Changes the movement style at runtime.
     *
     * @param movementStyle new movement style
     */
    public void setMovementStyle(MovementStyle movementStyle) {
        this.settings.setMovementStyle(movementStyle);
    }

    /**
     * Changes the current behavior according to the active settings.
     */
    private void updateBehavior() {
        if (this.target == null) return;

        switch (this.settings.getMode()) {
            case APPROACH -> handleApproach();
            case KEEP_DISTANCE -> handleKeepDistance();
            case HIDE_AND_OBSERVE -> handleHideAndObserve();
            case FOLLOW -> handleFollow();
            case WATCH -> handleWatch();
        }
    }

    private void handleApproach() {
        double distanceSqr = this.mob.distanceToSqr(this.target);
        double preferredDistance =
                this.settings.getPreferredDistance();

        if (distanceSqr > preferredDistance * preferredDistance) {
            moveTowardTarget();
        } else {
            stopAndObserve();
        }
    }

    private void handleKeepDistance() {
        double distanceSqr = this.mob.distanceToSqr(this.target);

        double minDistance = this.settings.getMinDistance();
        double maxDistance = this.settings.getMaxDistance();

        if (distanceSqr < minDistance * minDistance) {
            moveAwayFromTarget();
        } else if (distanceSqr > maxDistance * maxDistance) {
            moveTowardTarget();
        } else {
            stopAndObserve();
        }
    }

    private void handleHideAndObserve() {
        /*
         * TODO:
         *
         * 1. Find candidate positions.
         * 2. Prefer positions outside the target's direct view.
         * 3. Prefer positions where the mob can still observe the target.
         * 4. Path toward the selected position.
         * 5. Enter observation state after arrival.
         */
    }

    private void handleFollow() {
        double distanceSqr = this.mob.distanceToSqr(this.target);
        double preferredDistance =
                this.settings.getPreferredDistance();

        if (distanceSqr > preferredDistance * preferredDistance) {
            moveTowardTarget();
        } else {
            stopAndObserve();
        }
    }

    private void handleWatch() {
        this.mob.getNavigation().stop();
        this.observationTicks++;
    }

    private void moveTowardTarget() {
        this.mob.getNavigation().moveTo(
                this.target,
                this.settings.getSpeed()
        );
    }

    private void moveAwayFromTarget() {
        /*
         * TODO:
         *
         * Find a valid navigable position away from the target.
         */
    }

    private void stopAndObserve() {
        this.mob.getNavigation().stop();
        this.observationTicks++;
    }

    private Entity findTarget() {
        double searchRange = this.settings.getSearchRange();

        return this.mob.level()
                .getEntitiesOfClass(
                        Entity.class,
                        this.mob.getBoundingBox().inflate(searchRange),
                        this.targetPredicate
                )
                .stream()
                .min(
                        Comparator.comparingDouble(
                                this.mob::distanceToSqr
                        )
                )
                .orElse(null);
    }
}
