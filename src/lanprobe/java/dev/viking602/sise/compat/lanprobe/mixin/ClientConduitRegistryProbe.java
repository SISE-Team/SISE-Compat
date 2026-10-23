package dev.viking602.sise.compat.lanprobe.mixin;

import dev.viking602.sise.compat.lanprobe.ConduitRegistryProbe;
import net.minecraft.client.multiplayer.RegistryDataCollector;
import net.minecraft.core.RegistryAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RegistryDataCollector.class)
public abstract class ClientConduitRegistryProbe {
    @Inject(method = "collectGameRegistries", at = @At("RETURN"))
    private void probe$registries(CallbackInfoReturnable<RegistryAccess.Frozen> result) {
        ConduitRegistryProbe.inspect("client", result.getReturnValue());
    }
}
