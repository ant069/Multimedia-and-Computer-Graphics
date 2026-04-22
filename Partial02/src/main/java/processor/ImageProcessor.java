package processor;

import model.MediaFile;
import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Processes still images so they are correctly oriented and cropped for portrait output.
 */
public class ImageProcessor {
    /**
     * Loads an image, applies EXIF orientation correction, and scales it to fill the target bounds.
     *
     * @param file the media file describing the image to process
     * @param width the output width
     * @param height the output height
     * @return the processed image sized to the requested dimensions
     * @throws IOException if the image cannot be read or processed
     */
    public BufferedImage processImage(MediaFile file, int width, int height) throws IOException {
        BufferedImage original = ImageIO.read(file.getFile());
        if (original == null) {
            throw new IOException("Unsupported image format: " + file.getFile().getAbsolutePath());
        }

        BufferedImage oriented = applyOrientation(original, file.getOrientation());
        BufferedImage scaled = Thumbnails.of(oriented)
            .size(width, height)
            .keepAspectRatio(true)
            .asBufferedImage();

        return cropToFill(scaled, width, height);
    }

    /**
     * Applies EXIF orientation transforms to an image.
     *
     * @param image the original image
     * @param orientation the EXIF orientation value
     * @return the transformed image
     */
    private BufferedImage applyOrientation(BufferedImage image, int orientation) {
        if (orientation <= 1) {
            return image;
        }

        int sourceWidth = image.getWidth();
        int sourceHeight = image.getHeight();
        int targetWidth = sourceWidth;
        int targetHeight = sourceHeight;
        AffineTransform transform = new AffineTransform();

        switch (orientation) {
            case 2 -> {
                transform.scale(-1.0, 1.0);
                transform.translate(-sourceWidth, 0.0);
            }
            case 3 -> {
                transform.translate(sourceWidth, sourceHeight);
                transform.rotate(Math.PI);
            }
            case 4 -> {
                transform.scale(1.0, -1.0);
                transform.translate(0.0, -sourceHeight);
            }
            case 5 -> {
                targetWidth = sourceHeight;
                targetHeight = sourceWidth;
                transform.rotate(Math.PI / 2.0);
                transform.scale(1.0, -1.0);
            }
            case 6 -> {
                targetWidth = sourceHeight;
                targetHeight = sourceWidth;
                transform.translate(sourceHeight, 0.0);
                transform.rotate(Math.PI / 2.0);
            }
            case 7 -> {
                targetWidth = sourceHeight;
                targetHeight = sourceWidth;
                transform.scale(-1.0, 1.0);
                transform.translate(-sourceHeight, 0.0);
                transform.translate(0.0, sourceWidth);
                transform.rotate(3.0 * Math.PI / 2.0);
            }
            case 8 -> {
                targetWidth = sourceHeight;
                targetHeight = sourceWidth;
                transform.translate(0.0, sourceWidth);
                transform.rotate(3.0 * Math.PI / 2.0);
            }
            default -> {
                return image;
            }
        }

        BufferedImage destination = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = destination.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.drawImage(image, transform, null);
        graphics.dispose();
        return destination;
    }

    /**
     * Crops an image to exactly match the target size while keeping the center area.
     *
     * @param image the image to crop
     * @param targetWidth the desired width
     * @param targetHeight the desired height
     * @return a centered crop sized to the target bounds
     */
    private BufferedImage cropToFill(BufferedImage image, int targetWidth, int targetHeight) {
        double scale = Math.max((double) targetWidth / image.getWidth(), (double) targetHeight / image.getHeight());
        int scaledWidth = (int) Math.ceil(image.getWidth() * scale);
        int scaledHeight = (int) Math.ceil(image.getHeight() * scale);

        BufferedImage canvas = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = canvas.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.drawImage(image, 0, 0, scaledWidth, scaledHeight, null);
        graphics.dispose();

        int x = Math.max(0, (scaledWidth - targetWidth) / 2);
        int y = Math.max(0, (scaledHeight - targetHeight) / 2);
        BufferedImage subImage = canvas.getSubimage(x, y, targetWidth, targetHeight);
        BufferedImage result = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D copyGraphics = result.createGraphics();
        copyGraphics.drawImage(subImage, 0, 0, null);
        copyGraphics.dispose();
        return result;
    }
}
