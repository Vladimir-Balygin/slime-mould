package slime;

/**
 * How the simulation does the work inside each tick: on one thread, or
 * split across the processor's cores with parallel streams.
 *
 * An enum is a class with a fixed set of instances, here three. Like any
 * class it can have fields, a constructor and methods.
 */
public enum ExecutionMode {

    /** Every stage runs on one thread. */
    SEQUENTIAL("sequential", false),

    /** Every stage runs on parallel streams; deposits go through the DepositCollector. */
    PARALLEL("parallel", true),

    /**
     * Like PARALLEL, but every thread adds its agents' trail straight into
     * the one shared trail map. This is not thread safe: some deposits are
     * lost, and the simulation counts how many.
     */
    PARALLEL_UNSAFE("parallel, unsafe deposit", true);

    private final String label;
    private final boolean parallel;

    ExecutionMode(String label, boolean parallel) {
        this.label = label;
        this.parallel = parallel;
    }

    public String getLabel() {
        return label;
    }

    public boolean isParallel() {
        return parallel;
    }

    /** The next mode in the order above, going back to the first after the last. */
    public ExecutionMode next() {
        ExecutionMode[] modes = values();
        return modes[(ordinal() + 1) % modes.length];
    }
}
