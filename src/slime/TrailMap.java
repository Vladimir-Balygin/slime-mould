package slime;

import java.util.Arrays;

/**
 * The chemical trail that agents leave behind and follow.
 *
 * Stored as a flat array: cell (x, y) lives at index y * width + x.
 * The edges wrap around, so the world has no walls: an agent that
 * leaves on the right comes back on the left.
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

    /** Trail strength at any point. Points outside the map wrap around. */
    public double sample(double x, double y) {
        return cells[indexOf(x, y)];
    }

    /** Adds trail at a point. Points outside the map wrap around. */
    public void deposit(double x, double y, double amount) {
        cells[indexOf(x, y)] += amount;
    }

    /** Trail strength of one cell, used for drawing. */
    public double get(int x, int y) {
        return cells[y * width + x];
    }

    /** Removes all trail. */
    public void clear() {
        Arrays.fill(cells, 0.0);
    }

    /**
     * Spreads each cell's trail over its 3x3 neighbourhood, then fades
     * it by decayRate (0.1 removes 10% per tick).
     *
     * New values are written into a separate buffer. If we wrote back
     * into cells while still reading from it, a cell would average a
     * mix of old neighbours and neighbours that were already updated.
     */
    public void diffuseAndDecay(double decayRate) {
        double keep = 1.0 - decayRate;
        for (int y = 0; y < height; y++) {
            blurRow(y, keep);
        }
        double[] old = cells;   // swap: the buffer becomes the live map
        cells = buffer;
        buffer = old;
    }

    // Each call reads from cells and writes only row y of the buffer.
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

    // Converts any real coordinate into an array index, wrapping at the edges.
    private int indexOf(double x, double y) {
        int cellX = Math.floorMod((int) Math.floor(x), width);
        int cellY = Math.floorMod((int) Math.floor(y), height);
        return cellY * width + cellX;
    }
}
