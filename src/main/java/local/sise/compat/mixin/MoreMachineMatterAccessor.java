package local.sise.compat.mixin;

import mekanism.common.registration.impl.ItemRegistryObject;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value=com.jerry.mekmm.common.registries.MoreMachineItems.class,remap=false)
public interface MoreMachineMatterAccessor {
    @Mutable @Accessor("UU_MATTER") static void sise$matter(ItemRegistryObject<Item> value){throw new AssertionError();}
    @Mutable @Accessor("SCRAP") static void sise$scrap(ItemRegistryObject<Item> value){throw new AssertionError();}
    @Mutable @Accessor("SCRAP_BOX") static void sise$scrapBox(ItemRegistryObject<Item> value){throw new AssertionError();}
}
