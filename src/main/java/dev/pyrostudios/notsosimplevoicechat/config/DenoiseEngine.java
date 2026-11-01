package dev.pyrostudios.notsosimplevoicechat.config;

import net.minecraft.network.chat.Component;

public enum DenoiseEngine {
    DEEPFILTER,
    RNNOISE;

    public Component displayName() {
        return Component.translatable("gui.notsosimplevoicechat.engine." + name().toLowerCase());
    }
}
