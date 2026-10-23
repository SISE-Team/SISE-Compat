package local.sise.compat;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import twilightforest.entity.passive.quest.ram.QuestingRamContext;

/** The port's initial data reload precedes the integrated server; decode once its registries exist. */
public final class TwilightQuestData {
    private static boolean listening;
    private static Map<Identifier,JsonElement> pending;
    private static Consumer<QuestingRamContext> setter;
    public static int applied;
    public static synchronized void defer(Map<Identifier,JsonElement> source, Consumer<QuestingRamContext> target) {
        pending = Map.copyOf(source); setter = target;
        if (!listening) { listening = true; NeoForge.EVENT_BUS.addListener(TwilightQuestData::start); }
    }
    private static synchronized void start(ServerAboutToStartEvent event) {
        if (pending == null) return;
        var ops = event.getServer().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        boolean found = false;
        for (var entry : pending.entrySet()) if (entry.getKey().getPath().equals("questing_ram")) {
            setter.accept(QuestingRamContext.CODEC.parse(ops,entry.getValue()).getOrThrow()); found=true; applied++;
        }
        if (!found) setter.accept(QuestingRamContext.FALLBACK);
        pending=null; setter=null;
    }
    private TwilightQuestData() { }
}
