package slime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * The model: owns the agents and the trail map, and advances them one
 * tick at a time. It knows nothing about Swing, so it can be run,
 * tested or timed without opening a window.
 *
 * In the Observer pattern this class is the concrete subject: after every
 * change it measures itself and notifies whoever has registered.
 */
public class Simulation implements Subject {

    private final TrailMap trail;
    private final List<SlimeAgent> agents = new ArrayList<>();
    private final List<Observer> observers = new ArrayList<>();
    private final Random random;   // places and scatters the agents
    private SteeringRule rule = SteeringRules.FOLLOW;
    private String ruleName = "follow trail";
    private int tick = 0;
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

    /** One tick: every agent acts, then the whole trail spreads and fades. */
    public void step() {
        for (SlimeAgent agent : agents) {
            agent.update(trail);
        }
        trail.diffuseAndDecay(Settings.DECAY_RATE);
        tick++;
        changed();
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
     * pipeline only reads; none of them changes the simulation.
     */
    private SimulationStats measure() {
        double threshold = Settings.NETWORK_THRESHOLD;

        // One pass gives count, sum, average, minimum and maximum together.
        DoubleSummaryStatistics trailStats = trail.values().summaryStatistics();

        // How many cells are strong enough to count as part of the network?
        long networkCells = trail.values()
                .filter(value -> value > threshold)
                .count();

        // Split the agents into two groups, on the network or not, and count each group.
        Map<Boolean, Long> onNetwork = agents.stream()
                .collect(Collectors.partitioningBy(
                        agent -> trail.sample(agent.getX(), agent.getY()) > threshold,
                        Collectors.counting()));

        double agentShare = agents.isEmpty() ? 0 : (double) onNetwork.get(true) / agents.size();
        return new SimulationStats(tick, agents.size(), ruleName,
                (double) networkCells / trail.getCellCount(), agentShare,
                trailStats.getAverage(), trailStats.getMax());
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
