package local.sise.compat;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.io.*;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.event.ModifyRecipeJsonsEvent;
import net.neoforged.fml.loading.FMLPaths;

/** The complete reviewed production profile is applied after identity and conflict fixes. */
public final class ProductionPatches {
    public static final JsonObject RULES;
    public static volatile Map<String,Object> lastReport=Map.of();
    static{
        try(var reader=new InputStreamReader(Objects.requireNonNull(ProductionPatches.class.getResourceAsStream("/sise_compat/production.json")),StandardCharsets.UTF_8)){
            RULES=JsonParser.parseReader(reader).getAsJsonObject();
        }catch(IOException e){throw new ExceptionInInitializerError(e);}
    }
    public static String status(String id,JsonElement value){
        var rule=RULES.getAsJsonObject("patches").getAsJsonObject(id);if(rule==null)return "unrelated";
        if(rule.get("after").equals(value))return "already_applied";
        return rule.get("before").equals(value)?"apply":"source_changed";
    }
    public static void apply(ModifyRecipeJsonsEvent event){
        var applied=new TreeMap<String,String>();var skipped=new TreeMap<String,String>();
        for(var entry:RULES.getAsJsonObject("patches").entrySet()){
            var id=Identifier.parse(entry.getKey());var rule=entry.getValue().getAsJsonObject();var value=event.getRecipeJsons().get(id);
            if(value==null){skipped.put(entry.getKey(),"recipe absent");continue;}
            String status=status(entry.getKey(),value);
            if(status.equals("apply")){
                if(rule.get("after").isJsonNull())event.getRecipeJsons().remove(id);else event.getRecipeJsons().put(id,rule.get("after").deepCopy());
                applied.put(entry.getKey(),rule.get("reference").getAsString());
            }else if(!status.equals("already_applied"))skipped.put(entry.getKey(),status);
        }
        lastReport=Map.of("applied",applied,"skipped",skipped);
        try{var path=FMLPaths.GAMEDIR.get().resolve("logs/sise-compat-production.json");Files.createDirectories(path.getParent());Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(lastReport));}
        catch(IOException e){throw new IllegalStateException(e);}
    }
    private ProductionPatches(){}
}
