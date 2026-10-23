package local.sise.compat.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Recipe viewers ask for the base output without an input matrix; component copying needs a slot. */
@Pseudo
@Mixin(targets="com.buuz135.functionalstorage.recipe.CopyComponentsRecipe",remap=false)
public abstract class StorageRecipePreviewMixin {
    @Shadow @Final private ShapedRecipe wrapped;
    @Inject(method="assemble(Lnet/minecraft/world/item/crafting/CraftingInput;)Lnet/minecraft/world/item/ItemStack;",at=@At("HEAD"),cancellable=true)
    private void siseCompat$baseOutputPreview(CraftingInput input,CallbackInfoReturnable<ItemStack> result) {
        if(input.size()==0)result.setReturnValue(wrapped.assemble(input));
    }
}
