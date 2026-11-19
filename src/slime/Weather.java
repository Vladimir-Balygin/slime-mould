package slime;

import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

/**
 * The weather at one tick: the sky, the sunlight and rain strength, and the
 * position of the sun patch.
 *
 * Immutable, and next() is a pure function: its random numbers come from a seed
 * stored in the weather, so the same weather always leads to the same next one.
 */
public class Weather {

    /** No sun and no rain. Used while the weather is off. */
    public static final Weather CALM = new Weather(0, Sky.OVERCAST, 0, 0, 0, 0.5, 0.5, 0);

    private final int tick;          // ticks since the forecast began
    private final Sky sky;           // sunlight and rain move towards this sky's values
    private final int skySince;      // the tick when the sky last changed
    private final double sunlight;   // 0 = no sun, 1 = full sun
    private final double rain;       // 0 = dry, 1 = downpour
    private final double sunX;       // sun patch centre, as a share of the map width (fits any map size)
    private final double sunY;       // sun patch centre, as a share of the map height
    private final long seed;         // source of the next weather's random numbers

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

    /** Returns the first weather for a seed: a clear sky, with the sun about to come out. */
    public static Weather start(long seed) {
        Random random = new Random(seed);
        return new Weather(0, Sky.CLEAR, 0, 0, 0, random.nextDouble(), random.nextDouble(), random.nextLong());
    }

    /**
     * Returns the weather one tick later. After SHORTEST_SPELL ticks the sky may
     * change at random, sunlight and rain ease towards the sky's values, and the
     * sun patch moves with the wind, wrapping at the edges. Uses only this
     * weather's seed, so calling it twice gives the same result.
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

    /**
     * Returns the first change of sky within the next withinTicks ticks, or empty
     * if there is none. next() is pure, so looking ahead changes nothing and the
     * predicted change is exactly what happens.
     */
    public Optional<Weather> nextChange(int withinTicks) {
        return Stream.iterate(this, Weather::next)
                .skip(1)                                  // start from the next tick
                .limit(withinTicks)                       // the stream is infinite: bound it
                .filter(later -> later.getSky() != sky)
                .findFirst();
    }

    // Moves a value WEATHER_EASE of the way towards its target.
    private static double towards(double value, double target) {
        return value + (target - value) * Settings.WEATHER_EASE;
    }

    // Wraps a share of the map into [0, 1): leaving one edge re-enters on the other.
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
