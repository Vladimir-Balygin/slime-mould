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
 * Custom collector that counts the agents on each cell of the trail map, so the
 * simulation can deposit all trail in one pass.
 *
 * Thread safe in a parallel stream: the supplier gives each chunk its own list,
 * so no two threads write the same list. The combiner joins lists only after
 * their chunks are done, and the finisher runs once at the end.
 *
 * Each chunk stores one cell number per agent instead of a full grid: Benchmark
 * showed a grid per chunk made the parallel deposit slower than sequential. The
 * cost is boxing every cell number into an Integer.
 */
public class DepositCollector implements Collector<SlimeAgent, List<Integer>, int[]> {

    private final TrailMap trail;

    public DepositCollector(TrailMap trail) {
        if (trail == null) {
            throw new IllegalArgumentException("Trail map cannot be null");
        }
        this.trail = trail;
    }

    /** Supplies a new, empty list for each chunk of agents. */
    @Override
    public Supplier<List<Integer>> supplier() {
        return ArrayList::new;
    }

    /** Adds the cell number under one agent to its chunk's list. */
    @Override
    public BiConsumer<List<Integer>, SlimeAgent> accumulator() {
        return (cells, agent) -> cells.add(trail.indexOf(agent.getX(), agent.getY()));
    }

    /** Joins two chunks by appending the right list to the left one. */
    @Override
    public BinaryOperator<List<Integer>> combiner() {
        return (left, right) -> {
            left.addAll(right);
            return left;
        };
    }

    /** Counts the cell numbers into a grid, once, after all joins. */
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
     * UNORDERED: counting order cannot change the counts. Not IDENTITY_FINISH:
     * the finisher builds the grid. Not CONCURRENT: an ArrayList must not be
     * shared between threads.
     */
    @Override
    public Set<Characteristics> characteristics() {
        return Collections.unmodifiableSet(EnumSet.of(Characteristics.UNORDERED));
    }
}
