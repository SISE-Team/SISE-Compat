package local.sise.compat.probe;

import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import io.github.lounode.ae2cs.common.recipe.input.ThreeItemStackRecipeInput;

public final class MachineChecks {
    private static ItemStack sample(SizedIngredient input) {
        return new ItemStack(input.ingredient().items().findFirst().orElseThrow(),input.count());
    }
    public static Map<String,Integer> verify(MinecraftServer server) {
        var result=new TreeMap<String,Integer>();
        for(var holder:server.getRecipeManager().getRecipes()) {
            var recipe=holder.value();String family=null;boolean passed=true;
            if(recipe instanceof io.github.lounode.ae2cs.common.recipe.crystal_aggregator.CrystalAggregatorRecipe r) {
                family="ae2cs_aggregator_native_match_and_output";var required=r.required();var slots=new ItemStack[]{ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY};
                for(int i=0;i<required.size();i++)slots[i]=sample(required.get(i));
                var input=ThreeItemStackRecipeInput.of(slots[0],slots[1],slots[2]);
                passed=r.matches(input,server.overworld())&&!r.assemble(input).isEmpty()&&ItemStack.matches(r.result().create(),r.assemble(input));
                passed &= !r.matches(ThreeItemStackRecipeInput.of(ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY),server.overworld());
            } else if(recipe instanceof io.github.lounode.ae2cs.common.recipe.circuit_etcher.CircuitEtcherRecipe r) {
                family="ae2cs_etcher_native_match_and_output";var required=r.required();var slots=new ItemStack[]{ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY};
                for(int i=0;i<required.size();i++)slots[i]=sample(required.get(i));
                var input=ThreeItemStackRecipeInput.of(slots[0],slots[1],slots[2]);
                passed=r.matches(input,server.overworld())&&!r.assemble(input).isEmpty()&&ItemStack.matches(r.result().create(),r.assemble(input));
                passed &= !r.matches(ThreeItemStackRecipeInput.of(ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY),server.overworld());
            } else if(recipe instanceof io.github.lounode.ae2cs.common.recipe.crystal_pulverizer.CrystalPulverizerRecipe r) {
                family="ae2cs_pulverizer_native_match_and_output";var input=new SingleRecipeInput(sample(r.input()));
                passed=r.matches(input,server.overworld())&&!r.assemble(input).isEmpty()&&ItemStack.matches(r.result().create(),r.assemble(input));
                passed &= !r.matches(new SingleRecipeInput(ItemStack.EMPTY),server.overworld());
            } else if(recipe instanceof com.buuz135.functionalstorage.recipe.CustomCompactingRecipe r) {
                family="storage_compacting_conversion_templates";passed=!r.lowerStack().isEmpty()&&!r.higherStack().isEmpty();
            } else if(recipe instanceof baguchi.tofucraft.recipe.BitternRecipe r) {
                family="tofu_native_fluid_and_product";passed=!r.getFluid().create().isEmpty()&&r.getBitternIngredient().items().findAny().isPresent()&&!r.getResult().create().isEmpty();
            } else if(recipe instanceof baguchi.tofucraft.recipe.HardenRecipe r) {
                family="tofu_native_hardening_input_and_product";passed=r.getTofu().items().findAny().isPresent()&&!r.getResult().create().isEmpty();
            } else if(recipe instanceof cn.dancingsnow.neoecoae.recipe.CoolingRecipe r) {
                family="cooling_native_fluid_input";passed=!r.input().ingredient().fluids().isEmpty();
            }
            if(family!=null) {
                if(!recipe.isSpecial()||!passed)throw new AssertionError("Native machine recipe failed: "+holder.id()+" special="+recipe.isSpecial()+" native="+passed);
                result.merge(family,1,Integer::sum);
            }
        }
        return result;
    }
    private MachineChecks() { }
}
