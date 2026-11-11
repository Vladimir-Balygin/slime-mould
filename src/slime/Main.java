package slime;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Entry point: builds the simulation and the window, then starts the clock. */
public class Main {

    public static void main(String[] args) {
        // Swing is not thread-safe: all window work must happen on Swing's
        // own thread, the Event Dispatch Thread. invokeLater queues it there.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                createAndShowWindow();
            }
        });
    }

    private static void createAndShowWindow() {
        Simulation simulation = new Simulation(
                Settings.WIDTH, Settings.HEIGHT, Settings.AGENT_COUNT, Settings.SEED);
        SimulationPanel panel = new SimulationPanel(simulation, Settings.SCALE);

        JFrame frame = new JFrame("Slime Mould Networks");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);   // centre on screen
        frame.setVisible(true);

        // The Swing Timer also fires on the Event Dispatch Thread, so step()
        // and painting take turns and never run at the same time.
        // Both listeners in this file are anonymous inner classes (Week 4);
        // Phase 2 rewrites them as lambdas (Week 9).
        Timer timer = new Timer(Settings.FRAME_DELAY_MS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                simulation.step();
                panel.repaint();
            }
        });
        timer.start();
    }
}
