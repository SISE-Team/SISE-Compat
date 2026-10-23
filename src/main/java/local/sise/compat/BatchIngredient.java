package local.sise.compat;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.common.crafting.*;
import java.util.stream.Stream;

/** A native machine ingredient requiring a whole deterministic production batch. */
public record BatchIngredient(Ingredient ingredient,int count) implements ICustomIngredient {
    public static final MapCodec<BatchIngredient> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
        Ingredient.CODEC.fieldOf("ingredient").forGetter(BatchIngredient::ingredient),ExtraCodecs.intRange(2,64).fieldOf("count").forGetter(BatchIngredient::count)
    ).apply(i,BatchIngredient::new));
    @Override public boolean test(ItemStack stack){return stack.getCount()>=count && ingredient.test(stack);}
    @Override public Stream<Holder<Item>> items(){return ingredient.items();}
    @Override public boolean isSimple(){return false;}
    @Override public IngredientType<?> getType(){return SiseCompat.BATCH.get();}
    @Override public SlotDisplay display(){return new SlotDisplay.Composite(ingredient.items().map(h->(SlotDisplay)new SlotDisplay.ItemStackSlotDisplay(new ItemStackTemplate(h,count))).toList());}
    public static int count(Ingredient ingredient){return ingredient.getCustomIngredient() instanceof BatchIngredient batch?batch.count():1;}
}
