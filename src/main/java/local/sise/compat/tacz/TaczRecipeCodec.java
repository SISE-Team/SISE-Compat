package local.sise.compat.tacz;

import com.google.gson.*;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tacz.guns.crafting.*;
import com.tacz.guns.crafting.result.*;
import com.tacz.guns.resource.CommonAssetsManager;
import java.util.*;
import local.sise.compat.mixin.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/** Preserve raw gun definitions and arbitrary custom-stack components across the public recipe codec. */
public final class TaczRecipeCodec {
    private static final Codec<GunSmithTableIngredient> INGREDIENT = RecordCodecBuilder.create(i -> i.group(
        Ingredient.CODEC.fieldOf("item").forGetter(GunSmithTableIngredient::getIngredientOrThrow),
        ExtraCodecs.POSITIVE_INT.optionalFieldOf("count", 1).forGetter(GunSmithTableIngredient::getCount)
    ).apply(i, GunSmithTableIngredient::new));
    private static final Codec<GunSmithTableResult> RESULT = new Codec<>() {
        @Override public <T> DataResult<Pair<GunSmithTableResult,T>> decode(DynamicOps<T> ops, T input) {
            try {
                var json = ops.convertTo(JsonOps.INSTANCE, input).getAsJsonObject();
                String type = json.get("type").getAsString();
                if (!Set.of("gun", "ammo", "attachment", "custom").contains(type))
                    return DataResult.error(() -> "Unknown TaCZ result type: "+type);
                if (json.has("count") && json.get("count").getAsInt() < 1)
                    return DataResult.error(() -> "TaCZ result count must be positive");
                Identifier group = json.has("group") ? group(json.get("group").getAsString()) : null;
                GunSmithTableResult result;
                if (type.equals("custom") && json.has("item") && json.get("item").isJsonObject()
                        && json.getAsJsonObject("item").has("id")) {
                    var payload = JsonOps.INSTANCE.convertTo(ops, json.get("item"));
                    var stack = ItemStack.CODEC.parse(ops, payload).getOrThrow();
                    result = new GunSmithTableResult(stack, group);
                } else {
                    result = CommonAssetsManager.GSON.fromJson(json, GunSmithTableResult.class);
                    var access = (TaczResultAccessor)(Object)result;
                    if (access.siseCompat$raw() != null && group != null)
                        result = new GunSmithTableResult(access.siseCompat$raw(), group);
                }
                return DataResult.success(Pair.of(result, ops.empty()));
            } catch (RuntimeException error) {
                return DataResult.error(() -> "Invalid TaCZ recipe product: "+error.getMessage());
            }
        }
        @Override public <T> DataResult<T> encode(GunSmithTableResult result, DynamicOps<T> ops, T prefix) {
            try {
                var json = new JsonObject();
                var access = (TaczResultAccessor)(Object)result;
                if (access.siseCompat$raw() != null) {
                    var raw = (TaczRawResultAccessor)(Object)access.siseCompat$raw();
                    json.addProperty("type", raw.siseCompat$type());
                    json.addProperty("id", raw.siseCompat$id().toString());
                    if (raw.siseCompat$count() != 1) json.addProperty("count", raw.siseCompat$count());
                    if (raw.siseCompat$extra() != null) {
                        var extra = raw.siseCompat$extra();
                        if (extra.getAmmoCount() != 0) json.addProperty("ammo_count", extra.getAmmoCount());
                        if (extra.getAttachments() != null && !extra.getAttachments().isEmpty()) {
                            var attachments = new JsonObject();
                            extra.getAttachments().forEach((slot,id) -> attachments.addProperty(slot.name().toLowerCase(Locale.ROOT), id.toString()));
                            json.add("attachments", attachments);
                        }
                    }
                    if (raw.siseCompat$nbt() != null) json.addProperty("nbt", raw.siseCompat$nbt().toString());
                    if (access.siseCompat$groupOverride() != null)
                        json.addProperty("group", access.siseCompat$groupOverride().toString());
                } else {
                    json.addProperty("type", "custom");
                    var stack = result.getResult();
                    if (stack.isEmpty()) return DataResult.error(() -> "TaCZ custom product is empty");
                    var encoded = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
                    json.add("item", ops.convertTo(JsonOps.INSTANCE, encoded));
                    json.addProperty("group", result.getGroup().toString());
                }
                var builder = ops.mapBuilder();
                for (var entry : json.entrySet())
                    builder.add(entry.getKey(), JsonOps.INSTANCE.convertTo(ops, entry.getValue()));
                return builder.build(prefix);
            } catch (RuntimeException error) {
                return DataResult.error(() -> "Cannot encode TaCZ recipe product: "+error.getMessage());
            }
        }
    };
    public static final MapCodec<GunSmithTableRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        RESULT.fieldOf("result").forGetter(GunSmithTableRecipe::getResult),
        INGREDIENT.listOf().fieldOf("materials").forGetter(GunSmithTableRecipe::getInputs),
        Identifier.CODEC.optionalFieldOf("sise_compat:recipe_id").forGetter(r -> Optional.of(r.getId()))
    ).apply(i, (result, inputs, id) -> new GunSmithTableRecipe(id.orElseGet(() -> {
        var raw = ((TaczResultAccessor)(Object)result).siseCompat$raw();
        return raw == null ? Identifier.parse("tacz:custom") : ((TaczRawResultAccessor)(Object)raw).siseCompat$id();
    }), result, inputs)));
    private static Identifier group(String value) { return Identifier.parse(value.indexOf(':') < 0 ? "tacz:"+value : value); }
    private TaczRecipeCodec() { }
}
