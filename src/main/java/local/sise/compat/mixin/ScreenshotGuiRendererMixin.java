package local.sise.compat.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.xiaopiao.ae2ss.api.CraftingPlanScreenshotHelper;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Select the screenshot target after LDLib2's redirect has chosen the normal target. */
@Mixin(value = GuiRenderer.class, remap = false)
public abstract class ScreenshotGuiRendererMixin {
    @ModifyVariable(method = "draw", at = @At("STORE"), ordinal = 0, require = 1, remap = false)
    private RenderTarget siseCompat$screenshotTarget(RenderTarget normalTarget) {
        return CraftingPlanScreenshotHelper.resolveRenderTarget(normalTarget);
    }
}
