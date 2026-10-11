package local.sise.compat.chemical;

import com.enderio.enderio.api.conduits.ConduitRedstoneSignalAware;
import com.enderio.enderio.api.conduits.connection.config.ConnectionConfig;
import com.enderio.enderio.api.conduits.connection.config.ConnectionConfigType;
import com.enderio.enderio.api.conduits.connection.config.IOConnectionConfig;
import com.enderio.enderio.api.conduits.connection.config.RedstoneSensitiveConnectionConfig;
import com.enderio.enderio.api.io.RedstoneControl;
import com.enderio.enderio.api.network.MassiveStreamCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;

import java.util.List;

public record ChemicalConnectionConfig(boolean isInsert, DyeColor insertChannel, boolean isExtract,
        DyeColor extractChannel, RedstoneControl extractRedstoneControl, DyeColor extractRedstoneChannel, int insertPriority)
        implements IOConnectionConfig, RedstoneSensitiveConnectionConfig {

    public static final ChemicalConnectionConfig DEFAULT = new ChemicalConnectionConfig(false, DyeColor.GREEN, true,
            DyeColor.GREEN, RedstoneControl.NEVER_ACTIVE, DyeColor.RED, 0);

    public static final MapCodec<ChemicalConnectionConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(Codec.BOOL.fieldOf("is_insert").forGetter(ChemicalConnectionConfig::isInsert),
                    DyeColor.CODEC.fieldOf("insert_channel").forGetter(ChemicalConnectionConfig::insertChannel),
                    Codec.BOOL.fieldOf("is_extract").forGetter(ChemicalConnectionConfig::isExtract),
                    DyeColor.CODEC.fieldOf("extract_channel").forGetter(ChemicalConnectionConfig::extractChannel),
                    RedstoneControl.CODEC.fieldOf("extract_redstone_control")
                            .forGetter(ChemicalConnectionConfig::extractRedstoneControl),
                    DyeColor.CODEC.fieldOf("extract_redstone_channel")
                            .forGetter(ChemicalConnectionConfig::extractRedstoneChannel),
                    Codec.INT.optionalFieldOf("insert_priority", 0)
                            .forGetter(ChemicalConnectionConfig::insertPriority))
            .apply(instance, ChemicalConnectionConfig::new));

    // @formatter:off
    public static final StreamCodec<ByteBuf, ChemicalConnectionConfig> STREAM_CODEC = MassiveStreamCodec.composite(
        ByteBufCodecs.BOOL,
        ChemicalConnectionConfig::isInsert,
        DyeColor.STREAM_CODEC,
        ChemicalConnectionConfig::insertChannel,
        ByteBufCodecs.BOOL,
        ChemicalConnectionConfig::isExtract,
        DyeColor.STREAM_CODEC,
        ChemicalConnectionConfig::extractChannel,
        RedstoneControl.STREAM_CODEC,
        ChemicalConnectionConfig::extractRedstoneControl,
        DyeColor.STREAM_CODEC,
        ChemicalConnectionConfig::extractRedstoneChannel,
        ByteBufCodecs.INT,
        ChemicalConnectionConfig::insertPriority,
        ChemicalConnectionConfig::new);
    // @formatter:on

    public static final ConnectionConfigType<ChemicalConnectionConfig> TYPE = new ConnectionConfigType<>(CODEC,
            STREAM_CODEC.cast(), () -> DEFAULT);

    @Override
    public ConnectionConfig reconnected() {
        return new ChemicalConnectionConfig(DEFAULT.isInsert, insertChannel, DEFAULT.isExtract, extractChannel,
                extractRedstoneControl, extractRedstoneChannel, insertPriority);
    }

    @Override
    public ConnectionConfig disconnected() {
        return new ChemicalConnectionConfig(false, insertChannel, false, extractChannel, extractRedstoneControl,
                extractRedstoneChannel, insertPriority);
    }

    @Override
    public boolean canInsert(ConduitRedstoneSignalAware signalAware) {
        // TODO: insertRedstoneControl
        return isInsert();
    }

    @Override
    public boolean canExtract(ConduitRedstoneSignalAware signalAware) {
        if (!isExtract()) {
            return false;
        }

        if (extractRedstoneControl.isRedstoneSensitive()) {
            return extractRedstoneControl.isActive(signalAware.hasRedstoneSignal(extractRedstoneChannel));
        } else {
            return extractRedstoneControl == RedstoneControl.ALWAYS_ACTIVE;
        }
    }

    @Override
    public List<DyeColor> getRedstoneSignalColors() {
        if (extractRedstoneControl.isRedstoneSensitive()) {
            return List.of(extractRedstoneChannel);
        }

        return List.of();
    }

    public ChemicalConnectionConfig withIsInsert(boolean isInsert) {
        return new ChemicalConnectionConfig(isInsert, insertChannel, isExtract, extractChannel,
                extractRedstoneControl, extractRedstoneChannel, insertPriority);
    }

    public ChemicalConnectionConfig withInsertChannel(DyeColor insertChannel) {
        return new ChemicalConnectionConfig(isInsert, insertChannel, isExtract, extractChannel,
                extractRedstoneControl, extractRedstoneChannel, insertPriority);
    }

    public ChemicalConnectionConfig withIsExtract(boolean isExtract) {
        return new ChemicalConnectionConfig(isInsert, insertChannel, isExtract, extractChannel,
                extractRedstoneControl, extractRedstoneChannel, insertPriority);
    }

    public ChemicalConnectionConfig withExtractChannel(DyeColor extractChannel) {
        return new ChemicalConnectionConfig(isInsert, insertChannel, isExtract, extractChannel,
                extractRedstoneControl, extractRedstoneChannel, insertPriority);
    }

    public ChemicalConnectionConfig withExtractRedstoneControl(RedstoneControl extractRedstoneControl) {
        return new ChemicalConnectionConfig(isInsert, insertChannel, isExtract, extractChannel,
                extractRedstoneControl, extractRedstoneChannel, insertPriority);
    }

    public ChemicalConnectionConfig withExtractRedstoneChannel(DyeColor extractRedstoneChannel) {
        return new ChemicalConnectionConfig(isInsert, insertChannel, isExtract, extractChannel,
                extractRedstoneControl, extractRedstoneChannel, insertPriority);
    }

    public ChemicalConnectionConfig withPriority(int priority) {
        return new ChemicalConnectionConfig(isInsert, insertChannel, isExtract, extractChannel,
                extractRedstoneControl, extractRedstoneChannel, Math.min(9999, Math.max(-9999, priority)));
    }

    @Override
    public ConnectionConfigType<ChemicalConnectionConfig> type() {
        return TYPE;
    }
}
