package me.p0x38.fuckinguselessmod.entity.goals;

/**
 * Mutable configuration for {@link InvestigateEntityGoal}.
 *
 * <p>The settings may be changed while the goal is running, allowing
 * the mob or another AI system to dynamically alter its investigation
 * behavior.</p>
 */
public final class InvestigationSettings {
    private InvestigationMode mode = InvestigationMode.APPROACH;
    private MovementStyle movementStyle = MovementStyle.WALK;

    private double searchRange = 16.0D;
    private double speed = 1.0D;

    private double minDistance = 3.0D;
    private double preferredDistance = 5.0D;
    private double maxDistance = 12.0D;

    private int investigationDuration = 100;
    private int maxDuration = 200;

    /**
     * Returns the current investigation mode.
     */
    public InvestigationMode getMode() {
        return mode;
    }

    /**
     * Changes the investigation mode.
     *
     * @param mode new investigation mode
     */
    public void setMode(InvestigationMode mode) {
        this.mode = mode;
    }

    /**
     * Returns the preferred movement style.
     */
    public MovementStyle getMovementStyle() {
        return movementStyle;
    }

    /**
     * Changes the preferred movement style.
     *
     * @param movementStyle new movement style
     */
    public void setMovementStyle(MovementStyle movementStyle) {
        this.movementStyle = movementStyle;
    }

    public double getSearchRange() {
        return searchRange;
    }

    public void setSearchRange(double searchRange) {
        this.searchRange = Math.max(0.0D, searchRange);
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = Math.max(0.0D, speed);
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
}
