import java.util.*;
import javax.swing.*;
import java.awt.*;

public class AgentEvolution {

    // manually or randomly generate a world
    public static int[] worldGenerate(Scanner sc) {

        System.out.println("Generate manually (1) or randomly (0): ");
        int manually = sc.nextInt();
        System.out.println("Set the size: ");
        int size = sc.nextInt();
        int seed = 0;

        if (manually == 0) {
            System.out.println("Set the seed:");
            seed = sc.nextInt();
        }

        Random rand = new Random(seed);
        int[] WORLD = new int[size];

        /*
         * OBSTACLE DEFINITION:
         * 0: nothing
         * 1: bush
         * 2: bird
         * 
         * for random generation, the probabilities are:
         * 0.5 - 0, 0.25 - 1, 0.25 - 2
         * we dont want obstacles everywhere
         */

        if (manually == 1) {
            System.out.println("Create the level: 0 - no obstacle, 1 - bush, 2 - bird");
            for (int i = 0; i < size; i++) {
                WORLD[i] = sc.nextInt();
            }
        } else {
            for (int i = 0; i < size; i++) {
                if (rand.nextInt(10) < 5) {
                    WORLD[i] = 0;
                } else if (rand.nextInt(10) < 5) {
                    WORLD[i] = 1;

                } else {
                    WORLD[i] = 2;
                }
            }
        }

        /*
         * helper print function to test
         * for (int i = 0; i < size; i++) {
         * System.out.print(WORLD[i] + " ");
         * }
         * System.out.println("");
         */

        return WORLD;
    }

    // checks if world finish is reachable
    // if not fixes it
    public static int[] worldFix(int[] world) {
        int[] fixedWorld = world;

        for (int i = 0; i + 1 < world.length; i++) {
            // checks if there is a bird directly after a bush
            // or a bush directly after a bird and removes it
            if (fixedWorld[i] == 1 && fixedWorld[i + 1] == 2 || fixedWorld[i] == 2 && fixedWorld[i + 1] == 1) {
                fixedWorld[i] = 0;
            }
        }

        return fixedWorld;
    }

    // generate agents for the first generation randomly
    public static int[] firstAgentGenerate(int seed, int maxMoves) {
        Random rand = new Random(seed);

        int[] MOVES = new int[maxMoves];

        /*
         * MOVEMENT DEFINITION
         * 0: no movement
         * 1: right
         * 2: down
         * 3: left
         * 4: up
         */

        for (int i = 0; i < maxMoves; i++) {
            MOVES[i] = rand.nextInt(5);
        }

        return MOVES;
    }

    public static int evaluation(int[] world, int[] agentMoves) {
        int position = 0;

        // height is always between 0 and 1
        int height = 0;

        for (int i = 0; i < agentMoves.length; i++) {

            // if an agent reaches the finish
            // it doesnt matter what is after
            if (position + 1 == world.length) {
                return position;
            }

            if (agentMoves[i] == 1) {
                if (world[position + 1] != 1 + height) {
                    // moving right
                    position++;
                }
            } else if (agentMoves[i] == 2) {
                if (height == 1 && world[position] != 1) {
                    // moving down
                    height = 0;
                }
            } else if (agentMoves[i] == 3) {
                if (position != 0 && world[position - 1] != 1 + height) {
                    // moving left
                    position--;
                }
            } else if (agentMoves[i] == 4) {
                if (height == 0 && world[position] != 2) {
                    // moving up
                    height = 1;
                }
            }
        }

        return position;
    }

    // perform crossover between two parent genomes
    // to produce a child genome
    public static int[] crossover(int[] parent1, int[] parent2, Random rand) {
        int length = parent1.length;
        int crossoverPoint = rand.nextInt(length);

        int[] child = new int[length];
        for (int i = 0; i < length; i++) {
            if (i < crossoverPoint) {
                child[i] = parent1[i];
            } else {
                child[i] = parent2[i];
            }
        }
        return child;
    }

    // mutate a genome with given mutation rate
    public static void mutate(int[] genome, double mutationRate, Random rand) {
        for (int i = 0; i < genome.length; i++) {
            if (rand.nextDouble() < mutationRate) {
                // mutate to a random move (0-4)
                genome[i] = rand.nextInt(5);
            }
        }
    }

    public static int[][] evolve(int[][] population, int[] world, int generations, double mutationRate, Random rand) {
        int populationSize = population.length;
        int genomeLength = population[0].length;

        int[] avgPerGen = new int[generations]; // store average position per generation
        int[] bestPerGen = new int[generations];
        int[] worstPerGen = new int[generations];
        int[][] bestDNAperGen = new int[generations][genomeLength]; // store best agent DNA per generation

        for (int gen = 0; gen < generations; gen++) {
            // evaluate current population
            int[] fitness = new int[populationSize];
            for (int i = 0; i < populationSize; i++) {
                fitness[i] = evaluation(world, population[i]);
            }

            // sort population by evaluation descending
            Integer[] indices = new Integer[populationSize];
            for (int i = 0; i < populationSize; i++)
                indices[i] = i;
            Arrays.sort(indices, (a, b) -> Integer.compare(fitness[b], fitness[a]));

            // create new population
            int[][] newPopulation = new int[populationSize][genomeLength];

            // elitism: copy top 10% unchanged
            int eliteCount = populationSize / 10;
            for (int i = 0; i < eliteCount; i++) {
                newPopulation[i] = population[indices[i]];
            }

            // create rest of population by crossover and mutation
            for (int i = eliteCount; i < populationSize; i++) {
                int parent1Index = indices[rand.nextInt(populationSize / 2)];
                int parent2Index = indices[rand.nextInt(populationSize / 2)];

                int[] child = crossover(population[parent1Index], population[parent2Index], rand);
                mutate(child, mutationRate, rand);
                newPopulation[i] = child;
            }

            population = newPopulation;

            // compute average
            int sum = 0;
            int min = Integer.MAX_VALUE;
            int max = 0;
            for (int i = 0; i < populationSize; i++) {
                sum += fitness[i];
                if (fitness[i] < min) {
                    min = fitness[i];
                }
                if (fitness[i] > max) {
                    max = fitness[i];
                }
            }
            avgPerGen[gen] = sum / populationSize;
            bestPerGen[gen] = max;
            worstPerGen[gen] = min;

            // store best agent DNA
            bestDNAperGen[gen] = Arrays.copyOf(population[indices[0]], genomeLength);

            // print best fitness and average of each generation
            System.out.println("Generation " + gen + " best position: " + fitness[indices[0]]
                    + " and average position: " + avgPerGen[gen]);

            // printAgentPath(world, bestDNAperGen[gen]);
            // System.out.println("Best DNA: " + Arrays.toString(bestDNAperGen[gen]));

            /*
             * PLOTTING A SIMPLE GENERATION GRAPH
             */
            if (gen + 1 == generations) {
                plotEvolutionGraph(avgPerGen, bestPerGen, worstPerGen);
                visualizeBestDNAEvolution(bestDNAperGen);
            }
        }

        // print average positions array after evolution
        System.out.println("Average positions per generation: " + Arrays.toString(avgPerGen));

        return population;
    }

    public static void printAgentPath(int[] world, int[] agentMoves) {
        int pos = 0;
        // int height = 0;
        for (int i = 0; i < world.length; i++) {
            if (pos == i)
                System.out.print("A");
            else if (world[i] == 0)
                System.out.print(".");
            else if (world[i] == 1)
                System.out.print("B");
            else if (world[i] == 2)
                System.out.print("^");
        }
        System.out.println();
    }

    public static void plotEvolutionGraph(int[] avgPerGen, int[] bestPerGen, int[] worstPerGen) {
        JFrame frame = new JFrame("Evolution Graph");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 400);

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                int padding = 80;
                int width = getWidth() - 2 * padding;
                int height = getHeight() - 2 * padding;

                int generations = avgPerGen.length;
                int maxValue = 0;
                for (int i = 0; i < generations; i++) {
                    if (avgPerGen[i] > maxValue)
                        maxValue = avgPerGen[i];
                    if (bestPerGen[i] > maxValue)
                        maxValue = bestPerGen[i];
                    if (worstPerGen[i] > maxValue)
                        maxValue = worstPerGen[i];
                }

                // draw axes
                g.drawLine(padding, padding + height, padding + width, padding + height); // x-axis
                g.drawLine(padding, padding, padding, padding + height); // y-axis

                // x-axis label
                g.drawString("Generation", padding + width / 2 - 30, padding + height + 40);

                // y-axis label - vertical using Graphics2D rotation
                Graphics2D g2 = (Graphics2D) g;
                g2.rotate(-Math.PI / 2);
                g2.drawString("Distance Reached", -padding - height / 2 - 60, padding - 60);
                g2.rotate(Math.PI / 2); // reset rotation

                // y-axis numbers
                for (int i = 0; i <= 5; i++) {
                    int yValue = maxValue * i / 5;
                    int y = padding + height - (yValue * height / maxValue);
                    g.drawString(Integer.toString(yValue), padding - 40, y + 5);
                    g.drawLine(padding - 5, y, padding + 5, y);
                }

                // x-axis numbers
                for (int i = 0; i < generations; i += Math.max(1, generations / 10)) {
                    int x = padding + (i * width) / (generations - 1);
                    g.drawString(Integer.toString(i), x - 10, padding + height + 20);
                    g.drawLine(x, padding + height - 5, x, padding + height + 5);
                }

                // plot average (blue)
                g.setColor(Color.BLUE);
                for (int i = 0; i < generations - 1; i++) {
                    int x1 = padding + (i * width) / (generations - 1);
                    int y1 = padding + height - (avgPerGen[i] * height / maxValue);
                    int x2 = padding + ((i + 1) * width) / (generations - 1);
                    int y2 = padding + height - (avgPerGen[i + 1] * height / maxValue);
                    g.drawLine(x1, y1, x2, y2);
                }

                // plot best (green)
                g.setColor(Color.GREEN);
                for (int i = 0; i < generations - 1; i++) {
                    int x1 = padding + (i * width) / (generations - 1);
                    int y1 = padding + height - (bestPerGen[i] * height / maxValue);
                    int x2 = padding + ((i + 1) * width) / (generations - 1);
                    int y2 = padding + height - (bestPerGen[i + 1] * height / maxValue);
                    g.drawLine(x1, y1, x2, y2);
                }

                // plot worst (red)
                g.setColor(Color.RED);
                for (int i = 0; i < generations - 1; i++) {
                    int x1 = padding + (i * width) / (generations - 1);
                    int y1 = padding + height - (worstPerGen[i] * height / maxValue);
                    int x2 = padding + ((i + 1) * width) / (generations - 1);
                    int y2 = padding + height - (worstPerGen[i + 1] * height / maxValue);
                    g.drawLine(x1, y1, x2, y2);
                }

                // optional legend
                g.setColor(Color.BLACK);
                g.drawString("Average (Blue)", padding + 10, padding);
                g.drawString("Best (Green)", padding + 110, padding);
                g.drawString("Worst (Red)", padding + 210, padding);
            }
        };

        frame.add(panel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // visualize differences between best DNA of current and previous generation
    public static void visualizeBestDNAEvolution(int[][] bestDNAperGen) {
        JFrame frame = new JFrame("Best DNA Evolution");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 700);

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                int generations = bestDNAperGen.length;
                int geneCount = bestDNAperGen[0].length;

                int paddingLeft = 80;
                int paddingTop = 40;

                int cellWidth = Math.max(2, (getWidth() - paddingLeft - 100) / geneCount);
                int cellHeight = Math.max(4, (getHeight() - paddingTop - 100) / generations);

                // background
                g.setColor(new Color(245, 245, 245));
                g.fillRect(0, 0, getWidth(), getHeight());

                // title
                g.setColor(Color.BLACK);
                g.setFont(new Font("SansSerif", Font.BOLD, 16));
                g.drawString("Best Genome Evolution (Green = same move, Red = changed)", paddingLeft, 25);

                // draw generations
                for (int gen = 0; gen < generations; gen++) {
                    // label the generation
                    g.setColor(Color.BLACK);
                    g.setFont(new Font("Monospaced", Font.PLAIN, 12));
                    if (gen % 5 == 0) {
                        g.drawString("Gen " + gen, 10, paddingTop + gen * cellHeight + cellHeight - 3);
                    }
                    // draw each gene cell
                    for (int i = 0; i < geneCount; i++) {
                        Color color = Color.GRAY; // first generation base color
                        if (gen > 0) {
                            if (bestDNAperGen[gen][i] == bestDNAperGen[gen - 1][i])
                                color = new Color(0, 180, 0); // green for same
                            else
                                color = new Color(220, 0, 0); // red for changed
                        }
                        g.setColor(color);
                        g.fillRect(paddingLeft + i * cellWidth, paddingTop + gen * cellHeight, cellWidth, cellHeight);
                    }
                }
            }
        };

        // optional scrolling if genome is very long
        JScrollPane scroll = new JScrollPane(panel);
        frame.add(scroll);
        frame.setVisible(true);
    }

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        int[] testWorld = worldGenerate(sc);
        testWorld = worldFix(testWorld);

        // maximum amount of moves for each agent is
        // 10-times the length of the level
        int maxMoves = testWorld.length * 10;

        // get the number of agents wanted
        // for the first generation
        System.out.println("Set the number of agents: ");

        int numberOfAgents = sc.nextInt();

        // set the seed for agent creation
        System.out.println("Set the seed: ");
        int seed = sc.nextInt();

        // create the first agents
        int[][] firstAgents = new int[numberOfAgents][maxMoves];
        for (int i = 0; i < numberOfAgents; i++) {
            firstAgents[i] = firstAgentGenerate(seed + i, maxMoves);
        }

        // evaluation
        int[] evaluations = new int[numberOfAgents];
        for (int i = 0; i < numberOfAgents; i++) {
            evaluations[i] = evaluation(testWorld, firstAgents[i]);
            // System.out.println(evaluations[i]);
        }

        System.out.println("Set the number of generations: ");
        int generations = sc.nextInt();

        double mutationRate = 0.05;

        int[][] finalPopulation = evolve(firstAgents, testWorld, generations, mutationRate, new Random(seed));

        // evaluate final population
        int[] finalEvaluations = new int[numberOfAgents];
        for (int i = 0; i < numberOfAgents; i++) {
            finalEvaluations[i] = evaluation(testWorld, finalPopulation[i]);
            // System.out.println("Agent " + i + " final position: " + finalEvaluations[i]);
        }

        sc.close();
    }
}
