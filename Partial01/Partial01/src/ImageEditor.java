import operations.ImageOperation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImageEditor {

    private final BufferedImage sourceImage;
    private final String        sourcePath;
    /**
     * The operation pipeline. Exposed for GUI preview only (read-only).
     */
    public final List<ImageOperation> pipeline;

    /**
     * Loads an image from disk.
     *
     * @param filepath path to the source image file
     * @throws IOException           if the file cannot be read
     * @throws IllegalArgumentException if the file does not exist
     */
    public ImageEditor(String filepath) throws IOException {
        File file = new File(filepath);
        if (!file.exists()) {
            throw new IllegalArgumentException("File not found: " + filepath);
        }
        this.sourcePath  = filepath;
        this.sourceImage = ImageIO.read(file);
        this.pipeline    = new ArrayList<>();

        System.out.printf("  [✓] Loaded '%s'  (%d×%d px)%n",
            filepath, sourceImage.getWidth(), sourceImage.getHeight());
    }

    // ── Pipeline management ───────────────────────────────────────────────

    /**
     * Appends an operation to the pipeline.
     * Returns {@code this} so calls can be chained fluently.
     *
     * @param operation the operation to add
     * @return this editor (for method chaining)
     */
    public ImageEditor addOperation(ImageOperation operation) {
        pipeline.add(operation);
        System.out.println("  [+] Added: " + operation);
        return this;
    }

    /** Removes all pending operations without saving. */
    public void clearOperations() {
        pipeline.clear();
        System.out.println("  [!] Pipeline cleared.");
    }

    /** Prints the current operation pipeline to stdout. */
    public void previewPipeline() {
        if (pipeline.isEmpty()) {
            System.out.println("  Pipeline is empty.");
            return;
        }
        System.out.println("  Current pipeline:");
        for (int i = 0; i < pipeline.size(); i++) {
            System.out.printf("    %d. %s%n", i + 1, pipeline.get(i));
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────

    /**
     * Applies every operation in order and writes the result to
     * {@code outputPath}.  The source image is never modified.
     *
     * @param outputPath destination file path (format inferred from extension)
     * @throws IOException if the file cannot be written
     */
    public void save(String outputPath) throws IOException {
        if (pipeline.isEmpty()) {
            System.out.println("  [!] No operations – saving original image.");
        }

        // Work on a working copy; never touch sourceImage
        BufferedImage current = deepCopy(sourceImage);

        for (ImageOperation op : pipeline) {
            System.out.println("  Applying " + op + " …");
            current = op.apply(current);
        }

        // Determine format from file extension (default: png)
        String fmt = outputPath.contains(".")
            ? outputPath.substring(outputPath.lastIndexOf('.') + 1).toLowerCase()
            : "png";

        ImageIO.write(current, fmt, new File(outputPath));
        System.out.printf("  [✓] Saved → %s  (%d×%d px)%n",
            outputPath, current.getWidth(), current.getHeight());
    }

    /**
     * Returns a deep copy of the source image with all operations applied in order.
     * Used for GUI preview.
     * @return BufferedImage with all operations applied
     */
    public BufferedImage getPreviewImage() {
        BufferedImage img = deepCopy(sourceImage);
        for (ImageOperation op : pipeline) {
            img = op.apply(img);
        }
        return img;
    }

    // ── Getters ───────────────────────────────────────────────────────────

    public int getWidth()  { return sourceImage.getWidth();  }
    public int getHeight() { return sourceImage.getHeight(); }
    public String getSourcePath() { return sourcePath; }

    // ── Helper ────────────────────────────────────────────────────────────

    private static BufferedImage deepCopy(BufferedImage src) {
        BufferedImage copy = new BufferedImage(
            src.getWidth(), src.getHeight(), src.getType());
        copy.createGraphics().drawImage(src, 0, 0, null);
        return copy;
    }
}