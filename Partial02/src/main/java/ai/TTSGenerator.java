package ai;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Generates speech audio files using the Google Cloud Text-to-Speech REST API.
 */
public class TTSGenerator {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final String apiKey;

    /**
     * Creates a TTS generator using the API key from configuration.
     */
    public TTSGenerator() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build();
        this.gson = new Gson();
        this.apiKey = readConfigValue("api.key");
    }

    /**
     * Converts text into an MP3 file using Google Cloud TTS.
     *
     * @param text the text to synthesize
     * @param outputPath the destination file path
     * @return the saved audio file
     * @throws IOException if synthesis fails or the file cannot be written
     */
    public File generateAudio(String text, String outputPath) throws IOException {
        File outputFile = new File(outputPath);
        if (outputFile.getParentFile() != null) {
            Files.createDirectories(outputFile.getParentFile().toPath());
        }

        JsonObject body = new JsonObject();
        JsonObject input = new JsonObject();
        input.addProperty("text", text);
        body.add("input", input);

        JsonObject voice = new JsonObject();
        voice.addProperty("languageCode", "en-US");
        voice.addProperty("name", "en-US-Standard-C");
        body.add("voice", voice);

        JsonObject audioConfig = new JsonObject();
        audioConfig.addProperty("audioEncoding", "MP3");
        audioConfig.addProperty("speakingRate", 1.0d);
        body.add("audioConfig", audioConfig);

        Request request = new Request.Builder()
            .url("https://texttospeech.googleapis.com/v1/text:synthesize?key=" + apiKey)
            .post(RequestBody.create(gson.toJson(body), JSON))
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                throw new IOException("TTS request failed: " + response.code() + " - " + responseBody);
            }

            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            if (json == null || !json.has("audioContent")) {
                throw new IOException("TTS response did not contain audio content.");
            }

            byte[] audioBytes = Base64.getDecoder().decode(json.get("audioContent").getAsString());
            try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                outputStream.write(audioBytes);
            }
        }

        return outputFile;
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
}
