package local.sise.compat;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.sise.compat.materials.RecipeSafety;
import local.sise.ores.RecipeTransform;

/** Test migrated conflicts against actual upstream recipes and unified outputs. */
public final class MaterialSafetyTests {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++; if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        var corpus = JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonObject();
        int fixed = 0;
        for (var entry : corpus.entrySet()) {
            String[] path = entry.getKey().split("/", 4);
            String id = path[1] + ":" + path[3].substring(0, path[3].length()-5);
            if (!entry.getValue().isJsonObject()) continue;
            var recipe = RecipeTransform.transform(entry.getValue().getAsJsonObject()).json();
            var output = recipe.has("result") ? recipe.get("result").deepCopy() : null;
            var reason = RecipeSafety.resolve(id, recipe);
            if (reason.isEmpty()) continue;
            fixed++;
            if (id.equals("modern_industrialization:materials/sulfur/craft/tiny_dust_from_dust")) {
                var legacy = output.getAsJsonObject(); legacy.add("item",legacy.remove("id"));
            }
            check(Objects.equals(output, recipe.get("result")), "exact output retained " + id);
            check(RecipeSafety.resolve(id, recipe).isEmpty(), "idempotent " + id);
            if (RecipeSafety.DUST_SPLITS.contains(id)) {
                check(recipe.get("type").getAsString().equals("ic2:shapeless"), "workbench split " + id);
                check(recipe.getAsJsonArray("ingredients").size()==2 && recipe.getAsJsonArray("ingredients").get(1).getAsString().equals("ic2:forge_hammer"), "hammer selects subdivision " + id);
                check(!recipe.get("consuming").getAsBoolean() && !RecipeSafety.manualOnly(id), "native tool remainder retained " + id);
            }
        }
        check(fixed == 7, "one hammer split, four steel and two silicon conflicts repaired: " + fixed);
        for (var id : RecipeSafety.DUST_SPLITS) if (id.startsWith("oritech:")) {
            var path="data/oritech/recipe/"+id.substring(8)+".json";
            var recipe=RecipeTransform.transform(corpus.getAsJsonObject(path)).json();
            check(RecipeSafety.resolve(id,recipe).isEmpty() && recipe.get("type").getAsString().equals("minecraft:crafting_shapeless"),"unified powder subdivision stays on workbench "+id);
            check(recipe.getAsJsonObject("result").get("count").getAsInt()==9,"original nine tiny powders retained "+id);
        }
        check(RecipeSafety.manualOnly("sise_ores:convert/tin_ingot/mekanism/ingot_tin"), "legacy material conversion remains manual");
        check(!RecipeSafety.manualOnly("minecraft:stone"), "normal recipes remain automatic");
        check(!RecipeSafety.manualOnly("ic2:shapeless/uranium_238"), "isotope unpacking stays on workbench");
        var unknown = JsonParser.parseString("{\"type\":\"minecraft:crafting_shaped\",\"key\":{\"I\":\"#c:ingots/steel\"},\"pattern\":[\"I\"],\"result\":{\"id\":\"sise_ores:steel_block\"}}").getAsJsonObject();
        var before = unknown.deepCopy();
        check(RecipeSafety.resolve("user:custom_steel_block", unknown).isEmpty() && before.equals(unknown), "unreviewed steel recipe preserved");
        System.out.println("Migrated material conflict guards passed: " + checks + " assertions; " + fixed + " real recipes");
    }
}
