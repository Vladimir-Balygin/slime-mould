package slime;

import java.util.Random;

/**
 * Concrete decorator: on some ticks the agent goes straight without asking the
 * wrapped rule. Because it decides first, an outer Momentum also skips every
 * decorator inside it, so decorator order matters.
 */
public class Momentum extends RuleDecorator {

    private final double chance;   // share of ticks that go straight, 0 to 1

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
