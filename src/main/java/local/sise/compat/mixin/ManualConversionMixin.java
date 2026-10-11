package local.sise.compat.mixin;

import aztech.modern_industrialization.machines.recipe.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.crafting.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reverse conversions are player-selected, not additional automatic cutting recipes. */
@Mixin(value=RecipeConversions.class,remap=false)
public abstract class ManualConversionMixin {
    @Inject(method="ofStonecutting",at=@At("HEAD"),cancellable=true,remap=false)
    private static void siseCompat$manual(RecipeHolder<StonecutterRecipe> holder,MachineRecipeType type,
            RegistryAccess registries,CallbackInfoReturnable<RecipeHolder<MachineRecipe>> callback) {
        String id = holder.id().identifier().toString();
        if (id.startsWith("sise_compat:manual/") || id.equals("justdirethings:charcoal_9x9")
                || local.sise.compat.materials.RecipeSafety.manualOnly(id)) callback.setReturnValue(null);
    }
}
