package dev.pyrostudios.notsosimplevoicechat.client;

import de.maxhenkel.voicechat.gui.VoiceChatScreenBase;
import de.maxhenkel.voicechat.gui.VoiceChatSettingsScreen;
import dev.pyrostudios.notsosimplevoicechat.mixin.ScreenWidgetAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Matches {@link VoiceChatSettingsScreen} constructor {@code super(TITLE, 248, 219)}.
 */
public final class VoiceChatSettingsUi {

    private static final int PANEL_WIDTH = 248;
    private static final int PANEL_HEIGHT = 219;

    private VoiceChatSettingsUi() {
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof VoiceChatSettingsScreen)) {
                return;
            }
            VoiceChatScreenBase panel = (VoiceChatScreenBase) screen;
            ScreenWidgetAccessor widgets = (ScreenWidgetAccessor) screen;
            int buttonY = panel.getGuiTop() + PANEL_HEIGHT - 28;
            widgets.notsosimplevoicechat$addRenderableWidget(
                    Button.builder(
                                    Component.translatable("gui.notsosimplevoicechat.open_settings"),
                                    button -> client.setScreenAndShow(new NotSoSimpleVoiceChatSettingsScreen(screen))
                            )
                            .bounds(panel.getGuiLeft() + 10, buttonY, PANEL_WIDTH - 20, 20)
                            .build()
            );
        });
    }
}
