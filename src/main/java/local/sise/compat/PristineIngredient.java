package local.sise.compat;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.common.crafting.*;
import java.util.stream.Stream;

/** Conversion never silently consumes a machine's stored state or a customized item. */
public record PristineIngredient(Item item) implements ICustomIngredient {
    public static final MapCodec<PristineIngredient> CODEC = BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").xmap(PristineIngredient::new,PristineIngredient::item);
    @Override public boolean test(ItemStack stack) { return stack.is(item) && stack.isComponentsPatchEmpty(); }
    @Override public Stream<Holder<Item>> items() { return Stream.of(item.builtInRegistryHolder()); }
    @Override public boolean isSimple() { return false; }
    @Override public IngredientType<?> getType() { return SiseCompat.PRISTINE.get(); }
}
