import ai.GeminiClient;
import ai.ImageGenerator;
import ai.TTSGenerator;
import ai.TextGenerator;
import map.MapRenderer;
import model.MediaFile;
import processor.ImageProcessor;
import processor.MediaProcessor;
import processor.VideoProcessor;
import video.VideoAssembler;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Entry point for the automatic travel video generator.
 *
 * <p>Workflow:
 * <ol>
 *   <li>Scan input folder, sort media by date.</li>
 *   <li>Generate an AI intro image and narration.</li>
 *   <li>For each photo/video: process frames + generate narration audio.</li>
 *   <li>Generate closing map image with AI inspirational phrase.</li>
 *   <li>Assemble everything into a portrait MP4.</li>
 * </ol>
 */
public class Main {

    /**
     * Runs the complete video generation workflow.
     *
     * @param args optional: args[0] = input folder path
     */
    public static void main(String[] args) {
        try {
            String inputFolder = resolveInputFolder(args);
            int[] resolution = parseResolution(Config.get("output.resolution"));
            int width = resolution[0];
            int height = resolution[1];
            double photoDuration = Double.parseDouble(Config.get("frame.duration"));

            // Instantiate collaborators
            MediaProcessor mediaProcessor = new MediaProcessor();
            ImageProcessor imageProcessor = new ImageProcessor();
            VideoProcessor videoProcessor = new VideoProcessor();
            GeminiClient geminiClient = new GeminiClient();
            TextGenerator textGenerator = new TextGenerator(geminiClient);
            ImageGenerator imageGenerator = new ImageGenerator(geminiClient);
            TTSGenerator ttsGenerator = new TTSGenerator();
            MapRenderer mapRenderer = new MapRenderer();
            VideoAssembler videoAssembler = new VideoAssembler();

            // 1 ── Scan & sort
            List<MediaFile> mediaFiles = mediaProcessor.scanAndSort(inputFolder);
            if (mediaFiles.isEmpty()) {
                throw new IOException("No supported media files found in: " + inputFolder);
            }

            Path workDir = Files.createTempDirectory("partial02-work-");
            List<VideoAssembler.FrameSegment> segments = new ArrayList<>();

            // 2 ── Intro segment
            System.out.println("Generating intro image...");
            BufferedImage introImage = imageGenerator.generateIntroImage(mediaFiles);
            String introText = geminiClient.generateText(buildIntroNarrationPrompt(mediaFiles));
            File introAudio = safeGenerateAudio(ttsGenerator, introText,
                    workDir.resolve("intro.wav").toString());
            segments.add(new VideoAssembler.FrameSegment(List.of(introImage), introAudio, photoDuration));

            // 3 ── Per-media segments
            int idx = 0;
            for (MediaFile mediaFile : mediaFiles) {
                System.out.printf("Processing [%d/%d]: %s%n",
                        ++idx, mediaFiles.size(), mediaFile.getFile().getName());

                String description = textGenerator.describeMedia(mediaFile);
                File audioFile = safeGenerateAudio(ttsGenerator, description,
                        workDir.resolve("media_" + idx + ".wav").toString());

                if ("PHOTO".equals(mediaFile.getType())) {
                    BufferedImage processedImage = imageProcessor.processImage(mediaFile, width, height);
                    segments.add(new VideoAssembler.FrameSegment(List.of(processedImage), audioFile, photoDuration));
                } else {
                    List<BufferedImage> videoFrames = videoProcessor.extractFrames(mediaFile, width, height);
                    double duration = videoProcessor.getDuration(mediaFile);
                    segments.add(new VideoAssembler.FrameSegment(videoFrames, audioFile, duration));
                }
            }

            // 4 ── Closing map segment
            System.out.println("Generating closing map...");
            MediaFile firstMedia = mediaFiles.get(0);
            MediaFile lastMedia = mediaFiles.get(mediaFiles.size() - 1);

            // Validate GPS availability
            if (!hasGps(firstMedia) || !hasGps(lastMedia)) {
                throw new IOException(
                        "First or last media file is missing GPS data. "
                        + "Please ensure your photos/videos contain GPS EXIF metadata.");
            }

            String firstLocation = formatLocation(firstMedia.getGpsLat(), firstMedia.getGpsLon());
            String lastLocation = formatLocation(lastMedia.getGpsLat(), lastMedia.getGpsLon());
            String closingPhrase = textGenerator.generateInspirationalPhrase(firstLocation, lastLocation);

            BufferedImage mapImage = mapRenderer.renderMap(
                    firstMedia.getGpsLat(), firstMedia.getGpsLon(),
                    lastMedia.getGpsLat(), lastMedia.getGpsLon(),
                    closingPhrase);

            File closingAudio = safeGenerateAudio(ttsGenerator, closingPhrase,
                    workDir.resolve("closing.wav").toString());
            segments.add(new VideoAssembler.FrameSegment(List.of(mapImage), closingAudio, 7.0));

            // 5 ── Assemble
            System.out.println("Assembling final video...");
            Path inputDirectory = Path.of(inputFolder);
            String outputFileName = Config.get("output.filename");
            Path outputPath = (inputDirectory.getParent() == null)
                    ? Path.of(outputFileName)
                    : inputDirectory.getParent().resolve(outputFileName);

            videoAssembler.assemble(segments, outputPath.toString());
            System.out.println("Done! Video saved to: " + outputPath.toAbsolutePath());

        } catch (Exception e) {
            System.err.println("Video generation failed: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Resolves the input folder from command-line args or prompts the user interactively.
     *
     * @param args the command-line arguments
     * @return the resolved input folder path
     * @throws IOException if the path is not a valid directory
     */
    private static String resolveInputFolder(String[] args) throws IOException {
        String inputFolder;
        if (args != null && args.length > 0 && args[0] != null && !args[0].isBlank()) {
            inputFolder = args[0].trim();
        } else {
            System.out.print("Enter the input folder path: ");
            inputFolder = new Scanner(System.in).nextLine().trim();
        }
        if (!Files.isDirectory(Path.of(inputFolder))) {
            throw new IOException("Input folder does not exist: " + inputFolder);
        }
        return inputFolder;
    }

    /**
     * Parses a resolution string in WIDTHxHEIGHT format.
     *
     * @param value the resolution string (e.g. "1080x1920")
     * @return a two-element array {width, height}
     */
    private static int[] parseResolution(String value) {
        String[] parts = value.toLowerCase().split("x");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid output.resolution: " + value);
        }
        return new int[]{Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())};
    }

    /**
     * Generates audio safely, falling back to null (silence) on failure.
     *
     * @param tts        the TTS generator
     * @param text       the text to synthesize
     * @param outputPath the destination file path
     * @return the audio file, or null if generation failed
     */
    private static File safeGenerateAudio(TTSGenerator tts, String text, String outputPath) {
        try {
            return tts.generateAudio(text, outputPath);
        } catch (IOException e) {
            System.err.println("Warning: TTS failed (" + e.getMessage() + "), using silence.");
            return null;
        }
    }

    /**
     * Returns true when a media file carries non-zero GPS coordinates.
     *
     * @param media the media file to check
     * @return true if GPS data is present
     */
    private static boolean hasGps(MediaFile media) {
        return media.getGpsLat() != 0.0 || media.getGpsLon() != 0.0;
    }

    /**
     * Builds the narration prompt for the intro scene.
     *
     * @param mediaFiles the scanned media files
     * @return the prompt text
     */
    private static String buildIntroNarrationPrompt(List<MediaFile> mediaFiles) {
        StringBuilder sb = new StringBuilder();
        sb.append("Write a warm 2-sentence introduction for a portrait travel video. "
                + "The journey includes these moments: ");
        int limit = Math.min(mediaFiles.size(), 6);
        for (int i = 0; i < limit; i++) {
            MediaFile f = mediaFiles.get(i);
            sb.append(f.getType())
              .append(" on ").append(f.getDate())
              .append(" near ").append(formatLocation(f.getGpsLat(), f.getGpsLon()));
            if (i < limit - 1) sb.append("; ");
        }
        return sb.toString();
    }

    /**
     * Formats a latitude/longitude pair as a readable coordinate string.
     *
     * @param lat latitude
     * @param lon longitude
     * @return formatted coordinate string
     */
    private static String formatLocation(double lat, double lon) {
        return String.format("%.6f, %.6f", lat, lon);
    }
}
