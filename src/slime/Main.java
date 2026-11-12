package slime;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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

        // The Swing Timer also fires on the Event Dispatch Thread, so step()
        // and painting take turns and never run at the same time.
        Timer timer = new Timer(Settings.FRAME_DELAY_MS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                simulation.step();
                panel.repaint();
            }
        });

        // Keyboard controls. A number key swaps the strategy while the
        // simulation runs; the agents themselves do not change.
        panel.setFocusable(true);
        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent event) {
                switch (Character.toLowerCase(event.getKeyChar())) {
                    case '1':
                        simulation.setSteeringRule(new FollowTrail());
                        break;
                    case '2':
                        simulation.setSteeringRule(new AvoidTrail());
                        break;
                    case '3':
                        simulation.setSteeringRule(new Wander());
                        break;
                    case 'r':
                        simulation.scatter();
                        break;
                    case ' ':
                        if (timer.isRunning()) {
                            timer.stop();
                        } else {
                            timer.start();
                        }
                        break;
                    default:
                        return;   // any other key: nothing changed, nothing to redraw
                }
                panel.repaint();   // show the change at once, even while paused
            }
        });

        JFrame frame = new JFrame("Slime Mould Networks");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);   // centre on screen
        frame.setVisible(true);
        panel.requestFocusInWindow();        // so key presses reach the panel

        timer.start();
    }
}
