package local.sise.compat.mixin;

import local.sise.ores.FluidCatalog;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value=net.neoforged.neoforge.fluids.crafting.SimpleFluidIngredient.class,remap=false)
public abstract class UnifiedFluidIngredientMixin {
    @ModifyVariable(method="<init>",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private static HolderSet<Fluid> sise$identity(HolderSet<Fluid> sources){return FluidCatalog.canonicalSet(sources);}
}
