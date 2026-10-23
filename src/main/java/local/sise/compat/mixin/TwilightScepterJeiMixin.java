package local.sise.compat.mixin;

import net.minecraft.core.HolderSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Scepter repair ingredients may use NeoForge custom equivalence ingredients. */
@Pseudo
@Mixin(targets = "twilightforest.compat.jei.extension.ScepterRepairExtension", remap = false)
public abstract class TwilightScepterJeiMixin {
    @Redirect(method = "lambda$setRecipe$0", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/crafting/Ingredient;getValues()Lnet/minecraft/core/HolderSet;"))
    private static HolderSet<Item> siseCompat$customIngredientItems(Ingredient ingredient) {
        return HolderSet.direct(ingredient.items().toList());
    }
}
