package local.sise.compat.mixin;

import local.sise.ores.FluidCatalog;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** Normalize reviewed identity only; quantities and component maps remain untouched. */
@Mixin(value=mekanism.api.chemical.ChemicalStackTemplate.class,remap=false)
public abstract class UnifiedChemicalTemplateMixin implements mekanism.api.chemical.ChemicalInstance {
    @Shadow public abstract Holder<mekanism.api.chemical.Chemical> typeHolder();
    @Override public boolean is(mekanism.api.chemical.Chemical value){return value!=null && typeHolder().value()==FluidCatalog.canonicalChemical(value);}
    @Override public boolean is(Holder<mekanism.api.chemical.Chemical> value){return value!=null && typeHolder().value()==FluidCatalog.canonicalChemical(value).value();}
    @Override public boolean is(ResourceKey<mekanism.api.chemical.Chemical> key){return is(mekanism.api.MekanismAPI.CHEMICAL_REGISTRY.get(key).orElse(null));}
    @ModifyVariable(method={"<init>(Lnet/minecraft/core/Holder;I)V"},at=@At("HEAD"),argsOnly=true,ordinal=0)
    private static Holder<mekanism.api.chemical.Chemical> sise$identity(Holder<mekanism.api.chemical.Chemical> source){return FluidCatalog.canonicalChemical(source);}
}
