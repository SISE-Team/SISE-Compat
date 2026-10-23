package local.sise.compat.mixin;

import local.sise.ores.MaterialLootModifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Native scrapbox drops bypass JSON recipes and loot tables; normalize their physical templates. */
@Mixin(value=ic2ce.neoforge.recipe.processing.ScrapboxDrops.class,remap=false)
public abstract class UnifiedScrapboxDropsMixin {
    @ModifyVariable(method="entry",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private static ItemStack sise$drop(ItemStack stack){return MaterialLootModifier.normalize(stack);}
}
