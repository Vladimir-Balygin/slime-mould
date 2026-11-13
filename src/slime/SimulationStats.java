package slime;

/**
 * The simulation's numbers after one change. Immutable: every field is
 * final and there are no setters, so an observer can keep a snapshot
 * without anyone changing it underneath.
 */
public class SimulationStats {

    private final int tick;
    private final int agentCount;
    private final String ruleName;
    private final double networkFraction;           // share of cells strong enough to count as network
    private final double agentsOnNetworkFraction;   // share of agents standing on those cells
    private final double meanTrail;
    private final double maxTrail;

    public SimulationStats(int tick, int agentCount, String ruleName, double networkFraction,
                           double agentsOnNetworkFraction, double meanTrail, double maxTrail) {
        this.tick = tick;
        this.agentCount = agentCount;
        this.ruleName = ruleName;
        this.networkFraction = networkFraction;
        this.agentsOnNetworkFraction = agentsOnNetworkFraction;
        this.meanTrail = meanTrail;
        this.maxTrail = maxTrail;
    }

    public int getTick() {
        return tick;
    }

    public int getAgentCount() {
        return agentCount;
    }

    public String getRuleName() {
        return ruleName;
    }

    public double getNetworkFraction() {
        return networkFraction;
    }

    public double getAgentsOnNetworkFraction() {
        return agentsOnNetworkFraction;
    }

    public double getMeanTrail() {
        return meanTrail;
    }

    public double getMaxTrail() {
        return maxTrail;
    }

    @Override
    public String toString() {
        return String.format("tick %6d | %-12s | network %5.1f%% of cells | %5.1f%% of agents on it"
                        + " | mean trail %5.2f | max %7.2f",
                tick, ruleName, networkFraction * 100, agentsOnNetworkFraction * 100, meanTrail, maxTrail);
    }
}
