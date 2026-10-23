package local.sise.compat.probe;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.nio.file.*;
import java.util.*;
import local.sise.compat.*;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Isolated regression instrumentation; excluded from the production artifact. */
@Mod("sise_update_probe")
public final class UpdateProbe {
    private static Path directory;
    private static boolean clientDone;
    private static int clientWaiting;
    public UpdateProbe() {
        if (!Boolean.getBoolean("sise.update.verify")) return;
        directory = Path.of(System.getProperty("sise.update.fixture"));
        if (!Files.isRegularFile(directory.resolve("MOD_UPDATE_AUTOTEST_ONLY")))
            throw new IllegalStateException("Update probe requires a disposable fixture");
        RecipeAudit.register();
        NeoForge.EVENT_BUS.addListener(UpdateProbe::server);
        NeoForge.EVENT_BUS.addListener(UpdateProbe::client);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) -> event.getEntity().setInvulnerable(true));
    }
    private static void server(ServerStartedEvent event) {
        event.getServer().setDifficulty(net.minecraft.world.Difficulty.PEACEFUL,true);
        var report = new LinkedHashMap<String,Object>();
        var checks = new ArrayList<String>();
        try {
            checks.addAll(RuntimeFixChecks.verify(event.getServer()));
            checks.addAll(ProductionChecks.verify(event.getServer()));
            report.put("native_machine_recipe_checks",MachineChecks.verify(event.getServer()));
            report.put("extra_data_checks",ExtraDataChecks.verify(event.getServer()));
            if (TwilightQuestData.applied == 0) throw new IllegalStateException("Questing Ram data was not decoded with the starting server registries");
            if (ReviewedConversionData.iceAndFireRepairs == 0)
                throw new IllegalStateException("Ice and Fire conversion data was not repaired");
            checks.add("reviewed Ice and Fire ProjectE conversion file parsed without the duplicate talon declaration");
            var emc = new TreeMap<String,Long>();
            for (String id : List.of("dragonbone", "hippogryph_talon", "hydra_heart", "cyclops_eye")) {
                long value = moze_intel.projecte.api.proxy.IEMCProxy.INSTANCE.getValue(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("iceandfire:"+id)));
                if (value <= 0) throw new IllegalStateException("Ice and Fire EMC mapping absent: "+id);
                emc.put(id, value);
            }
            report.put("iceandfire_emc_values", emc);
            if (!((Map<?,?>)RecipePatches.lastReport.get("skipped")).isEmpty())
                throw new IllegalStateException("Unmatched recipe repairs: "+RecipePatches.lastReport.get("skipped"));
            checks.add("every current compatibility recipe source guard matched");
            var ops = event.getServer().registryAccess().createSerializationContext(JsonOps.INSTANCE);
            var normalization = new TreeMap<String,Object>();
            for (var holder : event.getServer().getRecipeManager().getRecipes()) {
                var encoded = Recipe.CODEC.encodeStart(ops, holder.value()).getOrThrow();
                var decoded = Recipe.CODEC.parse(ops, encoded).getOrThrow();
                var second = Recipe.CODEC.encodeStart(ops, decoded).getOrThrow();
                if (!encoded.equals(second)) {
                    normalization.put(holder.id().identifier().toString(), Map.of("initial", encoded, "normalized", second));
                    throw new IllegalStateException("Recipe codec loses data: "+holder.id()+" original="+encoded+" decoded="+second);
                }
            }
            write("recipe-codec-normalization.json", normalization);
            report.put("recipe_json_codec_differences", normalization.size());
            report.put("recipe_codec_decode_checks", event.getServer().getRecipeManager().getRecipes().size());
            report.put("exact_recipe_json_roundtrips", event.getServer().getRecipeManager().getRecipes().size()-normalization.size());
            if (!((Map<?,?>)local.sise.ores.RecipeUnifier.lastReport.get("unresolved")).isEmpty())
                throw new IllegalStateException("Unresolved unified recipe materials: "+local.sise.ores.RecipeUnifier.lastReport.get("unresolved"));
            var features = event.getServer().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE);
            for (String id : List.of("resource_node_common", "resource_node_other", "resource_node_rare", "uranium_patch")) {
                var feature = features.getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE, net.minecraft.resources.Identifier.parse("oritech:"+id))).value();
                var encoded = net.minecraft.world.level.levelgen.feature.ConfiguredFeature.DIRECT_CODEC.encodeStart(ops, feature).getOrThrow();
                net.minecraft.world.level.levelgen.feature.ConfiguredFeature.DIRECT_CODEC.parse(ops, encoded).getOrThrow();
                checks.add("Oritech weighted configured feature loads and roundtrips: "+id);
            }
            RecipeAudit.export(event.getServer(), directory);
            var export = JsonParser.parseString(Files.readString(directory.resolve("recipe-audit-export.json"))).getAsJsonObject();
            var failures = export.getAsJsonObject("execution").getAsJsonObject("failures");
            if (!failures.isEmpty()) throw new IllegalStateException("Recipe execution failures: "+failures);
            report.put("recipe_execution_by_family", export.getAsJsonObject("execution").get("passed_by_family"));
            report.put("passed", true);
        } catch (Throwable error) {
            report.put("passed", false); report.put("error", error.toString()); error.printStackTrace();
        }
        report.put("checks", checks);
        write("upgrade-server.json", report);
    }
    private static void client(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.player != null && mc.screen instanceof net.minecraft.client.gui.screens.DeathScreen) { mc.player.respawn(); mc.setScreen(null); }
        if (clientDone || mc.level == null || local.sise.ores.compat.OresJeiPlugin.runtime == null) return;
        if (++clientWaiting<80)return;
        clientDone = true;
        var report = new LinkedHashMap<String,Object>();
        try {
            report.put("resource_source_guards_active", ReviewedResources.active.size());
            report.put("resource_source_guards_skipped", ReviewedResources.skipped);
            report.put("legacy_atlas_definitions_migrated", ReviewedResources.repairedAtlasReads);
            report.put("unavailable_manual_integration_entries", ReviewedResources.filtered);
            if (!ReviewedResources.skipped.isEmpty()) throw new IllegalStateException("Resource source guard changed: "+ReviewedResources.skipped);
            report.put("baked_resources",ResourceChecks.client(mc));
            report.put("checks", ProductionChecks.client());
            checkTwilightLayouts(mc);
            report.put("twilight_repair_and_smithing_layouts", 2);
            report.put("passed", true);
        } catch (Throwable error) {
            report.put("passed", false); report.put("error", error.toString()); error.printStackTrace();
        }
        write("upgrade-client.json", report);
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void checkTwilightLayouts(Minecraft mc) {
        var runtime = local.sise.ores.compat.OresJeiPlugin.runtime;
        var manager = runtime.getRecipeManager();
        var focus = runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup();
        for (String id : List.of("twilightforest:equipment/zombie_scepter", "twilightforest:emperors_cloth_smithing")) {
            boolean found = false;
            for (var category : manager.createRecipeCategoryLookup().includeHidden().get().toList()) {
                for (Object recipe : manager.createRecipeLookup(category.getRecipeType()).includeHidden().get().toList()) {
                    if (!(recipe instanceof net.minecraft.world.item.crafting.RecipeHolder<?> holder) || !holder.id().identifier().toString().equals(id)) continue;
                    if (manager.createRecipeLayoutDrawable((mezz.jei.api.recipe.category.IRecipeCategory)category, recipe, focus).isEmpty())
                        throw new IllegalStateException("JEI layout rejected: "+id);
                    found = true;
                }
            }
            if (!found) throw new IllegalStateException("JEI recipe missing: "+id);
        }
    }
    private static void write(String file, Object value) {
        try { Files.writeString(directory.resolve(file), new GsonBuilder().setPrettyPrinting().create().toJson(value)); }
        catch (Exception error) { throw new RuntimeException(error); }
    }
}
