package slime;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createAndShowWindow);
    }

    private static void createAndShowWindow() {
        Simulation simulation = new Simulation(
                Settings.WIDTH, Settings.HEIGHT, Settings.AGENT_COUNT, Settings.SEED);
        NetworkChart chart = new NetworkChart(simulation, Settings.CHART_LENGTH);
        SimulationPanel panel = new SimulationPanel(simulation, chart, Settings.SCALE);

        // Where the weather comes from: an endless forecast, from the same seed
        Forecast forecast = new Forecast(Weather.start(Settings.SEED));

        // Observer: the simulation calls these back after every change
        simulation.registerObserver(chart);
        simulation.registerObserver(panel);
        simulation.registerObserver(new ConsoleReporter(simulation, Settings.REPORT_INTERVAL));

        // The Swing Timer fires on the Event Dispatch Thread, so step() and
        // painting take turns and never run at the same time. The Timer is
        // itself a subject: it notifies its ActionListener every 33 ms.
        // It only advances the simulation; the panel repaints because it
        // is an observer, not because the Timer tells it to.
        //
        // Threading versus parallelisation: the Event Dispatch Thread keeps
        // the window responsive by taking turns between key presses, ticks
        // and painting. Inside step(), parallel streams split one tick's
        // work across the cores. step() returns only when every parallel
        // task has finished, so painting never sees a half-finished tick.
        Timer timer = new Timer(Settings.FRAME_DELAY_MS, event -> simulation.step());

        // Keyboard controls. A number key swaps the strategy while the
        // simulation runs; the agents themselves do not change. J, D and M
        // wrap the current strategy in one more decorator, so they can be
        // stacked in any order; a number key goes back to a plain rule.
        // W turns the weather on and off.
        // KeyListener has three abstract methods (keyTyped, keyPressed,
        // keyReleased), so it is not a functional interface and cannot be
        // a lambda. KeyAdapter is a class with empty versions of all three,
        // so an anonymous inner class overrides only the one it needs.
        panel.setFocusable(true);
        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent event) {
                switch (Character.toLowerCase(event.getKeyChar())) {
                    case '1':
                        simulation.setSteeringRule("follow trail", SteeringRules.FOLLOW);
                        break;
                    case '2':
                        simulation.setSteeringRule("avoid trail", SteeringRules.AVOID);
                        break;
                    case '3':
                        simulation.setSteeringRule("wander", SteeringRules.WANDER);
                        break;
                    case '4':
                        simulation.setSteeringRule("follow trail", SteeringRules.SWIRL);
                        break;
                    case 'j':
                        simulation.decorateSteeringRule(rule -> rule.withJitter(Settings.JITTER_ANGLE));
                        break;
                    case 'd':
                        simulation.decorateSteeringRule(rule -> rule.withDrift(Settings.DRIFT_ANGLE));
                        break;
                    case 'm':
                        simulation.decorateSteeringRule(rule -> rule.withMomentum(Settings.MOMENTUM_CHANCE));
                        break;
                    case 'w':   // each start asks the forecast for a new iterator, from its first weather
                        if (simulation.isWeatherOn()) {
                            simulation.stopWeather();
                        } else {
                            simulation.startWeather(forecast);
                        }
                        break;
                    case 'r':
                        simulation.scatter();
                        break;
                    case 'p':   // sequential -> parallel -> parallel, unsafe deposit -> ...
                        simulation.setExecutionMode(simulation.getExecutionMode().next());
                        break;
                    case ' ':
                        if (timer.isRunning()) {
                            timer.stop();
                        } else {
                            timer.start();
                        }
                        break;
                    default:
                        break;   // any other key does nothing
                }
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
