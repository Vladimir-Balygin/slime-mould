package slime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * A custom collector that counts how many agents stand on each cell of the
 * trail map. The simulation then deposits all their trail in one pass.
 *
 * The three type parameters of {@code Collector<SlimeAgent, List<Integer>, int[]>}:
 * the stream's elements are SlimeAgents, the container each chunk fills is
 * a list of cell numbers, and the final result is an int[] grid of counts.
 *
 * Why it is thread safe in a parallel stream: the stream splits the agents
 * into chunks and calls the supplier once per chunk, so each thread adds to
 * its own private list. No two threads ever write the same list. The
 * combiner joins two lists only when their chunks are joined, after the
 * threads that filled them have finished, and the finisher runs once, at
 * the very end.
 *
 * The first version gave every chunk a whole grid of 120,000 counts. The
 * Benchmark showed the cost: with more cores the stream makes more chunks,
 * and the parallel deposit stage became slower than the sequential one.
 * Now each chunk only stores one number per agent, and the grid is built
 * once, in the finisher. The price is boxing: a List cannot hold plain
 * ints, so every cell number becomes an Integer object.
 */
public class DepositCollector implements Collector<SlimeAgent, List<Integer>, int[]> {

    private final TrailMap trail;

    public DepositCollector(TrailMap trail) {
        if (trail == null) {
            throw new IllegalArgumentException("Trail map cannot be null");
        }
        this.trail = trail;
    }

    /** Makes a new, empty list: one for each chunk of agents. */
    @Override
    public Supplier<List<Integer>> supplier() {
        return ArrayList::new;
    }

    /** Adds the number of the cell one agent stands on to its own chunk's list. */
    @Override
    public BiConsumer<List<Integer>, SlimeAgent> accumulator() {
        return (cells, agent) -> cells.add(trail.indexOf(agent.getX(), agent.getY()));
    }

    /** Joins two chunks: appends the right list to the left one and keeps the left. */
    @Override
    public BinaryOperator<List<Integer>> combiner() {
        return (left, right) -> {
            left.addAll(right);
            return left;
        };
    }

    /** Runs once, after every join: counts the cell numbers into a grid. */
    @Override
    public Function<List<Integer>, int[]> finisher() {
        return cells -> {
            int[] agentsPerCell = new int[trail.getCellCount()];
            for (int cell : cells) {
                agentsPerCell[cell]++;
            }
            return agentsPerCell;
        };
    }

    /**
     * UNORDERED: the order agents are counted in cannot change the counts.
     *
     * Not IDENTITY_FINISH, because the finisher does real work: it turns
     * the list into a grid.
     *
     * Deliberately not CONCURRENT. A concurrent collector tells the stream
     * that all threads may share one container, and an ArrayList is not
     * safe to share: two threads adding at the same moment can overwrite
     * each other.
     */
    @Override
    public Set<Characteristics> characteristics() {
        return Collections.unmodifiableSet(EnumSet.of(Characteristics.UNORDERED));
    }
}
