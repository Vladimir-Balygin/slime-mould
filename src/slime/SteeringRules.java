package slime;

import java.util.Random;

/**
 * The concrete strategies, written as lambdas and a method reference because a
 * class per rule would hold only one method. SWIRL is a ready-made decorator stack.
 */
public final class SteeringRules {

    /** Turns toward the strongest trail: the rule that builds networks. */
    public static final SteeringRule FOLLOW = SteeringRules::towardStrongest;

    /** Turns toward the weakest trail, by negating the readings and reusing FOLLOW. */
    public static final SteeringRule AVOID =
            (left, front, right, random) -> FOLLOW.turn(-left, -front, -right, random);

    /** Ignores the trail and turns at random. Control case: no network should form. */
    public static final SteeringRule WANDER =
            (left, front, right, random) -> (random.nextInt(3) - 1) * Settings.TURN_ANGLE;

    /** FOLLOW, then drift twice and jitter: the agents circle and the network churns like foam. */
    public static final SteeringRule SWIRL = FOLLOW
            .withDrift(Settings.DRIFT_ANGLE)
            .withDrift(Settings.DRIFT_ANGLE)
            .withJitter(Settings.JITTER_ANGLE);

    private SteeringRules() {
        // constants only: never instantiated
    }

    /** Returns the turn toward the strongest of the three readings: FOLLOW's logic. */
    private static double towardStrongest(double left, double front, double right, Random random) {
        if (front > left && front > right) {
            return 0;   // strongest ahead: keep going
        }
        if (front < left && front < right) {
            // weakest ahead: turn either way at random
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
