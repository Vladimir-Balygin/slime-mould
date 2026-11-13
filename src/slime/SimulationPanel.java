package slime;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/**
 * The view: draws the current state of the simulation, scaled up, with
 * its statistics and the network chart on top.
 *
 * It is also a concrete observer: when the simulation changes, update()
 * asks Swing to repaint, so nothing else has to remember to do it.
 */
public class SimulationPanel extends JPanel implements Observer {

    private static final int CHART_WIDTH = 230;
    private static final int CHART_HEIGHT = 80;

    private final Simulation simulation;   // the subject this observer pulls its data from
    private final NetworkChart chart;
    private final TrailRenderer renderer;
    private final int scale;

    public SimulationPanel(Simulation simulation, NetworkChart chart, int scale) {
        this.simulation = simulation;
        this.chart = chart;
        this.scale = scale;
        TrailMap trail = simulation.getTrail();
        this.renderer = new TrailRenderer(trail.getWidth(), trail.getHeight());
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

        // Smooth scaling, so each cell blends into its neighbours instead of showing as a block
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        TrailMap trail = simulation.getTrail();
        g2.drawImage(renderer.render(trail), 0, 0,
                trail.getWidth() * scale, trail.getHeight() * scale, null);

        drawInfo(g2);
        chart.draw(g2, getWidth() - CHART_WIDTH - 10, getHeight() - CHART_HEIGHT - 10,
                CHART_WIDTH, CHART_HEIGHT);
    }

    private void drawInfo(Graphics2D g2) {
        SimulationStats stats = simulation.getStats();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        g2.setColor(new Color(255, 255, 255, 200));
        g2.drawString("tick " + stats.getTick()
                + "   agents " + stats.getAgentCount()
                + "   rule " + stats.getRuleName(), 10, 20);
        g2.drawString(String.format("network %.1f%% of map   %.0f%% of agents on it",
                stats.getNetworkFraction() * 100, stats.getAgentsOnNetworkFraction() * 100), 10, 38);
        g2.drawString("1 follow   2 avoid   3 wander   R scatter   Space pause",
                10, getHeight() - 12);
    }
}
