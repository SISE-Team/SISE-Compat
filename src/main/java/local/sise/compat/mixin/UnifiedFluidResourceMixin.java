package local.sise.compat.mixin;

import local.sise.ores.FluidCatalog;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** Normalize reviewed identity only; quantities and component maps remain untouched. */
@Mixin(value=net.neoforged.neoforge.transfer.fluid.FluidResource.class,remap=false)
public abstract class UnifiedFluidResourceMixin implements net.neoforged.neoforge.transfer.resource.DataComponentHolderResource<net.minecraft.world.level.material.Fluid> {
    @Shadow public abstract Holder<net.minecraft.world.level.material.Fluid> typeHolder();
    @Override public boolean is(net.minecraft.world.level.material.Fluid value){return value!=null && typeHolder().value()==FluidCatalog.canonical(value);}
    @Override public boolean is(Holder<net.minecraft.world.level.material.Fluid> value){return value!=null && typeHolder().value()==FluidCatalog.canonical(value).value();}
    @Override public boolean is(ResourceKey<net.minecraft.world.level.material.Fluid> key){return is(net.minecraft.core.registries.BuiltInRegistries.FLUID.get(key).orElse(null));}
    @ModifyVariable(method={"of(Lnet/minecraft/world/level/material/Fluid;)Lnet/neoforged/neoforge/transfer/fluid/FluidResource;","of(Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/core/component/DataComponentPatch;)Lnet/neoforged/neoforge/transfer/fluid/FluidResource;"},at=@At("HEAD"),argsOnly=true,ordinal=0)
    private static net.minecraft.world.level.material.Fluid sise$resource(net.minecraft.world.level.material.Fluid source){return FluidCatalog.canonical(source);}
}
