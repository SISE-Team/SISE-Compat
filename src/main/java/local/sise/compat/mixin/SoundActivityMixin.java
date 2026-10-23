package local.sise.compat.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.Map;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** RSLS synchronizes each map operation, but the contains/get pair must be atomic too. */
@Mixin(SoundEngine.class)
public abstract class SoundActivityMixin {
    @Shadow private Map<SoundInstance, Integer> soundDeleteTime;

    @WrapMethod(method = "isActive")
    private boolean sise$atomicSoundActivity(SoundInstance sound, Operation<Boolean> original) {
        // RSLS 1.2.2 wraps this map with Collections.synchronizedMap in its constructor.
        // Keep every upstream return modifier, including RSLS's pending-sound handling.
        synchronized (soundDeleteTime) {
            return original.call(sound);
        }
    }
}
