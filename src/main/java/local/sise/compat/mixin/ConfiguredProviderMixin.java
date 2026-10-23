package local.sise.compat.mixin;

import net.neoforged.fml.ModContainer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 2.7.5's metadata lists a JEI provider that its NeoForge artifact does not contain. */
@Pseudo
@Mixin(targets = "com.mrcrayfish.configured.platform.NeoForgeConfigHelper", remap = false)
public abstract class ConfiguredProviderMixin {
    @Inject(method = "createProviderInstance", at = @At("HEAD"), cancellable = true)
    private static void siseCompat$bundledProviders(ModContainer container, String provider, CallbackInfoReturnable<Object> result) {
        if (container.getModId().equals("configured") && provider.equals("com.mrcrayfish.configured.impl.jei.JeiConfigProvider")) result.setReturnValue(null);
    }
}
