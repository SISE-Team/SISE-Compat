package local.sise.compat.probe;

import com.buuz135.industrial.item.infinity.item.ItemInfinityBackpack;
import com.buuz135.industrial.utils.IFAttachments;
import com.buuz135.industrial.worlddata.BackpackDataManager;
import com.hrznstudio.titanium.network.locator.PlayerInventoryFinder;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Exercise the original upstream finder after the Compat mixin, including real pickup events. */
final class TitaniumChecks {
    static void verify(MinecraftServer server) {
        var level = server.overworld();
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "CompatInventory"));
        var finder = PlayerInventoryFinder.get(PlayerInventoryFinder.MAIN).orElseThrow();
        require(finder.getSlotAmountGetter().apply(player) == player.getInventory().getContainerSize(), "incomplete inventory scan");
        for (int invalid : new int[]{-1, player.getInventory().getContainerSize(), Integer.MAX_VALUE}) {
            require(finder.getStackGetter().apply(player, invalid).isEmpty(), "invalid slot returned an item");
            finder.getStackSetter().consume(player, invalid, new ItemStack(Items.DIAMOND));
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            finder.getStackSetter().consume(player, slot, new ItemStack(Items.DIAMOND, 2));
            require(finder.getStackGetter().apply(player, slot).getCount() == 2, "finder write lost at " + slot);
            require(player.getInventory().getItem(slot).is(Items.DIAMOND), "wrong inventory index " + slot);
            finder.getStackSetter().consume(player, slot, ItemStack.EMPTY);
        }
        var normalDrop = new ItemEntity(level, 32, 100, 32, new ItemStack(Items.DIAMOND, 3));
        normalDrop.playerTouch(player);
        require(player.getInventory().countItem(Items.DIAMOND) == 3 && normalDrop.isRemoved(), "normal pickup lost items");
        normalDrop.discard();
        for (int slot : new int[]{0, 10, 35, 40}) {
            var wearer = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "CompatPickup"));
            var backpack = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("industrialforegoing:infinity_backpack")));
            require(backpack.getItem() instanceof ItemInfinityBackpack, "missing backpack");
            var id = UUID.randomUUID();
            var manager = BackpackDataManager.getData(level);
            manager.createBackPack(id);
            var handler = manager.getBackpack(id.toString());
            backpack.set(IFAttachments.INFINITY_BACKPACK_ID, id.toString());
            ItemInfinityBackpack.setPickUpMode(backpack, 1);
            wearer.getInventory().setItem(slot, backpack);
            handler.insertItem(0, new ItemStack(Items.DIAMOND), false);
            var targets = ItemInfinityBackpack.findAllBackpacks(wearer);
            require(targets.size() == 1 && targets.getFirst().getSlot() == slot, "backpack not located at " + slot);
            var drop = new ItemEntity(level, 32, 100, 32, new ItemStack(Items.DIAMOND, 7));
            drop.playerTouch(wearer);
            require(handler.getStackInSlot(0).getCount() == 8 && drop.getItem().isEmpty(), "pickup did not conserve items at " + slot);
            require(wearer.getInventory().countItem(Items.DIAMOND) == 0, "pickup duplicated items");
            ItemInfinityBackpack.setPickUpMode(backpack, 2);
            var disabled = new ItemEntity(level, 32, 100, 32, new ItemStack(Items.DIAMOND, 3));
            disabled.playerTouch(wearer);
            require(handler.getStackInSlot(0).getCount() == 8 && wearer.getInventory().countItem(Items.DIAMOND) == 3, "disabled backpack blocked normal pickup");
            drop.discard(); disabled.discard(); manager.itemHandlers.remove(id.toString()); manager.setDirty();
        }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
