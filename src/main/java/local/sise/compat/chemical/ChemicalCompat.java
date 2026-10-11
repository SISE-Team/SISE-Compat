package local.sise.compat.chemical;

import com.enderio.enderio.api.EnderIORegistries;
import com.enderio.enderio.api.conduits.ConduitType;
import com.enderio.enderio.api.conduits.connection.config.ConnectionConfigType;
import com.enderio.enderio.api.conduits.connection.path.PriorityConnectionPathComparator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

/** Restores the omitted Ender IO 9.0.6 chemical module using Mekanism 10.8 transactions. */
public final class ChemicalCompat {
    public static final String MOD_ID = local.sise.compat.SiseCompat.ID;
    public static final int TICK_RATE = 5;
    private static final DeferredRegister<ConduitType<?, ?>> TYPES =
        DeferredRegister.create(EnderIORegistries.CONDUIT_TYPE, "enderio");
    private static final DeferredRegister<ConnectionConfigType<?>> CONFIGS =
        DeferredRegister.create(EnderIORegistries.CONDUIT_CONNECTION_CONFIG_TYPE, "enderio");
    public static final Supplier<ConduitType<ChemicalConduit, ChemicalConnectionConfig>> CHEMICAL =
        TYPES.register("chemical", () -> ConduitType.builder(ChemicalConduit.CODEC, ChemicalConnectionConfig.TYPE)
            .doesRequireNetworkCaches()
            .ticker(new ChemicalTicker(), TICK_RATE)
            .connectionComparerFromReference(new PriorityConnectionPathComparator(connection ->
                connection.node().getConnectionConfig(connection.connectionSide(), ChemicalConnectionConfig.TYPE).insertPriority()))
            .build());
    static { CONFIGS.register("chemical", () -> ChemicalConnectionConfig.TYPE); }
    public static void register(IEventBus bus) {
        TYPES.register(bus);
        CONFIGS.register(bus);
        if (FMLEnvironment.getDist().isClient()) ChemicalClient.register(bus);
    }
}
