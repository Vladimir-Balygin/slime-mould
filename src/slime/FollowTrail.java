package slime;

import java.util.Random;

/** Concrete strategy: turn toward the strongest trail. This is the rule that builds networks. */
public class FollowTrail implements SteeringRule {

    @Override
    public double turn(double left, double front, double right, Random random) {
        if (front > left && front > right) {
            return 0;   // strongest straight ahead: keep going
        }
        if (front < left && front < right) {
            // weakest straight ahead: turn to either side at random
            return random.nextBoolean() ? Settings.TURN_ANGLE : -Settings.TURN_ANGLE;
        }
        if (left > right) {
            return -Settings.TURN_ANGLE;
        }
        if (right > left) {
            return Settings.TURN_ANGLE;
        }
        return 0;   // all readings equal: keep going
    }

    @Override
    public String toString() {
        return "follow trail";
    }
}
