package slime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

/**
 * The model: owns the agents and the trail map and advances them one tick at a
 * time. No Swing code, so it can run, be tested or be timed without a window.
 *
 * Observer pattern: the concrete subject, which measures itself and notifies
 * its observers after every change. Iterator pattern: with the weather on, it
 * takes one Weather per tick from an iterator. The work in each tick runs
 * sequentially or on parallel streams, depending on the ExecutionMode.
 */
public class Simulation implements Subject {

    private final TrailMap trail;
    private final List<SlimeAgent> agents = new ArrayList<>();
    private final List<Observer> observers = new ArrayList<>();
    private final Random random;   // places and scatters the agents
    private SteeringRule rule = SteeringRules.FOLLOW;
    private String ruleName = "follow trail";   // the plain rule under any decorators
    private ExecutionMode mode = ExecutionMode.PARALLEL;
    private int tick = 0;
    private long lostDeposits = 0;   // deposits lost since the mode last changed
    private StepTimes stepTimes = StepTimes.NONE;
    private Iterator<Weather> forecast = Collections.emptyIterator();   // empty while the weather is off
    private Weather weather = Weather.CALM;   // the weather for the next tick
    private SimulationStats stats;

    public Simulation(int width, int height, int agentCount, long seed) {
        if (agentCount < 0) {
            throw new IllegalArgumentException("Agent count cannot be negative: " + agentCount);
        }
        this.trail = new TrailMap(width, height);

        // One seeded generator places every agent and seeds its Random, so the same seed gives the same run.
        this.random = new Random(seed);
        for (int i = 0; i < agentCount; i++) {
            double x = random.nextDouble() * width;
            double y = random.nextDouble() * height;
            double heading = random.nextDouble() * 2 * Math.PI;
            agents.add(new SlimeAgent(x, y, heading, new Random(random.nextLong()), rule));
        }
        this.stats = measure();
    }

    /**
     * Advances one tick in three stages, each finished before the next starts:
     * move (agents sense, turn and move; the trail is only read), deposit, and
     * spread (blur and fade). Every agent senses the same trail, so agent order
     * does not matter, and the work inside each stage can be split across threads.
     */
    public void step() {
        // Stage 1: each agent reads the trail and changes only itself, so agents are independent tasks.
        long move = timeNanos(() -> agentStream().forEach(agent -> agent.senseAndMove(trail)));

        // Stage 2, then a check that no deposit went missing.
        double trailBefore = trail.total();
        long deposit = timeNanos(this::depositTrail);
        countLostDeposits(trailBefore);

        // Stage 3. Every row of the trail is an independent task (see TrailMap).
        long spread = timeNanos(this::spreadTrail);

        stepTimes = stepTimes.record(move, deposit, spread);
        weather = nextWeather();   // the weather for the next tick
        tick++;
        changed();
    }

    // Runs one stage and returns how long it took, in nanoseconds.
    private static long timeNanos(Runnable stage) {
        long start = System.nanoTime();
        stage.run();
        return System.nanoTime() - start;
    }

    // Stage 2: every agent adds trail where it now stands.
    private void depositTrail() {
        if (mode == ExecutionMode.PARALLEL_UNSAFE) {
            // Deliberately not thread safe: the lambda changes the shared trail map, and "+="
            // is read-add-write, so two threads on one cell can both read the old value and
            // one deposit is lost.
            agents.parallelStream()
                    .forEach(agent -> trail.deposit(agent.getX(), agent.getY(), Settings.DEPOSIT_AMOUNT));
        } else {
            // Thread safe: each chunk collects its agents' cells in a private list, and the
            // lists are merged at the join (see DepositCollector).
            int[] agentsPerCell = agentStream().collect(new DepositCollector(trail));
            trail.deposit(agentsPerCell, Settings.DEPOSIT_AMOUNT);
        }
    }

    // Stage 3: the trail spreads and fades. Rain raises the decay rate and sunlight
    // bleaches the trail; with the weather off (CALM), neither applies. Sunlight is applied
    // after diffuseAndDecay returns, so after every parallel row task has finished.
    private void spreadTrail() {
        double decayRate = Settings.DECAY_RATE + weather.getRain() * Settings.RAIN_DECAY;
        trail.diffuseAndDecay(decayRate, mode.isParallel());
        if (weather.getSunlight() > 0) {
            trail.bleach(weather.getSunX() * trail.getWidth(), weather.getSunY() * trail.getHeight(),
                    Settings.SUN_RADIUS, weather.getSunlight());
        }
    }

    // The trail must grow by exactly agents x DEPOSIT_AMOUNT per step; any shortfall
    // is counted as lost deposits.
    private void countLostDeposits(double trailBefore) {
        double expected = agents.size() * Settings.DEPOSIT_AMOUNT;
        double added = trail.total() - trailBefore;
        lostDeposits += Math.round((expected - added) / Settings.DEPOSIT_AMOUNT);
    }

    /** Returns the agents as a sequential or parallel stream, so each pipeline is written once. */
    private Stream<SlimeAgent> agentStream() {
        return mode.isParallel() ? agents.parallelStream() : agents.stream();
    }

    /** Returns every cell's trail value as a sequential or parallel stream, depending on the mode. */
    private DoubleStream trailValues() {
        return mode.isParallel() ? trail.values().parallel() : trail.values();
    }

    /**
     * Gives every agent the same strategy. Rules hold no state, so one rule object
     * is shared by all agents. The name is passed in because a lambda has no
     * useful toString().
     */
    public void setSteeringRule(String name, SteeringRule rule) {
        useRule(name, rule);
    }

    /**
     * Wraps the current rule in one more decorator. The decorator comes as a
     * UnaryOperator, so new decorators need no change here.
     */
    public void decorateSteeringRule(UnaryOperator<SteeringRule> decorator) {
        useRule(ruleName, decorator.apply(rule));   // same plain rule underneath, one more layer on top
    }

    private void useRule(String name, SteeringRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("Steering rule cannot be null");
        }
        this.rule = rule;
        this.ruleName = name;
        agents.forEach(agent -> agent.setSteeringRule(rule));
        changed();
    }

    /** Switches how each tick's work runs. Both safe modes give identical results; only speed changes. */
    public void setExecutionMode(ExecutionMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("Execution mode cannot be null");
        }
        this.mode = mode;
        lostDeposits = 0;   // count and time the new mode from scratch
        stepTimes = StepTimes.NONE;
        changed();
    }

    /**
     * Turns the weather on: each tick takes the next weather from the source's
     * iterator. Only hasNext() and next() are used, so an endless Forecast and a
     * finite List work the same way.
     */
    public void startWeather(Iterable<Weather> source) {
        if (source == null) {
            throw new IllegalArgumentException("Weather source cannot be null");
        }
        forecast = source.iterator();
        weather = nextWeather();
        changed();
    }

    /** Turns the weather off: no more sun or rain. */
    public void stopWeather() {
        forecast = Collections.emptyIterator();
        weather = Weather.CALM;
        changed();
    }

    // External iteration: one weather per tick, at the simulation's pace. An exhausted source means CALM.
    private Weather nextWeather() {
        return forecast.hasNext() ? forecast.next() : Weather.CALM;
    }

    /** Clears the trail and scatters every agent, so the network has to form again from noise. */
    public void scatter() {
        trail.clear();
        for (SlimeAgent agent : agents) {
            double x = random.nextDouble() * trail.getWidth();
            double y = random.nextDouble() * trail.getHeight();
            double heading = random.nextDouble() * 2 * Math.PI;
            agent.placeAt(x, y, heading);
        }
        changed();
    }

    // Every change ends here: measure the new state, then tell the observers.
    private void changed() {
        stats = measure();
        notifyObservers();
    }

    /**
     * Measures the trail and the agents with read-only stream pipelines. In a
     * parallel mode these are parallel reductions: per-chunk results, combined
     * at the join.
     */
    private SimulationStats measure() {
        double threshold = Settings.NETWORK_THRESHOLD;

        // One pass gives count, sum, average, minimum and maximum together.
        DoubleSummaryStatistics trailStats = trailValues().summaryStatistics();

        long networkCells = trailValues()
                .filter(value -> value > threshold)
                .count();

        // true: agents on the network; false: the rest
        Map<Boolean, Long> onNetwork = agentStream()
                .collect(Collectors.partitioningBy(
                        agent -> trail.sample(agent.getX(), agent.getY()) > threshold,
                        Collectors.counting()));

        double agentShare = agents.isEmpty() ? 0 : (double) onNetwork.get(true) / agents.size();
        return new SimulationStats(tick, agents.size(), rule.describe(ruleName), mode,
                (double) networkCells / trail.getCellCount(), agentShare,
                trailStats.getAverage(), trailStats.getMax(), stepTimes, lostDeposits);
    }

    @Override
    public void registerObserver(Observer observer) {
        if (observer == null) {
            throw new IllegalArgumentException("Observer cannot be null");
        }
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    /**
     * Calls update() on every observer. An observer that throws is removed and the
     * rest are still updated, so one fault cannot stop the simulation. Loops over a
     * copy, so removing an observer is safe.
     */
    @Override
    public void notifyObservers() {
        for (Observer observer : new ArrayList<>(observers)) {
            try {
                observer.update();
            } catch (RuntimeException e) {
                System.err.println("Observer " + observer.getClass().getSimpleName()
                        + " failed and was removed: " + e);
                observers.remove(observer);
            }
        }
    }

    public SimulationStats getStats() {
        return stats;
    }

    public SteeringRule getSteeringRule() {
        return rule;
    }

    /** Returns the rule's full description, such as "follow trail + drift + jitter". */
    public String getSteeringRuleName() {
        return rule.describe(ruleName);
    }

    public ExecutionMode getExecutionMode() {
        return mode;
    }

    /** Returns true while the weather is on; CALM is used only while it is off. */
    public boolean isWeatherOn() {
        return weather != Weather.CALM;
    }

    /** Returns the weather for the next tick, or CALM while the weather is off. */
    public Weather getWeather() {
        return weather;
    }

    public TrailMap getTrail() {
        return trail;
    }

    public int getTick() {
        return tick;
    }

    /** Returns a read-only view of the agents. */
    public List<SlimeAgent> getAgents() {
        return Collections.unmodifiableList(agents);
    }
}
