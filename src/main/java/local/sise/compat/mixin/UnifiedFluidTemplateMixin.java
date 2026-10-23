package local.sise.compat.mixin;

import local.sise.ores.FluidCatalog;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** Normalize reviewed identity only; quantities and component maps remain untouched. */
@Mixin(value=net.neoforged.neoforge.fluids.FluidStackTemplate.class,remap=false)
public abstract class UnifiedFluidTemplateMixin implements net.neoforged.neoforge.fluids.FluidInstance {
    @Shadow public abstract Holder<net.minecraft.world.level.material.Fluid> typeHolder();
    @Override public boolean is(net.minecraft.world.level.material.Fluid value){return value!=null && typeHolder().value()==FluidCatalog.canonical(value);}
    @Override public boolean is(Holder<net.minecraft.world.level.material.Fluid> value){return value!=null && typeHolder().value()==FluidCatalog.canonical(value).value();}
    @Override public boolean is(ResourceKey<net.minecraft.world.level.material.Fluid> key){return is(net.minecraft.core.registries.BuiltInRegistries.FLUID.get(key).orElse(null));}
    @ModifyVariable(method={"<init>(Lnet/minecraft/core/Holder;ILnet/minecraft/core/component/DataComponentPatch;)V"},at=@At("HEAD"),argsOnly=true,ordinal=0)
    private static Holder<net.minecraft.world.level.material.Fluid> sise$identity(Holder<net.minecraft.world.level.material.Fluid> source){return FluidCatalog.canonical(source);}
}
