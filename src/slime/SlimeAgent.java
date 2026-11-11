package slime;

import java.util.Random;

/**
 * One slime mould agent. Every tick it senses the trail ahead, turns
 * toward the strongest smell, steps forward and leaves trail behind.
 * Networks emerge from thousands of agents doing only this.
 */
public class SlimeAgent {

    private double x;
    private double y;
    private double heading;        // direction of travel, in radians
    private final Random random;   // each agent owns one, so no agent shares it

    public SlimeAgent(double x, double y, double heading, Random random) {
        this.x = x;
        this.y = y;
        this.heading = heading;
        this.random = random;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getHeading() {
        return heading;
    }

    /** One tick of behaviour: sense and turn, move, deposit. */
    public void update(TrailMap trail) {
        senseAndTurn(trail);
        move(trail.getWidth(), trail.getHeight());
        trail.deposit(x, y, Settings.DEPOSIT_AMOUNT);
    }

    private void senseAndTurn(TrailMap trail) {
        double left = senseAt(trail, heading - Settings.SENSOR_ANGLE);
        double front = senseAt(trail, heading);
        double right = senseAt(trail, heading + Settings.SENSOR_ANGLE);

        // The "follow" rule, written directly into the agent for now.
        // Phase 2 moves this decision out into a separate strategy.
        if (front > left && front > right) {
            return;   // strongest straight ahead: keep going
        }
        if (front < left && front < right) {
            // weakest straight ahead: turn to either side at random
            heading += random.nextBoolean() ? Settings.TURN_ANGLE : -Settings.TURN_ANGLE;
        } else if (left > right) {
            heading -= Settings.TURN_ANGLE;
        } else if (right > left) {
            heading += Settings.TURN_ANGLE;
        }
        // otherwise all readings are equal: keep going
    }

    // Reads the trail at one sensor. Without the limit, agents always
    // prefer the very strongest path, strong paths steal agents from
    // weaker ones, and the network collapses into a few thick bands.
    private double senseAt(TrailMap trail, double angle) {
        double sensorX = x + Math.cos(angle) * Settings.SENSOR_DISTANCE;
        double sensorY = y + Math.sin(angle) * Settings.SENSOR_DISTANCE;
        return Math.min(trail.sample(sensorX, sensorY), Settings.SENSOR_LIMIT);
    }

    private void move(int width, int height) {
        x = wrap(x + Math.cos(heading) * Settings.STEP_SIZE, width);
        y = wrap(y + Math.sin(heading) * Settings.STEP_SIZE, height);
    }

    // Keeps a coordinate inside 0..size, wrapping around the edges.
    private static double wrap(double value, int size) {
        double result = value % size;
        return (result < 0) ? result + size : result;
    }
}
