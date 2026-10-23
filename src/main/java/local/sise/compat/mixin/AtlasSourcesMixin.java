package local.sise.compat.mixin;

import java.io.*;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceList;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(SpriteSourceList.class)
public abstract class AtlasSourcesMixin {
    @Redirect(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/resources/Resource;openAsReader()Ljava/io/BufferedReader;"))
    private static BufferedReader siseCompat$reviewedAtlasSources(Resource resource) throws IOException {
        return new BufferedReader(local.sise.compat.ReviewedResources.atlasReader(resource));
    }
}
