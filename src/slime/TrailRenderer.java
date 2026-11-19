package slime;

import java.awt.image.BufferedImage;
import java.util.stream.DoubleStream;

/**
 * Converts trail strength to colour: near-black for none, through amber, to
 * pale yellow for the strongest (real slime mould is bright yellow).
 */
public class TrailRenderer {

    private static final int BACKGROUND = 0x0B0C10;
    private static final int AMBER = 0xC77B12;
    private static final int GLOW = 0xFFF2A0;

    private static final double HALF_BRIGHTNESS = 20.0;   // trail strength drawn at half brightness

    private final BufferedImage image;

    public TrailRenderer(int width, int height) {
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    /**
     * Paints the trail map into the image and returns it. Each pixel depends only
     * on its own cell, so colours can be computed in parallel; toArray() keeps
     * the pixels in their original order.
     */
    public BufferedImage render(TrailMap trail, boolean parallel) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (trail.getWidth() != width || trail.getHeight() != height) {
            throw new IllegalArgumentException("Trail map size does not match the renderer");
        }
        DoubleStream values = parallel ? trail.values().parallel() : trail.values();
        int[] pixels = values.mapToInt(TrailRenderer::colourOf).toArray();

        // One block copy: Benchmark showed per-pixel setRGB took longer than the colour work itself.
        image.getRaster().setDataElements(0, 0, width, height, pixels);
        return image;
    }

    // Trail strength to colour. No side effects, so it is safe on many threads at once.
    private static int colourOf(double value) {
        double brightness = value / (value + HALF_BRIGHTNESS);   // always between 0 and 1
        if (brightness < 0.5) {
            return blend(BACKGROUND, AMBER, brightness * 2);
        }
        return blend(AMBER, GLOW, (brightness - 0.5) * 2);
    }

    // Mixes two 0xRRGGBB colours: t = 0 gives "from", t = 1 gives "to".
    private static int blend(int from, int to, double t) {
        int red = mix((from >> 16) & 0xFF, (to >> 16) & 0xFF, t);
        int green = mix((from >> 8) & 0xFF, (to >> 8) & 0xFF, t);
        int blue = mix(from & 0xFF, to & 0xFF, t);
        return (red << 16) | (green << 8) | blue;
    }

    private static int mix(int from, int to, double t) {
        return (int) Math.round(from + (to - from) * t);
    }
}
