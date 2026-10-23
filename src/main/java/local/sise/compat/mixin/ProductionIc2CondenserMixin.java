package local.sise.compat.mixin;

import ic2ce.api.fluid.Ic2FluidStack;
import ic2ce.neoforge.fluid.Ic2FluidTank;
import local.sise.ores.FluidCatalog;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** Keep the condenser's saved credit in water-equivalent units for either steam phase. */
@Mixin(value=ic2ce.neoforge.processing.fluid.SteamCondenser.class,remap=false)
public abstract class ProductionIc2CondenserMixin {
    @ModifyConstant(method="updateEntityServer",constant={@Constant(intValue=100,ordinal=2),@Constant(intValue=100,ordinal=3),@Constant(intValue=100,ordinal=4)})
    private int sise$feedUnit(int original){return 1;}
    @Redirect(method="updateEntityServer",at=@At(value="INVOKE",target="Lic2ce/api/fluid/Ic2FluidStack;getAmountMb()I"))
    private int sise$waterCredit(Ic2FluidStack drained){return drained.getAmountMb()*100;}
}
