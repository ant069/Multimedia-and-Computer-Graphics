package map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLConnection;
import java.util.stream.Collectors;

/**
 * Converts GPS coordinates to human-readable place names using the
 * OpenStreetMap Nominatim reverse-geocoding API.
 *
 * <p>This is a purely programmatic operation — no AI is involved.</p>
 */
public class GeocodingService {

    private static final String NOMINATIM_URL =
            "https://nominatim.openstreetmap.org/reverse?format=json&lat=%s&lon=%s";

    private final Gson gson = new Gson();

    /**
     * Resolves a latitude/longitude pair to a readable location name.
     *
     * <p>The method tries progressively coarser fields until it finds a non-empty
     * value: city → town → village → county → state → country. If Nominatim is
     * unreachable the raw coordinate string is returned as a safe fallback.</p>
     *
     * @param lat latitude
     * @param lon longitude
     * @return a human-readable location name, never null
     */
    public String resolveName(double lat, double lon) {
        try {
            String url = String.format(NOMINATIM_URL, lat, lon);
            String json = fetchJson(url);
            JsonObject root = gson.fromJson(json, JsonObject.class);

            if (root == null || !root.has("address")) {
                return fallback(lat, lon);
            }

            JsonObject address = root.getAsJsonObject("address");

            // Try fields from most to least specific
            for (String field : new String[]{
                    "city", "town", "village", "municipality",
                    "county", "state_district", "state", "country"}) {
                if (address.has(field)) {
                    String value = address.get(field).getAsString().trim();
                    if (!value.isEmpty()) {
                        return value;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Warning: reverse geocoding failed for "
                    + lat + "," + lon + ": " + e.getMessage());
        }

        return fallback(lat, lon);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Fetches JSON from a URL, setting a proper User-Agent for Nominatim policy.
     *
     * @param urlStr the URL to fetch
     * @return the raw JSON response body
     * @throws IOException if the request fails
     */
    private String fetchJson(String urlStr) throws IOException {
        URLConnection connection = URI.create(urlStr).toURL().openConnection();
        connection.setRequestProperty("User-Agent",
                "AI-Travel-Video-Generator/1.0 (educational project)");
        connection.setConnectTimeout(8_000);
        connection.setReadTimeout(8_000);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()))) {
            return reader.lines().collect(Collectors.joining());
        }
    }

    /**
     * Returns a minimal coordinate string when geocoding is unavailable.
     *
     * @param lat latitude
     * @param lon longitude
     * @return formatted coordinate fallback
     */
    private String fallback(double lat, double lon) {
        return String.format("%.4f°, %.4f°", lat, lon);
    }
}
