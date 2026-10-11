package local.sise.compat.mixin;

import java.util.List;
import java.util.Set;
import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Apply optional fixes only to the reviewed upstream releases. */
public final class CompatMixinPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String mixinPackage) {
        if (reviewed("ae2screenshot", "26.1.2-1.0.0") && loaded("ldlib2"))
            local.sise.compat.ScreenshotMixinCanceller.register();
    }
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        String mod, version;
        if (mixin.endsWith(".AAANamespaceMixin")) { mod = "aaa_particles"; version = "2.2.3"; }
        else if (mixin.endsWith(".ArtifactsConfigurationMixin")) { mod = "artifacts"; version = "15.1.3"; }
        else if (mixin.endsWith(".RaritySyncSnapshotMixin")) { mod = "raritycore"; version = "2601.14.2"; }
        else if (mixin.endsWith(".TitaniumInventoryMixin")) { mod = "titanium"; version = "4.0.8"; }
        else if (mixin.endsWith(".ScreenshotGuiRendererMixin")) return reviewed("ae2screenshot", "26.1.2-1.0.0") && loaded("ldlib2");
        else return true;
        return reviewed(mod, version);
    }
    private static boolean reviewed(String mod, String version) {
        return LoadingModList.get().getMods().stream().anyMatch(info ->
            info.getModId().equals(mod) && info.getVersion().toString().equals(version));
    }
    private static boolean loaded(String mod) { return LoadingModList.get().getModFileById(mod) != null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
}
