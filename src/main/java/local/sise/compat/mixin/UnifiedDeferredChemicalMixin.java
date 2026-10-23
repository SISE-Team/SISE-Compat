package local.sise.compat.mixin;

import local.sise.ores.FluidCatalog;
import mekanism.api.chemical.ChemicalResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Deferred chemical resources may have been cached before the SISE registry entries were bound. */
@Mixin(value=mekanism.common.registration.impl.DeferredChemical.class,remap=false)
public abstract class UnifiedDeferredChemicalMixin {
    @Inject(method="asResource",at=@At("RETURN"),cancellable=true)
    private void sise$cachedResource(CallbackInfoReturnable<ChemicalResource> result){
        var value=result.getReturnValue();var holder=FluidCatalog.canonicalChemical(value.typeHolder());
        if(holder!=value.typeHolder())result.setReturnValue(ChemicalResource.of(holder));
    }
}
