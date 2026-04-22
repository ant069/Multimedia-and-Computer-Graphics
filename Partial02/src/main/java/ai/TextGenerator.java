package ai;

import model.MediaFile;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * Generates descriptive narration text for media items and closing phrases.
 *
 * <p>All text generation is delegated to the Gemini API via {@link GeminiClient}.
 * Callers are responsible for resolving GPS coordinates to place names before
 * calling {@link #generateInspirationalPhrase} so that the resulting phrase
 * contains real location names rather than raw coordinates.</p>
 */
public class TextGenerator {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final GeminiClient geminiClient;

    /**
     * Creates a text generator using a new shared Gemini client.
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
     * Generates a short two-sentence narration for a single media file.
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
     * Generates a short inspirational phrase that references the first and last locations.
     *
     * <p>Pass resolved place names (e.g. "Guadalajara" and "Mexico City") rather
     * than raw coordinate strings so that Gemini can produce a meaningful phrase.</p>
     *
     * @param firstPlaceName human-readable name of the first location
     * @param lastPlaceName  human-readable name of the last location
     * @return an inspirational travel phrase
     * @throws IOException if the AI service cannot generate text
     */
    public String generateInspirationalPhrase(String firstPlaceName, String lastPlaceName)
            throws IOException {
        String prompt = "Write one short inspirational travel phrase in English. "
                + "It must naturally mention both of these real places: \""
                + firstPlaceName + "\" and \"" + lastPlaceName + "\". "
                + "Keep it under 25 words and make it beautiful and suitable for a closing travel video scene.";
        return geminiClient.generateText(prompt).replaceAll("\\s+", " ").trim();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Formats a latitude/longitude pair for use in prompts.
     *
     * @param latitude  the latitude value
     * @param longitude the longitude value
     * @return a human-readable coordinate string
     */
    private String formatCoordinates(double latitude, double longitude) {
        return String.format("%.6f, %.6f", latitude, longitude);
    }
}
