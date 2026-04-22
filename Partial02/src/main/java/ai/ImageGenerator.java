package ai;

import model.MediaFile;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates the introduction image that summarizes the uploaded journey.
 */
public class ImageGenerator {
    private final GeminiClient geminiClient;

    /**
     * Creates an image generator using the shared Gemini client.
     */
    public ImageGenerator() {
        this(new GeminiClient());
    }

    /**
     * Creates an image generator with an explicit Gemini client.
     *
     * @param geminiClient the Gemini client to use
     */
    public ImageGenerator(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    /**
     * Generates the intro image based on a summary of the media collection.
     *
     * @param files the media files included in the final video
     * @return the generated intro image
     * @throws IOException if image generation fails
     */
    public BufferedImage generateIntroImage(List<MediaFile> files) throws IOException {
        String summary = files.stream()
            .limit(8)
            .map(file -> file.getType() + " captured on " + file.getDate() + " near "
                + String.format("%.5f, %.5f", file.getGpsLat(), file.getGpsLon()))
            .collect(Collectors.joining("; "));

        String prompt = "Create a cinematic portrait travel poster that captures the essence of this media collection. "
            + "Use warm lighting, a sense of motion, and layered scenery. Summary: " + summary + ". "
            + "The image must look like a polished vertical cover for a personal travel story.";
        return geminiClient.generateImage(prompt);
    }

    /**
     * Generates a map image showing the travel route from start to end locations.
     *
     * @param lat1 latitude of the start location
     * @param lon1 longitude of the start location
     * @param lat2 latitude of the end location
     * @param lon2 longitude of the end location
     * @return the generated map image
     * @throws IOException if image generation fails
     */
    public BufferedImage generateMapImage(double lat1, double lon1, double lat2, double lon2) throws IOException {
        String prompt = "Generate a stylized world map in portrait orientation (1080x1920) showing a travel journey. "
            + "Include a green pin or marker at the starting location (" + lat1 + ", " + lon1 + ") and a red pin at the ending location (" + lat2 + ", " + lon2 + "). "
            + "Draw a dotted or dashed line connecting the two points to represent the travel route. "
            + "Use a clean, modern design with continents in muted colors, oceans in blue, and the pins clearly visible. "
            + "Make it look like an artistic travel map poster.";
        return geminiClient.generateImage(prompt);
    }
}
