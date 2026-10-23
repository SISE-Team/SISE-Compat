package local.sise.compat.mixin;

import java.util.Optional;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.item.recipe.NoTemplateSmithingRecipe;

/** JEI's NeoForge helper otherwise only accepts the two vanilla smithing implementations. */
@Pseudo
@Mixin(targets = "mezz.jei.neoforge.platform.RecipeHelper", remap = false)
public abstract class TwilightSmithingJeiMixin {
    @Inject(method = "getBase", at = @At("HEAD"), cancellable = true)
    private void siseCompat$base(SmithingRecipe recipe, CallbackInfoReturnable<Ingredient> result) {
        if (recipe instanceof NoTemplateSmithingRecipe twilight) result.setReturnValue(twilight.baseIngredient());
    }
    @Inject(method = "getAddition", at = @At("HEAD"), cancellable = true)
    private void siseCompat$addition(SmithingRecipe recipe, CallbackInfoReturnable<Optional<Ingredient>> result) {
        if (recipe instanceof NoTemplateSmithingRecipe twilight) result.setReturnValue(twilight.additionIngredient());
    }
    @Inject(method = "getTemplate", at = @At("HEAD"), cancellable = true)
    private void siseCompat$template(SmithingRecipe recipe, CallbackInfoReturnable<Optional<Ingredient>> result) {
        if (recipe instanceof NoTemplateSmithingRecipe twilight) result.setReturnValue(twilight.templateIngredient());
    }
}
