package local.sise.compat.probe;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.brew.BarCabinetBlockEntity;
import com.github.ysbbbbbb.kaleidoscopetavern.init.ModBlocks;
import net.blay09.mods.farmingforblockheads.loot.ModLootContextParams;

public final class ExtraDataChecks {
    public static Map<String,Object> verify(MinecraftServer server) {
        var report=new LinkedHashMap<String,Object>();var state=ModBlocks.BAR_CABINET.get().defaultBlockState();
        var pos=new BlockPos(20,90,0);int cases=0;
        for(boolean single:List.of(false,true))for(int mask=0;mask<4;mask++) {
            var cabinet=new BarCabinetBlockEntity(pos,state);cabinet.setLevel(server.overworld());cabinet.setSingle(single);
            cabinet.setLeftItem((mask&1)==0?ItemStack.EMPTY:new ItemStack(Items.GLASS_BOTTLE,2));
            cabinet.setRightItem((mask&2)==0?ItemStack.EMPTY:new ItemStack(Items.HONEY_BOTTLE));
            var encoded=cabinet.saveWithFullMetadata(server.registryAccess());
            if(!encoded.contains("left_item")||!encoded.contains("right_item"))throw new AssertionError("Cabinet serializer omitted an empty slot");
            var decoded=(BarCabinetBlockEntity)BlockEntity.loadStatic(pos,state,encoded,server.registryAccess());
            if(decoded==null||!ItemStack.matches(cabinet.getLeftItem(),decoded.getLeftItem())||!ItemStack.matches(cabinet.getRightItem(),decoded.getRightItem())||single!=decoded.isSingle())throw new AssertionError("Cabinet slot or display mode changed");
            cases++;
        }
        report.put("cabinet_empty_and_occupied_slot_roundtrips",cases);
        var table=server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("farmingforblockheads:gameplay/shipping_bin")));
        var sales=new TreeMap<Integer,Integer>();
        for(int value:List.of(128,256,8192)) {
            var params=new LootParams.Builder(server.overworld()).withParameter(LootContextParams.ORIGIN,Vec3.ZERO)
                .withParameter(ModLootContextParams.SHIPMENT_VALUE,value).withParameter(ModLootContextParams.SHIPMENT_ITEMS,List.of(new ItemStack(Items.WHEAT))).create(ModLootContextParams.SHIPPING_BIN_CONTEXT);
            var output=table.getRandomItems(params);int emeralds=output.stream().filter(s->s.is(Items.EMERALD)).mapToInt(ItemStack::getCount).sum();
            if(emeralds!=value/128)throw new AssertionError("Shipping value conversion changed: "+value+" -> "+output);
            sales.put(value,emeralds);
        }
        report.put("shipping_value_to_emeralds",sales);
        report.put("router_module_tool_component_crafting",routerModules(server));return report;
    }
    private static int routerModules(MinecraftServer server) {
        int cases=0;
        for(var item:List.of(Items.WOODEN_PICKAXE,Items.IRON_PICKAXE,Items.DIAMOND_PICKAXE)) {
            var pickaxe=new ItemStack(item);pickaxe.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Preserved router tool"));
            var breaker=me.desht.modularrouters.recipe.PickaxeModuleRecipe.BreakerModuleRecipe.INSTANCE;
            var extruder=me.desht.modularrouters.recipe.PickaxeModuleRecipe.ExtruderModule1Recipe.INSTANCE;
            ItemStack module=ItemStack.EMPTY;
            for(var recipe:List.of(breaker,extruder)) {
                var method=me.desht.modularrouters.recipe.PickaxeModuleRecipe.class.getDeclaredMethods();
                java.util.List<net.minecraft.world.item.crafting.Ingredient> ingredients;
                try {var accessor=me.desht.modularrouters.recipe.PickaxeModuleRecipe.class.getDeclaredMethod("ingredients");accessor.setAccessible(true);@SuppressWarnings("unchecked") var list=(java.util.List<net.minecraft.world.item.crafting.Ingredient>)accessor.invoke(recipe);ingredients=list;}
                catch(ReflectiveOperationException error){throw new RuntimeException(error);}
                var stacks=new ArrayList<ItemStack>();
                for(var ingredient:ingredients) {
                    var sample=new ItemStack(ingredient.items().findFirst().orElseThrow());
                    if(ingredient.test(pickaxe))sample=pickaxe.copy();
                    else if(!module.isEmpty()&&ingredient.test(module))sample=module.copy();
                    stacks.add(sample);
                }
                while(stacks.size()<9)stacks.add(ItemStack.EMPTY);
                var input=net.minecraft.world.item.crafting.CraftingInput.of(3,3,stacks);
                if(!recipe.matches(input,server.overworld()))throw new AssertionError("Router module recipe rejected its original ingredients");
                module=recipe.assemble(input);
                var expectedTool=pickaxe.copy();expectedTool.set(net.minecraft.core.component.DataComponents.UNBREAKABLE,net.minecraft.util.Unit.INSTANCE);
                if(module.isEmpty()||!ItemStack.matches(expectedTool,me.desht.modularrouters.item.module.IPickaxeUser.getPickaxe(module)))throw new AssertionError("Router module crafting lost its original tool components");
                if(recipe.display().isEmpty())throw new AssertionError("Router module has no recipe display");cases++;
            }
        }
        return cases;
    }
    private ExtraDataChecks() { }
}
