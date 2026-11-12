package slime;

import java.util.Random;

/**
 * The concrete steering strategies, written as lambdas instead of classes.
 *
 * Before this change, FollowTrail, AvoidTrail and Wander were three classes
 * that each implemented SteeringRule. Each class existed only to hold one
 * method, so a lambda or a method reference now does the same job.
 */
public final class SteeringRules {

    /**
     * Turn toward the strongest trail: the rule that builds networks.
     * A method reference to the static method below.
     */
    public static final SteeringRule FOLLOW = SteeringRules::towardStrongest;

    /**
     * Turn toward the weakest trail. Built by reusing FOLLOW: negating the
     * readings makes the weakest reading the strongest one.
     */
    public static final SteeringRule AVOID =
            (left, front, right, random) -> FOLLOW.turn(-left, -front, -right, random);

    /**
     * Ignore the trail and turn left, right or not at all, at random.
     * The control experiment: no network should form under this rule.
     */
    public static final SteeringRule WANDER =
            (left, front, right, random) -> (random.nextInt(3) - 1) * Settings.TURN_ANGLE;

    private SteeringRules() {
        // constants only: never instantiated
    }

    private static double towardStrongest(double left, double front, double right, Random random) {
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
}
