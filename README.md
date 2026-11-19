# Slime Mould Networks

This is my COMP2000 project: a simulation of the slime mould *Physarum polycephalum*. A slime mould has only one cell and no brain, but can still build networks to transport food.

In my simulation, 15,000 agents move on a map. Each agent senses the trail ahead, turns towards the strongest trail, moves one step and leaves more trail behind. The trail spreads and fades a little every tick. No agent knows about the network, yet after a few seconds a network appears from random noise and then becomes simpler over time.

To build the project, run `javac -d out src/slime/*.java`. On Windows, use `src\slime\*.java` instead. To start the simulation, run `java -cp out slime.Main`. To time the execution modes without a window, run `java -cp out slime.Benchmark`.

Keys 1, 2 and 3 select the follow, avoid and wander rules, and key 4 selects swirl. J, D and M add jitter, drift and momentum on top of the current rule, and each key can be pressed many times. W turns the weather on and off. P switches between the parallel, unsafe parallel and sequential modes. R scatters the agents and clears the trail, and Space pauses the simulation.

The code uses four design patterns: Strategy for the steering rules, Observer for the window, the chart and the console, Decorator for jitter, drift and momentum, and Iterator for the weather. The simulation also uses lambdas, streams, parallel streams and a custom collector.

The agent model is based on a study of Physarum transport networks. The sunlight idea comes from another study, in which light kept a real slime mould away from some areas while the mould grew a network similar to the Tokyo rail system.

**AI Acknowledgment**

AI tools were used responsibly during this project, in accordance with Macquarie University's Responsible and Ethical Use of Artificial Intelligence Policy. The tools were used for research, for learning the course concepts and for testing.