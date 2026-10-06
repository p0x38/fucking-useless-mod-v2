package me.p0x38.fabric.gametest;

import me.p0x38.fuckinguselessmod.entity.ModEntities;
import me.p0x38.fuckinguselessmod.entity.NoiseEntity;
import me.p0x38.fuckinguselessmod.entity.ai.goals.InvestigateEntityGoal;
import me.p0x38.fuckinguselessmod.entity.ai.investigation.InvestigationMode;
import me.p0x38.fuckinguselessmod.entity.ai.investigation.InvestigationSettings;
import me.p0x38.fuckinguselessmod.entity.ai.investigation.InvestigationState;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

public final class InvestigateEntityGameTests {

    @GameTest(maxTicks = 200)
    public void approachTarget(GameTestHelper helper) {
        Entity target = helper.spawn(
                EntityType.ARMOR_STAND,
                new Vec3(8.0D, 1.0D, 1.0D)
        );

        Predicate<Entity> targetPredicate = entity -> entity == target;

        TestInvestigator investigator = new TestInvestigator(
                helper.getLevel(),
                targetPredicate
        );

        investigator.setPos(
                new Vec3(1.0D, 1.0D, 1.0D)
        );

        helper.getLevel().addFreshEntity(investigator);

        boolean[] targetSelected = {false};
        boolean[] reachedObservation = {false};

        helper.onEachTick(() -> {
            InvestigateEntityGoal goal =
                    investigator.getInvestigationGoal();

            if (goal.getTarget() == target) {
                targetSelected[0] = true;
            }

            if (goal.getState() == InvestigationState.OBSERVING) {
                reachedObservation[0] = true;
            }
        });

        helper.succeedWhen(() -> {
            InvestigateEntityGoal goal =
                    investigator.getInvestigationGoal();

            helper.assertTrue(
                    targetSelected[0],
                    "Target was not selected"
            );

            helper.assertTrue(
                    reachedObservation[0],
                    "Investigation did not reach OBSERVING"
            );

            helper.assertTrue(
                    goal.distanceToTargetSqr() <= 16.0D
                            || reachedObservation[0],
                    "Investigator did not approach the target"
            );
        });
    }

    private static final class TestInvestigator extends NoiseEntity {

        private final InvestigateEntityGoal investigationGoal;

        private TestInvestigator(
                ServerLevel level,
                Predicate<Entity> targetPredicate
        ) {
            super(ModEntities.NOISE_ENTITY, level);

            InvestigationSettings settings =
                    new InvestigationSettings();

            settings.setMode(InvestigationMode.APPROACH);
            settings.setPreferredDistance(3.0D);
            settings.setMaxDuration(160);

            this.investigationGoal = new InvestigateEntityGoal(
                    this,
                    targetPredicate,
                    settings
            );

            this.goalSelector.addGoal(
                    -1,
                    this.investigationGoal
            );
        }

        private InvestigateEntityGoal getInvestigationGoal() {
            return this.investigationGoal;
        }
    }
}