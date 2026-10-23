package local.sise.compat;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public final class ConversionDataTests {
    public static void main(String[] args) throws Exception {
        JsonObject rule;
        try (var reader = new InputStreamReader(ConversionDataTests.class.getResourceAsStream("/sise_compat/iceandfire_emc.json"), StandardCharsets.UTF_8)) {
            rule = JsonParser.parseReader(reader).getAsJsonObject();
        }
        var repaired = JsonParser.parseReader(ReviewedConversionData.repairIceAndFire(new StringReader(rule.get("before").toString())));
        if (!repaired.equals(rule.get("after"))) throw new AssertionError("Reviewed duplicate not repaired");
        var values = repaired.getAsJsonObject().getAsJsonObject("values").getAsJsonArray("before");
        int talons = 0;
        for (var e : values) if (e.getAsJsonObject().has("id") && e.getAsJsonObject().get("id").getAsString().equals("iceandfire:hippogryph_talon")) {
            talons++;
            if (e.getAsJsonObject().get("emc_value").getAsInt() != 4096) throw new AssertionError("ProjectE first-entry value changed");
        }
        if (talons != 1) throw new AssertionError("Duplicate remains");
        var custom = rule.getAsJsonObject("before").deepCopy(); custom.addProperty("user_override", true);
        if (!JsonParser.parseReader(ReviewedConversionData.repairIceAndFire(new StringReader(custom.toString()))).equals(custom))
            throw new AssertionError("User override changed");
        if (!JsonParser.parseReader(ReviewedConversionData.repairIceAndFire(new StringReader(repaired.toString()))).equals(repaired))
            throw new AssertionError("Not idempotent");
        System.out.println("Ice and Fire EMC source guard, first-entry value, uniqueness, user override and idempotence passed");
    }
}
