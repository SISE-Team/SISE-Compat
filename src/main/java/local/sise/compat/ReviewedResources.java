package local.sise.compat;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.*;
import net.minecraft.server.packs.resources.*;
import net.neoforged.fml.ModList;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.resource.*;

/** A required, highest-priority mod pack. Every replaced upstream resource has an exact source guard. */
public final class ReviewedResources {
    public static final Set<String> active = new TreeSet<>();
    public static final Set<String> skipped = new TreeSet<>();
    public static final Set<String> filtered = new TreeSet<>();
    public static int repairedAtlasReads;
    public static int repairedSoundReads;
    private static final JsonObject MANIFEST = json("resource_repairs.json");
    private static final JsonObject STACK = json("atlas_stack_repairs.json");
    private static final JsonObject FILTERS = json("resource_filters.json");
    private static Set<String> enabled;
    private static JsonObject json(String path) {
        try (var in = Objects.requireNonNull(ReviewedResources.class.getResourceAsStream("/sise_compat/"+path));
             var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException error) { throw new UncheckedIOException(error); }
    }
    private static String sha(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (java.security.NoSuchAlgorithmException error) { throw new AssertionError(error); }
    }
    private static synchronized Set<String> enabled() {
        if (enabled != null) return enabled;
        var files = new HashMap<String,JarContents>();
        for (var file : ModList.get().getModFiles()) files.put(file.getFile().getFileName(), file.getFile().getContents());
        var checked = new HashMap<String,Boolean>();
        for (var entry : MANIFEST.entrySet()) {
            boolean valid = true;
            var detail=entry.getValue().getAsJsonObject();
            for (var source : detail.getAsJsonArray("sources")) {
                var guard = source.getAsJsonObject();
                String file = guard.get("file").getAsString(), path = guard.get("path").getAsString();
                String key = file+"/"+path+"/"+guard.get("sha256").getAsString();
                valid &= checked.computeIfAbsent(key, unused -> {
                    var contents = files.get(file);
                    if (contents == null) return false;
                    var resource = contents.get(path);
                    if (resource == null) return false;
                    try (var stream = resource.open()) { return sha(stream.readAllBytes()).equals(guard.get("sha256").getAsString()); }
                    catch (IOException error) { throw new UncheckedIOException(error); }
                });
            }
            if(detail.has("external_sources"))for(var source:detail.getAsJsonArray("external_sources")) {
                var guard=source.getAsJsonObject();var path=net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().resolve(guard.get("path").getAsString()).normalize();
                if(!path.startsWith(net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().toAbsolutePath().normalize()))throw new IllegalArgumentException("External resource source outside game directory");
                if(java.nio.file.Files.exists(path))try {valid &= sha(java.nio.file.Files.readAllBytes(path)).equals(guard.get("sha256").getAsString());}catch(IOException error){throw new UncheckedIOException(error);}
            }
            if(detail.has("if_missing_items"))for(var id:detail.getAsJsonArray("if_missing_items"))valid &= !net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(Identifier.parse(id.getAsString()));
            (valid ? active : skipped).add(entry.getKey());
        }
        enabled = Set.copyOf(active);
        for (var entry : FILTERS.entrySet()) {
            var detail=entry.getValue().getAsJsonObject();boolean valid=true;
            for (var source : detail.getAsJsonArray("sources")) {
                var guard=source.getAsJsonObject();var file=files.get(guard.get("file").getAsString());
                var resource=file==null?null:file.get(guard.get("path").getAsString());
                if(resource==null){valid=false;break;}
                try(var stream=resource.open()){valid &= sha(stream.readAllBytes()).equals(guard.get("sha256").getAsString());}
                catch(IOException error){throw new UncheckedIOException(error);}
            }
            boolean missing=detail.has("if_missing_block")
                ? !net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(Identifier.parse(detail.get("if_missing_block").getAsString()))
                : detail.has("if_missing_mod")&&!ModList.get().isLoaded(detail.get("if_missing_mod").getAsString());
            if(valid&&missing)filtered.add(entry.getKey());
        }
        return enabled;
    }
    public static void packs(AddPackFindersEvent event) {
        var contents = ModList.get().getModFileById(SiseCompat.ID).getFile().getContents();
        var allowed = enabled();
        var supplier = new Pack.ResourcesSupplier() {
            @Override public PackResources openPrimary(PackLocationInfo location) { return new Filtered(location, contents, allowed); }
            @Override public PackResources openFull(PackLocationInfo location, Pack.Metadata metadata) { return openPrimary(location); }
        };
        var pack = Pack.readMetaAndCreate(new PackLocationInfo("mod/sise_compat:resource_repairs", Component.literal("SISE reviewed resource repairs"), PackSource.BUILT_IN, Optional.empty()),
                supplier, event.getPackType(), new PackSelectionConfig(true, Pack.Position.TOP, true));
        if (pack == null) throw new IllegalStateException("SISE resource repair pack metadata rejected");
        event.addRepositorySource(consumer -> consumer.accept(pack));
    }
    private static final class Filtered extends JarContentsPackResources {
        private final Set<String> allowed;
        Filtered(PackLocationInfo location, JarContents contents, Set<String> allowed) { super(location, contents, "resource_repairs"); this.allowed = allowed; }
        private boolean allows(PackType type, Identifier id) { return allowed.contains(type.getDirectory()+"/"+id.getNamespace()+"/"+id.getPath()); }
        @Override public IoSupplier<InputStream> getRootResource(String... path) {
            var original=super.getRootResource(path);
            if(path.length!=1||!path[0].equals("pack.mcmeta")||filtered.isEmpty())return original;
            return () -> {
                JsonObject metadata;
                try(var in=Objects.requireNonNull(original).get();var reader=new InputStreamReader(in,StandardCharsets.UTF_8)){metadata=JsonParser.parseReader(reader).getAsJsonObject();}
                var blocked=new JsonArray();
                for(String id:filtered){var parts=id.split("/",3);var rule=new JsonObject();rule.addProperty("namespace",java.util.regex.Pattern.quote(parts[1]));rule.addProperty("path",java.util.regex.Pattern.quote(parts[2]));blocked.add(rule);}
                var filter=new JsonObject();filter.add("block",blocked);metadata.add("filter",filter);
                return new ByteArrayInputStream(metadata.toString().getBytes(StandardCharsets.UTF_8));
            };
        }
        @Override public IoSupplier<InputStream> getResource(PackType type, Identifier id) { return allows(type,id) ? super.getResource(type,id) : null; }
        @Override public void listResources(PackType type, String namespace, String prefix, PackResources.ResourceOutput output) {
            super.listResources(type,namespace,prefix,(id,source) -> { if (allows(type,id)) output.accept(id,source); });
        }
    }
    /** Atlas JSON files stack rather than override; fix only the audited old definition. */
    public static Reader atlasReader(Resource resource) throws IOException {
        return stackedReader(resource,true);
    }
    public static Reader soundReader(Resource resource) throws IOException {
        return stackedReader(resource,false);
    }
    private static Reader stackedReader(Resource resource, boolean atlas) throws IOException {
        try (var reader = resource.openAsReader()) {
            var out = new StringWriter(); reader.transferTo(out);
            String text = out.toString();
            var replacement = STACK.get(sha(text.getBytes(StandardCharsets.UTF_8)));
            if (replacement != null) { synchronized (ReviewedResources.class) { if(atlas) repairedAtlasReads++; else repairedSoundReads++; } text = replacement.getAsJsonObject().get("value").toString(); }
            return new StringReader(text);
        }
    }
    private ReviewedResources() { }
}
