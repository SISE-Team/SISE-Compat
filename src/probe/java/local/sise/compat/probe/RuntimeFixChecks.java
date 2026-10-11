package local.sise.compat.probe;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.fml.ModList;

/** Check real transformed classes and data, with no standalone compatibility mods present. */
final class RuntimeFixChecks {
    static Map<String, List<String>> firstStructures;
    static List<String> verify(MinecraftServer server) throws Exception {
        var checks = new ArrayList<String>();
        for (String id : List.of("registry_concurrency_fix", "enderio_chemical_compat"))
            require(!ModList.get().isLoaded(id), "retired mod still loaded: " + id);
        checks.add("only SISE Compat supplies migrated runtime fixes");
        TitaniumChecks.verify(server);
        checks.add("original Titanium inventory finder: every slot and invalid index; normal pickup and four backpack locations conserve items");
        for (char value = 0; value < 65535; value++) {
            boolean expected = value == '_' || value == '-' || value >= 'a' && value <= 'z'
                || value >= '0' && value <= '9' || value == '.';
            require(Identifier.validNamespaceChar(value) == expected, "namespace char " + (int)value);
        }
        require(!Identifier.validNamespaceChar((char)65535), "namespace final char");
        for (String value : List.of("invalid namespace", "ABC", "bad/path", "..", "中文", "bad:ns"))
            require(!Identifier.isValidNamespace(value) && Identifier.tryBuild(value,"test") == null, "invalid namespace " + value);
        for (String value : List.of("minecraft", "sise_compat", "a-b.c_1", "", "."))
            require(Identifier.isValidNamespace(value), "valid namespace " + value);
        require(Identifier.isValidPath("Effects/My Effect") && !Identifier.isValidPath("DUMMY"), "AAA effect paths changed");
        var factory = (mod.chloeprime.aaaparticles.client.internal.LimitlessResourceLocationFactory)(Object)Identifier.parse("sise_compat:factory");
        var effect = factory.aaa$createUninitialized("aaa_particles", "Effects/My Effect");
        require(effect.getNamespace().equals("aaa_particles") && effect.getPath().equals("Effects/My Effect"), "AAA factory changed");
        checks.add("all 65536 namespace characters validated; AAA effect paths and factory preserved");
        var biomes = server.registryAccess().lookupOrThrow(Registries.BIOME);
        var structures = server.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var ocean = TagKey.create(Registries.BIOME, Identifier.parse("minecraft:is_ocean"));
        var actualStructures = new TreeMap<String, List<String>>();
        var definitions = new ArrayList<com.google.gson.JsonElement>();
        for (String name : List.of("tower_leaf", "tower_main", "tower_sand", "tower_spruce")) {
            var id = Identifier.fromNamespaceAndPath("apotheosis", name);
            var structure = structures.getOrThrow(ResourceKey.create(Registries.STRUCTURE, id)).value();
            var tag = TagKey.create(Registries.BIOME, id);
            var expected = biomes.listElements().filter(b -> b.is(tag) && !b.is(ocean)).map(b -> b.key().identifier().toString()).sorted().toList();
            var actual = structure.biomes().stream().map(b -> b.unwrapKey().orElseThrow().identifier().toString()).sorted().toList();
            require(!expected.isEmpty() && expected.equals(actual), "tower biome filter " + id);
            actualStructures.put(name, actual);
            try (var reader = server.getResourceManager().getResource(Identifier.parse("apotheosis:worldgen/structure/" + name + ".json")).orElseThrow().openAsReader()) {
                definitions.add(JsonParser.parseReader(reader));
            }
        }
        if (firstStructures == null) firstStructures = actualStructures;
        else require(firstStructures.equals(actualStructures), "tower biome filters changed after resource reload");
        var ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var start = new CyclicBarrier(16);
        var tasks = new ArrayList<Future<?>>();
        try (var pool = Executors.newFixedThreadPool(16)) {
            for (int t = 0; t < 16; t++) tasks.add(pool.submit(() -> {
                start.await(30, TimeUnit.SECONDS);
                for (int i = 0; i < 64; i++) for (var definition : definitions) {
                    var parsed = Structure.DIRECT_CODEC.parse(ops, definition).getOrThrow();
                    require(parsed.biomes().isBound() && parsed.biomes().size() > 0, "parallel structure decode");
                }
                return null;
            }));
            for (var task : tasks) task.get(60, TimeUnit.SECONDS);
        }
        checks.add("4096 real Apotheosis structure decodes on 16 threads; four biome filters preserved across reload");
        return checks;
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
