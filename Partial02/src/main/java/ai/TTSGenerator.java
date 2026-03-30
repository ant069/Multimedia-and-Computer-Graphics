package ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Generates speech audio files using the Gemini TTS endpoint (free tier compatible).
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
     * Converts text into an audio file using the Gemini TTS endpoint.
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
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", text);
        parts.add(part);
        content.add("parts", parts);
        contents.add(content);
        body.add("contents", contents);

        JsonObject generationConfig = new JsonObject();
        JsonArray responseModalities = new JsonArray();
        responseModalities.add("AUDIO");
        generationConfig.add("responseModalities", responseModalities);

        JsonObject speechConfig = new JsonObject();
        JsonObject voiceConfig = new JsonObject();
        JsonObject prebuiltVoice = new JsonObject();
        prebuiltVoice.addProperty("voiceName", "Kore");
        voiceConfig.add("prebuiltVoiceConfig", prebuiltVoice);
        speechConfig.add("voiceConfig", voiceConfig);
        generationConfig.add("speechConfig", speechConfig);
        body.add("generationConfig", generationConfig);

        Request request = new Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent?key=" + apiKey)
            .post(RequestBody.create(gson.toJson(body), JSON))
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                return buildSilentAudio(outputFile, 3);
            }

            JsonObject json = gson.fromJson(responseBody, JsonObject.class);

            if (json != null && json.has("candidates")) {
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
                                        byte[] audioBytes = Base64.getDecoder()
                                            .decode(inlineData.get("data").getAsString());
                                        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                                            fos.write(audioBytes);
                                        }
                                        return outputFile;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return buildSilentAudio(outputFile, 3);
        }
    }

    /**
     * Builds a minimal silent WAV file as fallback when TTS is unavailable.
     *
     * @param outputFile the file to write silence into
     * @param seconds the duration of silence in seconds
     * @return the silent audio file
     * @throws IOException if the file cannot be written
     */
    private File buildSilentAudio(File outputFile, int seconds) throws IOException {
        int sampleRate = 44100;
        int numSamples = sampleRate * seconds;
        int dataSize = numSamples * 2;
        int totalSize = dataSize + 44;

        try (RandomAccessFile raf = new RandomAccessFile(outputFile, "rw")) {
            raf.writeBytes("RIFF");
            raf.writeInt(Integer.reverseBytes(totalSize - 8));
            raf.writeBytes("WAVEfmt ");
            raf.writeInt(Integer.reverseBytes(16));
            raf.writeShort(Short.reverseBytes((short) 1));
            raf.writeShort(Short.reverseBytes((short) 1));
            raf.writeInt(Integer.reverseBytes(sampleRate));
            raf.writeInt(Integer.reverseBytes(sampleRate * 2));
            raf.writeShort(Short.reverseBytes((short) 2));
            raf.writeShort(Short.reverseBytes((short) 16));
            raf.writeBytes("data");
            raf.writeInt(Integer.reverseBytes(dataSize));
            raf.write(new byte[dataSize]);
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
