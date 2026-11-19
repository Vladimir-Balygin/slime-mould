package slime;

/**
 * Simulation parameters. Units: cells, ticks and radians.
 */
public final class Settings {

    public static final int WIDTH = 400;
    public static final int HEIGHT = 300;

    public static final int AGENT_COUNT = 15_000;
    public static final long SEED = 42L;   // same seed, same run (not in PARALLEL_UNSAFE)

    // Sensing and movement (Jones 2010)
    public static final double SENSOR_ANGLE = Math.toRadians(22.5);
    public static final double SENSOR_DISTANCE = 9.0;
    public static final double TURN_ANGLE = Math.toRadians(45.0);
    public static final double STEP_SIZE = 1.0;
    public static final double DEPOSIT_AMOUNT = 5.0;

    // Cap on sensor readings. Without it, the network shrinks to a few thick lines.
    public static final double SENSOR_LIMIT = 5.0;

    public static final double DECAY_RATE = 0.1;

    // Rule decorators (keys J, D and M)
    public static final double JITTER_ANGLE = Math.toRadians(22.5);   // largest random extra turn, either way
    public static final double DRIFT_ANGLE = Math.toRadians(11.25);   // extra turn to the right, every tick
    public static final double MOMENTUM_CHANCE = 0.5;                 // chance to skip the rule and go straight

    // Weather (key W)
    public static final int SHORTEST_SPELL = 200;
    public static final double WEATHER_CHANGE_CHANCE = 1.0 / 300;   // per tick, after the shortest spell
    public static final double WEATHER_EASE = 0.02;                 // share of the way to a new sky, per tick
    public static final double WIND_X = 1.0 / 2000;                 // sun patch speed, share of the map per tick
    public static final double WIND_Y = 1.0 / 6000;
    public static final double SUN_RADIUS = 50.0;
    public static final double RAIN_DECAY = 0.4;                    // extra decay in a storm, half in showers
    public static final int OUTLOOK_TICKS = 600;

    // Statistics
    public static final double NETWORK_THRESHOLD = 10.0;   // cells with more trail count as network
    public static final int CHART_LENGTH = 300;
    public static final int REPORT_INTERVAL = 100;

    // Display
    public static final int SCALE = 2;              // pixels per cell
    public static final int FRAME_DELAY_MS = 33;    // about 30 ticks per second

    private Settings() {
        // no instances: constants only
    }
}
