package local.sise.compat.mixin;

import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=ic2ce.neoforge.kinetic.SteamTurbine.class,remap=false)
public abstract class ProductionIc2TurbineUnitsMixin {
    @ModifyConstant(method="settleCondensation",constant=@Constant(intValue=100))
    private int sise$condensateUnit(int original){return 1;}
}
