package local.sise.compat;

import com.google.gson.*;
import java.nio.file.*;
import local.sise.ores.RecipeTransform;
import local.sise.compat.materials.RecipeSafety;

/** Export the exact JSON stage seen by the final production compatibility listener. */
public final class ProductionCorpus {
    public static void main(String[] args)throws Exception{
        var root=JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonObject();
        var out=new JsonObject();
        for(var entry:root.getAsJsonObject("raw_before").entrySet()){
            var value=entry.getValue().deepCopy();if(!value.isJsonObject() || entry.getKey().startsWith("sise_ores:"))continue;
            if(RecipePatches.patch(entry.getKey(),value).equals("apply"))value=RecipePatches.RULES.getAsJsonObject("patches").getAsJsonObject(entry.getKey()).get("after").deepCopy();
            var transformed=RecipeTransform.transform(value.getAsJsonObject()).json();RecipeSafety.resolve(entry.getKey(),transformed);out.add(entry.getKey(),transformed);
        }
        Files.writeString(Path.of(args[1]),new GsonBuilder().setPrettyPrinting().create().toJson(out));
        System.out.println("Production corpus: "+out.size());
    }
}
