package local.sise.compat.mixin;

import ic2ce.neoforge.fluid.EnvFluidHandler;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.item.BucketItem;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value=EnvFluidHandler.FluidRefs.class,remap=false)
public interface Ic2FluidRefsAccessor {
    @Mutable @Accessor("still") void sise$still(Fluid value);
    @Mutable @Accessor("flowing") void sise$flowing(Fluid value);
    @Mutable @Accessor("bucket") void sise$bucket(BucketItem value);
}
