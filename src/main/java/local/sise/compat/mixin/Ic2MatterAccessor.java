package local.sise.compat.mixin;

import ic2ce.neoforge.material.MaterialItems;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value=MaterialItems.class,remap=false)
public interface Ic2MatterAccessor {
    @Mutable @Accessor("UU_MATTER") static void sise$matter(Item value){throw new AssertionError();}
    @Mutable @Accessor("SCRAP") static void sise$scrap(Item value){throw new AssertionError();}
    @Mutable @Accessor("SCRAP_BOX") static void sise$scrapBox(Item value){throw new AssertionError();}
}
