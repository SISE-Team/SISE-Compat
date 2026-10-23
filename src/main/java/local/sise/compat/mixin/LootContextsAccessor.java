package local.sise.compat.mixin;

import com.google.common.collect.BiMap;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LootContextParamSets.class)
public interface LootContextsAccessor {
    @Accessor("REGISTRY") static BiMap<Identifier,ContextKeySet> siseCompat$contexts() { throw new AssertionError(); }
}
