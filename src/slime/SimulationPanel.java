package slime;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Random;
import javax.swing.JPanel;

/**
 * The view: draws the current state of the simulation, scaled up, with
 * the weather, its statistics and the network chart on top.
 *
 * It is also a concrete observer: when the simulation changes, update()
 * asks Swing to repaint, so nothing else has to remember to do it.
 */
public class SimulationPanel extends JPanel implements Observer {

    private static final int CHART_WIDTH = 230;
    private static final int CHART_HEIGHT = 80;
    private static final int LINE_HEIGHT = 18;
    private static final int NO_WARNING = -1;   // no line of a text box is drawn in red
    private static final Color TEXT = new Color(255, 255, 255, 200);
    private static final Color WARNING = new Color(255, 90, 90);
    private static final Color BOX = new Color(0, 0, 0, 160);
    private static final int RAINDROPS = 500;   // raindrops drawn in a downpour
    private static final int RAIN_FALL = 12;    // pixels a raindrop falls each tick

    private final Simulation simulation;   // the subject this observer pulls its data from
    private final NetworkChart chart;
    private final TrailRenderer renderer;
    private final int scale;
    private double renderMillis = 0;       // how long rendering takes, smoothed
    private final int[] dropX = new int[RAINDROPS];   // where each raindrop starts, in pixels
    private final int[] dropY = new int[RAINDROPS];

    public SimulationPanel(Simulation simulation, NetworkChart chart, int scale) {
        this.simulation = simulation;
        this.chart = chart;
        this.scale = scale;
        TrailMap trail = simulation.getTrail();
        this.renderer = new TrailRenderer(trail.getWidth(), trail.getHeight());

        // The raindrops only decorate the picture, so they use a generator
        // of their own and never touch the simulation's random numbers.
        Random random = new Random();
        for (int i = 0; i < RAINDROPS; i++) {
            dropX[i] = random.nextInt(trail.getWidth() * scale);
            dropY[i] = random.nextInt(trail.getHeight() * scale);
        }
    }

    /**
     * Observer: the simulation changed, so schedule a repaint.
     * JPanel already has an update(Graphics) method; this update() takes
     * no parameters, so it is an overload, not an override.
     */
    @Override
    public void update() {
        repaint();
    }

    // Tells the window how big this panel wants to be; frame.pack() asks for it.
    @Override
    public Dimension getPreferredSize() {
        TrailMap trail = simulation.getTrail();
        return new Dimension(trail.getWidth() * scale, trail.getHeight() * scale);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // The picture is rendered sequentially or in parallel, like the simulation.
        TrailMap trail = simulation.getTrail();
        long start = System.nanoTime();
        BufferedImage frame = renderer.render(trail, simulation.getExecutionMode().isParallel());
        recordRenderTime(System.nanoTime() - start);

        // Smooth scaling, so each cell blends into its neighbours instead of showing as a block
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(frame, 0, 0, trail.getWidth() * scale, trail.getHeight() * scale, null);

        drawWeather(g2);
        drawInfo(g2);
        chart.draw(g2, getWidth() - CHART_WIDTH - 10, getHeight() - CHART_HEIGHT - 10,
                CHART_WIDTH, CHART_HEIGHT);
    }

    // Smoothed like the step time, so the number does not flicker.
    private void recordRenderTime(long nanos) {
        double millis = nanos / 1_000_000.0;
        renderMillis = (renderMillis == 0) ? millis : renderMillis + (millis - renderMillis) / 10;
    }

    // The weather over the trail: a patch of sunlight, and falling rain.
    private void drawWeather(Graphics2D g2) {
        if (!simulation.isWeatherOn()) {
            return;
        }
        Weather weather = simulation.getWeather();
        if (weather.getSunlight() > 0) {
            drawSun(g2, weather);
        }
        if (weather.getRain() > 0) {
            drawRain(g2, weather);
        }
    }

    // A glow that fades from the centre to the edge, as the sunlight's strength does.
    private void drawSun(Graphics2D g2, Weather weather) {
        int width = getWidth();
        int height = getHeight();
        int radius = (int) Math.round(Settings.SUN_RADIUS * scale);
        int x = (int) Math.round(weather.getSunX() * width);
        int y = (int) Math.round(weather.getSunY() * height);
        int alpha = (int) Math.round(weather.getSunlight() * 110);
        Color[] colours = {new Color(255, 240, 190, alpha), new Color(255, 240, 190, 0)};
        // The map wraps around, so near an edge the patch also shows on the opposite side.
        for (int dx = -width; dx <= width; dx += width) {
            for (int dy = -height; dy <= height; dy += height) {
                g2.setPaint(new RadialGradientPaint(x + dx, y + dy, radius, new float[] {0f, 1f}, colours));
                g2.fillOval(x + dx - radius, y + dy - radius, 2 * radius, 2 * radius);
            }
        }
    }

    // A blue tint, and raindrops that fall a little further each tick, slanting with the wind.
    private void drawRain(Graphics2D g2, Weather weather) {
        int width = getWidth();
        int height = getHeight();
        g2.setColor(new Color(40, 70, 140, (int) Math.round(weather.getRain() * 80)));
        g2.fillRect(0, 0, width, height);
        int fallen = simulation.getTick() * RAIN_FALL;
        int drops = (int) Math.round(weather.getRain() * RAINDROPS);
        g2.setColor(new Color(180, 205, 255, 150));
        for (int i = 0; i < drops; i++) {
            int x = Math.floorMod(dropX[i] - fallen / 4, width);
            int y = Math.floorMod(dropY[i] + fallen, height);
            g2.drawLine(x, y, x - 3, y + 10);
        }
    }

    private void drawInfo(Graphics2D g2) {
        SimulationStats stats = simulation.getStats();
        StepTimes times = stats.getStepTimes();
        String[] info = {
                "tick " + stats.getTick() + "   agents " + stats.getAgentCount()
                        + "   rule " + stats.getRuleName(),
                String.format("network %.1f%% of map   %.0f%% of agents on it",
                        stats.getNetworkFraction() * 100, stats.getAgentsOnNetworkFraction() * 100),
                describeWeather(),
                String.format("%s   move %.2f  deposit %.2f  spread %.2f  render %.2f ms",
                        stats.getMode().getLabel(), times.getMoveMillis(), times.getDepositMillis(),
                        times.getSpreadMillis(), renderMillis),
                "lost deposits " + stats.getLostDeposits()
        };
        String[] help = {
                "rules       1 follow  2 avoid  3 wander  4 swirl",
                "decorators  J jitter  D drift  M momentum",
                "W weather  R scatter  P parallel  Space pause"
        };

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        // Deposits can only be lost in the unsafe mode; red makes them stand out.
        int warning = (stats.getLostDeposits() > 0) ? info.length - 1 : NO_WARNING;
        drawTextBox(g2, info, 10, 20, warning);
        drawTextBox(g2, help, 10, getHeight() - 12 - (help.length - 1) * LINE_HEIGHT, NO_WARNING);
    }

    // One line about the weather.
    private String describeWeather() {
        if (!simulation.isWeatherOn()) {
            return "weather off";
        }
        return "weather " + simulation.getWeather();
    }

    // Draws lines of text on a dark box, like the chart's, so they stay
    // readable over bright trail. The line at index warning is drawn in red.
    private void drawTextBox(Graphics2D g2, String[] lines, int x, int firstBaseline, int warning) {
        FontMetrics metrics = g2.getFontMetrics();
        int width = Arrays.stream(lines).mapToInt(metrics::stringWidth).max().orElse(0);
        g2.setColor(BOX);
        g2.fillRect(x - 6, firstBaseline - 15, width + 12, lines.length * LINE_HEIGHT + 6);
        for (int i = 0; i < lines.length; i++) {
            g2.setColor(i == warning ? WARNING : TEXT);
            g2.drawString(lines[i], x, firstBaseline + i * LINE_HEIGHT);
        }
    }
}
