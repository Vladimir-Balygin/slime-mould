package slime;

import java.util.Random;

/**
 * Concrete strategy: ignore the trail and turn at random. This is the
 * control experiment: if no network forms under this rule, the network
 * really does come from following the trail.
 */
public class Wander implements SteeringRule {

    @Override
    public double turn(double left, double front, double right, Random random) {
        // nextInt(3) - 1 gives -1, 0 or 1: left, straight or right, equally likely
        return (random.nextInt(3) - 1) * Settings.TURN_ANGLE;
    }

    @Override
    public String toString() {
        return "wander";
    }
}
