package local.sise.compat.mixin;

import com.google.gson.JsonElement;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.entity.passive.quest.QuestReloadListener;
import twilightforest.entity.passive.quest.ram.QuestingRamCurrentContext;

@Mixin(value=QuestReloadListener.class,remap=false)
public abstract class TwilightQuestDataMixin {
    @Shadow private static QuestingRamCurrentContext questingRamCurrentContext;
    @Inject(method="apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",at=@At("HEAD"),cancellable=true)
    private void siseCompat$initialRegistryContext(Map<Identifier,JsonElement> data, ResourceManager manager, ProfilerFiller profiler, CallbackInfo ci) {
        if (ServerLifecycleHooks.getCurrentServer()==null) {
            local.sise.compat.TwilightQuestData.defer(data, context -> questingRamCurrentContext.setContext(context));
            ci.cancel();
        }
    }
}
