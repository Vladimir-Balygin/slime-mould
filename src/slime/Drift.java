package slime;

import java.util.Random;

/**
 * Concrete decorator: adds the same small turn to the right on every step,
 * so agents curve steadily even while they follow the trail. Wrapping a rule
 * in Drift twice doubles the curve.
 */
public class Drift extends RuleDecorator {

    private final double angle;   // the extra turn on every step, in radians

    public Drift(SteeringRule inner, double angle) {
        super(inner, "drift");
        this.angle = angle;
    }

    /** Asks the wrapped rule first, then adds the drift. */
    @Override
    public double turn(double left, double front, double right, Random random) {
        return inner.turn(left, front, right, random) + angle;
    }
}
