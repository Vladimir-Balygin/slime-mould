package slime;

import java.awt.image.BufferedImage;

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
    private final int[] pixels;

    public TrailRenderer(int width, int height) {
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        this.pixels = new int[width * height];
    }

    /** Paints the whole trail map into the image and returns it. */
    public BufferedImage render(TrailMap trail) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (trail.getWidth() != width || trail.getHeight() != height) {
            throw new IllegalArgumentException("Trail map size does not match the renderer");
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double value = trail.get(x, y);
                double brightness = value / (value + HALF_BRIGHTNESS);   // always between 0 and 1
                pixels[y * width + x] = colourFor(brightness);
            }
        }
        image.setRGB(0, 0, width, height, pixels, 0, width);
        return image;
    }

    private static int colourFor(double brightness) {
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
