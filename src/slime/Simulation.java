package slime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

/**
 * The model: owns the agents and the trail map, and advances them one
 * tick at a time. It knows nothing about Swing, so it can be run,
 * tested or timed without opening a window.
 *
 * In the Observer pattern this class is the concrete subject: after every
 * change it measures itself and notifies whoever has registered.
 *
 * The work inside each tick runs either on one thread or on parallel
 * streams, depending on the ExecutionMode.
 */
public class Simulation implements Subject {

    private final TrailMap trail;
    private final List<SlimeAgent> agents = new ArrayList<>();
    private final List<Observer> observers = new ArrayList<>();
    private final Random random;   // places and scatters the agents
    private SteeringRule rule = SteeringRules.FOLLOW;
    private String ruleName = "follow trail";
    private ExecutionMode mode = ExecutionMode.PARALLEL;
    private int tick = 0;
    private long lostDeposits = 0;   // deposits lost since the mode last changed
    private StepTimes stepTimes = StepTimes.NONE;
    private SimulationStats stats;

    public Simulation(int width, int height, int agentCount, long seed) {
        if (agentCount < 0) {
            throw new IllegalArgumentException("Agent count cannot be negative: " + agentCount);
        }
        this.trail = new TrailMap(width, height);

        // One seeded generator places every agent, so the same seed
        // always produces the same run.
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
     * One tick, in three stages. Each stage finishes before the next starts:
     *   1. move:    every agent senses, turns and moves (the trail is only read)
     *   2. deposit: every agent adds trail where it now stands
     *   3. spread:  the whole trail blurs and fades
     *
     * Before, each agent deposited straight after it moved, so agents later
     * in the list could sense trail that earlier agents had just laid. Now
     * every agent senses the same trail, so the order the agents are
     * processed in no longer changes the result. That is what allows the
     * work inside a stage to be shared out between threads.
     *
     * Each stage is passed to timeNanos as a lambda or a method reference,
     * so the screen and the Benchmark can show how long each one takes.
     */
    public void step() {
        // Stage 1. Each agent only reads the trail and only changes its own
        // fields, so every agent is an independent task.
        long move = timeNanos(() -> agentStream().forEach(agent -> agent.senseAndMove(trail)));

        // Stage 2, then a check that no deposit went missing.
        double trailBefore = trail.total();
        long deposit = timeNanos(this::depositTrail);
        countLostDeposits(trailBefore);

        // Stage 3. Every row of the trail is an independent task (see TrailMap).
        long spread = timeNanos(() -> trail.diffuseAndDecay(Settings.DECAY_RATE, mode.isParallel()));

        stepTimes = stepTimes.record(move, deposit, spread);
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
            // NOT thread safe, on purpose. The lambda captures the shared trail
            // map and changes it. "+=" reads a cell, adds to it and writes it
            // back. If two threads do that to the same cell at the same moment,
            // both read the same old value, the second write overwrites the
            // first, and one deposit is lost.
            agents.parallelStream()
                    .forEach(agent -> trail.deposit(agent.getX(), agent.getY(), Settings.DEPOSIT_AMOUNT));
        } else {
            // Thread safe: each chunk of agents is counted into its own private
            // grid, and the grids are only added together at the join.
            int[] agentsPerCell = agentStream().collect(new DepositCollector(trail));
            trail.deposit(agentsPerCell, Settings.DEPOSIT_AMOUNT);
        }
    }

    // Every agent deposits the same amount, so the trail must have grown by
    // exactly agents x DEPOSIT_AMOUNT since trailBefore. Anything missing was lost.
    private void countLostDeposits(double trailBefore) {
        double expected = agents.size() * Settings.DEPOSIT_AMOUNT;
        double added = trail.total() - trailBefore;
        lostDeposits += Math.round((expected - added) / Settings.DEPOSIT_AMOUNT);
    }

    /**
     * The agents as a stream: sequential or parallel, depending on the mode.
     * Each pipeline that uses it is written once and works either way;
     * only the source of the stream changes.
     */
    private Stream<SlimeAgent> agentStream() {
        return mode.isParallel() ? agents.parallelStream() : agents.stream();
    }

    /** Every cell's trail value as a stream: sequential or parallel, depending on the mode. */
    private DoubleStream trailValues() {
        return mode.isParallel() ? trail.values().parallel() : trail.values();
    }

    /**
     * Gives every agent the same new strategy. Rules hold no state of their
     * own, so one rule object can safely be shared by all 15,000 agents.
     *
     * The name travels with the rule because a lambda has no toString()
     * of its own to describe itself.
     */
    public void setSteeringRule(String name, SteeringRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("Steering rule cannot be null");
        }
        this.rule = rule;
        this.ruleName = name;
        agents.forEach(agent -> agent.setSteeringRule(rule));
        changed();
    }

    /**
     * Switches how the work inside each tick is done. The agents and the
     * trail are not touched: in the two safe modes the model behaves
     * exactly the same, only the speed changes.
     */
    public void setExecutionMode(ExecutionMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("Execution mode cannot be null");
        }
        this.mode = mode;
        lostDeposits = 0;   // count and time the new mode from scratch
        stepTimes = StepTimes.NONE;
        changed();
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
     * Measures the trail and the agents with stream pipelines. Each
     * pipeline only reads; none of them changes the simulation. In a
     * parallel mode they become parallel reductions: each chunk gets its
     * own partial result, and the partial results are combined at the join.
     */
    private SimulationStats measure() {
        double threshold = Settings.NETWORK_THRESHOLD;

        // One pass gives count, sum, average, minimum and maximum together.
        DoubleSummaryStatistics trailStats = trailValues().summaryStatistics();

        // How many cells are strong enough to count as part of the network?
        long networkCells = trailValues()
                .filter(value -> value > threshold)
                .count();

        // Split the agents into two groups, on the network or not, and count each group.
        Map<Boolean, Long> onNetwork = agentStream()
                .collect(Collectors.partitioningBy(
                        agent -> trail.sample(agent.getX(), agent.getY()) > threshold,
                        Collectors.counting()));

        double agentShare = agents.isEmpty() ? 0 : (double) onNetwork.get(true) / agents.size();
        return new SimulationStats(tick, agents.size(), ruleName, mode,
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
     * Calls update() on every registered observer.
     *
     * Fault tolerance: if one observer throws, the others are still updated
     * and the simulation keeps running. The faulty observer is removed so it
     * cannot fail again on every tick. The loop runs over a copy of the list,
     * so removing an observer does not disturb the loop.
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

    public String getSteeringRuleName() {
        return ruleName;
    }

    public ExecutionMode getExecutionMode() {
        return mode;
    }

    public TrailMap getTrail() {
        return trail;
    }

    public int getTick() {
        return tick;
    }

    /** A read-only view: callers can look at the agents but cannot add or remove any. */
    public List<SlimeAgent> getAgents() {
        return Collections.unmodifiableList(agents);
    }
}
