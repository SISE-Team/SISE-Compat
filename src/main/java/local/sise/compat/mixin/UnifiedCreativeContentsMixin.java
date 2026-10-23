package local.sise.compat.mixin;

import local.sise.compat.materials.FluidAliases;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Registry aliases collapse filled-tank variants; don't insert identical canonical contents twice. */
@Mixin(value=BuildCreativeModeTabContentsEvent.class,remap=false)
public abstract class UnifiedCreativeContentsMixin {
    @Inject(method="accept(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/CreativeModeTab$TabVisibility;)V",at=@At("HEAD"),cancellable=true)
    private void sise$uniqueContents(ItemStack stack,CreativeModeTab.TabVisibility visibility,CallbackInfo result){
        sise$deduplicate(stack,visibility,result);
    }
    @Inject(method={"insertAfter","insertBefore"},at=@At("HEAD"),cancellable=true)
    private void sise$uniqueInsertion(ItemStack anchor,ItemStack stack,CreativeModeTab.TabVisibility visibility,CallbackInfo result){
        sise$deduplicate(stack,visibility,result);
    }
    private void sise$deduplicate(ItemStack stack,CreativeModeTab.TabVisibility visibility,CallbackInfo result){
        if(!FluidAliases.ready)return;
        var event=(BuildCreativeModeTabContentsEvent)(Object)this;
        boolean parent=event.getParentEntries().contains(stack),search=event.getSearchEntries().contains(stack);
        if(!parent && !search)return;
        if(visibility==CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS){
            if(!parent)event.accept(stack,CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
            if(!search)event.accept(stack,CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        }else if(visibility==CreativeModeTab.TabVisibility.PARENT_TAB_ONLY && !parent || visibility==CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY && !search)return;
        result.cancel();
    }
}
