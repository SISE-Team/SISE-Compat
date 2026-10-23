package local.sise.compat.mixin;

import com.tacz.guns.crafting.result.GunSmithTableResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Both JSON and network consumers must see the resolved product, never the lazy placeholder. */
@Mixin(value = GunSmithTableResult.class, remap = false)
public abstract class TaczResultInitMixin {
    @Shadow public abstract void init();
    @Inject(method = {"getResult", "getGroup"}, at = @At("HEAD"))
    private void siseCompat$resolveBeforeRead(CallbackInfoReturnable<?> result) { init(); }
}
