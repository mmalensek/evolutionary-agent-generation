import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;

public class LevelEvolution {

    // WORLD GENERATION
    public static int[] generateWorldGenome(int seed, int size) {
        Random rand = new Random(seed);
        int[] genome = new int[size];
        for (int i = 0; i < size; i++) {
            double p = rand.nextDouble();
            if (p < 0.6)
                genome[i] = 0; // empty
            else if (p < 0.8)
                genome[i] = 1; // bush
            else
                genome[i] = 2; // bird
        }
        return genome;
    }

    // CROSSOVER
    public static int[] crossover(int[] parent1, int[] parent2, Random rand) {
        int length = parent1.length;
        int[] child = new int[length];
        for (int i = 0; i < length; i++) {
            if (rand.nextBoolean())
                child[i] = parent1[i];
            else
                child[i] = parent2[i];
        }
        return child;
    }

    // MUTATION
    public static void mutate(int[] genome, double mutationRate, Random rand) {
        for (int i = 0; i < genome.length; i++) {
            if (rand.nextDouble() < mutationRate) {
                int oldVal = genome[i];
                int newVal;
                do {
                    newVal = rand.nextInt(3);
                } while (newVal == oldVal);
                genome[i] = newVal;
            }
        }
    }

    // FITNESS FUNCTION
    public static double evaluateWorld(int[] world) {
        double score = 100.0;

        // penalize impossible transitions
        for (int i = 0; i + 1 < world.length; i++) {
            if ((world[i] == 1 && world[i + 1] == 2) ||
                    (world[i] == 2 && world[i + 1] == 1)) {
                score -= 40;
            }
        }

        // reward balanced obstacle ratio
        // ideal ratio set to 30%
        int obstacles = 0;
        for (int w : world)
            if (w != 0)
                obstacles++;

        double ratio = (double) obstacles / world.length;
        double ideal = 0.3;
        double diff = Math.abs(ideal - ratio);
        score -= diff * 100;

        // penalize long empty regions
        int emptyRun = 0, maxEmpty = 0;
        for (int w : world) {
            if (w == 0)
                emptyRun++;
            else {
                maxEmpty = Math.max(maxEmpty, emptyRun);
                emptyRun = 0;
            }
        }
        maxEmpty = Math.max(maxEmpty, emptyRun);
        score -= maxEmpty * 3;

        // reward diversity of obstacles
        boolean hasBush = false, hasBird = false;
        for (int w : world) {
            if (w == 1)
                hasBush = true;
            if (w == 2)
                hasBird = true;
        }
        if (hasBush && hasBird)
            score += 20;

        // reward "interesting" obstacle patterns
        // in this case just [empty, bush/bird, empty]
        for (int i = 0; i + 2 < world.length; i++) {
            if (world[i] == 0 && world[i + 1] != 0 && world[i + 2] == 0)
                score += 5;
        }

        return score;
        // return Math.max(0, score);
    }

    // TOURNAMENT SELECTION
    public static int tournamentSelect(double[] fitness, int tournamentSize, Random rand) {
        int bestIndex = rand.nextInt(fitness.length);
        double bestFit = fitness[bestIndex];
        for (int i = 1; i < tournamentSize; i++) {
            int idx = rand.nextInt(fitness.length);
            if (fitness[idx] > bestFit) {
                bestFit = fitness[idx];
                bestIndex = idx;
            }
        }
        return bestIndex;
    }

    // EVOLUTION LOOP
    public static void evolveWorlds(int[][] population, int generations, double mutationRate, Random rand) {
        int popSize = population.length;
        int genomeLen = population[0].length;

        double[] avg = new double[generations];
        double[] best = new double[generations];
        double[] worst = new double[generations];
        int[][] bestPerGen = new int[generations][genomeLen];

        double lastBest = -1;
        int stagnation = 0;

        for (int gen = 0; gen < generations; gen++) {

            // evaluate current population
            double[] fitness = new double[popSize];
            for (int i = 0; i < popSize; i++)
                fitness[i] = evaluateWorld(population[i]);

            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            double sum = 0;
            int count = fitness.length;

            if (count == 0) {
                min = 0;
                max = 0;
            } else {
                for (double value : fitness) {
                    if (value < min) {
                        min = value;
                    }
                    if (value > max) {
                        max = value;
                    }
                    sum += value;
                }
            }

            double avgVal = (count > 0) ? (sum / count) : 0;

            avg[gen] = avgVal;
            best[gen] = max;
            worst[gen] = min;

            // track stagnation
            if (Math.abs(max - lastBest) < 0.01)
                stagnation++;
            else
                stagnation = 0;
            lastBest = max;

            // dynamic mutation
            double dynamicMutation = mutationRate * (1 + stagnation * 0.05);
            dynamicMutation = Math.min(0.15, dynamicMutation); // Cap at 15%

            // sort population by fitness (descending)
            Integer[] idx = new Integer[popSize];
            for (int i = 0; i < popSize; i++)
                idx[i] = i;
            Arrays.sort(idx, (a, b) -> Double.compare(fitness[b], fitness[a]));

            // store best world for this generation
            bestPerGen[gen] = Arrays.copyOf(population[idx[0]], genomeLen);

            System.out.printf("Gen %3d | Best: %.2f | Avg: %.2f | Worst: %.2f | Mutation: %.3f%n",
                    gen, max, avgVal, min, dynamicMutation);

            // create a new population
            int[][] newPop = new int[popSize][genomeLen];
            int eliteCount = Math.max(2, popSize / 10);

            // keep elites
            for (int i = 0; i < eliteCount; i++)
                newPop[i] = Arrays.copyOf(population[idx[i]], genomeLen);

            // breed remaining using tournament selection
            for (int i = eliteCount; i < popSize; i++) {
                int p1 = tournamentSelect(fitness, 5, rand);
                int p2;
                do {
                    p2 = tournamentSelect(fitness, 5, rand);
                } while (p1 == p2);

                int[] child = crossover(population[p1], population[p2], rand);
                mutate(child, dynamicMutation, rand);
                newPop[i] = child;
            }

            // update population for the next generation
            population = newPop;

            // in the and also do the plotting
            if (gen == generations - 1) {
                plotEvolutionGraph(avg, best, worst);
                visualizeBestWorlds(bestPerGen);
            }
        }
    }

    // PLOT EVOLUTION RESULTS
    public static void plotEvolutionGraph(double[] avg, double[] best, double[] worst) {
        JFrame frame = new JFrame("Level Evolution Progress");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 400);

        JPanel panel = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int pad = 60;
                int w = getWidth() - 2 * pad;
                int h = getHeight() - 2 * pad;
                int gens = avg.length;

                double maxVal = Arrays.stream(best).max().getAsDouble();
                double minVal = Arrays.stream(worst).min().getAsDouble();
                if (maxVal == minVal)
                    maxVal = minVal + 1;

                // Draw axes
                g2.setColor(Color.BLACK);
                g2.drawLine(pad, pad + h, pad + w, pad + h);
                g2.drawLine(pad, pad, pad, pad + h);
                g2.drawString("Generations", pad + w / 2 - 30, pad + h + 40);

                // Y-axis label
                Graphics2D g2Rotated = (Graphics2D) g2.create();
                g2Rotated.rotate(-Math.PI / 2);
                g2Rotated.drawString("Fitness", -(pad + h / 2 + 20), 20);
                g2Rotated.dispose();

                // Y-axis tick marks and labels
                int numYTicks = 5;
                for (int i = 0; i <= numYTicks; i++) {
                    double val = minVal + (maxVal - minVal) * i / numYTicks;
                    int yPos = pad + h - (int) ((val - minVal) / (maxVal - minVal) * h);
                    g2.drawLine(pad - 5, yPos, pad, yPos);
                    g2.drawString(String.format("%.0f", val), pad - 45, yPos + 5);
                }

                // X-axis tick marks and labels
                int numXTicks = Math.min(10, gens - 1);
                for (int i = 0; i <= numXTicks; i++) {
                    int genNum = i * (gens - 1) / numXTicks;
                    int xPos = pad + (int) (genNum * w / (double) (gens - 1));
                    g2.drawLine(xPos, pad + h, xPos, pad + h + 5);
                    g2.drawString(String.valueOf(genNum), xPos - 10, pad + h + 20);
                }

                plotLine(g2, avg, Color.BLUE, pad, w, pad, h, maxVal, minVal);
                plotLine(g2, best, Color.GREEN, pad, w, pad, h, maxVal, minVal);
                plotLine(g2, worst, Color.RED, pad, w, pad, h, maxVal, minVal);

                g2.setColor(Color.BLACK);
                g2.drawString("Avg (Blue)  Best (Green)  Worst (Red)", pad + 20, pad - 10);
            }

            void plotLine(Graphics2D g2, double[] data, Color color, int px, int w, int py, int h, double maxVal,
                    double minVal) {
                g2.setColor(color);
                for (int i = 0; i < data.length - 1; i++) {
                    double x1 = px + i * (w / (double) (data.length - 1));
                    double y1 = py + h - ((data[i] - minVal) / (maxVal - minVal) * h);
                    double x2 = px + (i + 1) * (w / (double) (data.length - 1));
                    double y2 = py + h - ((data[i + 1] - minVal) / (maxVal - minVal) * h);
                    g2.draw(new Line2D.Double(x1, y1, x2, y2));
                }
            }
        };

        frame.add(panel);
        frame.setVisible(true);
    }

    // VISUALIZE WORLD EVOLUTION
    public static void visualizeBestWorlds(int[][] bestWorlds) {
        JFrame frame = new JFrame("Best Levels Over Generations");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 600);

        JPanel panel = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int gens = bestWorlds.length;
                int len = bestWorlds[0].length;

                int cellW = Math.max(2, (getWidth() - 100) / len);
                int cellH = Math.max(4, (getHeight() - 80) / gens);

                for (int gen = 0; gen < gens; gen++) {
                    for (int i = 0; i < len; i++) {
                        int val = bestWorlds[gen][i];
                        switch (val) {
                            case 0 -> g.setColor(Color.WHITE);
                            case 1 -> g.setColor(new Color(40, 200, 60));
                            case 2 -> g.setColor(new Color(90, 90, 255));
                        }
                        g.fillRect(60 + i * cellW, 30 + gen * cellH, cellW, cellH);
                    }
                    if (gen % Math.max(1, gens / 20) == 0) {
                        g.setColor(Color.BLACK);
                        g.drawString("Gen " + gen, 10, 30 + gen * cellH + cellH / 2);
                    }
                }

                g.setColor(Color.BLACK);
                g.drawString("Legend: White=Empty  Green=Bush  Blue=Bird", 60, getHeight() - 20);
            }
        };

        frame.add(new JScrollPane(panel));
        frame.setVisible(true);
    }

    // MAIN
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("World size: ");
        int size = sc.nextInt();
        System.out.print("Population size: ");
        int pop = sc.nextInt();
        System.out.print("Generations: ");
        int gens = sc.nextInt();
        System.out.print("Seed: ");
        int seed = sc.nextInt();

        Random rand = new Random(seed);
        double mutationRate = 0.05;

        int[][] population = new int[pop][size];
        for (int i = 0; i < pop; i++)
            population[i] = generateWorldGenome(seed + i, size);

        evolveWorlds(population, gens, mutationRate, rand);

        sc.close();
    }
}