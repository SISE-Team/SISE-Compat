package local.sise.compat.mixin;

import com.tacz.guns.crafting.result.RawGunTableResult;
import com.tacz.guns.resource.pojo.data.recipe.GunResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = RawGunTableResult.class, remap = false)
public interface TaczRawResultAccessor {
    @Accessor("type") String siseCompat$type();
    @Accessor("id") Identifier siseCompat$id();
    @Accessor("count") int siseCompat$count();
    @Accessor("extraData") GunResult siseCompat$extra();
    @Accessor("nbt") CompoundTag siseCompat$nbt();
}
