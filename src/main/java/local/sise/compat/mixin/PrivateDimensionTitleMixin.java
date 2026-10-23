package local.sise.compat.mixin;

import com.yungnickyoung.minecraft.travelerstitles.TravelersTitlesCommon;
import com.yungnickyoung.minecraft.travelerstitles.module.SoundModule;
import com.yungnickyoung.minecraft.travelerstitles.render.TitleRenderManager;
import com.yungnickyoung.minecraft.travelerstitles.render.TitleRenderer;
import dev.viking602.sise.space.client.SpaceClientState;
import java.util.Objects;
import local.sise.core.space.SpaceData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Dynamic spaces share one dimension type, but each owner has a different title. */
@Mixin(TitleRenderManager.class)
public abstract class PrivateDimensionTitleMixin {
    @Shadow @Final public TitleRenderer<DimensionType> dimensionTitleRenderer;
    @Unique private Level sise$lastSpace;
    @Unique private String sise$lastName;

    @Inject(method="updateDimensionTitle",at=@At("HEAD"),cancellable=true)
    private void sise$ownerTitle(Level level,Player player,boolean underground,CallbackInfo callback){
        if(SpaceData.owner(level.dimension())==null){sise$lastSpace=null;sise$lastName=null;return;}
        callback.cancel();var config=TravelersTitlesCommon.CONFIG.dimensions;
        if(!dimensionTitleRenderer.enabled||underground&&config.onlyUpdateAtSurface||config.dimensionBlacklist.contains(level.dimension().identifier().toString()))return;
        var title=SpaceClientState.dimensionName(level.dimension());var name=title.getString();
        if(level==sise$lastSpace&&Objects.equals(name,sise$lastName))return;
        boolean entered=level!=sise$lastSpace;sise$lastSpace=level;sise$lastName=name;
        dimensionTitleRenderer.setColor(dimensionTitleRenderer.titleDefaultTextColor);
        dimensionTitleRenderer.displayTitle(title,null);
        if(entered)player.playSound(SoundModule.DIMENSION.get(),(float)TravelersTitlesCommon.CONFIG.sound.dimensionVolume,(float)TravelersTitlesCommon.CONFIG.sound.dimensionPitch);
    }
}
