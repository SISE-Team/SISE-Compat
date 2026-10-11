package local.sise.compat.chemical;

import com.enderio.enderio.api.conduits.screen.RegisterConduitScreenTypesEvent;
import net.neoforged.bus.api.IEventBus;

public final class ChemicalClient {
    public static void register(IEventBus bus) {
        bus.addListener((RegisterConduitScreenTypesEvent event) ->
            event.register(ChemicalCompat.CHEMICAL.get(), new ChemicalScreenType()));
    }
}
