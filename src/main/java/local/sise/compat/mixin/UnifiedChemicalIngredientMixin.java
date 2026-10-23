package local.sise.compat.mixin;

import local.sise.ores.FluidCatalog;
import net.minecraft.core.Holder;
import mekanism.api.chemical.Chemical;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value=mekanism.api.recipes.ingredients.chemical.SingleChemicalIngredient.class,remap=false)
public abstract class UnifiedChemicalIngredientMixin {
    @ModifyVariable(method="<init>",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private static Holder<Chemical> sise$identity(Holder<Chemical> source){return FluidCatalog.canonicalChemical(source);}
}
