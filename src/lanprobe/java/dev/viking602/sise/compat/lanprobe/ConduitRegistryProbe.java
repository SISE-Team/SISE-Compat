package dev.viking602.sise.compat.lanprobe;

import com.enderio.enderio.api.EnderIORegistries;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import local.sise.compat.chemical.ChemicalConduit;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Temporary registry synchronization diagnostics; never packaged in the installed mod. */
@Mod("sise_compat_lan_probe")
public final class ConduitRegistryProbe {
    public static final List<String> IDS = List.of("enderio:chemical", "enderio:pressurized_chemical",
        "enderio:ender_chemical", "enderio_evolution:melodic_chemical_conduit", "enderio_evolution:stellar_chemical_conduit");

    public ConduitRegistryProbe() {
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> inspect("server", event.getServer().registryAccess()));
    }

    public static void inspect(String side, RegistryAccess access) {
        Map<String, Object> report = new LinkedHashMap<>();
        Map<String, Integer> rates = new LinkedHashMap<>();
        try {
            var registry = access.lookupOrThrow(EnderIORegistries.Keys.CONDUIT);
            for (String id : IDS) {
                var value = registry.getValue(Identifier.parse(id));
                if (value instanceof ChemicalConduit chemical) rates.put(id, chemical.transferRatePerTick());
            }
            report.put("passed", rates.size() == IDS.size());
            report.put("registry_size", registry.size());
            report.put("chemical_rates", rates);
        } catch (Throwable error) {
            report.put("passed", false);
            report.put("error", error.toString());
        }
        String role = System.getProperty("watut.probeRole", "host");
        Path directory = Path.of(System.getProperty("watut.probeDir"));
        try {
            Files.createDirectories(directory);
            Files.writeString(directory.resolve(role + "-" + side + "-conduits.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
        } catch (Exception error) { throw new RuntimeException(error); }
    }
}
