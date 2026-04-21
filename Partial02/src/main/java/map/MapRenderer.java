package map;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Downloads OpenStreetMap tiles and composes a portrait map with two distinct markers and a phrase overlay.
 */
public class MapRenderer {

    private static final int TILE_SIZE = 256;
    private static final int OUTPUT_WIDTH = 1080;
    private static final int OUTPUT_HEIGHT = 1920;
    private static final int ZOOM = 5;

    /**
     * Renders a portrait map image with a green start pin, red end pin, and an inspirational phrase.
     *
     * @param lat1   latitude of the first (start) location
     * @param lon1   longitude of the first (start) location
     * @param lat2   latitude of the last (end) location
     * @param lon2   longitude of the last (end) location
     * @param phrase the AI-generated phrase to overlay at the bottom
     * @return the composed map image
     * @throws IOException if tiles cannot be fetched
     */
    public BufferedImage renderMap(double lat1, double lon1, double lat2, double lon2, String phrase)
            throws IOException {

        // Use midpoint as map center
        double centerLat = (lat1 + lat2) / 2.0;
        double centerLon = (lon1 + lon2) / 2.0;

        // How many tiles we need to cover the output dimensions
        int tilesX = (int) Math.ceil((double) OUTPUT_WIDTH / TILE_SIZE) + 2;
        int tilesY = (int) Math.ceil((double) OUTPUT_HEIGHT / TILE_SIZE) + 2;

        // Center tile
        int centerTileX = lonToTileX(centerLon, ZOOM);
        int centerTileY = latToTileY(centerLat, ZOOM);

        int startTileX = centerTileX - tilesX / 2;
        int startTileY = centerTileY - tilesY / 2;

        // Build the canvas
        int canvasWidth = tilesX * TILE_SIZE;
        int canvasHeight = tilesY * TILE_SIZE;
        BufferedImage canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        g.setColor(new Color(200, 200, 200));
        g.fillRect(0, 0, canvasWidth, canvasHeight);

        // Draw tiles
        for (int tx = 0; tx < tilesX; tx++) {
            for (int ty = 0; ty < tilesY; ty++) {
                int tileX = startTileX + tx;
                int tileY = startTileY + ty;
                BufferedImage tile = fetchTile(tileX, tileY, ZOOM);
                if (tile != null) {
                    g.drawImage(tile, tx * TILE_SIZE, ty * TILE_SIZE, null);
                }
            }
        }

        // Pixel positions of the two markers on the canvas
        int px1 = geoToPixelX(lon1, startTileX);
        int py1 = geoToPixelY(lat1, startTileY);
        int px2 = geoToPixelX(lon2, startTileX);
        int py2 = geoToPixelY(lat2, startTileY);

        drawPin(g, px1, py1, new Color(34, 197, 94), "START");   // green
        drawPin(g, px2, py2, new Color(220, 38, 38), "END");     // red

        g.dispose();

        // Crop to portrait centered around the map center
        int centerPixelX = geoToPixelX(centerLon, startTileX);
        int centerPixelY = geoToPixelY(centerLat, startTileY);
        int cropX = Math.max(0, Math.min(centerPixelX - OUTPUT_WIDTH / 2, canvasWidth - OUTPUT_WIDTH));
        int cropY = Math.max(0, Math.min(centerPixelY - OUTPUT_HEIGHT / 2, canvasHeight - OUTPUT_HEIGHT));

        // Clamp crop to canvas bounds
        if (cropX + OUTPUT_WIDTH > canvasWidth) cropX = Math.max(0, canvasWidth - OUTPUT_WIDTH);
        if (cropY + OUTPUT_HEIGHT > canvasHeight) cropY = Math.max(0, canvasHeight - OUTPUT_HEIGHT);

        BufferedImage portrait;
        if (canvasWidth >= OUTPUT_WIDTH && canvasHeight >= OUTPUT_HEIGHT) {
            portrait = canvas.getSubimage(cropX, cropY, OUTPUT_WIDTH, OUTPUT_HEIGHT);
            BufferedImage copy = new BufferedImage(OUTPUT_WIDTH, OUTPUT_HEIGHT, BufferedImage.TYPE_INT_RGB);
            copy.createGraphics().drawImage(portrait, 0, 0, null);
            portrait = copy;
        } else {
            // Scale up if tiles didn't cover the full output
            portrait = new BufferedImage(OUTPUT_WIDTH, OUTPUT_HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D pg = portrait.createGraphics();
            pg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            pg.drawImage(canvas, 0, 0, OUTPUT_WIDTH, OUTPUT_HEIGHT, null);
            pg.dispose();
        }

        // Overlay the phrase
        overlayPhrase(portrait, phrase);
        return portrait;
    }

    // ── Map math ──────────────────────────────────────────────────────────────

    private int lonToTileX(double lon, int zoom) {
        return (int) Math.floor((lon + 180.0) / 360.0 * (1 << zoom));
    }

    private int latToTileY(double lat, int zoom) {
        double latRad = Math.toRadians(lat);
        return (int) Math.floor((1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * (1 << zoom));
    }

    private int geoToPixelX(double lon, int startTileX) {
        double tileF = (lon + 180.0) / 360.0 * (1 << ZOOM);
        return (int) ((tileF - startTileX) * TILE_SIZE);
    }

    private int geoToPixelY(double lat, int startTileY) {
        double latRad = Math.toRadians(lat);
        double tileF = (1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * (1 << ZOOM);
        return (int) ((tileF - startTileY) * TILE_SIZE);
    }

    // ── Tile fetching ─────────────────────────────────────────────────────────

    /**
     * Downloads a single OSM tile with a browser-like User-Agent to avoid 403 responses.
     *
     * @param x    tile X
     * @param y    tile Y
     * @param zoom zoom level
     * @return the tile image or null on failure
     */
    private BufferedImage fetchTile(int x, int y, int zoom) {
        int maxTile = 1 << zoom;
        x = ((x % maxTile) + maxTile) % maxTile;
        y = ((y % maxTile) + maxTile) % maxTile;

        String[] servers = {"a", "b", "c"};
        String server = servers[(x + y) % servers.length];
        String urlStr = "https://" + server + ".tile.openstreetmap.org/" + zoom + "/" + x + "/" + y + ".png";

        try {
            URL url = URI.create(urlStr).toURL();
            var connection = url.openConnection();
            connection.setRequestProperty("User-Agent", "AI-Travel-Video-Generator/1.0 (educational project)");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            return ImageIO.read(connection.getInputStream());
        } catch (Exception e) {
            System.err.println("Warning: could not fetch tile " + urlStr + ": " + e.getMessage());
            return buildGrayTile();
        }
    }

    /** Returns a plain gray tile as a fallback. */
    private BufferedImage buildGrayTile() {
        BufferedImage tile = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = tile.createGraphics();
        g.setColor(new Color(210, 210, 210));
        g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
        g.dispose();
        return tile;
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    /**
     * Draws a teardrop-style map pin with a label.
     *
     * @param g     graphics context
     * @param px    pin tip X
     * @param py    pin tip Y
     * @param color pin color
     * @param label short label (START / END)
     */
    private void drawPin(Graphics2D g, int px, int py, Color color, String label) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int r = 28;          // circle radius
        int tipLen = 20;     // spike below circle

        // Shadow
        g.setColor(new Color(0, 0, 0, 80));
        g.fillOval(px - r + 4, py - r * 2 - tipLen + 4, r * 2, r * 2);

        // Circle body
        g.setColor(color);
        g.fillOval(px - r, py - r * 2 - tipLen, r * 2, r * 2);

        // Spike (triangle)
        int[] xPoints = {px - r / 2, px + r / 2, px};
        int[] yPoints = {py - tipLen - r, py - tipLen - r, py};
        g.fillPolygon(xPoints, yPoints, 3);

        // White inner dot
        g.setColor(Color.WHITE);
        int dotR = r / 2;
        g.fill(new Ellipse2D.Float(px - dotR, py - dotR - r - tipLen, dotR * 2, dotR * 2));

        // Label above pin
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        FontMetrics fm = g.getFontMetrics();
        int lw = fm.stringWidth(label);
        int lx = px - lw / 2;
        int ly = py - r * 2 - tipLen - 10;

        // Label shadow
        g.setColor(new Color(0, 0, 0, 160));
        g.drawString(label, lx + 2, ly + 2);
        g.setColor(Color.WHITE);
        g.drawString(label, lx, ly);
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

        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }
}
