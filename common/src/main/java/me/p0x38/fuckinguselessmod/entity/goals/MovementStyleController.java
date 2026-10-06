package me.p0x38.fuckinguselessmod.entity.goals;

import net.minecraft.world.entity.PathfinderMob;

/**
 * Applies player-like movement state flags to a pathfinding mob.
 *
 * <p>Movement speed itself is selected by {@link InvestigateEntityGoal};
 * this class is responsible for the sneak/sprint state flags.</p>
 */
public final class MovementStyleController {

    private final PathfinderMob mob;

    private boolean captured;
    private boolean previousSneaking;
    private boolean previousSprinting;

    public MovementStyleController(PathfinderMob mob) {
        this.mob = mob;
    }

    /**
     * Captures the mob's current movement state before the goal takes control.
     */
    public void start() {
        if (this.captured) {
            return;
        }

        this.previousSneaking = this.mob.isSneaking();
        this.previousSprinting = this.mob.isSprinting();
        this.captured = true;
    }

    /**
     * Applies the requested movement style.
     *
     * @param style desired movement style
     */
    public void apply(MovementStyle style) {
        if (!this.captured) {
            start();
        }

        switch (style) {
            case WALK -> {
                this.mob.setShiftKeyDown(false);
                this.mob.setSprinting(false);
            }

            case SNEAK -> {
                this.mob.setSprinting(false);
                this.mob.setShiftKeyDown(true);
            }

            case SPRINT -> {
                this.mob.setShiftKeyDown(false);
                this.mob.setSprinting(true);
            }
        }
    }

    /**
     * Restores the movement state that existed before the goal started.
     */
    public void stop() {
        if (!this.captured) {
            return;
        }

        this.mob.setShiftKeyDown(this.previousSneaking);
        this.mob.setSprinting(this.previousSprinting);
        this.captured = false;
    }
}
