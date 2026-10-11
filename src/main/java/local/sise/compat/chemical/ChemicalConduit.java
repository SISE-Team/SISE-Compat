package local.sise.compat.chemical;

import com.enderio.enderio.api.conduits.*;
import com.enderio.enderio.api.conduits.connection.path.ConnectionPathProperty;
import com.enderio.enderio.api.conduits.connection.path.ConnectionPathPropertyConsumer;
import com.enderio.enderio.api.conduits.network.node.ConduitNode;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import java.util.function.Consumer;

public record ChemicalConduit(Identifier texture, Component description, int transferRatePerTick)
        implements Conduit<ChemicalConduit, ChemicalConnectionConfig> {
    public static final MapCodec<ChemicalConduit> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
        Identifier.CODEC.fieldOf("texture").forGetter(ChemicalConduit::texture),
        ComponentSerialization.CODEC.fieldOf("description").forGetter(ChemicalConduit::description),
        Codec.intRange(1, Integer.MAX_VALUE).fieldOf("transfer_rate").forGetter(ChemicalConduit::transferRatePerTick)
    ).apply(builder, ChemicalConduit::new));
    public static final ConnectionPathProperty<Integer> PATH_RATE = ConnectionPathProperty.minInt(0);
    @Override public ConduitType<ChemicalConduit, ChemicalConnectionConfig> type() { return ChemicalCompat.CHEMICAL.get(); }
    @Override public boolean hasMenu() { return true; }
    @Override public boolean canReplaceConduit(ChemicalConduit other) { return compareTo(other) > 0; }
    @Override public boolean canConnectToConduit(ChemicalConduit other) { return true; }
    @Override public void collectNodePathProperties(ConduitNode node, ConnectionPathPropertyConsumer consumer) {
        consumer.accept(PATH_RATE, transferRatePerTick);
    }
    @Override public boolean canConnectToBlock(Level level, ConduitCapabilityAccessor accessor, BlockPos pos, Direction side) {
        return accessor.getSidedCapability(Capabilities.CHEMICAL.block(), side) != null;
    }
    @Override public int compareTo(ChemicalConduit other) { return Integer.compare(transferRatePerTick, other.transferRatePerTick); }
    @Override public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
        consumer.accept(Component.translatable("tooltip.sise_compat.chemical_rate", String.format("%,d", transferRatePerTick)));
    }
}
