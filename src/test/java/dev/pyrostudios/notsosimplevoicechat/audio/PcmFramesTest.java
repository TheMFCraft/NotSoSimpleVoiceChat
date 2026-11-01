package dev.pyrostudios.notsosimplevoicechat.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PcmFramesTest {

    @Test
    void mixFullReplace() {
        short[] original = {100, 200, 300};
        short[] processed = {10, 20, 30};
        PcmFrames.mix(original, processed, 1.0);
        assertEquals(10, original[0]);
        assertEquals(20, original[1]);
        assertEquals(30, original[2]);
    }

    @Test
    void mixHalf() {
        short[] original = {100, 0};
        short[] processed = {0, 100};
        PcmFrames.mix(original, processed, 0.5);
        assertEquals(50, original[0]);
        assertEquals(50, original[1]);
    }
}
