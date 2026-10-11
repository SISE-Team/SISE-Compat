package local.sise.compat.mixin;

import net.minecraft.network.protocol.common.ServerCommonPacketListener;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.ConfigurationTask;
import net.neoforged.neoforge.network.handling.ServerPayloadContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Complete Artifacts configuration on the packet processor thread before preparing spawn. */
@Pseudo
@Mixin(targets = "artifacts.neoforge.ArtifactsNeoForge$1", remap = false)
public abstract class ArtifactsConfigurationMixin {
    @Redirect(method = "start", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/network/protocol/configuration/ServerConfigurationPacketListener;finishCurrentTask(Lnet/minecraft/server/network/ConfigurationTask$Type;)V"), remap = false)
    private void siseCompat$finish(ServerConfigurationPacketListener listener, ConfigurationTask.Type type) {
        var context = new ServerPayloadContext((ServerCommonPacketListener) listener,
            Identifier.fromNamespaceAndPath("artifacts", "configuration"));
        context.enqueueWork(() -> listener.finishCurrentTask(type));
    }
}
