package local.sise.compat;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Exact upstream data guards preserve custom datapacks while repairing the reviewed duplicate. */
public final class ReviewedConversionData {
    private static final JsonObject RULE;
    public static volatile int iceAndFireRepairs;
    static {
        try (var reader = new InputStreamReader(ReviewedConversionData.class.getResourceAsStream("/sise_compat/iceandfire_emc.json"), StandardCharsets.UTF_8)) {
            RULE = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException error) { throw new ExceptionInInitializerError(error); }
    }
    public static Reader repairIceAndFire(Reader reader) throws IOException {
        var text = new StringWriter(); reader.transferTo(text);
        String source = text.toString();
        try {
            if (RULE.get("before").equals(JsonParser.parseString(source))) {
                iceAndFireRepairs++;
                return new StringReader(RULE.get("after").toString());
            }
        } catch (JsonParseException ignored) { /* The original parser retains responsibility for invalid data. */ }
        return new StringReader(source);
    }
    private ReviewedConversionData() { }
}
