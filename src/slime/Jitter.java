package slime;

import java.util.Random;

/**
 * Concrete decorator: adds a random wobble to every turn, as if the agent
 * misjudges its own steering a little.
 */
public class Jitter extends RuleDecorator {

    private final double maxAngle;   // the largest wobble, in radians, either way

    public Jitter(SteeringRule inner, double maxAngle) {
        super(inner, "jitter");
        if (maxAngle < 0) {
            throw new IllegalArgumentException("Jitter angle cannot be negative: " + maxAngle);
        }
        this.maxAngle = maxAngle;
    }

    /** Asks the wrapped rule first, then adds a wobble between -maxAngle and +maxAngle. */
    @Override
    public double turn(double left, double front, double right, Random random) {
        double wobble = (random.nextDouble() * 2 - 1) * maxAngle;
        return inner.turn(left, front, right, random) + wobble;
    }
}
