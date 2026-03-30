package ai;

import model.MediaFile;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * Generates descriptive narration text for media items and closing phrases.
 */
public class TextGenerator {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final GeminiClient geminiClient;

    /**
     * Creates a text generator using the shared Gemini client.
     */
    public TextGenerator() {
        this(new GeminiClient());
    }

    /**
     * Creates a text generator with an explicit Gemini client.
     *
     * @param geminiClient the Gemini client to use
     */
    public TextGenerator(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    /**
     * Generates a short two-sentence narration for a media file.
     *
     * @param file the media file to describe
     * @return a short description suitable for voice narration
     * @throws IOException if the AI service cannot generate text
     */
    public String describeMedia(MediaFile file) throws IOException {
        String prompt = "Write exactly two short sentences for a travel video voiceover. "
            + "Describe this media item in natural English using only this metadata. "
            + "Filename: " + file.getFile().getName() + ". "
            + "Type: " + file.getType() + ". "
            + "Date: " + file.getDate().format(DATE_FORMATTER) + ". "
            + "GPS: " + formatCoordinates(file.getGpsLat(), file.getGpsLon()) + ". "
            + "Keep it vivid, concise, and suitable for narration.";
        return geminiClient.generateText(prompt).replaceAll("\\s+", " ").trim();
    }

    /**
     * Generates a short inspirational phrase that mentions the first and last locations.
     *
     * @param firstLocation the location associated with the first media file
     * @param lastLocation the location associated with the last media file
     * @return an inspirational travel phrase
     * @throws IOException if the AI service cannot generate text
     */
    public String generateInspirationalPhrase(String firstLocation, String lastLocation) throws IOException {
        String prompt = "Write one short inspirational travel phrase in English. "
            + "It must mention both of these places: " + firstLocation + " and " + lastLocation + ". "
            + "Keep it under 25 words and make it suitable for a closing video scene.";
        return geminiClient.generateText(prompt).replaceAll("\\s+", " ").trim();
    }

    /**
     * Formats latitude and longitude for prompts.
     *
     * @param latitude the latitude value
     * @param longitude the longitude value
     * @return a human-readable coordinate string
     */
    private String formatCoordinates(double latitude, double longitude) {
        return String.format("%.6f, %.6f", latitude, longitude);
    }
}
