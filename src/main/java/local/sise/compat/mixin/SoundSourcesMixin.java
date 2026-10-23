package local.sise.compat.mixin;

import java.io.*;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Stacked sound registrations are validated before later overrides; migrate the original JSON too. */
@Mixin(SoundManager.class)
public abstract class SoundSourcesMixin {
    @Redirect(method="prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Lnet/minecraft/client/sounds/SoundManager$Preparations;",
        at=@At(value="INVOKE",target="Lnet/minecraft/server/packs/resources/Resource;openAsReader()Ljava/io/BufferedReader;"))
    private BufferedReader siseCompat$reviewedRegistrations(Resource resource) throws IOException {
        return new BufferedReader(local.sise.compat.ReviewedResources.soundReader(resource));
    }
}
