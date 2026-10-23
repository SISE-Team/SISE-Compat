package local.sise.compat.mixin;

import local.sise.ores.FluidCatalog;
import aztech.modern_industrialization.inventory.MIFluidStorage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Ordinary steam follows the preferred Mekanism one-water-to-one-steam production unit. */
@Mixin(value=aztech.modern_industrialization.machines.components.SteamHeaterComponent.class,remap=false)
public abstract class ProductionMISteamMixin {
    @ModifyVariable(method="tryMakeSteam(Laztech/modern_industrialization/inventory/MIFluidStorage;Laztech/modern_industrialization/inventory/MIFluidStorage;Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/world/level/material/Fluid;I)D",at=@At("STORE"),ordinal=3)
    private int sise$ordinaryRatio(int original,MIFluidStorage input,MIFluidStorage output,Fluid water,Fluid steam,int euPerSteamMb){
        return BuiltInRegistries.FLUID.getKey(FluidCatalog.canonical(steam)).toString().equals("sise_ores:steam")?1:original;
    }
}
