package local.sise.compat.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Read JEI's runtime key mappings instead of permanently caching null keys during mod loading. */
@Pseudo
@Mixin(targets="vazkii.patchouli.client.jei.PatchouliJeiPlugin",remap=false)
public abstract class PatchouliJeiKeysMixin {
    @Shadow private static IJeiRuntime jeiRuntime;
    @Inject(method="handleRecipeKeybind",at=@At("HEAD"),cancellable=true)
    private static void siseCompat$runtimeKeys(KeyEvent event,ItemStack stack,CallbackInfoReturnable<Boolean> cir) {
        if(jeiRuntime==null||stack.isEmpty()){cir.setReturnValue(false);return;}
        var keys=jeiRuntime.getKeyMappings();var key=InputConstants.getKey(event);
        RecipeIngredientRole role=keys.getShowRecipe().isActiveAndMatches(key)?RecipeIngredientRole.OUTPUT:keys.getShowUses().isActiveAndMatches(key)?RecipeIngredientRole.INPUT:null;
        if(role!=null)jeiRuntime.getRecipesGui().show(jeiRuntime.getJeiHelpers().getFocusFactory().createFocus(role,VanillaTypes.ITEM_STACK,stack));
        cir.setReturnValue(role!=null);
    }
    @Redirect(method="<clinit>",at=@At(value="INVOKE",target="Lorg/apache/logging/log4j/Logger;warn(Ljava/lang/String;)V"))
    private static void siseCompat$modernKeys(org.apache.logging.log4j.Logger logger,String message) {
        logger.info("Patchouli recipe lookups use JEI's current runtime key mappings");
    }
}
