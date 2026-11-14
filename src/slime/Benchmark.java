package slime;

import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

/**
 * Times the simulation in each execution mode, without opening a window:
 *
 *     java -cp out slime.Benchmark
 *
 * Measure first, optimise second. Timing Java code is noisy: the JIT
 * compiler keeps optimising the code while it runs, and the garbage
 * collector can pause the program at any moment. So this benchmark
 *   - warms every mode up before timing anything,
 *   - times several runs of each mode and reports the average and the range,
 *   - takes turns between the modes, so a slow moment cannot land on one mode only.
 */
public final class Benchmark {

    private static final int WARM_UP_STEPS = 300;
    private static final int RUNS = 5;
    private static final int STEPS_PER_RUN = 200;

    /**
     * Everything measured for one mode. A static nested class, because it
     * only makes sense inside the benchmark.
     */
    private static class Results {
        final Simulation simulation;
        final double[] stepMillis = new double[RUNS];     // one average per run
        final double[] renderMillis = new double[RUNS];
        double moveMillis;      // the stage times, added up over the runs
        double depositMillis;
        double spreadMillis;
        long lostBefore;        // lost deposits already counted during the warm-up

        Results(Simulation simulation) {
            this.simulation = simulation;
        }
    }

    private Benchmark() {
        // only main() is used
    }

    public static void main(String[] args) {
        // availableProcessors() counts logical processors, not physical cores.
        // With Hyper-Threading, each physical core runs two hardware threads and
        // is counted twice, but those two threads share the one core's circuits.
        System.out.printf("Java sees %d logical processors (with Hyper-Threading, each physical core counts as two)%n",
                Runtime.getRuntime().availableProcessors());
        System.out.printf("Parallel streams use the calling thread plus the fork/join common pool's %d worker thread(s)%n",
                ForkJoinPool.commonPool().getParallelism());
        System.out.printf("%,d agents on a %d x %d map; %d runs of %d steps per mode after %d warm-up steps%n%n",
                Settings.AGENT_COUNT, Settings.WIDTH, Settings.HEIGHT, RUNS, STEPS_PER_RUN, WARM_UP_STEPS);

        Map<ExecutionMode, Results> results = new EnumMap<>(ExecutionMode.class);
        TrailRenderer renderer = new TrailRenderer(Settings.WIDTH, Settings.HEIGHT);

        // Warm-up: the same seed in every mode, so every mode times the same kind of network.
        for (ExecutionMode mode : ExecutionMode.values()) {
            Simulation simulation = new Simulation(
                    Settings.WIDTH, Settings.HEIGHT, Settings.AGENT_COUNT, Settings.SEED);
            simulation.setExecutionMode(mode);
            millisPerCall(WARM_UP_STEPS, simulation::step);
            millisPerCall(WARM_UP_STEPS, () -> renderer.render(simulation.getTrail(), mode.isParallel()));
            Results result = new Results(simulation);
            result.lostBefore = simulation.getStats().getLostDeposits();
            results.put(mode, result);
        }

        // The timed runs, taking turns between the modes.
        for (int run = 0; run < RUNS; run++) {
            for (ExecutionMode mode : ExecutionMode.values()) {
                Results result = results.get(mode);
                Simulation simulation = result.simulation;
                result.stepMillis[run] = millisPerCall(STEPS_PER_RUN, simulation::step);
                result.renderMillis[run] = millisPerCall(STEPS_PER_RUN,
                        () -> renderer.render(simulation.getTrail(), mode.isParallel()));

                // The simulation's own smoothed stage times, at the end of the run
                StepTimes times = simulation.getStats().getStepTimes();
                result.moveMillis += times.getMoveMillis();
                result.depositMillis += times.getDepositMillis();
                result.spreadMillis += times.getSpreadMillis();
            }
        }
        printTable(results);
    }

    private static void printTable(Map<ExecutionMode, Results> results) {
        double sequential = Arrays.stream(results.get(ExecutionMode.SEQUENTIAL).stepMillis)
                .average()
                .orElse(0);
        System.out.printf("%-25s %-20s %9s %7s %8s %7s %8s %10s%n", "mode", "step ms (range)",
                "speed-up", "move", "deposit", "spread", "render", "lost/step");
        results.forEach((mode, result) -> {
            DoubleSummaryStatistics steps = Arrays.stream(result.stepMillis).summaryStatistics();
            double render = Arrays.stream(result.renderMillis).average().orElse(0);
            long lost = result.simulation.getStats().getLostDeposits() - result.lostBefore;
            System.out.printf("%-25s %5.2f (%5.2f-%5.2f) %8.2fx %7.2f %8.2f %7.2f %8.2f %10.2f%n",
                    mode.getLabel(), steps.getAverage(), steps.getMin(), steps.getMax(),
                    sequential / steps.getAverage(),
                    result.moveMillis / RUNS, result.depositMillis / RUNS, result.spreadMillis / RUNS,
                    render, (double) lost / (RUNS * STEPS_PER_RUN));
        });
        System.out.printf("%nstep:      a whole step, including the statistics and the lost-deposit check%n"
                + "speed-up:  how many times faster a step is than sequential. Expect less than the number of%n"
                + "           processors: splitting and joining take time, and two hyper-threads share one core%n"
                + "move, deposit, spread: the three stages of a step on their own, in ms%n"
                + "render:    turning the trail into one frame's pixels, in ms%n"
                + "lost/step: deposits that threads overwrote, out of %,d per step%n", Settings.AGENT_COUNT);
    }

    // Runs the work many times and returns the average time of one call, in milliseconds.
    private static double millisPerCall(int calls, Runnable work) {
        long start = System.nanoTime();
        for (int i = 0; i < calls; i++) {
            work.run();
        }
        return (System.nanoTime() - start) / 1_000_000.0 / calls;
    }
}