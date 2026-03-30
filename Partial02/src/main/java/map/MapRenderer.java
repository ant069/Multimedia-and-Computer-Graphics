package map;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Downloads a static OpenStreetMap image and overlays a closing phrase.
 */
public class MapRenderer {
    /**
     * Renders a static map with two markers and an inspirational phrase.
     *
     * @param lat1 latitude for the first marker
     * @param lon1 longitude for the first marker
     * @param lat2 latitude for the second marker
     * @param lon2 longitude for the second marker
     * @param phrase the phrase to overlay on the map
     * @return the final map image with text overlay
     * @throws IOException if the map cannot be downloaded or rendered
     */
    public BufferedImage renderMap(double lat1, double lon1, double lat2, double lon2, String phrase) throws IOException {
        String url = "https://staticmap.openstreetmap.de/staticmap.php?size=1080x1920&maptype=mapnik"
            + "&markers=" + encodeMarker(lat1, lon1, "lightgreen1")
            + "&markers=" + encodeMarker(lat2, lon2, "red")
            + "&zoom=4";

        BufferedImage mapImage = ImageIO.read(new URL(url));
        if (mapImage == null) {
            throw new IOException("Could not load static map image.");
        }

        Graphics2D graphics = mapImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 56));

        List<String> lines = wrapText(graphics, phrase, mapImage.getWidth() - 140);
        FontMetrics metrics = graphics.getFontMetrics();
        int totalHeight = lines.size() * metrics.getHeight();
        int baseY = mapImage.getHeight() - 120 - totalHeight;

        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            int x = (mapImage.getWidth() - metrics.stringWidth(line)) / 2;
            int y = baseY + (index + 1) * metrics.getHeight();
            graphics.setColor(new Color(0, 0, 0, 180));
            graphics.drawString(line, x + 3, y + 3);
            graphics.setColor(Color.WHITE);
            graphics.drawString(line, x, y);
        }

        graphics.dispose();
        return mapImage;
    }

    /**
     * Encodes a marker parameter for the static map URL.
     *
     * @param latitude the marker latitude
     * @param longitude the marker longitude
     * @param color the marker color
     * @return the encoded marker string
     */
    private String encodeMarker(double latitude, double longitude, String color) {
        String marker = latitude + "," + longitude + "," + color;
        return URLEncoder.encode(marker, StandardCharsets.UTF_8);
    }

    /**
     * Wraps text to fit within the specified width.
     *
     * @param graphics the graphics context used to measure text
     * @param text the text to wrap
     * @param maxWidth the maximum line width
     * @return the wrapped lines
     */
    private List<String> wrapText(Graphics2D graphics, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String candidate = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (graphics.getFontMetrics().stringWidth(candidate) > maxWidth && !currentLine.isEmpty()) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                currentLine = new StringBuilder(candidate);
            }
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }

        return lines;
    }
}
