package local.sise.compat.mixin;
import java.util.Set;
import com.extendedae_plus.content.matrix.CrafterCorePlusBlockEntity;
import com.glodblock.github.extendedae.common.tileentities.matrix.TileAssemblerMatrixCrafter;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** ExtendedAE 1.0.5 changed the field descriptors from ReferenceSet to java.util.Set. */
@Mixin(targets="com.glodblock.github.extendedae.common.me.matrix.ClusterAssemblerMatrix",remap=false)
public abstract class MatrixCrafterSetsMixin {
    @Shadow @Final private Set<TileAssemblerMatrixCrafter> availableCrafters;
    @Shadow @Final private Set<TileAssemblerMatrixCrafter> busyCrafters;
    @Inject(method="addCrafter",at=@At("HEAD"),cancellable=true)
    private void sise$addPlusCrafter(TileAssemblerMatrixCrafter crafter,CallbackInfo ci){if(crafter instanceof CrafterCorePlusBlockEntity plus){(plus.usedThread()<CrafterCorePlusBlockEntity.MAX_THREAD?availableCrafters:busyCrafters).add(crafter);ci.cancel();}}
    @Inject(method="getBusyCrafterAmount",at=@At("HEAD"),cancellable=true)
    private void sise$countThreads(CallbackInfoReturnable<Integer> cir){int count=0;for(var crafter:busyCrafters)count+=crafter instanceof CrafterCorePlusBlockEntity?CrafterCorePlusBlockEntity.MAX_THREAD:TileAssemblerMatrixCrafter.MAX_THREAD;for(var crafter:availableCrafters)count+=crafter.usedThread();cir.setReturnValue(count);}
}
