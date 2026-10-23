package local.sise.compat;

import com.google.gson.*;
import java.nio.file.*;

public final class ProductionTests {
    public static void main(String[] args)throws Exception{
        int assertions=0;var rules=ProductionPatches.RULES;
        var corpus=JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonObject();
        var priority=rules.getAsJsonArray("priority");
        if(!priority.get(priority.size()-1).getAsString().equals("ic2"))throw new AssertionError("IC2 must remain last");assertions++;
        for(var entry:rules.getAsJsonObject("patches").entrySet()){
            var rule=entry.getValue().getAsJsonObject();var id=entry.getKey();
            if(!rule.get("before").equals(corpus.get(id)))throw new AssertionError("Outdated source "+id);
            if(!ProductionPatches.status(id,rule.get("before")).equals("apply"))throw new AssertionError(id);
            if(!ProductionPatches.status(id,rule.get("after")).equals("already_applied"))throw new AssertionError("Not idempotent "+id);
            var custom=rule.getAsJsonObject("before").deepCopy();custom.addProperty("user_override",true);
            if(!ProductionPatches.status(id,custom).equals("source_changed"))throw new AssertionError("User override overwritten "+id);
            if(rule.get("reason").getAsString().isBlank() || rule.get("reference").getAsString().isBlank())throw new AssertionError("Missing provenance "+id);
            assertions+=5;
        }
        if(!ProductionPatches.status("minecraft:stick",new JsonObject()).equals("unrelated"))throw new AssertionError("Unrelated recipe touched");
        System.out.println("Production source guards passed: "+(assertions+1)+" assertions");
    }
}
