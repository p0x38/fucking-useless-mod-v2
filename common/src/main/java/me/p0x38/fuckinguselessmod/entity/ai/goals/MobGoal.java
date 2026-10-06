package me.p0x38.fuckinguselessmod.entity.ai.goals;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Base class for reusable goals that operate on a {@link PathfinderMob}.
 *
 * @param <T> mob type controlled by the goal
 */
public abstract class MobGoal<T extends PathfinderMob> extends Goal {

    /**
     * Mob controlled by this goal.
     */
    protected final T mob;

    protected MobGoal(T mob) {
        this.mob = mob;
    }
}
