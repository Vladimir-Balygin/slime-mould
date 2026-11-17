package slime;

/**
 * Decorator pattern: the abstract decorator. A RuleDecorator is a
 * SteeringRule that wraps another SteeringRule and adds something to it,
 * without changing the rule it wraps.
 *
 * Implementing SteeringRule only buys type compatibility: a decorated rule
 * can go anywhere a plain rule can, and SlimeAgent cannot tell the
 * difference. The wrapped rule, the aggregated reference, does the real
 * work. A decorator can wrap another decorator, but every stack bottoms out
 * in a plain rule such as FOLLOW, because you cannot decorate something that
 * does not exist.
 *
 * Decorators are immutable (final fields only) and draw random numbers from
 * the agent's own Random, so one decorated rule can be shared by every
 * agent, even across threads in the parallel modes.
 */
public abstract class RuleDecorator implements SteeringRule {

    protected final SteeringRule inner;   // the wrapped rule: it does the real work
    private final String label;           // this decorator's part of the description

    protected RuleDecorator(SteeringRule inner, String label) {
        if (inner == null) {
            throw new IllegalArgumentException("A decorator needs a rule to wrap");
        }
        this.inner = inner;
        this.label = label;
    }

    /**
     * Overrides the default method: the wrapped rule describes itself first,
     * then this decorator adds its own part. Down a stack of decorators this
     * builds a description such as "follow trail + drift + jitter".
     */
    @Override
    public String describe(String name) {
        return inner.describe(name) + " + " + label;
    }
}
