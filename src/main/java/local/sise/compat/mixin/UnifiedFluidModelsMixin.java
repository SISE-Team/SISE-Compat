package local.sise.compat.mixin;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Aliased registry identities retain the exact model, tint and rendering behavior of their source. */
@Mixin(value = ClientHooks.class, remap = false)
public abstract class UnifiedFluidModelsMixin {
    @Redirect(method = "gatherFluidModels", at = @At(value = "INVOKE", target = "Lnet/neoforged/fml/ModLoader;postEvent(Lnet/neoforged/bus/api/Event;)V"))
    private static void siseCompat$completeAliases(Event raw) {
        var event = (RegisterFluidModelsEvent)raw;
        ModLoader.postEvent(event);
        var models = ((FluidModelEventAccessor)(Object)event).siseCompat$models();
        for (var entry : local.sise.ores.FluidCatalog.FLUIDS) {
            var canonical = net.minecraft.core.registries.BuiltInRegistries.FLUID.getValue(net.minecraft.resources.Identifier.parse(entry.target()));
            var preferred = net.minecraft.core.registries.BuiltInRegistries.FLUID.getValue(net.minecraft.resources.Identifier.parse(entry.preferred()));
            var model = models.get(canonical);
            if (model == null) model = models.get(preferred);
            if (model == null) for (var candidate : models.entrySet()) {
                if (candidate.getKey().getFluidType() == canonical.getFluidType()) { model = candidate.getValue(); break; }
            }
            if (model == null) throw new IllegalStateException("Unified fluid has no native rendering model: "+entry.target());
            models.putIfAbsent(canonical,model);
            models.putIfAbsent(local.sise.ores.UnifiedFluids.FLOWING.get(entry.name()).get(),model);
            for (String id : entry.aliases()) models.putIfAbsent(net.minecraft.core.registries.BuiltInRegistries.FLUID.getValue(net.minecraft.resources.Identifier.parse(id)),model);
        }
    }
}
