package operations;

import java.awt.image.BufferedImage;


public abstract class ImageOperation {

    /**
     * Apply this operation to the given image.
     * Implementations must NOT mutate the original image;
     * they must return a new or copied BufferedImage.
     *
     * @param image source image
     * @return resulting image after the operation
     */
    public abstract BufferedImage apply(BufferedImage image);

    /**
     * Human-readable description of the operation (for logging/pipeline preview).
     */
    @Override
    public abstract String toString();
}