package local.sise.compat.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import local.sise.core.space.SpaceData;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets="com.yungnickyoung.minecraft.travelerstitles.command.DimensionTitleCommand")
public abstract class PrivateDimensionTitleCommandMixin {
    @ModifyExpressionValue(method="displayTitle",at=@At(value="INVOKE",target="Lnet/minecraft/network/chat/Component;literal(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"))
    private static MutableComponent sise$ownerTitle(MutableComponent original,@Local(argsOnly=true) ServerLevel level){
        return SpaceData.owner(level.dimension())==null?original:level.getDescription().copy();
    }
}
