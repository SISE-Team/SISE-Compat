package local.sise.compat;

import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;

public final class ExtraLootContexts {
    public static void register() {
        var file=ModList.get().getModFileById("farmingforblockheads");
        if(file==null||file.getMods().stream().noneMatch(m->m.getModId().equals("farmingforblockheads")&&m.getVersion().toString().equals("26.1.2.5")))return;
        local.sise.compat.mixin.LootContextsAccessor.siseCompat$contexts().putIfAbsent(Identifier.parse("farmingforblockheads:shipping_bin"),net.blay09.mods.farmingforblockheads.loot.ModLootContextParams.SHIPPING_BIN_CONTEXT);
    }
    private ExtraLootContexts() { }
}
