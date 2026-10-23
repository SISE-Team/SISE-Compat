package local.sise.compat;

import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.*;

@Mod("sise_compat")
public final class SiseCompat {
    public static final String ID = "sise_compat";
    private static final DeferredRegister<IngredientType<?>> INGREDIENTS = DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES,"sise_compat");
    public static final java.util.function.Supplier<IngredientType<PristineIngredient>> PRISTINE = INGREDIENTS.register("pristine", () -> new IngredientType<>(PristineIngredient.CODEC));
    public static final java.util.function.Supplier<IngredientType<BatchIngredient>> BATCH = INGREDIENTS.register("production_batch", () -> new IngredientType<>(BatchIngredient.CODEC));
    public SiseCompat(IEventBus bus) {
        INGREDIENTS.register(bus);
        bus.addListener(ReviewedResources::packs);
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(ExtraLootContexts::register));
        local.sise.compat.chemical.ChemicalCompat.register(bus);
        bus.addListener(EventPriority.LOWEST, local.sise.compat.materials.FluidAliases::registerCapabilities);
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(local.sise.compat.materials.FluidAliases::bindNativeReferences));
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, RecipePatches::apply);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, local.sise.compat.materials.RecipeSafety::apply);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ProductionPatches::apply);
    }
}
