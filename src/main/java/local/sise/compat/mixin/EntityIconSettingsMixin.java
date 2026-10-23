package local.sise.compat.mixin;

import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** An explicit modern icon setting supersedes the old .invisible marker for that same entity. */
@Pseudo
@Mixin(targets="dev.ftb.mods.ftblibrary.icon.EntityIconLoader",remap=false)
public abstract class EntityIconSettingsMixin {
    @Redirect(method="prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Ljava/util/Map;",at=@At(value="INVOKE",target="Lnet/minecraft/server/packs/resources/ResourceManager;getResource(Lnet/minecraft/resources/Identifier;)Ljava/util/Optional;"))
    private Optional<Resource> siseCompat$modernSettings(ResourceManager manager,Identifier path) {
        if(path.getNamespace().equals("ftblibrary")&&path.getPath().endsWith(".invisible")
                &&manager.getResource(path.withPath(path.getPath().replace(".invisible",".json"))).isPresent())return Optional.empty();
        return manager.getResource(path);
    }
}
