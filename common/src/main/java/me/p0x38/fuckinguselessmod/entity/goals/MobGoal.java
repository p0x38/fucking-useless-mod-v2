package me.p0x38.fuckinguselessmod.entity.goals;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

public abstract class MobGoal<T extends PathfinderMob> extends Goal {
    protected final T mob;

    protected MobGoal(T mob) {
        this.mob = mob;
    }
}
