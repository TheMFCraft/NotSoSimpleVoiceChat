package dev.pyrostudios.notsosimplevoicechat.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AudioResamplerTest {

    @Test
    void downsampleAndUpsamplePreservesLength() {
        short[] frame48k = new short[AudioResampler.SVC_SAMPLES_PER_KRISP_FRAME];
        for (int i = 0; i < frame48k.length; i++) {
            frame48k[i] = (short) (i % 300);
        }

        short[] frame16k = AudioResampler.downsample48kTo16k(frame48k, 0, frame48k.length);
        assertEquals(AudioResampler.KRISP_FRAME_SAMPLES, frame16k.length);

        short[] back48k = AudioResampler.upsample16kTo48k(frame16k);
        assertEquals(frame48k.length, back48k.length);
    }

    @Test
    void pcmByteRoundTrip() {
        short[] samples = new short[]{-1, 0, 1, 1000, -1000};
        byte[] bytes = AudioResampler.shortsToLittleEndianBytes(samples);
        short[] roundTrip = AudioResampler.littleEndianBytesToShorts(bytes);
        assertEquals(samples.length, roundTrip.length);
        for (int i = 0; i < samples.length; i++) {
            assertEquals(samples[i], roundTrip[i]);
        }
    }
}
