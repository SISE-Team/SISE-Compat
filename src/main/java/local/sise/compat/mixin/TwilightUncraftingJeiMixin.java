package local.sise.compat.mixin;

import java.util.*;
import mezz.jei.api.gui.builder.*;
import mezz.jei.api.recipe.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.item.recipe.UncraftingRecipe;
import twilightforest.tags.TFItemTags;

/** Upstream's display-based layout, adapted to this port's CraftingRecipe category signature. */
@Pseudo
@Mixin(targets="twilightforest.compat.jei.categories.JEIUncraftingCategory",remap=false)
public abstract class TwilightUncraftingJeiMixin {
    @Inject(method="setRecipe(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;Lnet/minecraft/world/item/crafting/CraftingRecipe;Lmezz/jei/api/recipe/IFocusGroup;)V",at=@At("HEAD"),cancellable=true)
    private void siseCompat$displayLayout(IRecipeLayoutBuilder builder,CraftingRecipe recipe,IFocusGroup focuses,CallbackInfo ci) {
        var grid=new ArrayList<IRecipeSlotBuilder>();
        for(int y=0;y<3;y++)for(int x=0;x<3;x++)grid.add(builder.addSlot(RecipeIngredientRole.OUTPUT,x*18+63,y*18+1).setStandardSlotBackground());
        var context=grid.getFirst().getContextMap();
        var displays=recipe.display();
        RecipeDisplay display=displays.isEmpty()?null:displays.getFirst();
        List<SlotDisplay> ingredients=display instanceof ShapedCraftingRecipeDisplay shaped?shaped.ingredients():display instanceof ShapelessCraftingRecipeDisplay shapeless?shapeless.ingredients():List.of();
        int width=display instanceof ShapedCraftingRecipeDisplay shaped?shaped.width():0;
        int height=display instanceof ShapedCraftingRecipeDisplay shaped?shaped.height():0;
        for(int i=0;i<ingredients.size()&&i<grid.size();i++) {
            var stacks=ingredients.get(i).resolveForStacks(context).stream()
                .filter(stack->!stack.is(TFItemTags.BANNED_UNCRAFTING_INGREDIENTS))
                .filter(stack->stack.getCraftingRemainder()==null).toList();
            if(!stacks.isEmpty())grid.get(siseCompat$craftingIndex(width,height,i)).addItemStacks(stacks);
        }
        var input=builder.addSlot(RecipeIngredientRole.INPUT,5,19).setOutputSlotBackground();
        if(recipe instanceof UncraftingRecipe uncrafting)input.addItemStacks(uncrafting.getInput().items().map(item->new ItemStack(item,uncrafting.getCount())).toList());
        else if(display!=null)input.addItemStacks(List.of(display.result().resolveForFirstStack(context)));
        ci.cancel();
    }
    @Unique private static int siseCompat$craftingIndex(int width,int height,int i) {
        if(width==1)return height==2||height==3?i*3+1:4;
        if(height==1)return i+3;
        if(width==2)return i+(i>1?1:0)+(i>3?1:0);
        return height==2?i+3:i;
    }
}
