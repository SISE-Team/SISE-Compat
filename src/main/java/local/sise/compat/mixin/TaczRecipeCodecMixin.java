package local.sise.compat.mixin;

import com.mojang.serialization.MapCodec;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.crafting.GunSmithTableSerializer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GunSmithTableSerializer.class, remap = false)
public abstract class TaczRecipeCodecMixin {
    @Shadow @Final @Mutable public static MapCodec<GunSmithTableRecipe> CODEC;
    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void siseCompat$losslessRecipeCodec(CallbackInfo callback) {
        CODEC = local.sise.compat.tacz.TaczRecipeCodec.CODEC;
    }
}
