package slime;

import java.util.Random;

/**
 * The kinds of weather. Each sky brings its own sunlight and rain. The
 * weather moves towards those values a little every tick, so a change of
 * sky takes a while to arrive in full.
 *
 * Like ExecutionMode, this is an enum with fields: a fixed set of objects,
 * each with its own values.
 */
public enum Sky {

    CLEAR("clear", 1.0, 0.0),
    OVERCAST("overcast", 0.0, 0.0),
    SHOWERS("showers", 0.0, 0.5),
    STORM("storm", 0.0, 1.0);

    private final String label;
    private final double sunlight;   // 0 = no sun, 1 = full sun
    private final double rain;       // 0 = dry, 1 = downpour

    Sky(String label, double sunlight, double rain) {
        this.label = label;
        this.sunlight = sunlight;
        this.rain = rain;
    }

    public String getLabel() {
        return label;
    }

    public double getSunlight() {
        return sunlight;
    }

    public double getRain() {
        return rain;
    }

    /**
     * A different sky, chosen at random. Moving 1, 2 or 3 places along the
     * list, wrapping around at the end, can reach every other sky but
     * never this one.
     */
    public Sky change(Random random) {
        Sky[] skies = values();
        int places = 1 + random.nextInt(skies.length - 1);
        return skies[(ordinal() + places) % skies.length];
    }
}
