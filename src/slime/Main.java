package slime;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Entry point: builds the simulation and the window, then starts the clock. */
public class Main {

    public static void main(String[] args) {
        // Swing is not thread-safe, so the window is built on the Event Dispatch Thread.
        SwingUtilities.invokeLater(Main::createAndShowWindow);
    }

    private static void createAndShowWindow() {
        Simulation simulation = new Simulation(
                Settings.WIDTH, Settings.HEIGHT, Settings.AGENT_COUNT, Settings.SEED);
        NetworkChart chart = new NetworkChart(simulation, Settings.CHART_LENGTH);
        SimulationPanel panel = new SimulationPanel(simulation, chart, Settings.SCALE);

        // Endless forecast from the same seed, so the weather repeats from run to run.
        Forecast forecast = new Forecast(Weather.start(Settings.SEED));

        // Observer pattern: the simulation notifies these after every change, through the interface only.
        simulation.registerObserver(chart);
        simulation.registerObserver(panel);
        simulation.registerObserver(new ConsoleReporter(simulation, Settings.REPORT_INTERVAL));

        // The Timer fires on the Event Dispatch Thread, so ticks, key presses and painting
        // never overlap. It only steps the simulation; the panel repaints as an observer.
        // Parallel streams inside step() finish before it returns, so painting never sees
        // a half-finished tick.
        Timer timer = new Timer(Settings.FRAME_DELAY_MS, event -> simulation.step());

        // Keys 1-4 swap the strategy at runtime; J, D and M stack decorators on it; W toggles weather.
        // KeyListener has three abstract methods, so it cannot be a lambda: an anonymous
        // KeyAdapter overrides only keyTyped.
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
                    case 'w':   // each start takes a new iterator, from the first weather
                        if (simulation.isWeatherOn()) {
                            simulation.stopWeather();
                        } else {
                            simulation.startWeather(forecast);
                        }
                        break;
                    case 'r':
                        simulation.scatter();
                        break;
                    case 'p':   // SEQUENTIAL -> PARALLEL -> PARALLEL_UNSAFE -> ...
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
                        break;
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
