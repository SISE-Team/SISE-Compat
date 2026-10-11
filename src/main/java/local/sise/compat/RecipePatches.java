package local.sise.compat;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.ModifyRecipeJsonsEvent;

/** Exact reviewed sources protect user overrides and detect incompatible upstream updates. */
public final class RecipePatches {
    public static final JsonObject RULES;
    public static volatile Map<String,Object> lastReport = Map.of();
    static {
        try (var reader = new InputStreamReader(Objects.requireNonNull(RecipePatches.class.getResourceAsStream("/sise_compat/rules.json")),StandardCharsets.UTF_8)) {
            RULES = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) { throw new ExceptionInInitializerError(e); }
    }
    public static String patch(String id, JsonElement value) {
        var entry = RULES.getAsJsonObject("patches").getAsJsonObject(id);
        if (entry == null) return "unrelated";
        if (entry.get("after").equals(value)) return "already_applied";
        return entry.get("before").equals(value) ? "apply" : "source_changed";
    }
    public static void apply(ModifyRecipeJsonsEvent event) {
        var applied = new TreeMap<String,String>();
        var skipped = new TreeMap<String,String>();
        for (var rule : RULES.getAsJsonObject("patches").entrySet()) {
            var id = Identifier.parse(rule.getKey()); var value = event.getRecipeJsons().get(id);
            if (value == null) { skipped.put(rule.getKey(),"recipe absent"); continue; }
            String status = patch(rule.getKey(),value);
            if (status.equals("apply")) {
                event.getRecipeJsons().put(id,rule.getValue().getAsJsonObject().get("after").deepCopy());
                applied.put(rule.getKey(),rule.getValue().getAsJsonObject().get("reason").getAsString());
            } else if (!status.equals("already_applied")) {
                skipped.put(rule.getKey(),status);
                LogUtils.getLogger().warn("SISE Compat left changed upstream recipe untouched: {}",id);
            }
        }
        lastReport = Map.of("applied",applied,"skipped",skipped,"missing_item_recipe","avaritia:eio_creative_power");
        try {
            var path = FMLPaths.GAMEDIR.get().resolve("logs/sise-compat-recipes.json");
            Files.createDirectories(path.getParent());
            Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(lastReport));
        } catch (IOException e) { LogUtils.getLogger().warn("SISE Compat could not save report",e); }
        LogUtils.getLogger().info("SISE Compat: {} reviewed patches applied; {} need review",applied.size(),skipped.size());
    }
}
