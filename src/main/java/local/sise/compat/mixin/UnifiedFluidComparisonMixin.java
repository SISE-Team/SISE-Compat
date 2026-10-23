package local.sise.compat.mixin;

import local.sise.ores.FluidCatalog;
import local.sise.compat.materials.FluidAliases;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native validators such as IF latex use Fluid.isSame instead of fluid ingredients. */
@Mixin(value={Fluid.class,BaseFlowingFluid.class,com.hrznstudio.titanium.fluid.TitaniumFluid.class},remap=false)
public abstract class UnifiedFluidComparisonMixin {
    @Inject(method="isSame",at=@At("HEAD"),cancellable=true)
    private void sise$equivalence(Fluid other,CallbackInfoReturnable<Boolean> result){
        if(FluidAliases.ready && other!=null && FluidCatalog.canonical((Fluid)(Object)this)==FluidCatalog.canonical(other))result.setReturnValue(true);
    }
}
