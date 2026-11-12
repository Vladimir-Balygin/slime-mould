package slime;

import java.util.Random;

/** Concrete strategy: turn toward the weakest trail, so agents keep away from each other. */
public class AvoidTrail implements SteeringRule {

    @Override
    public double turn(double left, double front, double right, Random random) {
        if (front < left && front < right) {
            return 0;   // weakest straight ahead: keep going
        }
        if (front > left && front > right) {
            // strongest straight ahead: turn away to either side at random
            return random.nextBoolean() ? Settings.TURN_ANGLE : -Settings.TURN_ANGLE;
        }
        if (left < right) {
            return -Settings.TURN_ANGLE;
        }
        if (right < left) {
            return Settings.TURN_ANGLE;
        }
        return 0;   // all readings equal: keep going
    }

    @Override
    public String toString() {
        return "avoid trail";
    }
}
