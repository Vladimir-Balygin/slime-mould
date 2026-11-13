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
}
