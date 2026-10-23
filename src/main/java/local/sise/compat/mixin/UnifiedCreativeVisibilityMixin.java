package local.sise.compat.mixin;

import java.util.*;
import local.sise.ores.MaterialCatalog;
import local.sise.compat.materials.FluidAliases;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hide legacy entries after every mod has finished inserting relative to its original anchors. */
@Mixin(CreativeModeTab.class)
public abstract class UnifiedCreativeVisibilityMixin {
    @Shadow private Collection<ItemStack> displayItems;
    @Shadow private Set<ItemStack> displayItemsSearchTab;
    @Inject(method="buildContents",at=@At("RETURN"))
    private void sise$hideLegacy(CreativeModeTab.ItemDisplayParameters parameters,CallbackInfo result){
        if(!FluidAliases.ready)return;
        var visible=ItemStackLinkedSet.createTypeAndComponentsSet();
        for(var stack:displayItems)if(!MaterialCatalog.BY_SOURCE.containsKey(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()))visible.add(stack);
        displayItems=visible;
        var search=ItemStackLinkedSet.createTypeAndComponentsSet();
        for(var stack:displayItemsSearchTab)if(!MaterialCatalog.BY_SOURCE.containsKey(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()))search.add(stack);
        displayItemsSearchTab=search;
    }
}
