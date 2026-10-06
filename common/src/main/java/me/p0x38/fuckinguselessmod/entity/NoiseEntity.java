package me.p0x38.fuckinguselessmod.entity;

import me.p0x38.fuckinguselessmod.entity.ai.goals.InvestigateEntityGoal;
import me.p0x38.fuckinguselessmod.entity.ai.investigation.InvestigationSettings;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Common base entity for Chat Entity implementations. */
public class NoiseEntity extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> ALWAYS_UPDATE =
            SynchedEntityData.defineId(
                    NoiseEntity.class,
                    EntityDataSerializers.BOOLEAN
            );

    private InvestigateEntityGoal investigationGoal;

    /** Creates a Chat Entity in the supplied world. */
    public NoiseEntity(
            EntityType<? extends NoiseEntity> type,
            Level level
    ) {
        super(type, level);
    }

    /** @return a builder containing the entity's default attributes. */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ALWAYS_UPDATE, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, true));

        InvestigationSettings settings = new InvestigationSettings();
        this.investigationGoal = new InvestigateEntityGoal(
                this,
                entity -> entity instanceof Player,
                settings
        );

        this.goalSelector.addGoal(2, this.investigationGoal);
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    /**
     * Returns the investigation goal used by this entity.
     *
     * @return investigation goal
     */
    public InvestigateEntityGoal getInvestigationGoal() {
        return this.investigationGoal;
    }

    /** @return whether this entity's noise texture updates continuously. */
    public boolean isAlwaysUpdate() {
        return this.entityData.get(ALWAYS_UPDATE);
    }

    /**
     * Changes whether this entity's noise texture updates continuously.
     *
     * @param alwaysUpdate whether continuous updates are enabled
     */
    public void setAlwaysUpdate(boolean alwaysUpdate) {
        this.entityData.set(ALWAYS_UPDATE, alwaysUpdate);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("AlwaysUpdate", this.isAlwaysUpdate());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setAlwaysUpdate(input.getBooleanOr("AlwaysUpdate", false));
    }
}
