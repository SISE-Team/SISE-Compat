package local.sise.compat.mixin;

import local.sise.compat.BatchIngredient;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.init.recipes.OritechRecipe;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

@Mixin(value=MachineBlockEntity.class,remap=false)
public abstract class ProductionOritechBatchMixin {
    @Shadow protected OritechRecipe currentRecipe;
    @Shadow @Final public MachineBlockEntity.MachineInventoryStorage inventory;
    @Shadow protected abstract List<ItemStack> getInputView();
    @Inject(method="removeCraftingInputs",at=@At("HEAD"),cancellable=true)
    private void sise$batch(Transaction transaction,CallbackInfoReturnable<Boolean> result){
        var ingredients=currentRecipe.itemInputs();if(ingredients.stream().noneMatch(i->BatchIngredient.count(i)>1))return;
        var slots=OritechRecipe.findMatchingInputSlots(ingredients,getInputView());if(slots==null){result.setReturnValue(false);return;}
        var storage=inventory.getInputContainer();
        for(int i=0;i<slots.length;i++){
            int count=BatchIngredient.count(ingredients.get(i));var resource=storage.getResource(slots[i]);
            if(resource.isEmpty() || storage.extract(slots[i],resource,count,transaction)!=count){result.setReturnValue(false);return;}
        }
        result.setReturnValue(true);
    }
}
