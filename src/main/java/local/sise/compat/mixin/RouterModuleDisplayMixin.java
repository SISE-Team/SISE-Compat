package local.sise.compat.mixin;

import java.util.List;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.*;
import org.spongepowered.asm.mixin.*;

/** Supply the missing 26.1 display while keeping the native tool/component-sensitive crafting path. */
@Pseudo
@Mixin(targets="me.desht.modularrouters.recipe.PickaxeModuleRecipe",remap=false)
public abstract class RouterModuleDisplayMixin extends CustomRecipe {
    @Shadow @Final private ItemStackTemplate result;
    @Shadow protected abstract List<Ingredient> ingredients();
    @Override public List<RecipeDisplay> display() {
        return List.of(new ShapelessCraftingRecipeDisplay(ingredients().stream().map(Ingredient::display).toList(),new SlotDisplay.ItemStackSlotDisplay(result),new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
    }
}
