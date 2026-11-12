package slime;

/**
 * All tunable numbers in one place, so an experiment never means
 * hunting through the code. The agent values follow Jeff Jones's
 * 2010 Physarum model.
 */
public final class Settings {

    // World size, in trail-map cells
    public static final int WIDTH = 400;
    public static final int HEIGHT = 300;

    // Population; the seed makes every run repeatable
    public static final int AGENT_COUNT = 15_000;
    public static final long SEED = 42L;

    // How an agent senses and moves
    public static final double SENSOR_ANGLE = Math.toRadians(22.5);
    public static final double SENSOR_DISTANCE = 9.0;
    public static final double TURN_ANGLE = Math.toRadians(45.0);
    public static final double STEP_SIZE = 1.0;
    public static final double DEPOSIT_AMOUNT = 5.0;

    // Sensors saturate: any trail stronger than this reads the same.
    // Set it to Double.MAX_VALUE to see why it is needed.
    public static final double SENSOR_LIMIT = 5.0;

    // Fraction of the trail that fades away each tick
    public static final double DECAY_RATE = 0.1;

    // Display
    public static final int SCALE = 2;              // each cell is drawn as 2x2 pixels
    public static final int FRAME_DELAY_MS = 33;    // about 30 frames per second

    private Settings() {
        // constants only: never instantiated
    }
}
