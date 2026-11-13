package slime;

/**
 * How long each stage of a step takes, in milliseconds. Each value is
 * smoothed over the last few steps, so the numbers on screen do not
 * flicker. Immutable: recording a step returns a new StepTimes.
 */
public class StepTimes {

    /** Nothing has been timed yet. */
    public static final StepTimes NONE = new StepTimes(0, 0, 0);

    // How far each new measurement pulls the average towards itself
    private static final double WEIGHT = 0.1;

    private final double moveMillis;
    private final double depositMillis;
    private final double spreadMillis;

    private StepTimes(double moveMillis, double depositMillis, double spreadMillis) {
        this.moveMillis = moveMillis;
        this.depositMillis = depositMillis;
        this.spreadMillis = spreadMillis;
    }

    /** Adds one step's measurements, in nanoseconds, to the averages. */
    public StepTimes record(long moveNanos, long depositNanos, long spreadNanos) {
        // The first measurement is taken as it is. After that, each new
        // one moves the average a tenth of the way towards itself.
        double weight = (this == NONE) ? 1.0 : WEIGHT;
        return new StepTimes(
                towards(moveMillis, moveNanos, weight),
                towards(depositMillis, depositNanos, weight),
                towards(spreadMillis, spreadNanos, weight));
    }

    private static double towards(double average, long nanos, double weight) {
        double millis = nanos / 1_000_000.0;
        return average + (millis - average) * weight;
    }

    public double getMoveMillis() {
        return moveMillis;
    }

    public double getDepositMillis() {
        return depositMillis;
    }

    public double getSpreadMillis() {
        return spreadMillis;
    }

    public double getTotalMillis() {
        return moveMillis + depositMillis + spreadMillis;
    }
}
