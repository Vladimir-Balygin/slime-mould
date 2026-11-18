package slime;

import java.util.Random;

/**
 * The weather at one tick: the sky, how strong the sunlight and the rain
 * are, and where the patch of sunlight is.
 *
 * Immutable: next() does not change this weather, it returns a new
 * Weather for the tick after. And next() is a pure function: the same
 * weather always leads to the same next weather, because every random
 * number it needs comes from a seed stored in the weather itself.
 */
public class Weather {

    /** No sun and no rain. The simulation uses it while the weather is off. */
    public static final Weather CALM = new Weather(0, Sky.OVERCAST, 0, 0, 0, 0.5, 0.5, 0);

    private final int tick;          // ticks since the forecast began
    private final Sky sky;           // sunlight and rain move towards this sky's values
    private final int skySince;      // the tick when the sky last changed
    private final double sunlight;   // 0 = no sun, 1 = full sun
    private final double rain;       // 0 = dry, 1 = downpour
    private final double sunX;       // centre of the patch of sunlight, as a share of the map's width...
    private final double sunY;       // ...and of its height, so the weather works on a map of any size
    private final long seed;         // the random numbers for the next weather come from this

    private Weather(int tick, Sky sky, int skySince, double sunlight, double rain,
                    double sunX, double sunY, long seed) {
        this.tick = tick;
        this.sky = sky;
        this.skySince = skySince;
        this.sunlight = sunlight;
        this.rain = rain;
        this.sunX = sunX;
        this.sunY = sunY;
        this.seed = seed;
    }

    /**
     * The first weather of a forecast: a clear sky, with the sun about to
     * come out somewhere on the map. The same seed gives the same weather.
     */
    public static Weather start(long seed) {
        Random random = new Random(seed);
        return new Weather(0, Sky.CLEAR, 0, 0, 0, random.nextDouble(), random.nextDouble(), random.nextLong());
    }

    /**
     * The weather one tick later. Once a sky has lasted SHORTEST_SPELL
     * ticks, it may change at random. Sunlight and rain move a little
     * towards the sky's values, so the weather changes gradually. The wind
     * blows the patch of sunlight along, and like the agents it wraps
     * around the edges of the map.
     *
     * The Random is made from this weather's own seed, and it hands the
     * next weather a new seed. So calling next() twice on the same weather
     * gives the same answer twice: nothing outside this object is used or
     * changed.
     */
    public Weather next() {
        Random random = new Random(seed);
        boolean change = tick - skySince >= Settings.SHORTEST_SPELL
                && random.nextDouble() < Settings.WEATHER_CHANGE_CHANCE;
        Sky nextSky = change ? sky.change(random) : sky;
        return new Weather(tick + 1, nextSky, change ? tick + 1 : skySince,
                towards(sunlight, nextSky.getSunlight()),
                towards(rain, nextSky.getRain()),
                wrap(sunX + Settings.WIND_X),
                wrap(sunY + Settings.WIND_Y),
                random.nextLong());
    }

    // Moves a value a small part of the way towards its target.
    private static double towards(double value, double target) {
        return value + (target - value) * Settings.WEATHER_EASE;
    }

    // Keeps a share of the map between 0 and 1: what leaves one edge comes back on the other.
    private static double wrap(double share) {
        return share - Math.floor(share);
    }

    public int getTick() {
        return tick;
    }

    public Sky getSky() {
        return sky;
    }

    public double getSunlight() {
        return sunlight;
    }

    public double getRain() {
        return rain;
    }

    public double getSunX() {
        return sunX;
    }

    public double getSunY() {
        return sunY;
    }

    @Override
    public String toString() {
        return String.format("%s   sun %.0f%%  rain %.0f%%", sky.getLabel(), sunlight * 100, rain * 100);
    }
}
