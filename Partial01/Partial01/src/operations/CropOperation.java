package operations;

import java.awt.image.BufferedImage;

/**
 * CropOperation.java
 * ------------------
 * Crops the image to the rectangle defined by two corner points.
 * The resulting image contains only the selected region.
 *
 * Weight: 30% of the partial grade.
 *
 * Multimedia & Computer Graphics – First Partial
 * Universidad Panamericana | 2026
 */
public class CropOperation extends ImageOperation {

    private final int x1, y1, x2, y2;

    /**
     * @param x1 x-coordinate of the first corner
     * @param y1 y-coordinate of the first corner
     * @param x2 x-coordinate of the opposite corner
     * @param y2 y-coordinate of the opposite corner
     */
    public CropOperation(int x1, int y1, int x2, int y2) {
        // Normalise so (x1,y1) is always top-left
        this.x1 = Math.min(x1, x2);
        this.y1 = Math.min(y1, y2);
        this.x2 = Math.max(x1, x2);
        this.y2 = Math.max(y1, y2);
    }

    /**
     * Returns a new BufferedImage containing only the cropped region.
     *
     * @param image source image
     * @return cropped image
     * @throws IllegalArgumentException if the crop box falls outside the image
     */
    @Override
    public BufferedImage apply(BufferedImage image) {
        int imgW = image.getWidth();
        int imgH = image.getHeight();

        // Clamp to image boundaries
        int cx1 = Math.max(0, x1);
        int cy1 = Math.max(0, y1);
        int cx2 = Math.min(imgW, x2);
        int cy2 = Math.min(imgH, y2);

        if (cx2 <= cx1 || cy2 <= cy1) {
            throw new IllegalArgumentException(
                "Crop region is outside the image boundaries.");
        }

        int width  = cx2 - cx1;
        int height = cy2 - cy1;

        // getSubimage shares the raster; copy to an independent image
        BufferedImage cropped = new BufferedImage(width, height, image.getType());
        cropped.getGraphics().drawImage(
            image.getSubimage(cx1, cy1, width, height), 0, 0, null);
        return cropped;
    }

    @Override
    public String toString() {
        return String.format("CropOperation  (%d,%d) → (%d,%d)", x1, y1, x2, y2);
    }
}
