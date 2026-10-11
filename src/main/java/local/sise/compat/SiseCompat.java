package local.sise.compat;

import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.*;

@Mod("sise_compat")
public final class SiseCompat {
    private static final DeferredRegister<IngredientType<?>> INGREDIENTS = DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES,"sise_compat");
    public static final java.util.function.Supplier<IngredientType<PristineIngredient>> PRISTINE = INGREDIENTS.register("pristine", () -> new IngredientType<>(PristineIngredient.CODEC));
    public SiseCompat(IEventBus bus) {
        INGREDIENTS.register(bus);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, RecipePatches::apply);
    }
}
