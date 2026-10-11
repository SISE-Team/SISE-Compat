package local.sise.compat.mixin;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.HolderSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HolderSet.Named.class)
public abstract class NamedHolderSetMixin {
    @Shadow private List<Runnable> invalidationCallbacks;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void siseCompat$threadSafeListeners(CallbackInfo ci) {
        // Registry codecs run in parallel and share named tags. NotHolderSet and
        // CompositeHolderSet constructors all register on these same lists.
        // Snapshot iteration also permits registration from an invalidation callback
        // without holding a lock while calling other holder sets.
        this.invalidationCallbacks = new CopyOnWriteArrayList<>(this.invalidationCallbacks);
    }
}
