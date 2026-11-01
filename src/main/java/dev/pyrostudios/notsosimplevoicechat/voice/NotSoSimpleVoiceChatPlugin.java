package dev.pyrostudios.notsosimplevoicechat.voice;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientSoundEvent;
import de.maxhenkel.voicechat.api.events.ClientVoicechatConnectionEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import dev.pyrostudios.notsosimplevoicechat.NotSoSimpleVoiceChatClient;
import dev.pyrostudios.notsosimplevoicechat.config.ModConfig;
import dev.pyrostudios.notsosimplevoicechat.krisp.KrispVoiceIsolationClient;

public final class NotSoSimpleVoiceChatPlugin implements VoicechatPlugin {
    private static final int EVENT_PRIORITY = 100;

    private KrispVoiceIsolationClient krispClient;

    @Override
    public String getPluginId() {
        return NotSoSimpleVoiceChatClient.MOD_ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        ModConfig.load();
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(ClientVoicechatConnectionEvent.class, this::onVoiceConnection, EVENT_PRIORITY);
        registration.registerEvent(ClientSoundEvent.class, this::onClientSound, EVENT_PRIORITY);
    }

    private void onVoiceConnection(ClientVoicechatConnectionEvent event) {
        ModConfig config = ModConfig.get();
        if (!config.enabled) {
            stopKrisp();
            return;
        }

        if (event.isConnected()) {
            startKrisp(config);
        } else {
            stopKrisp();
        }
    }

    private void onClientSound(ClientSoundEvent event) {
        ModConfig config = ModConfig.get();
        if (!config.enabled) {
            return;
        }

        KrispVoiceIsolationClient client = krispClient;
        if (client == null || !client.isReady()) {
            return;
        }

        short[] raw = event.getRawAudio();
        short[] processed = client.processFrame(raw);
        if (processed != raw) {
            event.setRawAudio(processed);
        }
    }

    private synchronized void startKrisp(ModConfig config) {
        stopKrisp();
        krispClient = new KrispVoiceIsolationClient(config);
        krispClient.start();
        NotSoSimpleVoiceChatClient.LOGGER.info("Krisp Voice Isolation wird gestartet …");
    }

    private synchronized void stopKrisp() {
        if (krispClient != null) {
            krispClient.close();
            krispClient = null;
        }
    }
}
