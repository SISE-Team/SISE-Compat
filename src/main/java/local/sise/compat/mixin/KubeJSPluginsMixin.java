package local.sise.compat.mixin;

import java.util.stream.Stream;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** KubeJS 8.0.7 declares an Architectury plugin whose class is absent from its jar. */
@Pseudo
@Mixin(targets = "dev.latvian.mods.kubejs.plugin.KubeJSPlugins", remap = false)
public abstract class KubeJSPluginsMixin {
    @ModifyVariable(method = "loadFromFile", at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false)
    private static Stream<String> siseCompat$skipMissingBundledPlugin(Stream<String> lines) {
        return lines.filter(line -> {
            String trimmed = line.strip();
            if (trimmed.equals("com.lowdragmc.lowdraglib2.integration.kjs.LDLibKubeJSPlugin")
                && net.neoforged.fml.loading.LoadingModList.get().getMods().stream().anyMatch(info -> info.getModId().equals("ldlib2") && info.getVersion().toString().equals("26.1.2.42"))) return false;
            return !trimmed.equals("dev.latvian.mods.kubejs.integration.architectury.ArchitecturyIntegration")
                && !trimmed.startsWith("dev.latvian.mods.kubejs.integration.architectury.ArchitecturyIntegration ")
                && !trimmed.startsWith("dev.latvian.mods.kubejs.integration.architectury.ArchitecturyIntegration\t");
        });
    }
}
