package me.p0x38.fuckinguselessmod.entity.ai.investigation;

import me.p0x38.fuckinguselessmod.entity.ai.goals.InvestigateEntityGoal;
import me.p0x38.fuckinguselessmod.entity.ai.movement.MovementStyle;

import java.util.Objects;

/**
 * Mutable configuration for {@link InvestigateEntityGoal}.
 *
 * <p>The settings may be changed while the goal is running, allowing
 * the mob or another AI system to dynamically alter its investigation
 * behavior without replacing the goal.</p>
 */
public final class InvestigationSettings {
    private InvestigationMode mode = InvestigationMode.APPROACH;
    private MovementStyle movementStyle = MovementStyle.WALK;

    private double acquisitionRange = 16.0D;
    private double trackingRange = 32.0D;
    private double speed = 1.0D;
    private double sprintSpeed = 1.3D;
    private double sneakSpeed = 0.6D;

    private double minDistance = 3.0D;
    private double preferredDistance = 5.0D;
    private double maxDistance = 12.0D;
    private double hideDistance = 7.0D;

    private int investigationDuration = 100;
    private int maxDuration = 200;
    private int targetLossGraceTicks = 40;
    private int repathIntervalTicks = 10;
    private int hideRepositionIntervalTicks = 20;

    private boolean requireLineOfSight;
    private boolean allowTargetLoss = true;

    public InvestigationMode getMode() {
        return mode;
    }

    public void setMode(InvestigationMode mode) {
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    public MovementStyle getMovementStyle() {
        return movementStyle;
    }

    public void setMovementStyle(MovementStyle movementStyle) {
        this.movementStyle = Objects.requireNonNull(
                movementStyle,
                "movementStyle"
        );
    }

    public double getAcquisitionRange() {
        return acquisitionRange;
    }

    public void setAcquisitionRange(double acquisitionRange) {
        this.acquisitionRange = Math.max(0.0D, acquisitionRange);
    }

    public double getTrackingRange() {
        return trackingRange;
    }

    public void setTrackingRange(double trackingRange) {
        this.trackingRange = Math.max(0.0D, trackingRange);
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = Math.max(0.0D, speed);
    }

    public double getSprintSpeed() {
        return sprintSpeed;
    }

    public void setSprintSpeed(double sprintSpeed) {
        this.sprintSpeed = Math.max(0.0D, sprintSpeed);
    }

    public double getSneakSpeed() {
        return sneakSpeed;
    }

    public void setSneakSpeed(double sneakSpeed) {
        this.sneakSpeed = Math.max(0.0D, sneakSpeed);
    }

    public double getMinDistance() {
        return minDistance;
    }

    public void setMinDistance(double minDistance) {
        this.minDistance = Math.max(0.0D, minDistance);
    }

    public double getPreferredDistance() {
        return preferredDistance;
    }

    public void setPreferredDistance(double preferredDistance) {
        this.preferredDistance = Math.max(0.0D, preferredDistance);
    }

    public double getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(double maxDistance) {
        this.maxDistance = Math.max(0.0D, maxDistance);
    }

    public double getHideDistance() {
        return hideDistance;
    }

    public void setHideDistance(double hideDistance) {
        this.hideDistance = Math.max(0.0D, hideDistance);
    }

    public int getInvestigationDuration() {
        return investigationDuration;
    }

    public void setInvestigationDuration(int investigationDuration) {
        this.investigationDuration = Math.max(0, investigationDuration);
    }

    public int getMaxDuration() {
        return maxDuration;
    }

    public void setMaxDuration(int maxDuration) {
        this.maxDuration = Math.max(0, maxDuration);
    }

    public int getTargetLossGraceTicks() {
        return targetLossGraceTicks;
    }

    public void setTargetLossGraceTicks(int targetLossGraceTicks) {
        this.targetLossGraceTicks = Math.max(0, targetLossGraceTicks);
    }

    public int getRepathIntervalTicks() {
        return repathIntervalTicks;
    }

    public void setRepathIntervalTicks(int repathIntervalTicks) {
        this.repathIntervalTicks = Math.max(1, repathIntervalTicks);
    }

    public int getHideRepositionIntervalTicks() {
        return hideRepositionIntervalTicks;
    }

    public void setHideRepositionIntervalTicks(int hideRepositionIntervalTicks) {
        this.hideRepositionIntervalTicks = Math.max(
                1,
                hideRepositionIntervalTicks
        );
    }

    public boolean requiresLineOfSight() {
        return requireLineOfSight;
    }

    public void setRequireLineOfSight(boolean requireLineOfSight) {
        this.requireLineOfSight = requireLineOfSight;
    }

    public boolean allowsTargetLoss() {
        return allowTargetLoss;
    }

    public void setAllowTargetLoss(boolean allowTargetLoss) {
        this.allowTargetLoss = allowTargetLoss;
    }
}
