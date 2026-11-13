package slime;

import java.util.Collections;
import java.util.EnumSet;
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
 * The three type parameters of {@code Collector<SlimeAgent, int[], int[]>}:
 * the stream's elements are SlimeAgents, the container being filled is an
 * int[] grid of counts, and the final result is that same grid.
 *
 * Why it is thread safe in a parallel stream: the stream splits the agents
 * into chunks and calls the supplier once per chunk, so each thread counts
 * into its own private grid. No two threads ever write the same array. The
 * combiner adds two grids together only when their chunks are joined, after
 * the threads that filled them have finished.
 *
 * The trade-off: every chunk allocates a whole grid (120,000 ints, about
 * 470 KB), and every join adds two grids cell by cell. More cores means
 * more chunks, so more memory and more adding.
 */
public class DepositCollector implements Collector<SlimeAgent, int[], int[]> {

    private final TrailMap trail;

    public DepositCollector(TrailMap trail) {
        if (trail == null) {
            throw new IllegalArgumentException("Trail map cannot be null");
        }
        this.trail = trail;
    }

    /** Makes a new, empty grid: one for each chunk of agents. */
    @Override
    public Supplier<int[]> supplier() {
        return () -> new int[trail.getCellCount()];
    }

    /** Counts one agent into its own chunk's grid. */
    @Override
    public BiConsumer<int[], SlimeAgent> accumulator() {
        return (counts, agent) -> counts[trail.indexOf(agent.getX(), agent.getY())]++;
    }

    /** Joins two chunks: adds the right grid into the left one and keeps the left. */
    @Override
    public BinaryOperator<int[]> combiner() {
        return (left, right) -> {
            for (int i = 0; i < left.length; i++) {
                left[i] += right[i];
            }
            return left;
        };
    }

    /** The grid of counts is already the result, so the finisher changes nothing. */
    @Override
    public Function<int[], int[]> finisher() {
        return Function.identity();
    }

    /**
     * IDENTITY_FINISH: the finisher changes nothing, so the stream may skip it.
     * UNORDERED: the order agents are counted in cannot change the counts.
     *
     * Deliberately not CONCURRENT. A concurrent collector tells the stream
     * that all threads may share one container, and a plain int[] is not
     * safe to share: two threads adding to the same cell at the same moment
     * would lose a count.
     */
    @Override
    public Set<Characteristics> characteristics() {
        return Collections.unmodifiableSet(
                EnumSet.of(Characteristics.IDENTITY_FINISH, Characteristics.UNORDERED));
    }
}
