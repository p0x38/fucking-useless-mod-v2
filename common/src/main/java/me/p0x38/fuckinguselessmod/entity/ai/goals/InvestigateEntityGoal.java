package me.p0x38.fuckinguselessmod.entity.ai.goals;

import me.p0x38.fuckinguselessmod.entity.ai.investigation.*;
import me.p0x38.fuckinguselessmod.entity.ai.movement.MovementStyle;
import me.p0x38.fuckinguselessmod.entity.ai.movement.MovementStyleController;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * A reusable goal that causes a pathfinding mob to investigate a selected entity.
 *
 * <p>The goal delegates actual behavior to strategies selected by
 * {@link InvestigationMode}. Its mutable settings can be changed while
 * the goal is running, allowing the mob's own AI to dynamically change
 * its behavior.</p>
 */
public final class InvestigateEntityGoal extends MobGoal<PathfinderMob> {
    private final Predicate<Entity> targetPredicate;
    private final InvestigationSettings settings;
    private final MovementStyleController movementController;
    private final Map<InvestigationMode, InvestigationBehavior> behaviors;

    private Entity target;

    private InvestigationState state = InvestigationState.INACTIVE;
    private InvestigationMode activeMode;

    private int elapsedTicks;
    private int observationTicks;
    private int targetLossTicks;
    private int repathTicks;

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
        this(
                mob,
                targetPredicate,
                new InvestigationSettings()
        );
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
        super(Objects.requireNonNull(mob, "mob"));

        this.targetPredicate = Objects.requireNonNull(
                targetPredicate,
                "targetPredicate"
        );

        this.settings = Objects.requireNonNull(
                settings,
                "settings"
        );

        this.movementController = new MovementStyleController(mob);

        EnumMap<InvestigationMode, InvestigationBehavior> behaviors =
                new EnumMap<>(InvestigationMode.class);

        behaviors.put(InvestigationMode.APPROACH, new ApproachBehavior());
        behaviors.put(InvestigationMode.KEEP_DISTANCE, new KeepDistanceBehavior());
        behaviors.put(InvestigationMode.FOLLOW, new FollowBehavior());
        behaviors.put(InvestigationMode.WATCH, new WatchBehavior());
        behaviors.put(
                InvestigationMode.HIDE_AND_OBSERVE,
                new HideAndObserveBehavior()
        );

        this.behaviors = Map.copyOf(behaviors);

        this.setFlags(EnumSet.of(
                Flag.MOVE,
                Flag.LOOK
        ));
    }

    @Override
    public boolean canUse() {
        this.target = findTarget();

        return this.target != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.target == null || !this.target.isAlive()) {
            return false;
        }

        if (this.elapsedTicks >= this.settings.getMaxDuration()) {
            return false;
        }

        if (this.state == InvestigationState.OBSERVING
                && this.observationTicks >= this.settings.getInvestigationDuration()) {
            return false;
        }

        if (this.distanceToTargetSqr()
                > this.settings.getSearchRange() * this.settings.getSearchRange()) {

            if (!this.settings.allowsTargetLoss()) {
                return false;
            }

            return ++this.targetLossTicks
                    <= this.settings.getTargetLossGraceTicks();
        }

        this.targetLossTicks = 0;
        return true;
    }

    @Override
    public void start() {
        this.elapsedTicks = 0;
        this.observationTicks = 0;
        this.targetLossTicks = 0;
        this.repathTicks = 0;
        this.state = InvestigationState.APPROACHING;
        this.activeMode = null;

        this.movementController.start();
        this.switchBehavior();
    }

    @Override
    public void tick() {
        this.elapsedTicks++;
        this.repathTicks++;

        if (this.target == null || !this.target.isAlive()) {
            this.state = InvestigationState.LOST;
            return;
        }

        this.mob.getLookControl().setLookAt(this.target);

        if (this.activeMode != this.settings.getMode()) {
            this.switchBehavior();
        }

        this.movementController.apply(
                this.settings.getMovementStyle()
        );

        InvestigationBehavior behavior = this.behaviors.get(
                this.settings.getMode()
        );

        if (behavior != null) {
            behavior.tick(this);
        }
    }

    @Override
    public void stop() {
        InvestigationBehavior behavior = this.activeMode == null
                ? null
                : this.behaviors.get(this.activeMode);

        if (behavior != null) {
            behavior.stop(this);
        }

        this.mob.getNavigation().stop();
        this.movementController.stop();

        this.target = null;
        this.activeMode = null;
        this.state = InvestigationState.INACTIVE;

        this.elapsedTicks = 0;
        this.observationTicks = 0;
        this.targetLossTicks = 0;
        this.repathTicks = 0;
    }

    /**
     * Returns the mutable settings used by this goal.
     *
     * @return investigation settings
     */
    public InvestigationSettings getSettings() {
        return this.settings;
    }

    /**
     * Returns the mob running this goal.
     *
     * @return owning mob
     */
    public PathfinderMob getMob() {
        return this.mob;
    }

    /**
     * Returns the current investigation target.
     *
     * @return target, or {@code null} when inactive
     */
    public Entity getTarget() {
        return this.target;
    }

    /**
     * Returns the current investigation state.
     *
     * @return current state
     */
    public InvestigationState getState() {
        return this.state;
    }

    /**
     * Changes the current investigation state.
     *
     * @param state new state
     */
    public void setState(InvestigationState state) {
        this.state = Objects.requireNonNull(state, "state");
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
     * Returns the squared distance to the current target.
     *
     * @return squared distance, or {@link Double#MAX_VALUE} without a target
     */
    public double distanceToTargetSqr() {
        return this.target == null
                ? Double.MAX_VALUE
                : this.mob.distanceToSqr(this.target);
    }

    /**
     * Returns whether the mob currently has line of sight to its target.
     *
     * @return {@code true} when a target exists and can be seen
     */
    public boolean hasLineOfSightToTarget() {
        return this.target != null
                && this.mob.hasLineOfSight(this.target);
    }

    /**
     * Returns the current navigation object.
     *
     * @return mob navigation
     */
    public net.minecraft.world.entity.ai.navigation.PathNavigation getNavigation() {
        return this.mob.getNavigation();
    }

    /**
     * Moves toward the current target.
     */
    public void moveTowardTarget() {
        if (this.target == null) {
            return;
        }

        if (this.repathTicks >= this.settings.getRepathIntervalTicks()
                || !this.mob.getNavigation().isInProgress()) {

            this.mob.getNavigation().moveTo(
                    this.target,
                    getCurrentMovementSpeed()
            );

            this.repathTicks = 0;
        }
    }

    /**
     * Moves to a position selected by an investigation behavior.
     *
     * @param position target position
     */
    public void moveToPosition(net.minecraft.core.BlockPos position) {
        if (position == null) {
            return;
        }

        if (this.repathTicks >= this.settings.getRepathIntervalTicks()
                || !this.mob.getNavigation().isInProgress()) {

            this.mob.getNavigation().moveTo(
                    position.getX() + 0.5D,
                    position.getY(),
                    position.getZ() + 0.5D,
                    getCurrentMovementSpeed()
            );

            this.repathTicks = 0;
        }
    }

    /**
     * Moves to a point away from the current target.
     */
    public void moveAwayFromTarget() {
        if (this.target == null) {
            return;
        }

        double x = this.mob.getX() - this.target.getX();
        double z = this.mob.getZ() - this.target.getZ();

        double length = Math.sqrt(x * x + z * z);

        if (length < 1.0E-4D) {
            double angle = this.mob.getRandom().nextDouble() * Math.PI * 2.0D;
            x = Math.cos(angle);
            z = Math.sin(angle);
            length = 1.0D;
        }

        x /= length;
        z /= length;

        double distance = Math.max(
                this.settings.getPreferredDistance(),
                this.settings.getMinDistance() + 1.0D
        );

        this.mob.getNavigation().moveTo(
                this.target.getX() + x * distance,
                this.mob.getY(),
                this.target.getZ() + z * distance,
                getCurrentMovementSpeed()
        );
    }

    /**
     * Stops navigation immediately.
     */
    public void stopMovement() {
        this.mob.getNavigation().stop();
    }

    /**
     * Returns whether the mob is close enough to a block position.
     *
     * @param position position to test
     * @param radius maximum distance
     * @return whether the position has been reached
     */
    public boolean isAtPosition(
            net.minecraft.core.BlockPos position,
            double radius
    ) {
        return position != null
                && this.mob.distanceToSqr(position.getCenter())
                <= radius * radius;
    }

    /**
     * Increments the time spent observing the target.
     */
    public void incrementObservationTicks() {
        this.observationTicks++;
    }

    /**
     * Returns the number of observation ticks.
     *
     * @return observation ticks
     */
    public int getObservationTicks() {
        return this.observationTicks;
    }

    private void switchBehavior() {
        if (this.activeMode != null) {
            InvestigationBehavior previousBehavior =
                    this.behaviors.get(this.activeMode);

            if (previousBehavior != null) {
                previousBehavior.stop(this);
            }
        }

        this.activeMode = this.settings.getMode();

        InvestigationBehavior nextBehavior =
                this.behaviors.get(this.activeMode);

        if (nextBehavior != null) {
            nextBehavior.start(this);
        }
    }

    private double getCurrentMovementSpeed() {
        return switch (this.settings.getMovementStyle()) {
            case WALK -> this.settings.getSpeed();
            case SNEAK -> this.settings.getSneakSpeed();
            case SPRINT -> this.settings.getSprintSpeed();
        };
    }

    private Entity findTarget() {
        double searchRange = this.settings.getSearchRange();
        double searchRangeSqr = searchRange * searchRange;

        return this.mob.level()
                .getEntitiesOfClass(
                        Entity.class,
                        this.mob.getBoundingBox().inflate(searchRange),
                        entity -> entity != this.mob
                                && entity.isAlive()
                                && this.targetPredicate.test(entity)
                                && this.mob.distanceToSqr(entity) <= searchRangeSqr
                                && (!this.settings.requiresLineOfSight()
                                || this.mob.hasLineOfSight(entity))
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
