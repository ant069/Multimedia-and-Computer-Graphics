package gui;

import ai.GeminiClient;
import ai.ImageGenerator;
import ai.TTSGenerator;
import ai.TextGenerator;
import audio.AudioNormalizer;
import map.GeocodingService;
import map.MapRenderer;
import model.MediaFile;
import processor.ImageProcessor;
import processor.MediaProcessor;
import processor.VideoProcessor;
import video.VideoAssembler;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Background worker that runs the full video generation pipeline and
 * reports granular progress back to the {@link MainWindow}.
 *
 * <p>Progress milestones:
 * <ul>
 *   <li>5 %  – scan and sort media</li>
 *   <li>15 % – AI intro image + narration</li>
 *   <li>15–75 % – per-media processing (proportional)</li>
 *   <li>80 % – closing map + inspirational phrase</li>
 *   <li>90 % – video assembly</li>
 *   <li>100 % – done</li>
 * </ul>
 */
public class VideoGenerationWorker extends SwingWorker<String, String> {

    private final String inputFolder;
    private final MainWindow window;

    /**
     * Creates a worker for the given input folder.
     *
     * @param inputFolder the folder that contains the media files
     * @param window      the main window to receive progress updates
     */
    public VideoGenerationWorker(String inputFolder, MainWindow window) {
        this.inputFolder = inputFolder;
        this.window = window;
    }

    /**
     * Runs the complete pipeline on a background thread.
     *
     * @return the absolute path of the finished MP4 file
     * @throws Exception if any pipeline step fails unrecoverably
     */
    @Override
    protected String doInBackground() throws Exception {

        // -- Collaborators -----------------------------------------------------
        MediaProcessor   mediaProcessor  = new MediaProcessor();
        ImageProcessor   imageProcessor  = new ImageProcessor();
        VideoProcessor   videoProcessor  = new VideoProcessor();
        GeminiClient     geminiClient    = new GeminiClient();
        TextGenerator    textGenerator   = new TextGenerator(geminiClient);
        ImageGenerator   imageGenerator  = new ImageGenerator(geminiClient);
        TTSGenerator     ttsGenerator    = new TTSGenerator();
        AudioNormalizer  audioNormalizer = new AudioNormalizer();
        MapRenderer      mapRenderer     = new MapRenderer();
        GeocodingService geocoder        = new GeocodingService();
        VideoAssembler   videoAssembler  = new VideoAssembler();

        int[]  resolution    = parseResolution(getConfig("output.resolution"));
        int    width         = resolution[0];
        int    height        = resolution[1];
        double photoDuration = Double.parseDouble(getConfig("frame.duration"));

        Path workDir = Files.createTempDirectory("partial02-work-");

        // -- Step 1: Scan & sort -----------------------------------------------
        progress(2, "Scanning media files…");
        List<MediaFile> mediaFiles = mediaProcessor.scanAndSort(inputFolder);
        if (mediaFiles.isEmpty()) {
            throw new IOException("No supported media files found in: " + inputFolder);
        }
        progress(5, "Found " + mediaFiles.size() + " file(s).");
        publish("Found " + mediaFiles.size() + " file(s) — sorted oldest ? newest.");

        List<VideoAssembler.FrameSegment> segments = new ArrayList<>();

        // -- Step 2: AI intro image + narration -------------------------------
        progress(8, "Generating AI intro image…");
        publish("Generating AI intro image…");
        BufferedImage introImage = imageGenerator.generateIntroImage(mediaFiles);

        progress(12, "Generating intro narration…");
        publish("Generating intro narration…");
        String introText  = geminiClient.generateText(buildIntroPrompt(mediaFiles));
        File   introAudio = safeGenerateAudio(ttsGenerator, audioNormalizer, introText,
                workDir.resolve("intro.wav").toString());
        segments.add(new VideoAssembler.FrameSegment(List.of(introImage), introAudio, photoDuration));
        progress(15, "Intro ready.");

        // -- Step 3: Per-media segments ----------------------------------------
        int mediaCount = mediaFiles.size();
        for (int i = 0; i < mediaCount; i++) {
            MediaFile mediaFile = mediaFiles.get(i);
            int pctStart = 15 + (int) ((double) i       / mediaCount * 60);
            int pctEnd   = 15 + (int) ((double) (i + 1) / mediaCount * 60);

            publish(String.format("[%d/%d] %s", i + 1, mediaCount,
                    mediaFile.getFile().getName()));
            progress(pctStart, String.format("Processing %d/%d: %s",
                    i + 1, mediaCount, mediaFile.getFile().getName()));

            String description = textGenerator.describeMedia(mediaFile);
            File   audioFile   = safeGenerateAudio(ttsGenerator, audioNormalizer, description,
                    workDir.resolve("media_" + i + ".wav").toString());

            if ("PHOTO".equals(mediaFile.getType())) {
                BufferedImage img = imageProcessor.processImage(mediaFile, width, height);
                segments.add(new VideoAssembler.FrameSegment(List.of(img), audioFile, photoDuration));
            } else {
                List<BufferedImage> frames   = videoProcessor.extractFrames(mediaFile, width, height);
                double              duration = videoProcessor.getDuration(mediaFile);
                segments.add(new VideoAssembler.FrameSegment(frames, audioFile, duration));
            }

            progress(pctEnd, "Done: " + mediaFile.getFile().getName());
        }

        // -- Step 4: Closing map + inspirational phrase ------------------------
        progress(76, "Resolving location names…");
        publish("Resolving GPS coordinates to place names…");

        MediaFile firstMedia = mediaFiles.get(0);
        MediaFile lastMedia  = mediaFiles.get(mediaFiles.size() - 1);
        validateGps(firstMedia, lastMedia);

        // Reverse-geocode GPS ? real city/place names before calling Gemini
        String firstPlace = geocoder.resolveName(firstMedia.getGpsLat(), firstMedia.getGpsLon());
        String lastPlace  = geocoder.resolveName(lastMedia.getGpsLat(),  lastMedia.getGpsLon());
        publish("Start: " + firstPlace + "  ?  End: " + lastPlace);

        progress(79, "Generating inspirational phrase…");
        String closingPhrase = textGenerator.generateInspirationalPhrase(firstPlace, lastPlace);
        publish("Phrase: " + closingPhrase);

        progress(81, "Rendering map…");
        BufferedImage mapImage = mapRenderer.renderMap(
                firstMedia.getGpsLat(), firstMedia.getGpsLon(),
                lastMedia.getGpsLat(),  lastMedia.getGpsLon(),
                closingPhrase, imageGenerator);

        File closingAudio = safeGenerateAudio(ttsGenerator, audioNormalizer, closingPhrase,
                workDir.resolve("closing.wav").toString());
        segments.add(new VideoAssembler.FrameSegment(List.of(mapImage), closingAudio, 7.0));
        progress(84, "Map ready.");

        // -- Step 5: Assemble final video --------------------------------------
        progress(86, "Assembling final video…");
        publish("Assembling MP4…");

        Path   inputDir      = Path.of(inputFolder);
        String outputFileName = getConfig("output.filename");
        Path   outputPath    = (inputDir.getParent() == null)
                ? Path.of(outputFileName)
                : inputDir.getParent().resolve(outputFileName);

        videoAssembler.assemble(segments, outputPath.toString());
        progress(100, "Done!");
        return outputPath.toAbsolutePath().toString();
    }

    /** Forwards log messages from the background thread to the window. */
    @Override
    protected void process(List<String> chunks) {
        chunks.forEach(window::log);
    }

    /** Called on the EDT when the worker finishes; notifies the window. */
    @Override
    protected void done() {
        try {
            String outputPath = get();
            window.onGenerationComplete(true, "Video saved to: " + outputPath);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            window.onGenerationComplete(false, "Failed: " + cause.getMessage());
        }
    }

    // -- Private helpers -------------------------------------------------------

    private void progress(int percent, String message) {
        setProgress(percent);
        window.setProgress(percent, message);
    }

    private File safeGenerateAudio(TTSGenerator tts, AudioNormalizer normalizer,
                                   String text, String path) {
        try {
            File raw = tts.generateAudio(text, path);
            return normalizer.normalize(raw);
        } catch (Exception e) {
            publish("Warning: audio failed — " + e.getMessage());
            return null;
        }
    }

    private void validateGps(MediaFile first, MediaFile last) throws IOException {
        if ((first.getGpsLat() == 0.0 && first.getGpsLon() == 0.0)
                || (last.getGpsLat() == 0.0 && last.getGpsLon() == 0.0)) {
            throw new IOException(
                    "First or last media file is missing GPS metadata. "
                    + "Ensure your files contain embedded GPS EXIF data.");
        }
    }

    private int[] parseResolution(String value) {
        String[] parts = value.toLowerCase().split("x");
        if (parts.length != 2) throw new IllegalArgumentException("Invalid resolution: " + value);
        return new int[]{Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())};
    }

    private String buildIntroPrompt(List<MediaFile> files) {
        StringBuilder sb = new StringBuilder(
                "Write a warm 2-sentence introduction for a portrait travel video. "
                + "The journey includes: ");
        int limit = Math.min(files.size(), 6);
        for (int i = 0; i < limit; i++) {
            MediaFile f = files.get(i);
            sb.append(f.getType()).append(" on ").append(f.getDate())
              .append(" at ").append(String.format("%.5f, %.5f", f.getGpsLat(), f.getGpsLon()));
            if (i < limit - 1) sb.append("; ");
        }
        return sb.toString();
    }

    /**
     * Reads a value from the default-package Config class via reflection
     * (packaged classes cannot import default-package classes directly).
     *
     * @param key the property key
     * @return the property value
     */
    private String getConfig(String key) {
        try {
            Class<?> cfg = Class.forName("Config");
            Method   get = cfg.getMethod("get", String.class);
            return (String) get.invoke(null, key);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot read config key: " + key, e);
        }
    }
}
