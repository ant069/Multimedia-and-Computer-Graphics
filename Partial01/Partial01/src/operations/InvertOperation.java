package operations;

import java.awt.image.BufferedImage;

/**
 * InvertOperation.java
 * --------------------
 * Inverts (negates) every pixel inside a rectangular region.
 * Pixels outside the region are left untouched.
 *
 * Inversion formula per channel:  new = 255 - old
 *
 * Weight: 30% of the partial grade.
 *
 * Multimedia & Computer Graphics – First Partial
 * Universidad Panamericana | 2026
 */
public class InvertOperation extends ImageOperation {

    private final int x1, y1, x2, y2;

    /**
     * @param x1 x-coordinate of the top-left corner of the region
     * @param y1 y-coordinate of the top-left corner of the region
     * @param x2 x-coordinate of the bottom-right corner of the region
     * @param y2 y-coordinate of the bottom-right corner of the region
     */
    public InvertOperation(int x1, int y1, int x2, int y2) {
        this.x1 = Math.min(x1, x2);
        this.y1 = Math.min(y1, y2);
        this.x2 = Math.max(x1, x2);
        this.y2 = Math.max(y1, y2);
    }

    /**
     * Returns a full copy of the image with the selected region inverted.
     *
     * @param image source image
     * @return new image with the inverted region
     */
    @Override
    public BufferedImage apply(BufferedImage image) {
        // Deep-copy the entire image so the original is never mutated
        BufferedImage result = deepCopy(image);

        int imgW = result.getWidth();
        int imgH = result.getHeight();

        // Clamp region to image boundaries
        int rx1 = Math.max(0, x1);
        int ry1 = Math.max(0, y1);
        int rx2 = Math.min(imgW, x2);
        int ry2 = Math.min(imgH, y2);

        for (int y = ry1; y < ry2; y++) {
            for (int x = rx1; x < rx2; x++) {
                int argb = result.getRGB(x, y);

                int a = (argb >> 24) & 0xFF;          // keep alpha
                int r = 255 - ((argb >> 16) & 0xFF);  // invert red
                int g = 255 - ((argb >>  8) & 0xFF);  // invert green
                int b = 255 - ( argb        & 0xFF);  // invert blue

                result.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return result;
    }

    // ── Helper ────────────────────────────────────────────────────────────

    /**
     * Creates an independent pixel-by-pixel copy of a BufferedImage.
     */
    private static BufferedImage deepCopy(BufferedImage src) {
        BufferedImage copy = new BufferedImage(
            src.getWidth(), src.getHeight(), src.getType());
        copy.getGraphics().drawImage(src, 0, 0, null);
        return copy;
    }

    @Override
    public String toString() {
        return String.format("InvertOperation  (%d,%d) → (%d,%d)", x1, y1, x2, y2);
    }
}