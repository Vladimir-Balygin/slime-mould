package slime;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Concrete observer: remembers how much of the map was network over the
 * most recent ticks, and draws that history as a small line chart.
 */
public class NetworkChart implements Observer {

    private static final int PADDING = 8;
    private static final int LABEL_SPACE = 16;

    private final Simulation simulation;   // the subject this observer pulls its data from
    private final Deque<Double> history = new ArrayDeque<>();
    private final int length;
    private int lastTick = -1;

    public NetworkChart(Simulation simulation, int length) {
        if (length < 2) {
            throw new IllegalArgumentException("A chart needs at least 2 points, got " + length);
        }
        this.simulation = simulation;
        this.length = length;
    }

    @Override
    public void update() {
        SimulationStats stats = simulation.getStats();
        if (stats.getTick() == lastTick) {
            return;   // a key press, not a new tick: nothing new to plot
        }
        lastTick = stats.getTick();
        history.addLast(stats.getNetworkFraction());
        if (history.size() > length) {
            history.removeFirst();   // keep only the most recent ticks
        }
    }

    /** Draws the chart inside the given box, in panel coordinates. */
    public void draw(Graphics2D g2, int x, int y, int width, int height) {
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(x, y, width, height);
        if (history.isEmpty()) {
            return;
        }

        // Scale so the highest point fills the box. max() returns an
        // OptionalDouble because an empty stream has no maximum.
        double top = history.stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(1.0);
        if (top <= 0) {
            top = 1.0;   // all zeros, e.g. just after a scatter
        }

        int plotWidth = width - 2 * PADDING;
        int plotHeight = height - 2 * PADDING - LABEL_SPACE;
        int[] xs = new int[history.size()];
        int[] ys = new int[history.size()];
        int i = 0;
        for (double value : history) {   // the for-each loop asks the deque for its iterator
            xs[i] = x + PADDING + (int) Math.round(i * (double) plotWidth / (length - 1));
            ys[i] = y + height - PADDING - (int) Math.round(value / top * plotHeight);
            i++;
        }

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(new Color(0xFF, 0xC8, 0x5A));
        g2.drawPolyline(xs, ys, xs.length);

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        g2.setColor(new Color(255, 255, 255, 200));
        g2.drawString(String.format("network %.1f%% of map", history.peekLast() * 100),
                x + PADDING, y + PADDING + 10);
    }
}
