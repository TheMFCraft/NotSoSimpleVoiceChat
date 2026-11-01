package dev.pyrostudios.notsosimplevoicechat.audio;

import dev.pyrostudios.notsosimplevoicechat.NotSoSimpleVoiceChatClient;
import dev.pyrostudios.notsosimplevoicechat.config.DenoiseEngine;
import dev.pyrostudios.notsosimplevoicechat.config.ModConfig;

public final class LocalVoicePipeline implements AutoCloseable {
    private VoiceProcessor processor;

    public synchronized void reload(ModConfig config) {
        closeProcessor();
        if (!config.enabled) {
            return;
        }

        DenoiseEngine engine = config.engineEnum();
        processor = switch (engine) {
            case DEEPFILTER -> createDeepFilterOrFallback(config);
            case RNNOISE -> LocalRnnoiseProcessor.tryCreate(config);
        };

        if (processor == null) {
            NotSoSimpleVoiceChatClient.LOGGER.warn("Keine lokale Rauschunterdrückung verfügbar (Engine: {}).", engine);
        }
    }

    private static VoiceProcessor createDeepFilterOrFallback(ModConfig config) {
        VoiceProcessor deepFilter = LocalDeepFilterProcessor.tryCreate(config);
        if (deepFilter != null) {
            return deepFilter;
        }
        NotSoSimpleVoiceChatClient.LOGGER.warn("DeepFilterNet fehlgeschlagen – Fallback auf RNNoise.");
        return LocalRnnoiseProcessor.tryCreate(config);
    }

    public synchronized short[] process(short[] svcFrame) {
        VoiceProcessor active = processor;
        if (active == null || !active.isAvailable()) {
            return svcFrame;
        }
        return active.process(svcFrame);
    }

    @Override
    public synchronized void close() {
        closeProcessor();
    }

    private void closeProcessor() {
        if (processor != null) {
            processor.close();
            processor = null;
        }
    }
}
