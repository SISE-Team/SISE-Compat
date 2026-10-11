package local.sise.compat.mixin;

import com.hrznstudio.titanium.network.locator.PlayerInventoryFinder;
import java.util.HashMap;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Titanium finder positions are Inventory indices, rather than command SlotAccess IDs. */
@Pseudo
@Mixin(value = PlayerInventoryFinder.class, remap = false)
public abstract class TitaniumInventoryMixin {
    @Shadow public static String MAIN;
    @Shadow public static HashMap<String, PlayerInventoryFinder> FINDERS;

    @Inject(method = "<clinit>", at = @At("RETURN"), remap = false)
    private static void siseCompat$inventoryIndices(CallbackInfo callback) {
        FINDERS.put(MAIN, new PlayerInventoryFinder(player -> player.getInventory().getContainerSize(),
            (player, slot) -> slot >= 0 && slot < player.getInventory().getContainerSize()
                ? player.getInventory().getItem(slot) : ItemStack.EMPTY,
            (player, slot, stack) -> {
                if (slot >= 0 && slot < player.getInventory().getContainerSize()) {
                    player.getInventory().setItem(slot, stack);
                    player.getInventory().setChanged();
                }
            }));
    }
}
