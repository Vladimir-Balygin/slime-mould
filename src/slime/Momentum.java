package slime;

import java.util.Random;

/**
 * Concrete decorator: on some steps the agent keeps going straight without
 * asking the wrapped rule at all.
 *
 * Unlike Jitter and Drift, which always call the wrapped rule and then
 * adjust its answer, Momentum decides first whether to call it. That makes
 * the order of decorators matter: Momentum on the outside also skips any
 * decorators inside it.
 */
public class Momentum extends RuleDecorator {

    private final double chance;   // the share of steps that go straight, from 0 to 1

    public Momentum(SteeringRule inner, double chance) {
        super(inner, "momentum");
        if (chance < 0 || chance > 1) {
            throw new IllegalArgumentException("Momentum chance must be between 0 and 1: " + chance);
        }
        this.chance = chance;
    }

    @Override
    public double turn(double left, double front, double right, Random random) {
        if (random.nextDouble() < chance) {
            return 0;   // keep going straight; the wrapped rule is never asked
        }
        return inner.turn(left, front, right, random);
    }
}
