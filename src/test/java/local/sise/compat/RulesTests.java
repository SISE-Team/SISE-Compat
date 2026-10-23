package local.sise.compat;

import com.google.gson.*;
import java.util.*;

public final class RulesTests {
    public static void main(String[] args) {
        int assertions = 0;
        for (var entry : RecipePatches.RULES.getAsJsonObject("patches").entrySet()) {
            var r = entry.getValue().getAsJsonObject();
            if (!r.has("reason") || !r.get("reason").isJsonPrimitive() || r.get("reason").getAsString().isBlank())
                throw new AssertionError("Missing runtime patch metadata: "+entry.getKey());
            if (!RecipePatches.patch(entry.getKey(),r.get("before")).equals("apply")) throw new AssertionError(entry.getKey());
            if (!RecipePatches.patch(entry.getKey(),r.get("after")).equals("already_applied")) throw new AssertionError("not idempotent");
            var custom = r.getAsJsonObject("before").deepCopy(); custom.addProperty("user_customization",true);
            if (!RecipePatches.patch(entry.getKey(),custom).equals("source_changed")) throw new AssertionError("overwrites custom recipe");
            assertions += 4;
        }
        if (!RecipePatches.patch("minecraft:stick",new JsonObject()).equals("unrelated")) throw new AssertionError("unrelated recipe touched");
        System.out.println("Compatibility guards passed: "+(assertions+1)+" assertions");
    }
}
