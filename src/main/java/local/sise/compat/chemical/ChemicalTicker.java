package local.sise.compat.chemical;

import com.enderio.enderio.api.conduits.Conduit;
import com.enderio.enderio.api.conduits.connection.path.ConduitConnectionPath;
import com.enderio.enderio.api.conduits.network.ConduitNetwork;
import com.enderio.enderio.api.conduits.ticker.ConduitTickerBase;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ChemicalTicker extends ConduitTickerBase<ChemicalConduit> {
    public ChemicalTicker() { super(ChemicalCompat.CHEMICAL::get); }
    @Override protected void tickNetwork(ServerLevel level, ConduitNetwork network, List<Holder<Conduit<?, ?>>> conduits) {
        Map<ConduitConnectionPath, Long> used = new HashMap<>();
        for (var channel : network.allChannels()) {
            for (var source : network.extractConnections(channel)) {
                var paths = network.insertConnectionsFrom(source);
                var extract = source.getSidedCapability(Capabilities.CHEMICAL.block());
                if (extract == null || paths.isEmpty()) continue;
                long remaining = (long)source.node().conduit(conduitType()).value().transferRatePerTick() * ChemicalCompat.TICK_RATE;
                // The move helper commits extraction and insertion together; a full or incompatible
                // target leaves the source unchanged. No conduit buffers can lose contents on break.
                try (var transaction = Transaction.openRoot()) {
                    for (int slot = 0; slot < extract.size() && remaining > 0; slot++) {
                        var chemical = extract.getResource(slot);
                        if (chemical.isEmpty()) continue;
                        for (var path : paths) {
                            long available = (long)path.property(ChemicalConduit.PATH_RATE) * ChemicalCompat.TICK_RATE - used.getOrDefault(path, 0L);
                            if (available <= 0) continue;
                            var insert = path.end().getSidedCapability(Capabilities.CHEMICAL.block());
                            if (insert == null || insert == extract) continue;
                            // NeoForge accepts at most an int in one operation. Stellar uses
                            // Integer.MAX_VALUE per tick, so preserve its long burst budget and
                            // split a larger transfer into atomic operations without overflow.
                            while (remaining > 0 && available > 0) {
                                int requested = (int)Math.min(Integer.MAX_VALUE, Math.min(remaining, available));
                                int moved = ResourceHandlerUtil.move(extract, insert, chemical::equals, requested, transaction);
                                if (moved == 0) break;
                                remaining -= moved;
                                available -= moved;
                                used.merge(path, (long)moved, Long::sum);
                            }
                            if (remaining <= 0) break;
                        }
                    }
                    transaction.commit();
                }
            }
        }
    }
}
