package dev.pyrostudios.notsosimplevoicechat.mixin;

import de.maxhenkel.voicechat.voice.client.MicrophoneProcessor;
import dev.pyrostudios.notsosimplevoicechat.voice.NotSoSimpleVoiceChatPlugin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MicrophoneProcessor.class)
public class MicrophoneProcessorMixin {

    @Inject(method = "useDenoiser", at = @At("HEAD"), cancellable = true)
    private void notsosimplevoicechat$replaceSvcDenoiser(CallbackInfoReturnable<Boolean> cir) {
        if (NotSoSimpleVoiceChatPlugin.shouldReplaceSvcDenoiser()) {
            cir.setReturnValue(false);
        }
    }
}
