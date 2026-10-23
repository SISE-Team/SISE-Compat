package local.sise.compat.materials;

import local.sise.ores.*;
import local.sise.compat.mixin.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.item.BucketItem;
import ic2ce.neoforge.registry.FluidDefinitions;
import ic2ce.neoforge.fluid.EnvFluidHandler;

/** Bind public native machine references after registration, before any world/JEI machines exist. */
public final class FluidAliases {
    public static volatile boolean ready;
    /** NeoForge's default provider covers exact BucketItem classes, not IC2's bucket subclasses. */
    public static void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        for(var entry:FluidCatalog.FLUIDS)for(var id:entry.buckets()) {
            var item=BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
            if(item instanceof BucketItem && !event.isItemRegistered(net.neoforged.neoforge.capabilities.Capabilities.Fluid.ITEM,item))
                event.registerItem(net.neoforged.neoforge.capabilities.Capabilities.Fluid.ITEM,
                    (stack,access)->new net.neoforged.neoforge.transfer.fluid.BucketResourceHandler(access),item);
        }
    }
    public static void bindNativeReferences() {
        try {
            for(var field:FluidDefinitions.class.getFields())if(field.getType()==EnvFluidHandler.FluidRefs.class) {
                var ref=(EnvFluidHandler.FluidRefs)field.get(null);
                var original=BuiltInRegistries.FLUID.getKey(ref.still).toString();
                var entry=FluidCatalog.BY_SOURCE.get(original);if(entry==null)continue;
                var access=(Ic2FluidRefsAccessor)(Object)ref;
                access.sise$still(UnifiedFluids.SOURCES.get(entry.name()).get());
                access.sise$flowing(UnifiedFluids.FLOWING.get(entry.name()).get());
                access.sise$bucket(UnifiedFluids.BUCKETS.get(entry.name()).get());
            }
            var matter=BuiltInRegistries.ITEM.getValue(SiseOres.id("uu_matter"));
            Ic2MatterAccessor.sise$matter(matter);
            MoreMachineMatterAccessor.sise$matter(new mekanism.common.registration.impl.ItemRegistryObject<>(ResourceKey.create(Registries.ITEM,SiseOres.id("uu_matter"))));
            Ic2MatterAccessor.sise$scrap(BuiltInRegistries.ITEM.getValue(SiseOres.id("scrap")));
            Ic2MatterAccessor.sise$scrapBox(BuiltInRegistries.ITEM.getValue(SiseOres.id("scrap_box")));
            MoreMachineMatterAccessor.sise$scrap(new mekanism.common.registration.impl.ItemRegistryObject<>(ResourceKey.create(Registries.ITEM,SiseOres.id("scrap"))));
            MoreMachineMatterAccessor.sise$scrapBox(new mekanism.common.registration.impl.ItemRegistryObject<>(ResourceKey.create(Registries.ITEM,SiseOres.id("compressed_scrap_box"))));
            ready=true;
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Unable to bind reviewed IC2 fluid references",e);}
    }
    private FluidAliases(){}
}
