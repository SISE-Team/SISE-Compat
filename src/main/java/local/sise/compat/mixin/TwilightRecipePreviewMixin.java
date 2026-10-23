package local.sise.compat.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** Backport upstream's display-based preview; empty-grid assembly is invalid for dynamic recipes. */
@Pseudo
@Mixin(targets="twilightforest.compat.RecipeViewerConstants",remap=false)
public abstract class TwilightRecipePreviewMixin {
    @Redirect(method="lambda$getAllUncraftingRecipes$2",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/crafting/CraftingRecipe;assemble(Lnet/minecraft/world/item/crafting/RecipeInput;)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack siseCompat$displayResult(CraftingRecipe recipe,RecipeInput ignored) {
        var displays=recipe.display();
        if(displays.isEmpty())return ItemStack.EMPTY;
        return displays.getFirst().result().resolveForFirstStack(SlotDisplayContext.fromLevel(Minecraft.getInstance().level));
    }
}
