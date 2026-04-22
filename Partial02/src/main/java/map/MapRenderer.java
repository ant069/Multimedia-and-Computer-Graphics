package map;

import ai.ImageGenerator;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates an AI-created map image with markers and overlays an inspirational phrase.
 */
public class MapRenderer {

    private static final int OUTPUT_WIDTH = 1080;
    private static final int OUTPUT_HEIGHT = 1920;

    /**
     * Renders a portrait map image with a green start pin, red end pin, and an inspirational phrase.
     *
     * @param lat1   latitude of the first (start) location
     * @param lon1   longitude of the first (start) location
     * @param lat2   latitude of the last (end) location
     * @param lon2   longitude of the last (end) location
     * @param phrase the AI-generated phrase to overlay at the bottom
     * @param imageGenerator the AI image generator to create the map
     * @return the composed map image
     * @throws IOException if image generation fails
     */
    public BufferedImage renderMap(double lat1, double lon1, double lat2, double lon2, String phrase, ImageGenerator imageGenerator)
            throws IOException {

        // Generate the map image using AI
        BufferedImage mapImage = imageGenerator.generateMapImage(lat1, lon1, lat2, lon2);

        // Ensure the image is the correct size (portrait)
        if (mapImage.getWidth() != OUTPUT_WIDTH || mapImage.getHeight() != OUTPUT_HEIGHT) {
            BufferedImage resized = new BufferedImage(OUTPUT_WIDTH, OUTPUT_HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.drawImage(mapImage, 0, 0, OUTPUT_WIDTH, OUTPUT_HEIGHT, null);
            g.dispose();
            mapImage = resized;
        }

        // Overlay the phrase
        overlayPhrase(mapImage, phrase);
        return mapImage;
    }

    /**
     * Overlays the inspirational phrase at the bottom of the portrait map image.
     *
     * @param image  the image to overlay on
     * @param phrase the text to render
     */
    private void overlayPhrase(BufferedImage image, String phrase) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int margin = 70;
        int maxWidth = image.getWidth() - margin * 2;
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 52));

        List<String> lines = wrapText(g, phrase, maxWidth);
        FontMetrics fm = g.getFontMetrics();
        int lineH = fm.getHeight();
        int blockH = lines.size() * lineH + 40;

        // Semi-transparent background strip
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRoundRect(0, image.getHeight() - blockH - 80, image.getWidth(), blockH + 80, 0, 0);

        int baseY = image.getHeight() - 80 - (lines.size() - 1) * lineH;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int x = (image.getWidth() - fm.stringWidth(line)) / 2;
            int y = baseY + i * lineH;
            // Shadow
            g.setColor(new Color(0, 0, 0, 200));
            g.drawString(line, x + 3, y + 3);
            // Text
            g.setColor(Color.WHITE);
            g.drawString(line, x, y);
        }

        g.dispose();
    }

    /**
     * Wraps a text string to fit within maxWidth pixels.
     *
     * @param g        graphics context for measurement
     * @param text     the text to wrap
     * @param maxWidth the maximum pixel width per line
     * @return list of wrapped lines
     */
    private List<String> wrapText(Graphics2D g, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        FontMetrics fm = g.getFontMetrics();
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(candidate) > maxWidth && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }
}
