package me.p0x38.fuckinguselessmod.entity.goals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvestigationSettingsTest {

    @Test
    void defaultsAreSensible() {
        InvestigationSettings settings = new InvestigationSettings();

        assertEquals(InvestigationMode.APPROACH, settings.getMode());
        assertEquals(MovementStyle.WALK, settings.getMovementStyle());

        assertEquals(16.0D, settings.getSearchRange());
        assertEquals(1.0D, settings.getSpeed());
        assertEquals(1.3D, settings.getSprintSpeed());
        assertEquals(0.6D, settings.getSneakSpeed());

        assertEquals(3.0D, settings.getMinDistance());
        assertEquals(5.0D, settings.getPreferredDistance());
        assertEquals(12.0D, settings.getMaxDistance());
        assertEquals(7.0D, settings.getHideDistance());

        assertEquals(100, settings.getInvestigationDuration());
        assertEquals(200, settings.getMaxDuration());
        assertEquals(40, settings.getTargetLossGraceTicks());
        assertEquals(10, settings.getRepathIntervalTicks());
        assertEquals(20, settings.getHideRepositionIntervalTicks());

        assertFalse(settings.requiresLineOfSight());
        assertTrue(settings.allowsTargetLoss());
    }

    @Test
    void settingsCanBeChangedAtRuntime() {
        InvestigationSettings settings = new InvestigationSettings();

        settings.setMode(InvestigationMode.HIDE_AND_OBSERVE);
        settings.setMovementStyle(MovementStyle.SNEAK);
        settings.setSearchRange(32.0D);
        settings.setSpeed(0.8D);
        settings.setSprintSpeed(1.5D);
        settings.setSneakSpeed(0.4D);
        settings.setMinDistance(4.0D);
        settings.setPreferredDistance(8.0D);
        settings.setMaxDistance(14.0D);
        settings.setHideDistance(10.0D);
        settings.setInvestigationDuration(150);
        settings.setMaxDuration(300);
        settings.setTargetLossGraceTicks(60);
        settings.setRepathIntervalTicks(5);
        settings.setHideRepositionIntervalTicks(30);
        settings.setRequireLineOfSight(true);
        settings.setAllowTargetLoss(false);

        assertEquals(InvestigationMode.HIDE_AND_OBSERVE, settings.getMode());
        assertEquals(MovementStyle.SNEAK, settings.getMovementStyle());
        assertEquals(32.0D, settings.getSearchRange());
        assertEquals(0.8D, settings.getSpeed());
        assertEquals(1.5D, settings.getSprintSpeed());
        assertEquals(0.4D, settings.getSneakSpeed());
        assertEquals(4.0D, settings.getMinDistance());
        assertEquals(8.0D, settings.getPreferredDistance());
        assertEquals(14.0D, settings.getMaxDistance());
        assertEquals(10.0D, settings.getHideDistance());
        assertEquals(150, settings.getInvestigationDuration());
        assertEquals(300, settings.getMaxDuration());
        assertEquals(60, settings.getTargetLossGraceTicks());
        assertEquals(5, settings.getRepathIntervalTicks());
        assertEquals(30, settings.getHideRepositionIntervalTicks());
        assertTrue(settings.requiresLineOfSight());
        assertFalse(settings.allowsTargetLoss());
    }

    @Test
    void numericValuesAreClamped() {
        InvestigationSettings settings = new InvestigationSettings();

        settings.setSearchRange(-1.0D);
        settings.setSpeed(-1.0D);
        settings.setSprintSpeed(-1.0D);
        settings.setSneakSpeed(-1.0D);
        settings.setMinDistance(-1.0D);
        settings.setPreferredDistance(-1.0D);
        settings.setMaxDistance(-1.0D);
        settings.setHideDistance(-1.0D);

        settings.setInvestigationDuration(-1);
        settings.setMaxDuration(-1);
        settings.setTargetLossGraceTicks(-1);
        settings.setRepathIntervalTicks(-1);
        settings.setHideRepositionIntervalTicks(-1);

        assertEquals(0.0D, settings.getSearchRange());
        assertEquals(0.0D, settings.getSpeed());
        assertEquals(0.0D, settings.getSprintSpeed());
        assertEquals(0.0D, settings.getSneakSpeed());
        assertEquals(0.0D, settings.getMinDistance());
        assertEquals(0.0D, settings.getPreferredDistance());
        assertEquals(0.0D, settings.getMaxDistance());
        assertEquals(0.0D, settings.getHideDistance());

        assertEquals(0, settings.getInvestigationDuration());
        assertEquals(0, settings.getMaxDuration());
        assertEquals(0, settings.getTargetLossGraceTicks());

        assertEquals(1, settings.getRepathIntervalTicks());
        assertEquals(1, settings.getHideRepositionIntervalTicks());
    }

    @Test
    void nullEnumValuesAreRejected() {
        InvestigationSettings settings = new InvestigationSettings();

        assertThrows(
                NullPointerException.class,
                () -> settings.setMode(null)
        );

        assertThrows(
                NullPointerException.class,
                () -> settings.setMovementStyle(null)
        );
    }
}
