package local.sise.compat.materials;

import com.google.gson.*;
import java.util.*;

/** Explicit conflict resolutions verified against this instance's live recipe registry. */
public final class RecipeSafety {
    public static volatile Map<String, String> lastReport = Map.of();
    public static void apply(net.neoforged.neoforge.event.ModifyRecipeJsonsEvent event) {
        var changed = new TreeMap<String, String>();
        event.getRecipeJsons().forEach((id, value) -> {
            if (!value.isJsonObject()) return;
            var recipe = value.getAsJsonObject().deepCopy();
            var reason = resolve(id.toString(), recipe);
            if (!reason.isEmpty()) { event.getRecipeJsons().put(id, recipe); changed.put(id.toString(), reason); }
        });
        lastReport = Collections.unmodifiableMap(changed);
        try {
            var path = net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().resolve("logs/sise-compat-materials.json");
            java.nio.file.Files.createDirectories(path.getParent());
            java.nio.file.Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(changed));
        } catch (java.io.IOException error) { com.mojang.logging.LogUtils.getLogger().warn("Could not save SISE Compat material report", error); }
    }
    public static final Set<String> DUST_SPLITS = Set.of(
        "oritech:crafting/small_iron_dust_from_unpacking",
        "oritech:crafting/small_gold_dust_from_unpacking",
        "oritech:crafting/small_copper_dust_from_unpacking",
        "oritech:crafting/small_nickel_dust_from_unpacking",
        "oritech:crafting/small_platinum_dust_from_unpacking",
        "oritech:uranium_dustblockinv", "oritech:plutonium_dustblockinv",
        "modern_industrialization:materials/sulfur/craft/tiny_dust_from_dust");

    public static boolean manualOnly(String id) {
        return id.startsWith("sise_ores:convert/");
    }

    public static String resolve(String id, JsonObject recipe) {
        if (!recipe.has("type") || !recipe.get("type").isJsonPrimitive()) return "";
        // The public silicon block tag contains two distinct 9:1 storage families.
        if (id.equals("oritech:silicon_blockblockinv") && recipe.has("ingredients")) {
            var inputs = recipe.getAsJsonArray("ingredients");
            if (inputs.size() == 1 && inputs.get(0).equals(new JsonPrimitive("#c:storage_blocks/silicon"))) {
                inputs.set(0, new JsonPrimitive("#sise_ores:equivalents/storage_blocks/ordinary_silicon"));
                return "ordinary silicon unpacking excludes ingot blocks";
            }
        }
        if (id.equals("modern_industrialization:materials/silicon/unpacker/ingot") && recipe.has("item_inputs")) {
            var inputs = recipe.getAsJsonArray("item_inputs");
            if (inputs.size() == 1 && inputs.get(0).getAsJsonObject().get("ingredient").equals(new JsonPrimitive("#c:storage_blocks/silicon"))) {
                inputs.get(0).getAsJsonObject().addProperty("ingredient", "#sise_ores:equivalents/storage_blocks/silicon_metal");
                return "silicon ingot unpacking excludes ordinary silicon blocks";
            }
        }
        // Unified 1/9 powders now share one result; the seven Oritech splits can stay on the workbench.
        // Sulfur alone also has a single-powder dye recipe, so the reusable IC2 hammer selects subdivision.
        if (id.equals("modern_industrialization:materials/sulfur/craft/tiny_dust_from_dust")
                && recipe.get("type").getAsString().equals("minecraft:crafting_shapeless")
                && recipe.getAsJsonArray("ingredients").size() == 1) {
            recipe.getAsJsonArray("ingredients").add("ic2:forge_hammer");
            recipe.remove("category");
            recipe.addProperty("type", "ic2:shapeless");
            recipe.addProperty("consuming", false);
            var result = recipe.getAsJsonObject("result");
            if (result.has("id")) result.add("item", result.remove("id"));
            return "workbench sulfur subdivision with reusable hammer; original 1:9 yield retained";
        }
        if (Set.of("mekanism:storage_blocks/steel", "modern_industrialization:materials/steel/craft/block_from_ingot", "oritech:steelblock", "ic2:shaped/steel_block").contains(id)
                && Set.of("minecraft:crafting_shaped", "ic2:shaped").contains(recipe.get("type").getAsString())
                && recipe.has("result") && recipe.get("result").isJsonObject()
                && ((recipe.getAsJsonObject("result").has("id") && recipe.getAsJsonObject("result").get("id").getAsString().equals("sise_ores:steel_block"))
                    || (recipe.getAsJsonObject("result").has("item") && recipe.getAsJsonObject("result").get("item").isJsonPrimitive()
                        && recipe.getAsJsonObject("result").get("item").getAsString().equals("sise_ores:steel_block")))) {
            boolean changed = false;
            for (var entry : recipe.getAsJsonObject("key").entrySet()) {
                if (entry.getValue().equals(new JsonPrimitive("#c:ingots/steel"))) {
                    entry.setValue(new JsonPrimitive("#sise_ores:equivalents/ingots/steel"));
                    changed = true;
                } else if (entry.getValue().isJsonObject() && entry.getValue().getAsJsonObject().has("tag")
                        && Set.of("forge:ingots/steel", "c:ingots/steel").contains(entry.getValue().getAsJsonObject().get("tag").getAsString())) {
                    entry.getValue().getAsJsonObject().addProperty("tag", "sise_ores:equivalents/ingots/steel");
                    changed = true;
                }
            }
            if (changed) return "ordinary steel packing excludes biosteel";
        }
        return "";
    }
    private RecipeSafety() {}
}
