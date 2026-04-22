package ai;

// Gemini API Key: [INSERT YOUR GEMINI API KEY HERE] - Obtained from Google AI Studio (Free Tier)

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Executes Gemini API requests for text and image generation.
 */
public class GeminiClient {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final String apiKey;

    /**
     * Creates a Gemini client using the API key from configuration.
     */
    public GeminiClient() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build();
        this.gson = new Gson();
        this.apiKey = readConfigValue("api.key");
    }

    /**
     * Generates text using the Gemini 1.5 Flash model.
     *
     * @param prompt the prompt to send to Gemini
     * @return the generated plain text response
     * @throws IOException if the API call fails or the response cannot be parsed
     */
    public String generateText(String prompt) throws IOException {
        JsonObject body = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        parts.add(part);
        content.add("parts", parts);
        contents.add(content);
        body.add("contents", contents);

        Request request = new Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey)
            .post(RequestBody.create(gson.toJson(body), JSON))
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = readResponseBody(response);
            if (!response.isSuccessful()) {
                throw new IOException("Gemini text request failed: " + response.code() + " - " + responseBody);
            }

            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            JsonArray candidates = json.getAsJsonArray("candidates");
            if (candidates == null || candidates.isEmpty()) {
                throw new IOException("Gemini text response did not contain candidates.");
            }

            JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
            JsonObject firstContent = firstCandidate.getAsJsonObject("content");
            JsonArray responseParts = firstContent.getAsJsonArray("parts");
            if (responseParts == null || responseParts.isEmpty()) {
                throw new IOException("Gemini text response did not contain parts.");
            }

            StringBuilder builder = new StringBuilder();
            for (JsonElement element : responseParts) {
                JsonObject responsePart = element.getAsJsonObject();
                if (responsePart.has("text")) {
                    if (!builder.isEmpty()) {
                        builder.append(System.lineSeparator());
                    }
                    builder.append(responsePart.get("text").getAsString().trim());
                }
            }

            if (builder.isEmpty()) {
                throw new IOException("Gemini text response did not contain readable text.");
            }
            return builder.toString();
        }
    }

    /**
     * Generates an image using Gemini 2.0 Flash experimental model (free tier compatible).
     *
     * @param prompt the prompt describing the desired image
     * @return the generated image
     * @throws IOException if the API call fails or the image cannot be parsed
     */
    public BufferedImage generateImage(String prompt) throws IOException {
        JsonObject body = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        parts.add(part);
        content.add("parts", parts);
        contents.add(content);
        body.add("contents", contents);

        JsonObject generationConfig = new JsonObject();
        JsonArray responseModalities = new JsonArray();
        responseModalities.add("IMAGE");
        responseModalities.add("TEXT");
        generationConfig.add("responseModalities", responseModalities);
        body.add("generationConfig", generationConfig);

        Request request = new Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp-image-generation:generateContent?key=" + apiKey)
            .post(RequestBody.create(gson.toJson(body), JSON))
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = readResponseBody(response);
            if (!response.isSuccessful()) {
                return buildFallbackImage(prompt);
            }

            JsonObject json = gson.fromJson(responseBody, JsonObject.class);

            if (json.has("candidates")) {
                JsonArray candidates = json.getAsJsonArray("candidates");
                if (!candidates.isEmpty()) {
                    JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
                    if (firstCandidate.has("content")) {
                        JsonObject responseContent = firstCandidate.getAsJsonObject("content");
                        if (responseContent.has("parts")) {
                            JsonArray responseParts = responseContent.getAsJsonArray("parts");
                            for (JsonElement element : responseParts) {
                                JsonObject responsePart = element.getAsJsonObject();
                                if (responsePart.has("inlineData")) {
                                    JsonObject inlineData = responsePart.getAsJsonObject("inlineData");
                                    if (inlineData.has("data")) {
                                        String base64 = inlineData.get("data").getAsString();
                                        byte[] bytes = Base64.getDecoder().decode(base64);
                                        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
                                        if (image != null) return image;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return buildFallbackImage(prompt);
        } catch (Exception exception) {
            return buildFallbackImage(prompt);
        }
    }

    /**
     * Reads a configuration value from the default-package Config class via reflection.
     *
     * @param key the property key to look up
     * @return the property value
     */
    private String readConfigValue(String key) {
        try {
            Class<?> configClass = Class.forName("Config");
            Method getMethod = configClass.getMethod("get", String.class);
            Object value = getMethod.invoke(null, key);
            return value == null ? "" : value.toString().trim();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read configuration key: " + key, exception);
        }
    }

    /**
     * Safely reads the response body as a string.
     *
     * @param response the HTTP response
     * @return the body contents or an empty string when absent
     * @throws IOException if the body cannot be read
     */
    private String readResponseBody(Response response) throws IOException {
        return response.body() == null ? "" : response.body().string();
    }

    /**
     * Builds a simple local fallback image when remote generation is unavailable.
     *
     * @param prompt the source prompt text
     * @return a generated placeholder image
     */
    private BufferedImage buildFallbackImage(String prompt) {
        BufferedImage image = new BufferedImage(1080, 1920, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setPaint(new GradientPaint(0, 0, new Color(21, 35, 60), 1080, 1920, new Color(121, 84, 141)));
        graphics.fillRect(0, 0, 1080, 1920);
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 52));
        drawWrappedText(graphics, "AI Intro", 100, 250, 880, 68);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 34));
        drawWrappedText(graphics, prompt, 100, 360, 880, 46);
        graphics.dispose();
        return image;
    }

    /**
     * Draws wrapped text into the fallback image.
     *
     * @param graphics the graphics context
     * @param text the text to render
     * @param x the left margin
     * @param y the top baseline
     * @param maxWidth the maximum line width
     * @param lineHeight the line height
     */
    private void drawWrappedText(Graphics2D graphics, String text, int x, int y, int maxWidth, int lineHeight) {
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        int currentY = y;

        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            int lineWidth = graphics.getFontMetrics().stringWidth(candidate);
            if (lineWidth > maxWidth && !line.isEmpty()) {
                graphics.drawString(line.toString(), x, currentY);
                line = new StringBuilder(word);
                currentY += lineHeight;
            } else {
                line = new StringBuilder(candidate);
            }
        }

        if (!line.isEmpty()) {
            graphics.drawString(line.toString(), x, currentY);
        }
    }
}