package slime;

/**
 * Concrete observer with no window at all: prints a line of statistics
 * every few ticks. The simulation treats it exactly like the panel and
 * the chart, because it only knows its observers through the Observer
 * interface.
 */
public class ConsoleReporter implements Observer {

    private final Simulation simulation;   // the subject this observer pulls its data from
    private final int interval;
    private int lastReported = -1;

    public ConsoleReporter(Simulation simulation, int interval) {
        if (interval <= 0) {
            throw new IllegalArgumentException("Report interval must be positive, got " + interval);
        }
        this.simulation = simulation;
        this.interval = interval;
    }

    @Override
    public void update() {
        SimulationStats stats = simulation.getStats();
        int tick = stats.getTick();
        if (tick % interval == 0 && tick != lastReported) {
            lastReported = tick;
            System.out.println(stats);
        }
    }
}
