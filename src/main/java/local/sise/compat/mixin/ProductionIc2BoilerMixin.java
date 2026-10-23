package local.sise.compat.mixin;

import ic2ce.api.fluid.Ic2FluidStack;
import local.sise.ores.FluidCatalog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value=ic2ce.neoforge.processing.fluid.PressureBoiler.class,remap=false)
public abstract class ProductionIc2BoilerMixin {
    @Redirect(method="updateEntityServer",at=@At(value="INVOKE",target="Lic2ce/api/fluid/Ic2FluidStack;create(Lnet/minecraft/world/level/material/Fluid;I)Lic2ce/api/fluid/Ic2FluidStack;"))
    private Ic2FluidStack sise$product(Fluid fluid,int amount){
        return Ic2FluidStack.create(fluid,amount/100);
    }
    @ModifyConstant(method="updateEntityServer",constant=@Constant(intValue=100,ordinal=1))
    private int sise$capacityUnit(int original){return 1;}
}
