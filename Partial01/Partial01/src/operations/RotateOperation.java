package operations;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Set;

/**
 * RotateOperation.java
 * --------------------
 * Rotates a rectangular region inside the image by 90°, 180°, or 270°
 * (clockwise).  Pixels outside the region are left untouched.
 * Blank pixels introduced by 90°/270° rotations (when the region is not
 * square) are filled with black (0,0,0), as shown in the slides.
 *
 * Weight: 20% of the partial grade.
 *
 * Multimedia & Computer Graphics – First Partial
 * Universidad Panamericana | 2026
 */
public class RotateOperation extends ImageOperation {

    /** Allowed rotation angles in degrees (clockwise). */
    private static final Set<Integer> VALID_ANGLES = Set.of(90, 180, 270);

    private final int x1, y1, x2, y2;
    private final int degrees;

    /**
     * @param x1      x-coordinate of the top-left corner of the region
     * @param y1      y-coordinate of the top-left corner of the region
     * @param x2      x-coordinate of the bottom-right corner of the region
     * @param y2      y-coordinate of the bottom-right corner of the region
     * @param degrees clockwise rotation angle: 90, 180, or 270
     * @throws IllegalArgumentException if degrees is not 90, 180, or 270
     */
    public RotateOperation(int x1, int y1, int x2, int y2, int degrees) {
        if (!VALID_ANGLES.contains(degrees)) {
            throw new IllegalArgumentException(
                "degrees must be 90, 180, or 270. Got: " + degrees);
        }
        this.x1 = Math.min(x1, x2);
        this.y1 = Math.min(y1, y2);
        this.x2 = Math.max(x1, x2);
        this.y2 = Math.max(y1, y2);
        this.degrees = degrees;
    }

    /**
     * Returns a full copy of the image with the selected region rotated.
     * Blank pixels are filled with black.
     *
     * @param image source image
     * @return new image with the rotated region pasted back
     */
    @Override
    public BufferedImage apply(BufferedImage image) {
        BufferedImage result = deepCopy(image);

        int imgW = result.getWidth();
        int imgH = result.getHeight();

        // Clamp region to image boundaries
        int rx1 = Math.max(0, x1);
        int ry1 = Math.max(0, y1);
        int rx2 = Math.min(imgW, x2);
        int ry2 = Math.min(imgH, y2);

        int rW = rx2 - rx1;   // region width
        int rH = ry2 - ry1;   // region height

        if (rW <= 0 || rH <= 0) return result; // nothing to rotate

        // ── Extract the region pixels ──────────────────────────────────
        int[][] pixels = new int[rH][rW];
        for (int y = 0; y < rH; y++)
            for (int x = 0; x < rW; x++)
                pixels[y][x] = result.getRGB(rx1 + x, ry1 + y);

        // ── Rotate the pixel grid ──────────────────────────────────────
        int[][] rotated = rotatePixels(pixels, rW, rH, degrees);
        int newW = (degrees == 180) ? rW : rH;  // dimensions after rotation
        int newH = (degrees == 180) ? rH : rW;

        // ── Clear the original region to black ─────────────────────────
        for (int y = ry1; y < ry2; y++)
            for (int x = rx1; x < rx2; x++)
                result.setRGB(x, y, 0xFF000000); // opaque black

        // ── Paste rotated pixels, centred in the original region ───────
        int pasteX = rx1 + (rW - newW) / 2;
        int pasteY = ry1 + (rH - newH) / 2;

        for (int y = 0; y < newH; y++) {
            for (int x = 0; x < newW; x++) {
                int destX = pasteX + x;
                int destY = pasteY + y;
                if (destX >= rx1 && destX < rx2 && destY >= ry1 && destY < ry2) {
                    result.setRGB(destX, destY, rotated[y][x]);
                }
            }
        }
        return result;
    }

    // ── Private helpers ───────────────────────────────────────────────────

    /**
     * Rotates a 2-D pixel array clockwise by the given angle.
     *
     * @param src     source pixel grid [rows][cols]
     * @param srcW    number of columns
     * @param srcH    number of rows
     * @param degrees 90, 180, or 270
     * @return rotated pixel grid
     */
    private int[][] rotatePixels(int[][] src, int srcW, int srcH, int degrees) {
        return switch (degrees) {
            case 90  -> rotate90(src, srcW, srcH);
            case 180 -> rotate180(src, srcW, srcH);
            case 270 -> rotate270(src, srcW, srcH);
            default  -> src; // unreachable – validated in constructor
        };
    }

    /** Clockwise 90°: (x,y) → (srcH-1-y, x) in new grid of size srcH×srcW */
    private int[][] rotate90(int[][] src, int srcW, int srcH) {
        int[][] dst = new int[srcW][srcH]; // new dimensions: srcH rows, srcW cols → transposed
        for (int y = 0; y < srcH; y++)
            for (int x = 0; x < srcW; x++)
                dst[x][srcH - 1 - y] = src[y][x];
        return dst;
    }

    /** 180°: (x,y) → (srcW-1-x, srcH-1-y) */
    private int[][] rotate180(int[][] src, int srcW, int srcH) {
        int[][] dst = new int[srcH][srcW];
        for (int y = 0; y < srcH; y++)
            for (int x = 0; x < srcW; x++)
                dst[srcH - 1 - y][srcW - 1 - x] = src[y][x];
        return dst;
    }

    /** Clockwise 270° (= counter-clockwise 90°): (x,y) → (y, srcW-1-x) */
    private int[][] rotate270(int[][] src, int srcW, int srcH) {
        int[][] dst = new int[srcW][srcH];
        for (int y = 0; y < srcH; y++)
            for (int x = 0; x < srcW; x++)
                dst[srcW - 1 - x][y] = src[y][x];
        return dst;
    }

    /** Creates an independent copy of a BufferedImage. */
    private static BufferedImage deepCopy(BufferedImage src) {
        BufferedImage copy = new BufferedImage(
            src.getWidth(), src.getHeight(), src.getType());
        Graphics2D g = copy.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return copy;
    }

    @Override
    public String toString() {
        return String.format(
            "RotateOperation  (%d,%d) → (%d,%d)  %d°", x1, y1, x2, y2, degrees);
    }
}