package local.sise.compat.mixin;

import java.util.Map;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = RegisterFluidModelsEvent.class, remap = false)
public interface FluidModelEventAccessor {
    @Accessor("models") Map<Fluid,FluidModel> siseCompat$models();
}
