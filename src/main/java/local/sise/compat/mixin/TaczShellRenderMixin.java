package local.sise.compat.mixin;

import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.IFunctionalSubmitter;
import com.tacz.guns.client.model.functional.ShellRender;
import dev.viking602.sise.compat.tacz.TaczWorldShells;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Move the reviewed port's local-player shells out of the hand render pass. */
@Mixin(value = ShellRender.class, remap = false)
public abstract class TaczShellRenderMixin {
    @Shadow @Final private ConcurrentLinkedDeque<ShellRender.Data> SHELL_QUEUE;
    @Shadow @Final private BedrockGunModel bedrockGunModel;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void siseRegisterWorldShells(BedrockGunModel model, CallbackInfo ci) {
        TaczWorldShells.register();
    }

    @Inject(method = "extract", at = @At("HEAD"), cancellable = true)
    private void siseExtractWorldShells(IFunctionalSubmitter.ExtractionContext context, CallbackInfo ci) {
        if (TaczWorldShells.takeShells(bedrockGunModel, SHELL_QUEUE, context)) ci.cancel();
    }
}
