package local.sise.compat.serverprobe;

import com.google.gson.GsonBuilder;
import com.enderio.enderio.api.EnderIORegistries;
import com.enderio.enderio.api.conduits.Conduit;
import local.sise.compat.chemical.ChemicalConduit;
import local.sise.compat.RecipePatches;
import local.sise.compat.materials.RecipeSafety;
import net.minecraft.resources.*;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import java.nio.file.*;
import java.util.*;

/** Dedicated server smoke test; no client classes or real player data. */
@Mod("sise_compat_server_probe")
public final class ServerProbe {
    public ServerProbe() { NeoForge.EVENT_BUS.addListener(ServerProbe::started); }
    private static void started(ServerStartedEvent event) {
        var server = event.getServer();
        var report = new LinkedHashMap<String, Object>();
        var checks = new ArrayList<String>();
        try {
            var path = Path.of(System.getProperty("sise.compat.server.fixture", "")).toAbsolutePath().normalize();
            require(Files.isRegularFile(path.resolve("SISE_COMPAT_AUTOTEST_ONLY")), "fixture marker");
            require(!ModList.get().isLoaded("registry_concurrency_fix") && !ModList.get().isLoaded("enderio_chemical_compat"), "retired mod present");
            for (String id : List.of("enderio:chemical", "enderio:pressurized_chemical", "enderio:ender_chemical",
                    "enderio_evolution:melodic_chemical_conduit", "enderio_evolution:stellar_chemical_conduit")) {
                Conduit<?, ?> conduit = server.registryAccess().lookupOrThrow(EnderIORegistries.Keys.CONDUIT)
                    .getOrThrow(ResourceKey.create(EnderIORegistries.Keys.CONDUIT, Identifier.parse(id))).value();
                require(conduit instanceof ChemicalConduit, "chemical conduit missing " + id);
                checks.add("registered original conduit ID " + id);
            }
            require(((Map<?,?>)RecipePatches.lastReport.get("skipped")).isEmpty(), "recipe guard skipped");
            require(((Map<?,?>)RecipePatches.lastReport.get("applied")).size() == 87, "recipe count");
            require(RecipeSafety.lastReport.size() == 12, "material safety count");
            checks.add("87 source-guarded patches and 12 material safety repairs applied on dedicated server");
            var source = new java.util.concurrent.ConcurrentHashMap<Identifier,Integer>();
            source.put(Identifier.parse("sise_compat:test"), 1);
            var payload = new org.yanbwe.raritycore.network.RaritySyncPayload(source, source, List.of());
            source.clear();
            require(payload.itemRarityData().size() == 1 && payload.autoRarityData().size() == 1, "rarity snapshot");
            checks.add("RarityCore immutable snapshots work on dedicated server");
            var owner = new net.minecraft.core.HolderOwner<String>() {};
            var constructor = net.minecraft.core.HolderSet.Named.class.getDeclaredConstructor(net.minecraft.core.HolderOwner.class, net.minecraft.tags.TagKey.class);
            constructor.setAccessible(true);
            var named = constructor.newInstance(owner, net.minecraft.tags.TagKey.create(ResourceKey.<String>createRegistryKey(Identifier.parse("sise_compat:test_registry")),Identifier.parse("sise_compat:test_tag")));
            var callbacks = net.minecraft.core.HolderSet.Named.class.getDeclaredField("invalidationCallbacks");
            callbacks.setAccessible(true);
            require(callbacks.get(named) instanceof java.util.concurrent.CopyOnWriteArrayList<?>, "registry concurrency mixin");
            checks.add("thread-safe registry callbacks applied on dedicated server");
            report.put("passed", true);
        } catch (Throwable failure) {
            report.put("passed", false); report.put("error", failure.toString()); failure.printStackTrace();
        }
        report.put("checks", checks);
        try { Files.writeString(Path.of(System.getProperty("sise.compat.server.report")), new GsonBuilder().setPrettyPrinting().create().toJson(report)); }
        catch (java.io.IOException failure) { throw new RuntimeException(failure); }
        server.halt(false);
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
