package dev.pyrostudios.notsosimplevoicechat.voice;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientSoundEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import dev.pyrostudios.notsosimplevoicechat.NotSoSimpleVoiceChatClient;
import dev.pyrostudios.notsosimplevoicechat.audio.LocalVoicePipeline;
import dev.pyrostudios.notsosimplevoicechat.config.ModConfig;

public final class NotSoSimpleVoiceChatPlugin implements VoicechatPlugin {
    private static final int EVENT_PRIORITY = 100;

    private static NotSoSimpleVoiceChatPlugin instance;

    private final LocalVoicePipeline pipeline = new LocalVoicePipeline();

    @Override
    public String getPluginId() {
        return NotSoSimpleVoiceChatClient.MOD_ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        instance = this;
        ModConfig.load();
        reloadPipeline();
    }

    public static void reloadPipeline() {
        NotSoSimpleVoiceChatPlugin plugin = instance;
        if (plugin == null) {
            return;
        }
        ModConfig.load();
        synchronized (plugin) {
            plugin.pipeline.reload(ModConfig.get());
        }
        NotSoSimpleVoiceChatClient.LOGGER.info("Audio-Pipeline neu geladen ({})", ModConfig.get().engineEnum());
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(ClientSoundEvent.class, this::onClientSound, EVENT_PRIORITY);
    }

    private void onClientSound(ClientSoundEvent event) {
        ModConfig config = ModConfig.get();
        if (!config.enabled) {
            return;
        }

        short[] raw = event.getRawAudio();
        short[] processed;
        synchronized (this) {
            processed = pipeline.process(raw);
        }
        if (processed != raw) {
            event.setRawAudio(processed);
        }
    }

    public static boolean shouldReplaceSvcDenoiser() {
        ModConfig config = ModConfig.get();
        return config.enabled && config.replaceSvcDenoiser;
    }
}
