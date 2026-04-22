package audio;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalizes audio files to the YouTube loudness standard using FFmpeg's loudnorm filter.
 *
 * <p>Target values (matching YouTube / EBU R128):</p>
 * <ul>
 *   <li>Integrated loudness: -15 LUFS (within the -16 to -14 LUFS range)</li>
 *   <li>True peak: -1.5 dBTP (within the -2 to -1 dBTP range)</li>
 *   <li>Loudness range: 7 LU (within the 5 to 10 LU range)</li>
 * </ul>
 *
 * <p>Processing is done entirely with FFmpeg — no AI is involved.</p>
 */
public class AudioNormalizer {

    // YouTube standard target values
    private static final double TARGET_LUFS  = -15.0;
    private static final double TARGET_TP    = -1.5;
    private static final double TARGET_LRA   = 7.0;

    // Regex patterns to parse loudnorm analysis output
    private static final Pattern LUFS_PATTERN = Pattern.compile("\"input_i\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern TP_PATTERN   = Pattern.compile("\"input_tp\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern LRA_PATTERN  = Pattern.compile("\"input_lra\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern THRESH_PATTERN = Pattern.compile("\"input_thresh\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern OFFSET_PATTERN = Pattern.compile("\"target_offset\"\\s*:\\s*\"([^\"]+)\"");

    /**
     * Normalizes an audio WAV file in two passes (EBU R128 linear loudnorm).
     *
     * <p>If FFmpeg is not installed or the file cannot be processed, the original
     * file is returned unchanged so the rest of the pipeline continues.</p>
     *
     * @param inputFile the WAV file to normalize
     * @return the normalized WAV file (may be the same object if normalization fails)
     * @throws IOException if the temporary output file cannot be created
     */
    public File normalize(File inputFile) throws IOException {
        if (inputFile == null || !inputFile.exists() || !isFfmpegAvailable()) {
            return inputFile;
        }

        File outputFile = buildOutputPath(inputFile);

        try {
            LoudnessStats stats = measureLoudness(inputFile);
            applyNormalization(inputFile, outputFile, stats);
            return outputFile;
        } catch (Exception e) {
            System.err.println("Warning: Audio normalization failed (" + e.getMessage() + "), using original.");
            return inputFile;
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Checks whether FFmpeg is available on the system PATH.
     *
     * @return true if FFmpeg responds to -version
     */
    private boolean isFfmpegAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            Process p = pb.start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Builds the output file path by appending "_norm" before the extension.
     *
     * @param input the source file
     * @return the output file handle (not yet created)
     */
    private File buildOutputPath(File input) {
        String name = input.getName();
        int dot = name.lastIndexOf('.');
        String base = (dot > 0) ? name.substring(0, dot) : name;
        String ext  = (dot > 0) ? name.substring(dot)    : ".wav";
        return new File(input.getParentFile(), base + "_norm" + ext);
    }

    /**
     * Runs a first-pass loudnorm measurement and returns the parsed statistics.
     *
     * @param inputFile the file to analyze
     * @return measured loudness statistics
     * @throws IOException if FFmpeg fails or the output cannot be parsed
     */
    private LoudnessStats measureLoudness(File inputFile) throws IOException {
        List<String> cmd = new ArrayList<>(List.of(
                "ffmpeg", "-y", "-i", inputFile.getAbsolutePath(),
                "-af", String.format(
                        "loudnorm=I=%.1f:TP=%.1f:LRA=%.1f:print_format=json",
                        TARGET_LUFS, TARGET_TP, TARGET_LRA),
                "-f", "null", "-"
        ));

        String output = runFfmpeg(cmd);
        return parseLoudnessStats(output);
    }

    /**
     * Runs the second-pass normalization using measured stats.
     *
     * @param inputFile  the source audio file
     * @param outputFile the normalized output file
     * @param stats      the loudness statistics from the first pass
     * @throws IOException if FFmpeg fails
     */
    private void applyNormalization(File inputFile, File outputFile, LoudnessStats stats)
            throws IOException {
        List<String> cmd = new ArrayList<>(List.of(
                "ffmpeg", "-y", "-i", inputFile.getAbsolutePath(),
                "-af", String.format(
                        "loudnorm=I=%.1f:TP=%.1f:LRA=%.1f:measured_I=%.6f:measured_TP=%.6f:measured_LRA=%.6f:measured_thresh=%.6f:offset=%.6f:linear=true",
                        TARGET_LUFS, TARGET_TP, TARGET_LRA,
                        stats.inputI, stats.inputTp, stats.inputLra, stats.inputThresh, stats.targetOffset),
                "-ar", "24000", "-ac", "1",  // Ensure consistent format for JavaCV
                outputFile.getAbsolutePath()
        ));

        runFfmpeg(cmd);
    }

    /**
     * Executes an FFmpeg command and captures stderr (where FFmpeg writes loudnorm JSON).
     *
     * @param command the command tokens
     * @return the combined stdout+stderr output
     * @throws IOException if the process fails
     */
    private String runFfmpeg(List<String> command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }

        int exitCode = p.waitFor();
        if (exitCode != 0) {
            throw new IOException("FFmpeg failed with exit code " + exitCode + ": " + sb.toString());
        }

        return sb.toString();
    }

    /**
     * Parses the JSON loudnorm output from the first pass.
     *
     * @param output the raw FFmpeg output string
     * @return parsed loudness statistics
     * @throws IOException if required fields are missing
     */
    private LoudnessStats parseLoudnessStats(String output) throws IOException {
        double inputI = parseDouble(LUFS_PATTERN, output, "input_i");
        double inputTp = parseDouble(TP_PATTERN, output, "input_tp");
        double inputLra = parseDouble(LRA_PATTERN, output, "input_lra");
        double inputThresh = parseDouble(THRESH_PATTERN, output, "input_thresh");
        double targetOffset = parseDouble(OFFSET_PATTERN, output, "target_offset");

        return new LoudnessStats(inputI, inputTp, inputLra, inputThresh, targetOffset);
    }

    /**
     * Extracts a numeric value from the FFmpeg JSON output.
     *
     * @param pattern the regex to match
     * @param text    the raw output
     * @param field   field name used in error messages
     * @return the parsed double value
     * @throws IOException if the pattern does not match
     */
    private double parseDouble(Pattern pattern, String text, String field) throws IOException {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            throw new IOException("Could not parse " + field + " from FFmpeg output");
        }
        return Double.parseDouble(matcher.group(1));
    }

    // ── Value record ─────────────────────────────────────────────────────────

    /**
     * Holds the loudness measurement results from the first loudnorm pass.
     *
     * @param inputI      measured integrated loudness in LUFS
     * @param inputTp     measured true peak in dBTP
     * @param inputLra    measured loudness range in LU
     * @param inputThresh measured threshold in LUFS
     * @param targetOffset computed offset in LU
     */
    private record LoudnessStats(
            double inputI,
            double inputTp,
            double inputLra,
            double inputThresh,
            double targetOffset) {
    }
}
