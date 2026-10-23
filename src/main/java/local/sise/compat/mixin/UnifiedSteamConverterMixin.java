package local.sise.compat.mixin;

import ic2ce.neoforge.processing.fluid.IntegrationSteamConverter;
import ic2ce.neoforge.fluid.Ic2FluidTank;
import ic2ce.neoforge.registry.FluidDefinitions;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The former 10-to-16 adapter must not multiply a now-identical fluid. */
@Mixin(value=IntegrationSteamConverter.class,remap=false)
public abstract class UnifiedSteamConverterMixin {
    @Shadow protected Ic2FluidTank input;
    @Inject(method="getOutput",at=@At("RETURN"),cancellable=true)
    private void sise$noIdentityExpansion(CallbackInfoReturnable<Integer> result){
        if(!input.isEmpty() && input.getFluidStack().hasExactFluid(FluidDefinitions.STEAM.still))result.setReturnValue(0);
    }
}
