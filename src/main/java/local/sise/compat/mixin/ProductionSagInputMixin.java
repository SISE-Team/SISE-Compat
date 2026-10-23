package local.sise.compat.mixin;

import com.enderio.enderio.content.machines.sag_mill.SagMillBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The insertion filter checks identity; the real recipe matcher checks the paid batch. */
@Mixin(value=SagMillBlockEntity.class,remap=false)
public abstract class ProductionSagInputMixin {
    @Redirect(method="isValidInput",at=@At(value="INVOKE",target="Lnet/neoforged/neoforge/transfer/item/ItemResource;toStack()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack sise$acceptBatchIdentity(ItemResource resource){return resource.toStack(64);}
}
