package slime;

import java.util.Random;

/**
 * Strategy: decides how an agent turns, given what its three sensors read.
 *
 * SlimeAgent is the context. It does the sensing, then hands the decision
 * to whichever rule it currently holds, so a new behaviour can be added
 * without changing SlimeAgent (open for extension, closed for modification).
 *
 * The interface has exactly one abstract method, which makes it a
 * functional interface: any lambda with the same parameters and return
 * type can be used wherever a SteeringRule is expected. The annotation
 * asks the compiler to check that this stays true.
 *
 * It is also the component in the Decorator pattern: a RuleDecorator is a
 * SteeringRule that wraps another SteeringRule.
 */
@FunctionalInterface
public interface SteeringRule {

    /**
     * Returns the change of heading in radians: negative turns left,
     * positive turns right, zero keeps going straight.
     *
     * The agent passes in its own Random, so a rule needs no fields of its own.
     */
    double turn(double left, double front, double right, Random random);

    /**
     * Describes this rule, given the name of the plain rule underneath.
     *
     * A default method: every SteeringRule gets it without writing it,
     * including the lambdas in SteeringRules, which have no name of their
     * own and simply return the name they are given. Decorators override
     * it to add their own part. Adding a default method does not break any
     * existing rule, and the interface is still functional, because the
     * only abstract method is still turn().
     */
    default String describe(String name) {
        return name;
    }

    // A fluent interface for decorating. Each method returns this rule
    // wrapped in one more decorator, and the result is again a SteeringRule,
    // so the calls chain like a stream pipeline and read left to right:
    //     FOLLOW.withDrift(a).withJitter(b)
    // instead of the inside-out new Jitter(new Drift(FOLLOW, a), b).

    /** This rule with a random wobble of up to maxAngle either way on every turn. */
    default SteeringRule withJitter(double maxAngle) {
        return new Jitter(this, maxAngle);
    }

    /** This rule with the same extra turn to the right on every step. */
    default SteeringRule withDrift(double angle) {
        return new Drift(this, angle);
    }

    /** This rule, except that a share (chance) of steps go straight without asking it. */
    default SteeringRule withMomentum(double chance) {
        return new Momentum(this, chance);
    }
}
