package dev.pyrostudios.notsosimplevoicechat.audio;

import de.maxhenkel.rnnoise4j.Denoiser;
import dev.pyrostudios.notsosimplevoicechat.NotSoSimpleVoiceChatClient;
import dev.pyrostudios.notsosimplevoicechat.config.ModConfig;

import java.io.IOException;

/**
 * On-device Rauschunterdrückung über RNNoise (dieselbe Engine wie Simple Voice Chat, aber zentral über dieses Addon steuerbar).
 */
public final class LocalRnnoiseProcessor implements VoiceProcessor {
    private final Denoiser denoiser;
    private final int frameSize;
    private final double strength;

    public LocalRnnoiseProcessor(ModConfig config) throws IOException {
        try {
            this.denoiser = new Denoiser();
        } catch (Exception e) {
            throw new IOException("RNNoise konnte nicht initialisiert werden", e);
        }
        this.frameSize = denoiser.getFrameSize();
        this.strength = Math.clamp(config.strengthPercent, 0.0, 100.0) / 100.0;
        if (frameSize <= 0) {
            throw new IOException("Ungültige RNNoise-Framegröße: " + frameSize);
        }
    }

    public boolean isAvailable() {
        return !denoiser.isClosed();
    }

    public short[] process(short[] svcFrame) {
        if (svcFrame == null || svcFrame.length == 0) {
            return svcFrame;
        }
        if (strength <= 0.0) {
            return svcFrame;
        }
        if (svcFrame.length % frameSize != 0) {
            return svcFrame;
        }

        short[] working = svcFrame.clone();
        short[] denoised = new short[working.length];

        for (int offset = 0; offset < working.length; offset += frameSize) {
            short[] chunk = new short[frameSize];
            System.arraycopy(working, offset, chunk, 0, frameSize);
            denoiser.denoiseInPlace(chunk);
            System.arraycopy(chunk, 0, denoised, offset, frameSize);
        }

        PcmFrames.mix(working, denoised, strength);
        return working;
    }

    @Override
    public void close() {
        denoiser.close();
    }

    public static VoiceProcessor tryCreate(ModConfig config) {
        try {
            return new LocalRnnoiseProcessor(config);
        } catch (IOException | UnsatisfiedLinkError | ExceptionInInitializerError e) {
            NotSoSimpleVoiceChatClient.LOGGER.error("RNNoise konnte nicht geladen werden", e);
            return null;
        }
    }
}
