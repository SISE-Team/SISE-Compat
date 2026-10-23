package local.sise.compat.mixin;

import net.minecraft.server.packs.repository.Pack;
import net.neoforged.fml.jarcontents.*;
import net.neoforged.neoforge.resource.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Use NeoForge's namespace-aware mod resource implementation instead of enumerating JAR root files. */
@Mixin(value = ResourcePackLoader.class, remap = false)
public abstract class ModPackResourcesMixin {
    @Inject(method = "createPackForJarContents", at = @At("HEAD"), cancellable = true)
    private static void siseCompat$modNamespaces(JarContents contents, CallbackInfoReturnable<Pack.ResourcesSupplier> result) {
        if (contents instanceof JarFileContents) result.setReturnValue(new JarContentsPackResources.JarContentsResourcesSupplier(contents));
    }
}
