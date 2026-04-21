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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Generates speech audio files using the Gemini TTS endpoint.
 *
 * <p>Gemini returns raw 16-bit PCM (24 kHz, mono). This class wraps those bytes
 * in a proper WAV container so that FFmpegFrameGrabber can decode them reliably.</p>
 */
public class TTSGenerator {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // Gemini TTS output format (fixed by the API)
    private static final int TTS_SAMPLE_RATE = 24_000;
    private static final int TTS_CHANNELS = 1;
    private static final int TTS_BITS_PER_SAMPLE = 16;

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
     * Converts text into a WAV audio file using the Gemini TTS endpoint.
     *
     * @param text       the text to synthesize
     * @param outputPath the destination file path (will be saved as .wav)
     * @return the saved WAV file
     * @throws IOException if synthesis fails or the file cannot be written
     */
    public File generateAudio(String text, String outputPath) throws IOException {
        // Always save as .wav so FFmpeg can decode it correctly
        String wavPath = outputPath.replaceAll("\\.(mp3|ogg|aac)$", "") + ".wav";
        File outputFile = new File(wavPath);
        if (outputFile.getParentFile() != null) {
            Files.createDirectories(outputFile.getParentFile().toPath());
        }

        JsonObject body = buildRequestBody(text);

        Request request = new Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent?key=" + apiKey)
                .post(RequestBody.create(gson.toJson(body), JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() == null ? "" : response.body().string();

            if (response.isSuccessful()) {
                byte[] pcmBytes = extractPcmBytes(responseBody);
                if (pcmBytes != null && pcmBytes.length > 0) {
                    writePcmAsWav(pcmBytes, outputFile);
                    return outputFile;
                }
            }

            // Fallback: write silent WAV
            System.err.println("Warning: TTS fallback to silence. HTTP " + response.code());
            return buildSilentWav(outputFile, 3);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Builds the JSON body for the Gemini TTS request.
     *
     * @param text the text to synthesize
     * @return the request body object
     */
    private JsonObject buildRequestBody(String text) {
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

        return body;
    }

    /**
     * Extracts the raw PCM bytes from the Gemini JSON response.
     *
     * @param responseBody the raw JSON string
     * @return decoded PCM bytes or null if absent
     */
    private byte[] extractPcmBytes(String responseBody) {
        try {
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            if (json == null || !json.has("candidates")) return null;

            JsonArray candidates = json.getAsJsonArray("candidates");
            if (candidates.isEmpty()) return null;

            JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
            if (!firstCandidate.has("content")) return null;

            JsonObject responseContent = firstCandidate.getAsJsonObject("content");
            if (!responseContent.has("parts")) return null;

            JsonArray responseParts = responseContent.getAsJsonArray("parts");
            for (JsonElement element : responseParts) {
                JsonObject responsePart = element.getAsJsonObject();
                if (responsePart.has("inlineData")) {
                    JsonObject inlineData = responsePart.getAsJsonObject("inlineData");
                    if (inlineData.has("data")) {
                        return Base64.getDecoder().decode(inlineData.get("data").getAsString());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: could not parse TTS response: " + e.getMessage());
        }
        return null;
    }

    /**
     * Wraps raw 16-bit PCM bytes in a standard WAV container and writes them to disk.
     *
     * @param pcmBytes   the raw PCM audio data (16-bit, mono, 24 kHz)
     * @param outputFile the file to write
     * @throws IOException if writing fails
     */
    private void writePcmAsWav(byte[] pcmBytes, File outputFile) throws IOException {
        int byteRate = TTS_SAMPLE_RATE * TTS_CHANNELS * (TTS_BITS_PER_SAMPLE / 8);
        int blockAlign = TTS_CHANNELS * (TTS_BITS_PER_SAMPLE / 8);
        int dataChunkSize = pcmBytes.length;
        int totalFileSize = 36 + dataChunkSize;

        ByteBuffer header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        header.put("RIFF".getBytes());
        header.putInt(totalFileSize);
        header.put("WAVE".getBytes());
        header.put("fmt ".getBytes());
        header.putInt(16);                        // PCM chunk size
        header.putShort((short) 1);               // PCM format
        header.putShort((short) TTS_CHANNELS);
        header.putInt(TTS_SAMPLE_RATE);
        header.putInt(byteRate);
        header.putShort((short) blockAlign);
        header.putShort((short) TTS_BITS_PER_SAMPLE);
        header.put("data".getBytes());
        header.putInt(dataChunkSize);

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(header.array());
            fos.write(pcmBytes);
        }
    }

    /**
     * Builds a minimal silent WAV file as fallback when TTS is unavailable.
     *
     * @param outputFile the file to write
     * @param seconds    the duration of silence in seconds
     * @return the silent WAV file
     * @throws IOException if the file cannot be written
     */
    private File buildSilentWav(File outputFile, int seconds) throws IOException {
        int numSamples = TTS_SAMPLE_RATE * seconds * TTS_CHANNELS;
        byte[] silence = new byte[numSamples * (TTS_BITS_PER_SAMPLE / 8)];
        writePcmAsWav(silence, outputFile);
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
