package slime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The model: owns the agents and the trail map, and advances them one
 * tick at a time. It knows nothing about Swing, so it can be run,
 * tested or timed without opening a window.
 */
public class Simulation {

    private final TrailMap trail;
    private final List<SlimeAgent> agents = new ArrayList<>();
    private final Random random;   // places and scatters the agents
    private SteeringRule rule = new FollowTrail();
    private int tick = 0;

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
    }

    /** One tick: every agent acts, then the whole trail spreads and fades. */
    public void step() {
        for (SlimeAgent agent : agents) {
            agent.update(trail);
        }
        trail.diffuseAndDecay(Settings.DECAY_RATE);
        tick++;
    }

    /**
     * Gives every agent the same new strategy. Rules hold no state of their
     * own, so one rule object can safely be shared by all 15,000 agents.
     */
    public void setSteeringRule(SteeringRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("Steering rule cannot be null");
        }
        this.rule = rule;
        for (SlimeAgent agent : agents) {
            agent.setSteeringRule(rule);
        }
    }

    public SteeringRule getSteeringRule() {
        return rule;
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
