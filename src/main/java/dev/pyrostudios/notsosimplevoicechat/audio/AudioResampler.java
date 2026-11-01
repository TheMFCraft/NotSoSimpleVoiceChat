package dev.pyrostudios.notsosimplevoicechat.audio;

public final class AudioResampler {
    public static final int SVC_SAMPLE_RATE = 48_000;
    public static final int SVC_FRAME_SAMPLES = (SVC_SAMPLE_RATE / 1000) * 20;
    public static final int KRISP_SAMPLE_RATE = 16_000;
    public static final int KRISP_FRAME_MS = 30;
    public static final int KRISP_FRAME_SAMPLES = KRISP_SAMPLE_RATE * KRISP_FRAME_MS / 1000;
    public static final int SVC_SAMPLES_PER_KRISP_FRAME = SVC_SAMPLE_RATE * KRISP_FRAME_MS / 1000;

    private AudioResampler() {
    }

    public static short[] downsample48kTo16k(short[] input48k, int offset, int length) {
        int outLen = length / 3;
        short[] out = new short[outLen];
        for (int i = 0; i < outLen; i++) {
            int base = offset + i * 3;
            int sum = input48k[base] + input48k[base + 1] + input48k[base + 2];
            out[i] = (short) (sum / 3);
        }
        return out;
    }

    public static short[] upsample16kTo48k(short[] input16k) {
        short[] out = new short[input16k.length * 3];
        for (int i = 0; i < input16k.length; i++) {
            short sample = input16k[i];
            int base = i * 3;
            out[base] = sample;
            out[base + 1] = sample;
            out[base + 2] = sample;
        }
        return out;
    }

    public static byte[] shortsToLittleEndianBytes(short[] samples) {
        byte[] bytes = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            int idx = i * 2;
            bytes[idx] = (byte) (samples[i] & 0xFF);
            bytes[idx + 1] = (byte) ((samples[i] >> 8) & 0xFF);
        }
        return bytes;
    }

    public static short[] littleEndianBytesToShorts(byte[] bytes) {
        if (bytes.length % 2 != 0) {
            throw new IllegalArgumentException("Ungerade Byte-Länge");
        }
        short[] out = new short[bytes.length / 2];
        for (int i = 0; i < out.length; i++) {
            int idx = i * 2;
            out[i] = (short) ((bytes[idx] & 0xFF) | (bytes[idx + 1] << 8));
        }
        return out;
    }
}
