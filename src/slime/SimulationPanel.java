package slime;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/** The view: draws the current state of the simulation, scaled up. */
public class SimulationPanel extends JPanel {

    private final Simulation simulation;
    private final TrailRenderer renderer;
    private final int scale;

    public SimulationPanel(Simulation simulation, int scale) {
        this.simulation = simulation;
        this.scale = scale;
        TrailMap trail = simulation.getTrail();
        this.renderer = new TrailRenderer(trail.getWidth(), trail.getHeight());
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
    }

    private void drawInfo(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        g2.setColor(new Color(255, 255, 255, 190));
        g2.drawString("tick " + simulation.getTick()
                + "   agents " + simulation.getAgents().size(), 10, 20);
    }
}
