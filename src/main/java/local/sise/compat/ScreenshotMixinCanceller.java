package local.sise.compat;

import com.bawnorton.mixinsquared.canceller.MixinCancellerRegistrar;

/** Replace only AE2 Screenshot's conflicting redirect; all other upstream Mixins remain. */
public final class ScreenshotMixinCanceller {
    public static void register() {
        MixinCancellerRegistrar.register((targets, name) ->
            name.equals("com.xiaopiao.ae2ss.mixin.GuiRendererMixin"));
    }
    private ScreenshotMixinCanceller() {}
}
