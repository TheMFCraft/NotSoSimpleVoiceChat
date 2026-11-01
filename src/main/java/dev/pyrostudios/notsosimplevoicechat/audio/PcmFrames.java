package dev.pyrostudios.notsosimplevoicechat.audio;

/**
 * Simple Voice Chat liefert 20&nbsp;ms @ 48&nbsp;kHz ({@value #SVC_FRAME_SAMPLES} Samples).
 * RNNoise arbeitet typischerweise in 10&nbsp;ms-Frames ({@value #RNNOISE_FRAME_SAMPLES} Samples).
 */
public final class PcmFrames {
    public static final int SAMPLE_RATE = 48_000;
    public static final int SVC_FRAME_SAMPLES = (SAMPLE_RATE / 1000) * 20;
    public static final int RNNOISE_FRAME_SAMPLES = 480;

    private PcmFrames() {
    }

    public static void mix(short[] original, short[] processed, double processedWeight) {
        double w = Math.clamp(processedWeight, 0.0, 1.0);
        if (w <= 0.0) {
            return;
        }
        if (w >= 1.0) {
            System.arraycopy(processed, 0, original, 0, Math.min(original.length, processed.length));
            return;
        }
        double dry = 1.0 - w;
        for (int i = 0; i < original.length && i < processed.length; i++) {
            double sample = original[i] * dry + processed[i] * w;
            original[i] = (short) Math.max(Math.min(Math.round(sample), Short.MAX_VALUE), Short.MIN_VALUE);
        }
    }
}
