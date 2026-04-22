package video;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Assembles buffered image segments and optional audio into a final MP4 file.
 */
public class VideoAssembler {
    private static final int FRAME_RATE = 30;
    private static final int AUDIO_SAMPLE_RATE = 44_100;
    private static final int AUDIO_CHANNELS = 2;

    /**
     * Represents a continuous video segment made of frames, optional audio, and duration.
     *
     * @param frames the frames to render for the segment
     * @param audioFile the audio file to mux with the segment, or null for silence
     * @param duration the segment duration in seconds
     */
    public record FrameSegment(List<BufferedImage> frames, File audioFile, double duration) {
    }

    /**
     * Assembles all segments into a portrait MP4 file using H.264 video and AAC audio.
     *
     * @param segments the ordered segments to render
     * @param outputPath the final MP4 output path
     * @throws IOException if the output cannot be created
     */
    public void assemble(List<FrameSegment> segments, String outputPath) throws IOException {
        if (segments == null || segments.isEmpty()) {
            throw new IOException("No segments were provided for video assembly.");
        }

        File outputFile = new File(outputPath);
        if (outputFile.getParentFile() != null && !outputFile.getParentFile().exists() && !outputFile.getParentFile().mkdirs()) {
            throw new IOException("Could not create output directory: " + outputFile.getParentFile().getAbsolutePath());
        }

        int width = segments.get(0).frames().get(0).getWidth();
        int height = segments.get(0).frames().get(0).getHeight();
        Java2DFrameConverter converter = new Java2DFrameConverter();

        try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile, width, height, AUDIO_CHANNELS)) {
            recorder.setFormat("mp4");
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
            recorder.setFrameRate(FRAME_RATE);
            recorder.setSampleRate(AUDIO_SAMPLE_RATE);
            recorder.setAudioChannels(AUDIO_CHANNELS);
            recorder.setVideoBitrate(4_000_000);
            recorder.setAudioBitrate(192_000);
            recorder.start();

            long videoTimestampUs = 0L;
            long audioTimestampUs = 0L;

            for (FrameSegment segment : segments) {
                int totalFrames = Math.max(1, (int) Math.round(segment.duration() * FRAME_RATE));
                List<BufferedImage> frames = normalizeFrames(segment.frames(), totalFrames);

                for (int index = 0; index < totalFrames; index++) {
                    BufferedImage frameImage = frames.get(Math.min(index, frames.size() - 1));
                    recorder.setTimestamp(videoTimestampUs);
                    recorder.record(converter.convert(frameImage));
                    videoTimestampUs += 1_000_000L / FRAME_RATE;
                }

                long segmentAudioEndUs = audioTimestampUs + (long) (segment.duration() * 1_000_000L);
                if (segment.audioFile() != null && segment.audioFile().exists()) {
                    audioTimestampUs = recordAudioSegment(recorder, segment.audioFile(), audioTimestampUs, segmentAudioEndUs);
                }
                if (audioTimestampUs < segmentAudioEndUs) {
                    writeSilence(recorder, audioTimestampUs, segmentAudioEndUs);
                    audioTimestampUs = segmentAudioEndUs;
                }
            }

            recorder.stop();
        } catch (Exception exception) {
            throw new IOException("Could not assemble output video.", exception);
        }
    }

    /**
     * Expands or preserves frame lists so a segment always has enough frames for playback.
     *
     * @param frames the original frame list
     * @param totalFrames the desired number of output frames
     * @return a usable frame list
     */
    private List<BufferedImage> normalizeFrames(List<BufferedImage> frames, int totalFrames) {
        if (frames == null || frames.isEmpty()) {
            throw new IllegalArgumentException("Each segment must contain at least one frame.");
        }
        if (frames.size() >= totalFrames) {
            return frames;
        }

        List<BufferedImage> normalized = new ArrayList<>(totalFrames);
        for (int index = 0; index < totalFrames; index++) {
            int sourceIndex = (int) Math.floor((double) index / totalFrames * frames.size());
            normalized.add(frames.get(Math.min(sourceIndex, frames.size() - 1)));
        }
        return normalized;
    }

    /**
     * Records an external audio file into the output while keeping timing aligned with the segment duration.
     *
     * @param recorder the active recorder
     * @param audioFile the audio file to copy
     * @param startTimestampUs the audio start timestamp in microseconds
     * @param endTimestampUs the audio end timestamp in microseconds
     * @return the resulting audio timestamp after copying samples
     * @throws Exception if audio cannot be decoded or recorded
     */
    private long recordAudioSegment(FFmpegFrameRecorder recorder, File audioFile, long startTimestampUs, long endTimestampUs) throws Exception {
        long currentTimestampUs = startTimestampUs;
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(audioFile)) {
            grabber.start();
            Frame samplesFrame;
            while ((samplesFrame = grabber.grabSamples()) != null) {
                if (samplesFrame.samples == null || samplesFrame.samples.length == 0) {
                    continue;
                }

                int sampleCount = samplesFrame.samples[0].limit();
                long frameDurationUs = (long) ((sampleCount / (double) AUDIO_SAMPLE_RATE) * 1_000_000L);
                if (currentTimestampUs + frameDurationUs > endTimestampUs) {
                    break;
                }

                recorder.setTimestamp(currentTimestampUs);
                recorder.recordSamples(samplesFrame.sampleRate, samplesFrame.audioChannels, samplesFrame.samples);
                currentTimestampUs += frameDurationUs;
            }
            grabber.stop();
        }
        return currentTimestampUs;
    }

    /**
     * Writes silent audio samples to cover a time gap.
     *
     * @param recorder the active recorder
     * @param startTimestampUs the start timestamp in microseconds
     * @param endTimestampUs the end timestamp in microseconds
     * @throws Exception if silence cannot be recorded
     */
    private void writeSilence(FFmpegFrameRecorder recorder, long startTimestampUs, long endTimestampUs) throws Exception {
        long currentTimestampUs = startTimestampUs;
        int chunkSamples = 1024;

        while (currentTimestampUs < endTimestampUs) {
            int remainingSamples = (int) Math.ceil((endTimestampUs - currentTimestampUs) / 1_000_000.0d * AUDIO_SAMPLE_RATE);
            int samplesToWrite = Math.min(chunkSamples, remainingSamples);
            ShortBuffer[] buffers = new ShortBuffer[AUDIO_CHANNELS];
            for (int channel = 0; channel < AUDIO_CHANNELS; channel++) {
                buffers[channel] = ShortBuffer.allocate(samplesToWrite);
            }
            recorder.setTimestamp(currentTimestampUs);
            recorder.recordSamples(AUDIO_SAMPLE_RATE, AUDIO_CHANNELS, buffers);
            currentTimestampUs += (long) ((samplesToWrite / (double) AUDIO_SAMPLE_RATE) * 1_000_000L);
        }
    }
}
