package local.sise.compat.mixin;

import com.mojang.serialization.Codec;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** Empty cabinet sides are valid saved values, just as they are in the upstream reader. */
@Pseudo
@Mixin(targets="com.github.ysbbbbbb.kaleidoscopetavern.blockentity.brew.BarCabinetBlockEntity",remap=false)
public abstract class TavernCabinetCodecMixin {
    @ModifyArg(method="saveAdditional",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/storage/ValueOutput;store(Ljava/lang/String;Lcom/mojang/serialization/Codec;Ljava/lang/Object;)V"),index=1)
    private Codec<?> siseCompat$emptyBottleSlots(Codec<?> codec) { return codec==ItemStack.CODEC?ItemStack.OPTIONAL_CODEC:codec; }
}
