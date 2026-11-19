package slime;

import java.util.Random;

/**
 * Strategy pattern: decides how an agent turns. SlimeAgent is the context, so
 * new behaviour needs no change there. Also the component of the Decorator
 * pattern. One abstract method, so a lambda can implement it.
 */
@FunctionalInterface
public interface SteeringRule {

    /**
     * Returns the change of heading in radians: negative turns left, positive
     * turns right, 0 goes straight. Uses the agent's Random, so rules need no state.
     */
    double turn(double left, double front, double right, Random random);

    /** Returns this rule's description, given the plain rule's name. Decorators append their labels. */
    default String describe(String name) {
        return name;
    }

    // Fluent decorating: FOLLOW.withDrift(a).withJitter(b) instead of new Jitter(new Drift(FOLLOW, a), b).

    /** Returns this rule with a random extra turn of up to maxAngle either way. */
    default SteeringRule withJitter(double maxAngle) {
        return new Jitter(this, maxAngle);
    }

    /** Returns this rule with an extra turn to the right on every tick. */
    default SteeringRule withDrift(double angle) {
        return new Drift(this, angle);
    }

    /** Returns this rule, but a share (chance) of ticks go straight without asking it. */
    default SteeringRule withMomentum(double chance) {
        return new Momentum(this, chance);
    }
}
