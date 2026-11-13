package slime;

import java.util.Random;

/**
 * One slime mould agent. Every tick it senses the trail ahead, asks its
 * steering rule which way to turn, steps forward and leaves trail behind.
 *
 * In the Strategy pattern this class is the context: it has-a SteeringRule
 * and delegates the turning decision to it, so the same agent can follow,
 * avoid or ignore the trail depending on which rule it is given.
 */
public class SlimeAgent {

    private double x;
    private double y;
    private double heading;        // direction of travel, in radians
    private final Random random;   // each agent owns one, so no agent shares it
    private SteeringRule rule;     // the strategy this agent currently uses

    public SlimeAgent(double x, double y, double heading, Random random, SteeringRule rule) {
        this.x = x;
        this.y = y;
        this.heading = heading;
        this.random = random;
        this.rule = requireRule(rule);
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

    /** Swaps the strategy at run time; the agent's next turn uses the new rule. */
    public void setSteeringRule(SteeringRule rule) {
        this.rule = requireRule(rule);
    }

    // A null rule is a programming error, so it gets an unchecked exception.
    private static SteeringRule requireRule(SteeringRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("Steering rule cannot be null");
        }
        return rule;
    }

    /** Moves the agent to a new place, used when the simulation scatters its agents. */
    public void placeAt(double x, double y, double heading) {
        this.x = x;
        this.y = y;
        this.heading = heading;
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

        // The context does the sensing; the strategy makes the decision.
        heading += rule.turn(left, front, right, random);
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
