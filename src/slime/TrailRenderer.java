package slime;

import java.awt.image.BufferedImage;
import java.util.stream.DoubleStream;

/**
 * Turns trail strength into colour: near-black where there is no trail,
 * through amber, to pale yellow where it is strongest (real slime
 * mould is bright yellow).
 */
public class TrailRenderer {

    private static final int BACKGROUND = 0x0B0C10;
    private static final int AMBER = 0xC77B12;
    private static final int GLOW = 0xFFF2A0;

    // The trail strength that is drawn at half brightness
    private static final double HALF_BRIGHTNESS = 20.0;

    private final BufferedImage image;

    public TrailRenderer(int width, int height) {
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    /**
     * Paints the whole trail map into the image and returns it.
     *
     * A pipeline: every cell's trail value flows in, is mapped to a colour,
     * and the colours are collected into an array of pixels. Each pixel
     * depends only on its own cell, and colourOf changes nothing outside
     * itself, so in parallel the cells can be coloured on many threads at
     * once. toArray() puts the pixels back in their original order when
     * the chunks are joined.
     */
    public BufferedImage render(TrailMap trail, boolean parallel) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (trail.getWidth() != width || trail.getHeight() != height) {
            throw new IllegalArgumentException("Trail map size does not match the renderer");
        }
        DoubleStream values = parallel ? trail.values().parallel() : trail.values();
        int[] pixels = values.mapToInt(TrailRenderer::colourOf).toArray();

        // Writing into the image happens once, on the calling thread.
        image.setRGB(0, 0, width, height, pixels, 0, width);
        return image;
    }

    // Trail strength to colour. Same value in, same colour out, nothing else changed.
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
