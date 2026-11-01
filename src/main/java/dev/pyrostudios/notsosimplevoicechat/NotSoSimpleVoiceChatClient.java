package dev.pyrostudios.notsosimplevoicechat;

import dev.pyrostudios.notsosimplevoicechat.config.ModConfig;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NotSoSimpleVoiceChatClient implements ClientModInitializer {
    public static final String MOD_ID = "notsosimplevoicechat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        ModConfig.load();
        LOGGER.info("NotSoSimple Voice Chat geladen – Krisp Voice Isolation bereit.");
    }
}
