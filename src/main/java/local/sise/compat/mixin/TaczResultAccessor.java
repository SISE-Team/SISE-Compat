package local.sise.compat.mixin;

import com.google.gson.JsonElement;
import com.tacz.guns.crafting.result.GunSmithTableResult;
import com.tacz.guns.crafting.result.RawGunTableResult;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = GunSmithTableResult.class, remap = false)
public interface TaczResultAccessor {
    @Accessor("rawResult") RawGunTableResult siseCompat$raw();
    @Accessor("customResult") JsonElement siseCompat$custom();
    @Accessor("groupOverride") Identifier siseCompat$groupOverride();
}
