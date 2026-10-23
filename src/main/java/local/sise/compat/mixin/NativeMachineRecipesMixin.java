package local.sise.compat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** These native machine/world recipes have no crafting-grid placement or vanilla recipe-book entry. */
@Pseudo
@Mixin(targets={
    "io.github.lounode.ae2cs.common.recipe.crystal_aggregator.CrystalAggregatorRecipe",
    "io.github.lounode.ae2cs.common.recipe.circuit_etcher.CircuitEtcherRecipe",
    "io.github.lounode.ae2cs.common.recipe.crystal_pulverizer.CrystalPulverizerRecipe",
    "baguchi.tofucraft.recipe.BitternRecipe",
    "baguchi.tofucraft.recipe.HardenRecipe",
    "com.buuz135.functionalstorage.recipe.CustomCompactingRecipe",
    "cn.dancingsnow.neoecoae.recipe.CoolingRecipe"
},remap=false)
public abstract class NativeMachineRecipesMixin {
    public boolean isSpecial() { return true; }
}
