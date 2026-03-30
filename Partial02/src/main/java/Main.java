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
 */
public class Main {
    /**
     * Runs the complete video generation workflow.
     *
     * @param args command-line arguments, where args[0] may contain the input folder path
     */
    public static void main(String[] args) {
        try {
            String inputFolder = resolveInputFolder(args);
            int[] resolution = parseResolution(Config.get("output.resolution"));
            int width = resolution[0];
            int height = resolution[1];
            double photoDuration = Double.parseDouble(Config.get("frame.duration"));

            MediaProcessor mediaProcessor = new MediaProcessor();
            ImageProcessor imageProcessor = new ImageProcessor();
            VideoProcessor videoProcessor = new VideoProcessor();
            GeminiClient geminiClient = new GeminiClient();
            TextGenerator textGenerator = new TextGenerator(geminiClient);
            ImageGenerator imageGenerator = new ImageGenerator(geminiClient);
            TTSGenerator ttsGenerator = new TTSGenerator();
            MapRenderer mapRenderer = new MapRenderer();
            VideoAssembler videoAssembler = new VideoAssembler();

            List<MediaFile> mediaFiles = mediaProcessor.scanAndSort(inputFolder);
            if (mediaFiles.isEmpty()) {
                throw new IOException("No supported media files were found in: " + inputFolder);
            }

            Path workDirectory = Files.createTempDirectory("partial02-work-");
            List<VideoAssembler.FrameSegment> segments = new ArrayList<>();

            // Intro segment
            BufferedImage introImage = imageGenerator.generateIntroImage(mediaFiles);
            String introText = geminiClient.generateText(buildIntroNarrationPrompt(mediaFiles));

            File introAudio = null;
            try {
                introAudio = ttsGenerator.generateAudio(
                    introText,
                    workDirectory.resolve("intro.mp3").toString()
                );
            } catch (IOException audioException) {
                System.err.println("Warning: TTS failed for intro: " + audioException.getMessage());
            }
            segments.add(new VideoAssembler.FrameSegment(List.of(introImage), introAudio, photoDuration));

            // Media segments
            int mediaIndex = 0;
            for (MediaFile mediaFile : mediaFiles) {
                String description = textGenerator.describeMedia(mediaFile);

                File audioFile = null;
                try {
                    audioFile = ttsGenerator.generateAudio(
                        description,
                        workDirectory.resolve("media_" + mediaIndex + ".mp3").toString()
                    );
                } catch (IOException audioException) {
                    System.err.println("Warning: TTS failed for file "
                        + mediaFile.getFile().getName() + ": " + audioException.getMessage());
                }

                if ("PHOTO".equals(mediaFile.getType())) {
                    BufferedImage processedImage = imageProcessor.processImage(mediaFile, width, height);
                    segments.add(new VideoAssembler.FrameSegment(List.of(processedImage), audioFile, photoDuration));
                } else {
                    List<BufferedImage> videoFrames = videoProcessor.extractFrames(mediaFile, width, height);
                    double duration = videoProcessor.getDuration(mediaFile);
                    segments.add(new VideoAssembler.FrameSegment(videoFrames, audioFile, duration));
                }
                mediaIndex++;
            }

            // Closing segment
            MediaFile firstMedia = mediaFiles.get(0);
            MediaFile lastMedia = mediaFiles.get(mediaFiles.size() - 1);
            String firstLocation = formatLocation(firstMedia.getGpsLat(), firstMedia.getGpsLon());
            String lastLocation = formatLocation(lastMedia.getGpsLat(), lastMedia.getGpsLon());
            String closingPhrase = textGenerator.generateInspirationalPhrase(firstLocation, lastLocation);
            BufferedImage mapImage = mapRenderer.renderMap(
                firstMedia.getGpsLat(),
                firstMedia.getGpsLon(),
                lastMedia.getGpsLat(),
                lastMedia.getGpsLon(),
                closingPhrase
            );

            File closingAudio = null;
            try {
                closingAudio = ttsGenerator.generateAudio(
                    closingPhrase,
                    workDirectory.resolve("closing.mp3").toString()
                );
            } catch (IOException audioException) {
                System.err.println("Warning: TTS failed for closing: " + audioException.getMessage());
            }
            segments.add(new VideoAssembler.FrameSegment(List.of(mapImage), closingAudio, 7.0d));

            // Assemble final video
            Path inputDirectory = Path.of(inputFolder);
            String outputFileName = Config.get("output.filename");
            Path outputPath = inputDirectory.getParent() == null
                ? Path.of(outputFileName)
                : inputDirectory.getParent().resolve(outputFileName);

            videoAssembler.assemble(segments, outputPath.toString());
            System.out.println("Done! Video saved to: " + outputPath.getFileName());

        } catch (Exception exception) {
            System.err.println("Video generation failed: " + exception.getMessage());
            exception.printStackTrace(System.err);
        }
    }

    /**
     * Resolves the input folder from the command line or prompts the user for it.
     *
     * @param args the command-line arguments
     * @return the resolved input folder path
     * @throws IOException if the provided path is invalid
     */
    private static String resolveInputFolder(String[] args) throws IOException {
        String inputFolder;
        if (args != null && args.length > 0 && args[0] != null && !args[0].isBlank()) {
            inputFolder = args[0].trim();
        } else {
            System.out.print("Enter the input folder path: ");
            Scanner scanner = new Scanner(System.in);
            inputFolder = scanner.nextLine().trim();
        }

        if (!Files.isDirectory(Path.of(inputFolder))) {
            throw new IOException("Input folder does not exist: " + inputFolder);
        }
        return inputFolder;
    }

    /**
     * Parses a resolution string in WIDTHxHEIGHT format.
     *
     * @param value the resolution string
     * @return a two-element array containing width and height
     */
    private static int[] parseResolution(String value) {
        String[] parts = value.toLowerCase().split("x");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid output resolution: " + value);
        }
        return new int[]{Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())};
    }

    /**
     * Builds the narration prompt for the intro scene.
     *
     * @param mediaFiles the scanned media files
     * @return the prompt text
     */
    private static String buildIntroNarrationPrompt(List<MediaFile> mediaFiles) {
        StringBuilder builder = new StringBuilder();
        builder.append("Write a warm 2-sentence introduction for a portrait travel video. ");
        builder.append("The uploaded journey includes these moments: ");
        for (int index = 0; index < Math.min(mediaFiles.size(), 6); index++) {
            MediaFile file = mediaFiles.get(index);
            builder.append(file.getType())
                .append(" on ")
                .append(file.getDate())
                .append(" near ")
                .append(formatLocation(file.getGpsLat(), file.getGpsLon()));
            if (index < Math.min(mediaFiles.size(), 6) - 1) {
                builder.append("; ");
            }
        }
        return builder.toString();
    }

    /**
     * Formats a latitude-longitude pair for prompt generation.
     *
     * @param latitude the latitude value
     * @param longitude the longitude value
     * @return a formatted coordinate string
     */
    private static String formatLocation(double latitude, double longitude) {
        return String.format("%.6f, %.6f", latitude, longitude);
    }
}
