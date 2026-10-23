package local.sise.compat.mixin;

import local.sise.compat.BatchIngredient;
import com.enderio.enderio.content.machines.sag_mill.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets="com.enderio.enderio.content.machines.sag_mill.SagMillBlockEntity$1",remap=false)
public abstract class ProductionSagBatchMixin {
    @Shadow @Final private SagMillBlockEntity this$0;
    @Inject(method="consumeInputs(Lcom/enderio/enderio/content/machines/sag_mill/SagMillingRecipe;)V",at=@At("HEAD"))
    private void sise$consumeBatch(SagMillingRecipe recipe,CallbackInfo result){
        int extra=BatchIngredient.count(recipe.input())-1;
        if(extra>0)this$0.getInventory().mutateStack(SagMillBlockEntity.INPUT,stack->stack.shrink(extra));
    }
}
