package local.sise.compat.mixin;

import java.util.*;
import net.minecraft.resources.*;
import net.minecraft.server.packs.resources.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** NeoForge 26 discovers modifiers directly; old global index files are metadata, not modifiers. */
@Mixin(SimpleJsonResourceReloadListener.class)
public abstract class LegacyLootIndexMixin {
    @Redirect(method = "scanDirectory(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/resources/FileToIdConverter;Lcom/mojang/serialization/DynamicOps;Lcom/mojang/serialization/Codec;Ljava/util/Map;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/FileToIdConverter;listMatchingResources(Lnet/minecraft/server/packs/resources/ResourceManager;)Ljava/util/Map;"))
    private static Map<Identifier,Resource> siseCompat$modifierDefinitions(FileToIdConverter converter, ResourceManager manager) {
        var resources = converter.listMatchingResources(manager);
        if (!converter.prefix().equals("loot_modifiers")) return resources;
        var result = new LinkedHashMap<>(resources);
        result.remove(Identifier.parse("neoforge:loot_modifiers/global_loot_modifiers.json"));
        return result;
    }
}
