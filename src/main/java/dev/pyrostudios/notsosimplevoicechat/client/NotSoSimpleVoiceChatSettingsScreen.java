package dev.pyrostudios.notsosimplevoicechat.client;

import dev.pyrostudios.notsosimplevoicechat.config.DenoiseEngine;
import dev.pyrostudios.notsosimplevoicechat.config.ModConfig;
import dev.pyrostudios.notsosimplevoicechat.voice.NotSoSimpleVoiceChatPlugin;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class NotSoSimpleVoiceChatSettingsScreen extends Screen {
    private static final Component TITLE = Component.translatable("gui.notsosimplevoicechat.settings.title");
    private static final Component BACK = Component.translatable("message.voicechat.back");
    private static final Component SAVE = Component.translatable("gui.notsosimplevoicechat.save");

    private final Screen parent;
    private final ModConfig draft;

    private CycleButton<Boolean> enabledToggle;
    private CycleButton<Boolean> replaceSvcToggle;
    private CycleButton<DenoiseEngine> engineButton;
    private AbstractSliderButton strengthSlider;

    public NotSoSimpleVoiceChatSettingsScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
        this.draft = copyConfig(ModConfig.get());
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int y = 48;
        int fieldWidth = Math.min(320, width - 40);
        int fieldX = centerX - fieldWidth / 2;

        enabledToggle = CycleButton.onOffBuilder(draft.enabled)
                .create(fieldX, y, fieldWidth, 20, Component.translatable("gui.notsosimplevoicechat.enabled"), (button, value) -> draft.enabled = value);
        addRenderableWidget(enabledToggle);
        y += 24;

        replaceSvcToggle = CycleButton.onOffBuilder(draft.replaceSvcDenoiser)
                .create(fieldX, y, fieldWidth, 20, Component.translatable("gui.notsosimplevoicechat.replace_svc"), (button, value) -> draft.replaceSvcDenoiser = value);
        addRenderableWidget(replaceSvcToggle);
        y += 24;

        engineButton = CycleButton.builder(DenoiseEngine::displayName, draft.engineEnum())
                .withValues(DenoiseEngine.values())
                .create(fieldX, y, fieldWidth, 20, Component.translatable("gui.notsosimplevoicechat.engine"), (button, value) -> draft.engine = value.name());
        addRenderableWidget(engineButton);
        y += 28;

        strengthSlider = new AbstractSliderButton(fieldX, y, fieldWidth, 20,
                Component.translatable("gui.notsosimplevoicechat.strength", formatStrength(draft.strengthPercent)),
                clampStrength(draft.strengthPercent) / 100.0) {
            @Override
            protected void updateMessage() {
                double level = clampStrength(value * 100.0);
                draft.strengthPercent = level;
                setMessage(Component.translatable("gui.notsosimplevoicechat.strength", formatStrength(level)));
            }

            @Override
            protected void applyValue() {
                draft.strengthPercent = clampStrength(value * 100.0);
            }
        };
        addRenderableWidget(strengthSlider);
        y += 40;

        int half = (fieldWidth - 4) / 2;
        addRenderableWidget(Button.builder(SAVE, button -> saveAndClose())
                .bounds(fieldX, y, half, 20)
                .build());
        addRenderableWidget(Button.builder(BACK, button -> onClose())
                .bounds(fieldX + half + 4, y, half, 20)
                .build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractMenuBackground(graphics);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, TITLE, width / 2, 16, 0xFFFFFF);
        graphics.centeredText(font, Component.translatable("gui.notsosimplevoicechat.subtitle"), width / 2, 28, 0xA0A0A0);
        graphics.centeredText(font, Component.translatable("gui.notsosimplevoicechat.engine_note"), width / 2, height - 46, 0x808080);
    }

    @Override
    public void onClose() {
        minecraft.setScreenAndShow(parent);
    }

    private void saveAndClose() {
        draft.enabled = enabledToggle.getValue();
        draft.replaceSvcDenoiser = replaceSvcToggle.getValue();
        draft.engine = engineButton.getValue().name();
        ModConfig.applyFromDraft(draft);
        ModConfig.save();
        NotSoSimpleVoiceChatPlugin.reloadPipeline();
        onClose();
    }

    private static ModConfig copyConfig(ModConfig source) {
        ModConfig copy = new ModConfig();
        copy.enabled = source.enabled;
        copy.replaceSvcDenoiser = source.replaceSvcDenoiser;
        copy.strengthPercent = source.strengthPercent;
        copy.engine = source.engine;
        return copy;
    }

    private static double clampStrength(double value) {
        return Math.clamp(value, 0.0, 100.0);
    }

    private static String formatStrength(double value) {
        return String.valueOf((int) Math.round(clampStrength(value)));
    }
}
