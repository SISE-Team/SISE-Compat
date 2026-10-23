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
        if(reviewed("extendedae","26.1-1.0.5-neoforge")&&reviewed("extendedae_plus","1.6.3-viking602.1"))dev.viking602.sise.compat.matrix.MatrixMixinCanceller.register();
        if (reviewed("ae2screenshot", "26.1.2-1.0.0") && loaded("ldlib2"))
            local.sise.compat.ScreenshotMixinCanceller.register();
    }
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        if (mixin.endsWith(".SoundActivityMixin")) return reviewed("rsls", "1.2.2") && reviewed("neoforge", "26.1.2.114");
        if(mixin.endsWith(".PrivateDimensionTitleMixin")||mixin.endsWith(".PrivateDimensionTitleCommandMixin"))return reviewed("travelerstitles","26.1.2-NeoForge-6.1.0")&&loaded("sise_core");
        if(mixin.endsWith(".MatrixCrafterSetsMixin"))return reviewed("extendedae","26.1-1.0.5-neoforge")&&reviewed("extendedae_plus","1.6.3-viking602.1");
        String mod, version;
        if (mixin.endsWith(".AAANamespaceMixin")) { mod = "aaa_particles"; version = "2.2.3"; }
        else if (mixin.endsWith(".ArtifactsConfigurationMixin")) { mod = "artifacts"; version = "15.1.3"; }
        else if (mixin.endsWith(".RaritySyncSnapshotMixin")) { mod = "raritycore"; version = "2601.14.2"; }
        else if (mixin.endsWith(".TitaniumInventoryMixin")) return reviewed("titanium", "4.0.8") || reviewed("titanium", "4.0.12");
        else if (mixin.endsWith(".KubeJSPluginsMixin")) { mod = "kubejs"; version = "26.1.2-8.0.7"; }
        else if (mixin.endsWith(".ConfiguredProviderMixin")) { mod = "configured"; version = "2.7.5"; }
        else if (mixin.endsWith(".TwilightQuestDataMixin")) { mod = "twilightforest"; version = "4.8.4208"; }
        else if (mixin.endsWith(".TavernCabinetCodecMixin")) { mod = "kaleidoscope_tavern"; version = "1.2.0-viking602.1"; }
        else if (mixin.endsWith(".StorageRecipePreviewMixin")) { mod = "functionalstorage"; version = "26.1.2-1.6.4"; }
        else if (mixin.endsWith(".RouterModuleDisplayMixin")) { mod = "modularrouters"; version = "26.1.2.2"; }
        else if (mixin.endsWith(".EntityIconSettingsMixin")) { mod = "ftblibrary"; version = "26.1.2.9"; }
        else if (mixin.endsWith(".NativeMachineRecipesMixin")) {
            if (target.startsWith("io.github.lounode.ae2cs.")) return reviewed("ae2cs","1.1.12");
            if (target.startsWith("baguchi.tofucraft.")) return reviewed("tofucraft","26.1.2-26.11.5.0");
            if (target.startsWith("com.buuz135.functionalstorage.")) return reviewed("functionalstorage","26.1.2-1.6.4");
            if (target.startsWith("cn.dancingsnow.neoecoae.")) return reviewed("neoecoae","26.1.2-1.3.4-viking602.1");
            return false;
        }
        else if (mixin.endsWith(".ModPackResourcesMixin") || mixin.endsWith(".LegacyLootIndexMixin")) { mod = "neoforge"; version = "26.1.2.114"; }
        else if (mixin.endsWith(".IceAndFireEmcMixin")) return reviewed("iceandfire", "2.2-beta.4")
                && (reviewed("projecte", "26.1.2-1.1.1") || reviewed("projecte", "26.1.2-1.1.2"));
        else if (mixin.endsWith(".TwilightScepterJeiMixin")) return reviewed("twilightforest", "4.8.4208") && reviewed("jei", "29.47.0.111");
        else if (mixin.endsWith(".TwilightSmithingJeiMixin")) return reviewed("twilightforest", "4.8.4208") && reviewed("jei", "29.47.0.111");
        else if (mixin.endsWith(".TwilightRecipePreviewMixin")) return reviewed("twilightforest", "4.8.4208");
        else if (mixin.endsWith(".TwilightUncraftingJeiMixin")) return reviewed("twilightforest", "4.8.4208") && reviewed("jei", "29.47.0.111");
        else if (mixin.endsWith(".PatchouliJeiKeysMixin")) return reviewed("patchouli", "26.1-94") && reviewed("jei", "29.47.0.111");
        else if (mixin.endsWith(".TaczShellRenderMixin")) { mod = "tacz"; version = "1.1.8+neoforge.26.1.2.R3-hotfix2"; }
        else if (mixin.endsWith(".TaczResultAccessor") || mixin.endsWith(".TaczRawResultAccessor")
                || mixin.endsWith(".TaczResultInitMixin") || mixin.endsWith(".TaczRecipeCodecMixin")) {
            mod = "tacz"; version = "1.1.8+neoforge.26.1.2.R3-hotfix2";
        }
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
