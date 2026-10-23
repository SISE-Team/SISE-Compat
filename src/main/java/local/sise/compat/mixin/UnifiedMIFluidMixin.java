package local.sise.compat.mixin;

import local.sise.ores.FluidCatalog;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=aztech.modern_industrialization.definition.FluidDefinition.class,remap=false)
public abstract class UnifiedMIFluidMixin {
    @Inject(method="asFluid",at=@At("RETURN"),cancellable=true)
    private void sise$identity(CallbackInfoReturnable<Fluid> result){result.setReturnValue(FluidCatalog.canonical(result.getReturnValue()));}
}
