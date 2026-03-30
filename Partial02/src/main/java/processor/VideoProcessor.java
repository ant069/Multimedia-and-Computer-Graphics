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
 * Reads video files and converts their frames to portrait-sized buffered images.
 */
public class VideoProcessor {
    /**
     * Extracts all image frames from a video and resizes them to the requested output size.
     *
     * @param file the media file describing the source video
     * @param width the output width
     * @param height the output height
     * @return the resized video frames in chronological order
     * @throws IOException if the video cannot be opened or decoded
     */
    public List<BufferedImage> extractFrames(MediaFile file, int width, int height) throws IOException {
        List<BufferedImage> frames = new ArrayList<>();
        Java2DFrameConverter converter = new Java2DFrameConverter();

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(file.getFile())) {
            grabber.start();
            Frame frame;
            while ((frame = grabber.grabImage()) != null) {
                BufferedImage image = converter.getBufferedImage(frame);
                if (image != null) {
                    frames.add(scaleToFill(image, width, height));
                }
            }
            grabber.stop();
        } catch (Exception exception) {
            throw new IOException("Could not extract frames from video: " + file.getFile().getAbsolutePath(), exception);
        }

        return frames;
    }

    /**
     * Returns the duration of a video file in seconds.
     *
     * @param file the media file describing the source video
     * @return the video duration in seconds
     * @throws IOException if the video cannot be inspected
     */
    public double getDuration(MediaFile file) throws IOException {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(file.getFile())) {
            grabber.start();
            double duration = grabber.getLengthInTime() / 1_000_000.0d;
            grabber.stop();
            return duration;
        } catch (Exception exception) {
            throw new IOException("Could not determine video duration: " + file.getFile().getAbsolutePath(), exception);
        }
    }

    /**
     * Resizes and center-crops a video frame to fill the target size.
     *
     * @param image the source frame
     * @param targetWidth the output width
     * @param targetHeight the output height
     * @return the processed frame
     */
    private BufferedImage scaleToFill(BufferedImage image, int targetWidth, int targetHeight) {
        double scale = Math.max((double) targetWidth / image.getWidth(), (double) targetHeight / image.getHeight());
        int scaledWidth = (int) Math.ceil(image.getWidth() * scale);
        int scaledHeight = (int) Math.ceil(image.getHeight() * scale);

        BufferedImage scaled = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.drawImage(image, 0, 0, scaledWidth, scaledHeight, null);
        graphics.dispose();

        int x = Math.max(0, (scaledWidth - targetWidth) / 2);
        int y = Math.max(0, (scaledHeight - targetHeight) / 2);
        BufferedImage cropped = scaled.getSubimage(x, y, targetWidth, targetHeight);
        BufferedImage result = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D resultGraphics = result.createGraphics();
        resultGraphics.drawImage(cropped, 0, 0, null);
        resultGraphics.dispose();
        return result;
    }
}
