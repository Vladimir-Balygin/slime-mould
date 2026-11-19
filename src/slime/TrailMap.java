package slime;

import java.util.Arrays;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

/**
 * The chemical trail that agents deposit and follow. Stored as a flat array,
 * with cell (x, y) at index y * width + x. Edges wrap, so the world has no walls.
 */
public class TrailMap {

    private final int width;
    private final int height;
    private double[] cells;    // the live trail values
    private double[] buffer;   // scratch space used by diffuseAndDecay

    public TrailMap(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "Trail map size must be positive, got " + width + "x" + height);
        }
        this.width = width;
        this.height = height;
        this.cells = new double[width * height];
        this.buffer = new double[width * height];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /** Returns the trail strength at a point; points outside the map wrap around. */
    public double sample(double x, double y) {
        return cells[indexOf(x, y)];
    }

    /** Adds trail at a point; points outside the map wrap around. */
    public void deposit(double x, double y, double amount) {
        cells[indexOf(x, y)] += amount;
    }

    /** Adds trail for a whole grid at once: cell i gets agentsPerCell[i] * amountEach. */
    public void deposit(int[] agentsPerCell, double amountEach) {
        if (agentsPerCell.length != cells.length) {
            throw new IllegalArgumentException("Expected " + cells.length
                    + " cells, got " + agentsPerCell.length);
        }
        for (int i = 0; i < cells.length; i++) {
            cells[i] += agentsPerCell[i] * amountEach;
        }
    }

    /**
     * Removes trail under the sun: cells within radius lose the share given by
     * strength at the centre, falling to none at the edge. Wraps at the edges.
     */
    public void bleach(double centreX, double centreY, double radius, double strength) {
        if (strength < 0 || strength > 1) {
            throw new IllegalArgumentException("Strength must be between 0 and 1, got " + strength);
        }
        int reach = (int) Math.ceil(radius);
        for (int dy = -reach; dy <= reach; dy++) {
            for (int dx = -reach; dx <= reach; dx++) {
                double distance = Math.sqrt(dx * dx + dy * dy);
                if (distance < radius) {
                    cells[indexOf(centreX + dx, centreY + dy)] *= 1 - strength * (1 - distance / radius);
                }
            }
        }
    }

    /** Returns the trail strength of one cell. */
    public double get(int x, int y) {
        return cells[y * width + x];
    }

    /** Removes all trail. */
    public void clear() {
        Arrays.fill(cells, 0.0);
    }

    public int getCellCount() {
        return cells.length;
    }

    /**
     * Returns the total trail on the map. A plain loop, not values().sum(): it is
     * called twice per step, and the stream version measured several times slower.
     */
    public double total() {
        double total = 0;
        for (double value : cells) {
            total += value;
        }
        return total;
    }

    /** Returns every cell's value as a stream, so callers can read the map but not change it. */
    public DoubleStream values() {
        return Arrays.stream(cells);
    }

    /**
     * Spreads each cell over its 3x3 neighbourhood, then fades it by decayRate.
     *
     * Results go to a separate buffer, so every average uses old values only. In
     * parallel, rows are split between threads: each task reads cells, which no
     * task changes, and writes only its own buffer row. The swap happens after
     * forEach returns, when every row task has finished.
     */
    public void diffuseAndDecay(double decayRate, boolean parallel) {
        double keep = 1.0 - decayRate;
        IntStream rows = IntStream.range(0, height);
        if (parallel) {
            rows = rows.parallel();
        }
        rows.forEach(y -> blurRow(y, keep));
        double[] old = cells;   // swap: the buffer becomes the live map
        cells = buffer;
        buffer = old;
    }

    // Blurs row y: reads from cells and writes only row y of the buffer.
    private void blurRow(int y, double keep) {
        int up = (y == 0) ? height - 1 : y - 1;
        int down = (y == height - 1) ? 0 : y + 1;
        for (int x = 0; x < width; x++) {
            int left = (x == 0) ? width - 1 : x - 1;
            int right = (x == width - 1) ? 0 : x + 1;
            double sum = cells[up * width + left] + cells[up * width + x] + cells[up * width + right]
                    + cells[y * width + left] + cells[y * width + x] + cells[y * width + right]
                    + cells[down * width + left] + cells[down * width + x] + cells[down * width + right];
            buffer[y * width + x] = (sum / 9.0) * keep;
        }
    }

    /** Returns the index of the cell containing a point (edges wrap). Public for DepositCollector. */
    public int indexOf(double x, double y) {
        int cellX = Math.floorMod((int) Math.floor(x), width);
        int cellY = Math.floorMod((int) Math.floor(y), height);
        return cellY * width + cellX;
    }
}
