package local.sise.compat.mixin;

import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Restore 26.1 namespace validation while retaining AAA's effect paths and factory. */
@Mixin(value = Identifier.class, remap = false)
public abstract class AAANamespaceMixin {
    @Unique
    private static boolean siseCompat$namespaceChar(char value) {
        return value == '_' || value == '-' || value >= 'a' && value <= 'z'
            || value >= '0' && value <= '9' || value == '.';
    }

    @Inject(method = "isValidNamespace", at = @At("HEAD"), cancellable = true, remap = false)
    private static void siseCompat$namespace(String value, CallbackInfoReturnable<Boolean> callback) {
        boolean valid = !value.equals("..");
        for (int i = 0; valid && i < value.length(); i++) valid = siseCompat$namespaceChar(value.charAt(i));
        callback.setReturnValue(valid);
    }

    @Inject(method = "validNamespaceChar", at = @At("HEAD"), cancellable = true, remap = false)
    private static void siseCompat$character(char value, CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(siseCompat$namespaceChar(value));
    }
}
