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
 * Downloads OpenStreetMap tiles and composes a portrait map image with two
 * distinct markers (START = green, END = red) and an AI-generated phrase overlay.
 *
 * <p>All map math and tile fetching is programmatic — no AI is involved in
 * rendering the map itself. The inspirational phrase is generated elsewhere
 * and passed in as a plain string.</p>
 */
public class MapRenderer {

    private static final int TILE_SIZE     = 256;
    private static final int OUTPUT_WIDTH  = 1080;
    private static final int OUTPUT_HEIGHT = 1920;
    private static final int ZOOM          = 5;

    /**
     * Renders a portrait map image with a green start pin, a red end pin,
     * and an inspirational phrase overlaid at the bottom.
     *
     * @param lat1   latitude of the first (start) location
     * @param lon1   longitude of the first (start) location
     * @param lat2   latitude of the last (end) location
     * @param lon2   longitude of the last (end) location
     * @param phrase the AI-generated phrase to overlay
     * @return the composed portrait map image (1080 x 1920)
     * @throws IOException if tiles cannot be fetched
     */
    public BufferedImage renderMap(double lat1, double lon1,
                                   double lat2, double lon2,
                                   String phrase) throws IOException {

        double centerLat = (lat1 + lat2) / 2.0;
        double centerLon = (lon1 + lon2) / 2.0;

        int tilesX = (int) Math.ceil((double) OUTPUT_WIDTH  / TILE_SIZE) + 2;
        int tilesY = (int) Math.ceil((double) OUTPUT_HEIGHT / TILE_SIZE) + 2;

        int centerTileX = lonToTileX(centerLon, ZOOM);
        int centerTileY = latToTileY(centerLat, ZOOM);
        int startTileX  = centerTileX - tilesX / 2;
        int startTileY  = centerTileY - tilesY / 2;

        int canvasW = tilesX * TILE_SIZE;
        int canvasH = tilesY * TILE_SIZE;
        BufferedImage canvas = new BufferedImage(canvasW, canvasH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        g.setColor(new Color(200, 200, 200));
        g.fillRect(0, 0, canvasW, canvasH);

        for (int tx = 0; tx < tilesX; tx++) {
            for (int ty = 0; ty < tilesY; ty++) {
                BufferedImage tile = fetchTile(startTileX + tx, startTileY + ty, ZOOM);
                g.drawImage(tile, tx * TILE_SIZE, ty * TILE_SIZE, null);
            }
        }

        int px1 = geoToPixelX(lon1, startTileX);
        int py1 = geoToPixelY(lat1, startTileY);
        int px2 = geoToPixelX(lon2, startTileX);
        int py2 = geoToPixelY(lat2, startTileY);

        drawPin(g, px1, py1, new Color(34, 197, 94), "START");
        drawPin(g, px2, py2, new Color(220, 38, 38), "END");
        g.dispose();

        int cpx   = geoToPixelX(centerLon, startTileX);
        int cpy   = geoToPixelY(centerLat, startTileY);
        int cropX = Math.max(0, Math.min(cpx - OUTPUT_WIDTH  / 2, canvasW - OUTPUT_WIDTH));
        int cropY = Math.max(0, Math.min(cpy - OUTPUT_HEIGHT / 2, canvasH - OUTPUT_HEIGHT));
        if (cropX + OUTPUT_WIDTH  > canvasW) cropX = Math.max(0, canvasW - OUTPUT_WIDTH);
        if (cropY + OUTPUT_HEIGHT > canvasH) cropY = Math.max(0, canvasH - OUTPUT_HEIGHT);

        BufferedImage portrait;
        if (canvasW >= OUTPUT_WIDTH && canvasH >= OUTPUT_HEIGHT) {
            BufferedImage sub = canvas.getSubimage(cropX, cropY, OUTPUT_WIDTH, OUTPUT_HEIGHT);
            portrait = new BufferedImage(OUTPUT_WIDTH, OUTPUT_HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D pg = portrait.createGraphics();
            pg.drawImage(sub, 0, 0, null);
            pg.dispose();
        } else {
            portrait = new BufferedImage(OUTPUT_WIDTH, OUTPUT_HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D pg = portrait.createGraphics();
            pg.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            pg.drawImage(canvas, 0, 0, OUTPUT_WIDTH, OUTPUT_HEIGHT, null);
            pg.dispose();
        }

        overlayPhrase(portrait, phrase);
        return portrait;
    }

    // Map math

    private int lonToTileX(double lon, int zoom) {
        return (int) Math.floor((lon + 180.0) / 360.0 * (1 << zoom));
    }

    private int latToTileY(double lat, int zoom) {
        double r = Math.toRadians(lat);
        return (int) Math.floor(
                (1.0 - Math.log(Math.tan(r) + 1.0 / Math.cos(r)) / Math.PI) / 2.0 * (1 << zoom));
    }

    private int geoToPixelX(double lon, int startTileX) {
        return (int) (((lon + 180.0) / 360.0 * (1 << ZOOM)) - startTileX) * TILE_SIZE;
    }

    private int geoToPixelY(double lat, int startTileY) {
        double r = Math.toRadians(lat);
        double t = (1.0 - Math.log(Math.tan(r) + 1.0 / Math.cos(r)) / Math.PI) / 2.0 * (1 << ZOOM);
        return (int) ((t - startTileY) * TILE_SIZE);
    }

    /**
     * Downloads a single OSM tile. Returns a gray placeholder on failure.
     *
     * @param x    tile X index
     * @param y    tile Y index
     * @param zoom zoom level
     * @return the tile image, never null
     */
    private BufferedImage fetchTile(int x, int y, int zoom) {
        int max = 1 << zoom;
        x = ((x % max) + max) % max;
        y = ((y % max) + max) % max;
        String[] sub = {"a", "b", "c"};
        String urlStr = "https://" + sub[(x + y) % 3] + ".tile.openstreetmap.org/"
                + zoom + "/" + x + "/" + y + ".png";
        try {
            URL url  = URI.create(urlStr).toURL();
            var conn = url.openConnection();
            conn.setRequestProperty("User-Agent",
                    "AI-Travel-Video-Generator/1.0 (educational project)");
            conn.setConnectTimeout(8_000);
            conn.setReadTimeout(8_000);
            BufferedImage tile = ImageIO.read(conn.getInputStream());
            return tile != null ? tile : buildGrayTile();
        } catch (Exception e) {
            System.err.println("Warning: tile fetch failed: " + e.getMessage());
            return buildGrayTile();
        }
    }

    /** Returns a plain gray 256x256 tile as fallback. */
    private BufferedImage buildGrayTile() {
        BufferedImage t = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = t.createGraphics();
        g.setColor(new Color(210, 210, 210));
        g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
        g.dispose();
        return t;
    }

    /**
     * Draws a teardrop-style map pin with a text label above it.
     *
     * @param g     graphics context
     * @param px    pin tip X
     * @param py    pin tip Y
     * @param color pin fill color
     * @param label label text (START or END)
     */
    private void drawPin(Graphics2D g, int px, int py, Color color, String label) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int r = 28, tipLen = 20;

        g.setColor(new Color(0, 0, 0, 80));
        g.fillOval(px - r + 4, py - r * 2 - tipLen + 4, r * 2, r * 2);

        g.setColor(color);
        g.fillOval(px - r, py - r * 2 - tipLen, r * 2, r * 2);

        int[] xs = {px - r / 2, px + r / 2, px};
        int[] ys = {py - tipLen - r, py - tipLen - r, py};
        g.fillPolygon(xs, ys, 3);

        g.setColor(Color.WHITE);
        int dr = r / 2;
        g.fill(new Ellipse2D.Float(px - dr, py - dr - r - tipLen, dr * 2, dr * 2));

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        FontMetrics fm = g.getFontMetrics();
        int lx = px - fm.stringWidth(label) / 2;
        int ly = py - r * 2 - tipLen - 10;
        g.setColor(new Color(0, 0, 0, 160));
        g.drawString(label, lx + 2, ly + 2);
        g.setColor(Color.WHITE);
        g.drawString(label, lx, ly);
    }

    /**
     * Overlays the inspirational phrase at the bottom of the image with
     * a semi-transparent dark strip for readability.
     *
     * @param image  the image to draw on
     * @param phrase the text to render
     */
    private void overlayPhrase(BufferedImage image, String phrase) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int margin   = 70;
        int maxWidth = image.getWidth() - margin * 2;
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 52));

        List<String> lines = wrapText(g, phrase, maxWidth);
        FontMetrics fm     = g.getFontMetrics();
        int lineH          = fm.getHeight();
        int blockH         = lines.size() * lineH + 40;

        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(0, image.getHeight() - blockH - 80, image.getWidth(), blockH + 80);

        int baseY = image.getHeight() - 80 - (lines.size() - 1) * lineH;
        for (int i = 0; i < lines.size(); i++) {
            String ln = lines.get(i);
            int x = (image.getWidth() - fm.stringWidth(ln)) / 2;
            int y = baseY + i * lineH;
            g.setColor(new Color(0, 0, 0, 200));
            g.drawString(ln, x + 3, y + 3);
            g.setColor(Color.WHITE);
            g.drawString(ln, x, y);
        }
        g.dispose();
    }

    /**
     * Word-wraps text to fit within the given pixel width.
     *
     * @param g        graphics context for font measurements
     * @param text     the text to wrap
     * @param maxWidth maximum line width in pixels
     * @return ordered list of wrapped lines
     */
    private List<String> wrapText(Graphics2D g, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        FontMetrics fm     = g.getFontMetrics();
        String[] words     = text.split("\\s+");
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

