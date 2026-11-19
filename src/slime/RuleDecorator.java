package slime;

/**
 * Decorator pattern: the abstract decorator. Implementing SteeringRule gives
 * type compatibility; the wrapped rule (inner) does the real work, and every
 * stack ends in a plain rule such as FOLLOW. Immutable and uses the agent's
 * own Random, so one decorated rule is safe to share across threads.
 */
public abstract class RuleDecorator implements SteeringRule {

    protected final SteeringRule inner;   // the wrapped rule
    private final String label;           // this decorator's part of the description

    protected RuleDecorator(SteeringRule inner, String label) {
        if (inner == null) {
            throw new IllegalArgumentException("A decorator needs a rule to wrap");
        }
        this.inner = inner;
        this.label = label;
    }

    /** Returns the wrapped rule's description plus this label, e.g. "follow trail + drift + jitter". */
    @Override
    public String describe(String name) {
        return inner.describe(name) + " + " + label;
    }
}
