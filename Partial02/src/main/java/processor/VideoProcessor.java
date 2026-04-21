package processor;

import model.MediaFile;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads video files and converts a representative set of frames to portrait-sized buffered images.
 *
 * <p>To avoid out-of-memory errors on long videos, only up to {@value #MAX_FRAMES} evenly-spaced
 * frames are decoded. The {@link video.VideoAssembler} will interpolate (hold-last-frame) as needed
 * to fill the actual video duration.</p>
 */
public class VideoProcessor {

    /** Maximum number of frames kept in memory per video segment. */
    private static final int MAX_FRAMES = 300;   // ~10 s at 30 fps

    /**
     * Extracts up to {@value #MAX_FRAMES} evenly-spaced frames from a video
     * and resizes each one to the requested output size.
     *
     * @param file   the media file describing the source video
     * @param width  the output width
     * @param height the output height
     * @return the resized video frames in chronological order
     * @throws IOException if the video cannot be opened or decoded
     */
    public List<BufferedImage> extractFrames(MediaFile file, int width, int height) throws IOException {
        List<BufferedImage> frames = new ArrayList<>();
        Java2DFrameConverter converter = new Java2DFrameConverter();

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(file.getFile())) {
            grabber.start();

            long totalFrames = grabber.getLengthInFrames();
            // Decide sampling step: grab every Nth frame so we stay under MAX_FRAMES
            int step = (totalFrames > 0) ? (int) Math.max(1, totalFrames / MAX_FRAMES) : 1;

            int frameIndex = 0;
            Frame frame;
            while ((frame = grabber.grabImage()) != null) {
                if (frameIndex % step == 0) {
                    BufferedImage image = converter.getBufferedImage(frame);
                    if (image != null) {
                        frames.add(scaleToFill(image, width, height));
                    }
                }
                frameIndex++;

                // Hard cap — stop once we have enough representative frames
                if (frames.size() >= MAX_FRAMES) {
                    break;
                }
            }
            grabber.stop();
        } catch (Exception exception) {
            throw new IOException("Could not extract frames from video: "
                    + file.getFile().getAbsolutePath(), exception);
        }

        if (frames.isEmpty()) {
            throw new IOException("No decodable frames found in video: "
                    + file.getFile().getAbsolutePath());
        }

        return frames;
    }

    /**
     * Returns the duration of a video file in seconds.
     *
     * @param file the media file describing the source video
     * @return the video duration in seconds (minimum 1.0)
     * @throws IOException if the video cannot be inspected
     */
    public double getDuration(MediaFile file) throws IOException {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(file.getFile())) {
            grabber.start();
            long durationUs = grabber.getLengthInTime();
            grabber.stop();
            double duration = durationUs / 1_000_000.0;
            return Math.max(duration, 1.0);
        } catch (Exception exception) {
            throw new IOException("Could not determine video duration: "
                    + file.getFile().getAbsolutePath(), exception);
        }
    }

    /**
     * Resizes and center-crops a video frame to fill the target size while preserving aspect ratio.
     *
     * @param image        the source frame
     * @param targetWidth  the output width
     * @param targetHeight the output height
     * @return the processed frame
     */
    private BufferedImage scaleToFill(BufferedImage image, int targetWidth, int targetHeight) {
        double scale = Math.max(
                (double) targetWidth / image.getWidth(),
                (double) targetHeight / image.getHeight()
        );
        int scaledWidth = (int) Math.ceil(image.getWidth() * scale);
        int scaledHeight = (int) Math.ceil(image.getHeight() * scale);

        BufferedImage scaled = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(image, 0, 0, scaledWidth, scaledHeight, null);
        g.dispose();

        int x = Math.max(0, (scaledWidth - targetWidth) / 2);
        int y = Math.max(0, (scaledHeight - targetHeight) / 2);
        BufferedImage cropped = scaled.getSubimage(x, y, targetWidth, targetHeight);

        BufferedImage result = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D rg = result.createGraphics();
        rg.drawImage(cropped, 0, 0, null);
        rg.dispose();
        return result;
    }
}
