package local.sise.compat.mixin;

import java.io.IOException;
import java.io.Reader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** ProjectE rejects the entire Ice and Fire conversion file when the talon key repeats. */
@Pseudo
@Mixin(targets = "moze_intel.projecte.emc.mappers.customConversions.CustomConversionMapper", remap = false)
public abstract class IceAndFireEmcMixin {
    @ModifyArg(method = "load", at = @At(value = "INVOKE",
        target = "Lmoze_intel/projecte/impl/codec/PECodecHelper;read(Lcom/mojang/serialization/DynamicOps;Ljava/io/Reader;Lcom/mojang/serialization/Codec;Ljava/lang/String;)Ljava/util/Optional;"), index = 1)
    private static Reader siseCompat$reviewedIceAndFireData(Reader source) throws IOException {
        return local.sise.compat.ReviewedConversionData.repairIceAndFire(source);
    }
}
