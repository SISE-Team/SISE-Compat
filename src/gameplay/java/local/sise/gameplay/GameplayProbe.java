package local.sise.gameplay;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Fixture-only setup and telemetry. Machines process exclusively through ordinary world ticks. */
@Mod("sise_gameplay_probe")
public final class GameplayProbe {
    static Path dir;
    static boolean started, ready, batch;
    static int tick, step, index, delay;
    static JsonObject cases;
    static List<String> ids;
    static final List<Map<String,Object>> results=new ArrayList<>();
    static final List<Map<String,Object>> errors=new ArrayList<>();
    static final List<MachineCase> machines=new ArrayList<>();
    static final BlockPos TABLE=new BlockPos(2,64,4), CUTTER=new BlockPos(4,64,4), EXTREME=new BlockPos(6,64,4);
    static String current="";
    static ItemStack expected=ItemStack.EMPTY;
    static int resultSlot, inputSlots, menuId, selected=-1;
    static int machineTick;
    public GameplayProbe() { if(System.getProperty("sise.gameplay.fixture")!=null) {NeoForge.EVENT_BUS.addListener(GameplayProbe::tick);NeoForge.EVENT_BUS.addListener(GameplayProbe::powerTick);} }
    static void powerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Pre event) {if(!ready)return;for(var m:machines)if(!m.done && m.supply!=null)m.supplied+=m.supply.getAsDouble();}
    static Item item(String id) { return BuiltInRegistries.ITEM.getValue(Identifier.parse(id)); }
    static ItemStack stack(String id,int n) { return new ItemStack(item(id),n); }
    static ServerPlayer player(MinecraftServer s) { return s.getPlayerList().getPlayer(Minecraft.getInstance().player.getUUID()); }
    static Recipe<?> recipe(MinecraftServer s,String id) { return s.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,Identifier.parse(id))).orElseThrow().value(); }
    static JsonObject encode(MinecraftServer s,Recipe<?> r) { return Recipe.CODEC.encodeStart(s.registryAccess().createSerializationContext(JsonOps.INSTANCE),r).getOrThrow().getAsJsonObject(); }
    static ItemStack parse(MinecraftServer s,JsonElement j) { return ItemStack.CODEC.parse(s.registryAccess().createSerializationContext(JsonOps.INSTANCE),j).getOrThrow(); }
    static ItemStack sample(MinecraftServer s,JsonElement j) {
        if(j.isJsonObject() && j.getAsJsonObject().has("conduit_type")) {
            JsonObject v=new JsonObject(), c=new JsonObject(); v.addProperty("id","enderio:conduit");c.add("enderio:conduit",j.getAsJsonObject().get("conduit_type"));v.add("components",c);return parse(s,v);
        }
        return sample(Ingredient.CODEC.parse(s.registryAccess().createSerializationContext(JsonOps.INSTANCE),j).getOrThrow(),1);
    }
    static ItemStack sample(Ingredient ingredient,int count) {
        var options=ingredient.items().map(h->new ItemStack(h.value(),count)).filter(ingredient::test).toList();
        return options.stream().filter(x->BuiltInRegistries.ITEM.getKey(x.getItem()).getNamespace().equals("sise_ores")).findFirst().orElseGet(()->options.isEmpty()?ItemStack.EMPTY:options.getFirst());
    }
    static String describe(ItemStack s) { return s.isEmpty()?"empty":BuiltInRegistries.ITEM.getKey(s.getItem())+" x"+s.getCount()+" "+s.getComponentsPatch(); }
    static void require(boolean b,String text) { if(!b)throw new IllegalStateException(text); }
    static void write(String name,Object value) { try { Files.writeString(dir.resolve(name),new GsonBuilder().setPrettyPrinting().create().toJson(value)); } catch(Exception e) {throw new RuntimeException(e);} }
    static void report() {
        var m=machines.stream().map(MachineCase::status).toList();
        write("gameplay-report.json",Map.of("menus",results,"errors",errors,"machines",m,"batch_running",batch,"batch_index",index,"batch_total",ids==null?0:ids.size(),"current",current,"tick",tick));
    }
    static void shot(String name) { Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget(), screenshot->{try(screenshot){Files.createDirectories(dir.resolve("screenshots"));screenshot.writeToFile(dir.resolve("screenshots/"+name+".png"));}catch(Exception e){e.printStackTrace();}}); }
    static void setup(MinecraftServer s) {
        var level=s.overworld();var p=player(s);
        for(int x=-3;x<29;x++)for(int z=-3;z<13;z++)level.setBlock(new BlockPos(x,63,z),Blocks.SMOOTH_STONE.defaultBlockState(),3);
        level.setBlock(TABLE,Blocks.CRAFTING_TABLE.defaultBlockState(),3);level.setBlock(CUTTER,Blocks.STONECUTTER.defaultBlockState(),3);level.setBlock(EXTREME,BuiltInRegistries.BLOCK.getValue(Identifier.parse("avaritia:extreme_crafting_table")).defaultBlockState(),3);
        p.teleportTo(2.5,64,2.5);p.setGameMode(GameType.SURVIVAL);p.setYRot(0);p.setXRot(10);
        write("ready.json",Map.of("world","SISE actual gameplay verification","player",p.getName().getString()));
    }
    static void open(MinecraftServer s,String id,boolean wrong) {
        var p=player(s);p.closeContainer();p.getInventory().clearContent();p.inventoryMenu.setCarried(ItemStack.EMPTY);
        current=id;JsonObject j=cases.getAsJsonObject(id);require(j!=null,"No case "+id);expected=parse(s,j.get("result"));
        boolean stone=j.get("type").getAsString().equals("minecraft:stonecutting"), extreme=j.get("type").getAsString().equals("avaritia:shaped_table");
        p.teleportTo(extreme?6.5:3.5,64,3.0);
        if(extreme)p.openMenu((MenuProvider)s.overworld().getBlockEntity(EXTREME),buf->buf.writeBlockPos(EXTREME));
        else p.openMenu(new SimpleMenuProvider((sync,inv,pl)->stone?new StonecutterMenu(sync,inv,ContainerLevelAccess.create(s.overworld(),CUTTER)):new CraftingMenu(sync,inv,ContainerLevelAccess.create(s.overworld(),TABLE)),Component.literal("SISE 游戏验证 · "+id)));
        var menu=p.containerMenu;menuId=menu.containerId;selected=-1;
        if(stone) {
            resultSlot=1;inputSlots=1;menu.getSlot(0).set(sample(s,j.get("ingredient")));
            var sm=(StonecutterMenu)menu;
            for(int i=0;i<sm.getVisibleRecipes().entries().size();i++) {
                var rr=sm.getVisibleRecipes().entries().get(i).recipe().recipe();
                if(rr.isPresent() && rr.get().id().identifier().toString().equals(id)) {selected=i;break;}
            }
            require(selected>=0,"Stonecutting option missing "+id);
        } else {
            resultSlot=0;inputSlots=extreme?81:9;
            if(j.has("pattern")) {int y=0;for(var row:j.getAsJsonArray("pattern")){int x=0;for(char c:row.getAsString().toCharArray()){if(c!=' ')menu.getSlot(1+x+(extreme?9:3)*y).set(sample(s,j.getAsJsonObject("key").get(String.valueOf(c))));x++;}y++;}}
            else {int i=1;for(var ing:j.getAsJsonArray("ingredients"))menu.getSlot(i++).set(sample(s,ing));}
            if(wrong) for(int i=1;i<=9;i++)if(menu.getSlot(i).getItem().is(item("enderio:conduit")))menu.getSlot(i).set(stack("enderio:conduit",1));
        }
        menu.broadcastChanges();
        write("menu.json",Map.of("id",id,"container",menuId,"result_slot",resultSlot,"selection",selected,"expected",describe(expected),"result",describe(menu.getSlot(resultSlot).getItem()),"wrong_conduit",wrong));
    }
    static void verifyMenu(MinecraftServer s,String method) {
        var p=player(s);var menu=p.containerMenu;
        require(menu.containerId==menuId,"Menu changed");
        require(ItemStack.matches(menu.getCarried(),expected),"Taken stack mismatch: "+describe(menu.getCarried())+" expected "+describe(expected));
        int remaining=0;
        for(int i=0;i<inputSlots;i++)remaining+=menu.getSlot(resultSlot==0?i+1:0).getItem().getCount();
        require(remaining==0,"Ingredients remain: "+remaining);
        require(menu.getSlot(resultSlot).getItem().isEmpty(),"Output duplicated after consumption");
        results.add(Map.of("recipe",current,"method",method,"output",describe(menu.getCarried()),"remaining_inputs",remaining,"passed",true));report();
    }
    static void diagnose(MinecraftServer s) {
        String id="modern_industrialization:electric_age/transformer/lv_mv/down";
        var r=(CraftingRecipe)recipe(s,id);var plain=stack("modern_industrialization:lv_mv_transformer",1);var named=plain.copy();named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,Component.literal("Keep configured machine"));
        var input=CraftingInput.of(1,1,List.of(named));var out=new LinkedHashMap<String,Object>();out.put("encoded",encode(s,r));out.put("class",r.getClass().getName());out.put("named",describe(named));out.put("patch_empty",named.isComponentsPatchEmpty());out.put("direct_pristine",new local.sise.compat.PristineIngredient(plain.getItem()).test(named));out.put("recipe_matches_named",r.matches(input,s.overworld()));
        out.put("ingredients",r.placementInfo().ingredients().stream().map(ing->Map.of("custom",ing.isCustom(),"simple",ing.isSimple(),"test_named",ing.test(named),"implementation",ing.isCustom()?ing.getCustomIngredient().getClass().getName():"vanilla")).toList());
        out.put("matches",s.getRecipeManager().getRecipes().stream().filter(h->h.value() instanceof CraftingRecipe c && c.matches(input,s.overworld())).map(h->Map.of("id",h.id().identifier().toString(),"output",describe(((CraftingRecipe)h.value()).assemble(input)))).toList());
        var menu=player(s).containerMenu;out.put("menu_class",menu.getClass().getName());out.put("slots",menu.slots.stream().limit(10).map(sl->describe(sl.getItem())).toList());
        if(menu.getSlot(1).container instanceof CraftingContainer grid){var live=grid.asCraftInput();out.put("grid_class",grid.getClass().getName());out.put("live_input",live.items().stream().map(GameplayProbe::describe).toList());out.put("live_matches",r.matches(live,s.overworld()));}
        write("component-diagnosis.json",out);
    }
    static void batchStep(Minecraft mc,MinecraftServer s) {
        if(++delay<6)return;delay=0;
        try {
            if(step==0) {if(index>=ids.size()){batch=false;current="batch complete";report();return;}s.submit(()->open(s,ids.get(index),false)).join();step=1;}
            else if(step==1) {require(mc.player.containerMenu.containerId==menuId,"Client menu not synced");if(selected>=0)mc.gameMode.handleInventoryButtonClick(menuId,selected);step=2;}
            else if(step==2) {require(ItemStack.matches(mc.player.containerMenu.getSlot(resultSlot).getItem(),expected),"Client result mismatch "+current);mc.gameMode.handleContainerInput(menuId,resultSlot,0,ContainerInput.PICKUP,mc.player);step=3;}
            else {s.submit(()->verifyMenu(s,"real client click packet + server inventory consumption")).join();index++;step=0;}
        } catch(Throwable e) {errors.add(Map.of("case",current,"error",e.toString()));index++;step=0;report();}
    }
    static class MachineCase {
        String id, block; BlockPos pos; List<ItemStack> before; Supplier<List<ItemStack>> inputs,outputs; DoubleSupplier energy,supply; double initialEnergy,supplied; ItemStack expected; int started;boolean done,passed;String error=""; List<String> finalOutput=List.of();Supplier<Object> detail=()->"";
        Map<String,Object> status() {return Map.ofEntries(Map.entry("recipe",id),Map.entry("machine",block),Map.entry("position",pos.toShortString()),Map.entry("input",before.stream().map(GameplayProbe::describe).toList()),Map.entry("expected",describe(expected)),Map.entry("output",finalOutput),Map.entry("done",done),Map.entry("passed",passed),Map.entry("energy_before",initialEnergy),Map.entry("energy_supplied",supplied),Map.entry("energy_consumed",initialEnergy+supplied-energy.getAsDouble()),Map.entry("energy_now",energy.getAsDouble()),Map.entry("detail",detail.get()),Map.entry("error",error));}
        void poll() {if(done)return;var out=outputs.get();finalOutput=out.stream().filter(x->!x.isEmpty()).map(GameplayProbe::describe).toList();
            if(out.stream().anyMatch(x->!x.isEmpty())) {done=true;passed=out.stream().filter(x->!x.isEmpty()).count()==1 && out.stream().anyMatch(x->ItemStack.matches(x,expected)) && inputs.get().stream().allMatch(ItemStack::isEmpty) && energy.getAsDouble()<initialEnergy+supplied;if(!passed)error="Output/input/energy mismatch";}
            else if(tick-started>1800) {done=true;error="No output within 1800 client ticks";}
        }
    }
    static void startMachines(MinecraftServer s) {
        require(machines.isEmpty(),"Machine cases already started");
        var all=new ArrayList<String>(local.sise.compat.materials.MineralRecipes.CRUSHING);Collections.sort(all);
        all.add("mekanism:processing/iron/dust/from_ingot");
        for(String id:all) {
            var r=(mekanism.api.recipes.ItemStackToItemStackRecipe)recipe(s,id);var in=r.getInput().ingredient();
            addMek(s,id,"mekanism:crusher",sample(in.ingredient(),in.count()),parse(s,encode(s,r).get("output")));
        }
        for(String id:List.of("neoecoae:enriching/aluminum_dust_from_raw","neoecoae:enriching/tungsten_dust_from_raw")) {
            var r=(mekanism.api.recipes.ItemStackToItemStackRecipe)recipe(s,id);var in=r.getInput().ingredient();addMek(s,id,"mekanism:enrichment_chamber",sample(in.ingredient(),in.count()),parse(s,encode(s,r).get("output")));
        }
        for(String tier:List.of("lv_mv","mv_hv","hv_ev","ev_supra"))addMi(s,"modern_industrialization:assembler_generated/electric_age/transformer/"+tier+"/down","modern_industrialization:assembler");
        addMi(s,"modern_industrialization:assembler_generated/electric_age/transformer/lv_mv/up","modern_industrialization:assembler");
        addMi(s,"modern_industrialization:materials/silicon/unpacker/ingot","modern_industrialization:electric_unpacker");
        addAe(s,false);addAe(s,true);machineTick=tick;report();
    }
    static MachineCase placed(MinecraftServer s,String id,String block,ItemStack expected,List<ItemStack> input) {
        var c=new MachineCase();c.id=id;c.block=block;c.expected=expected;c.before=input.stream().map(ItemStack::copy).toList();c.started=tick;
        int n=machines.size();c.pos=new BlockPos(n%8*3,64,7+n/8*3);
        var b=BuiltInRegistries.BLOCK.getValue(Identifier.parse(block));require(b!=Blocks.AIR,"Missing block "+block);s.overworld().setBlock(c.pos,b.defaultBlockState(),3);return c;
    }
    static void addMek(MinecraftServer s,String id,String block,ItemStack input,ItemStack expected) {
        var c=placed(s,id,block,expected,List.of(input));var be=(mekanism.common.tile.prefab.TileEntityElectricMachine)s.overworld().getBlockEntity(c.pos);
        var inv=be.getInventorySlots();require(inv.size()==3,"Unexpected MEK inventory");inv.get(0).setContents(net.neoforged.neoforge.transfer.item.ItemResource.of(input),input.getCount(),null);be.energyContainer().setEnergy(be.energyContainer().getCapacityAsLong(),null);
        c.inputs=()->List.of(inv.get(0).resource().toStack(inv.get(0).amountAsInt()));c.outputs=()->List.of(inv.get(1).resource().toStack(inv.get(1).amountAsInt()));c.energy=()->be.energyContainer().getAmountAsLong();c.initialEnergy=c.energy.getAsDouble();c.supply=()->{long old=be.energyContainer().getAmountAsLong();be.energyContainer().setEnergy(be.energyContainer().getCapacityAsLong(),null);return be.energyContainer().getAmountAsLong()-old;};machines.add(c);
    }
    static void addMi(MinecraftServer s,String id,String block) {
        var r=(aztech.modern_industrialization.machines.recipe.MachineRecipe)recipe(s,id);var input=r.itemInputs.stream().map(i->sample(i.ingredient(),i.amount())).toList();
        var json=encode(s,r);var out=json.getAsJsonArray("item_outputs").get(0).getAsJsonObject();var expected=stack(out.get("id").getAsString(),out.get("amount").getAsInt());
        var c=placed(s,id,block,expected,input);var be=(aztech.modern_industrialization.machines.blockentities.ElectricCraftingMachineBlockEntity)s.overworld().getBlockEntity(c.pos);var inv=be.getInventory().getItemStacks();
        for(int i=0;i<input.size();i++){inv.get(i).setKey(net.neoforged.neoforge.transfer.item.ItemResource.of(input.get(i)));inv.get(i).setAmount(input.get(i).getCount());}
        be.getEnergyComponent().insertEu(be.getEnergyComponent().getCapacity(),aztech.modern_industrialization.util.Simulation.ACT);
        int inputCount=be.getCrafterComponent().getInventory().getItemInputs().size();
        c.inputs=()->inv.stream().limit(inputCount).map(x->x.toStack().copy()).toList();c.outputs=()->inv.stream().skip(inputCount).map(x->x.toStack().copy()).toList();c.energy=()->be.getEnergyComponent().getEu();c.initialEnergy=c.energy.getAsDouble();c.detail=()->Map.of("multiple_recipes",be.getCrafterComponent().matchesMultipleRecipes(),"active_recipe",be.getCrafterComponent().hasActiveRecipe(),"progress",Float.toString(be.getCrafterComponent().getProgress()));machines.add(c);
    }
    static void addAe(MinecraftServer s,boolean assembler) {
        String id=assembler?"neoecoae:crystal_assembler/superconducting_processor":"neoecoae:circuit_cutter/superconducting_processor_print";
        var input=assembler?List.of(stack("neoecoae:superconducting_processor_print",4),stack("neoecoae:crystal_matrix",4),stack("ae2:printed_silicon",4)):List.of(stack("neoecoae:energized_superconductive_block",1));
        var c=placed(s,id,assembler?"extendedae:crystal_assembler":"extendedae:circuit_cutter",stack(assembler?"neoecoae:superconducting_processor":"neoecoae:superconducting_processor_print",assembler?4:9),input);
        var base=(appeng.blockentity.grid.AENetworkedPoweredBlockEntity)s.overworld().getBlockEntity(c.pos);
        appeng.util.inv.AppEngInternalInventory in,out;
        if(assembler){var be=(com.glodblock.github.extendedae.common.tileentities.TileCrystalAssembler)base;in=be.getInput();out=be.getOutput();}
        else {var be=(com.glodblock.github.extendedae.common.tileentities.TileCircuitCutter)base;in=be.getInput();out=be.getOutput();}
        for(int i=0;i<input.size();i++)in.setItemDirect(i,input.get(i).copy());base.injectAEPower(base.getAEMaxPower(),appeng.api.config.Actionable.MODULATE);
        c.inputs=()->java.util.stream.IntStream.range(0,in.size()).mapToObj(i->in.getStackInSlot(i).copy()).toList();c.outputs=()->java.util.stream.IntStream.range(0,out.size()).mapToObj(i->out.getStackInSlot(i).copy()).toList();c.energy=base::getAECurrentPower;c.initialEnergy=c.energy.getAsDouble();machines.add(c);
    }
    static void command(Minecraft mc,MinecraftServer s,JsonObject cmd) {
        switch(cmd.get("action").getAsString()) {
            case "batch" -> {ids=cmd.has("only_extreme")?cases.keySet().stream().filter(k->k.startsWith("avaritia:")).toList():new ArrayList<>(cases.keySet());index=0;step=0;delay=0;batch=true;}
            case "open" -> s.submit(()->{open(s,cmd.get("id").getAsString(),cmd.has("wrong")&&cmd.get("wrong").getAsBoolean());if(cmd.has("named")){var menu=player(s).containerMenu;var st=menu.getSlot(1).getItem().copy();st.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,Component.literal("Keep configured machine"));menu.getSlot(1).set(st);menu.broadcastChanges();}}).join();
            case "verify" -> s.submit(()->verifyMenu(s,"Windows mouse click in visible game GUI")).join();
            case "negative" -> s.submit(()->{var menu=player(s).containerMenu;require(menu.getSlot(resultSlot).getItem().isEmpty() && menu.getCarried().isEmpty(),"Invalid input unexpectedly craftable");results.add(Map.of("recipe",current,"method","visible GUI negative input test","condition",cmd.get("condition").getAsString(),"passed",true));report();}).join();
            case "diagnose" -> s.submit(()->diagnose(s)).join();
            case "machines" -> s.submit(()->startMachines(s)).join();
            case "machine_lock" -> s.submit(()->{var c=machines.get(cmd.get("index").getAsInt());var be=(aztech.modern_industrialization.machines.blockentities.ElectricCraftingMachineBlockEntity)s.overworld().getBlockEntity(c.pos);write("mi-before-lock.json",c.status());be.getCrafterComponent().lockRecipe(ResourceKey.create(Registries.RECIPE,Identifier.parse(c.id)),player(s).getInventory());c.started=tick;c.done=false;c.error="";}).join();
            case "machine_menu" -> s.submit(()->{var c=machines.get(cmd.get("index").getAsInt());var p=player(s);p.closeContainer();p.teleportTo(c.pos.getX()+.5,64,c.pos.getZ()-1);var b=s.overworld().getBlockState(c.pos);b.useWithoutItem(s.overworld(),p,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(c.pos),net.minecraft.core.Direction.NORTH,c.pos,false));}).join();
            case "shot" -> shot(cmd.get("name").getAsString());
            case "close" -> {mc.player.closeContainer();}
            case "stop" -> {report();mc.stop();}
            default -> throw new IllegalArgumentException("Unknown fixture action");
        }
    }
    static void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();try {
            if(!started) {if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;dir=mc.gameDirectory.toPath().toAbsolutePath().normalize();require(dir.equals(Path.of(System.getProperty("sise.gameplay.fixture")).toAbsolutePath().normalize())&&Files.isRegularFile(dir.resolve("SISE_GAMEPLAY_ONLY")),"Not fixture");cases=JsonParser.parseString(Files.readString(dir.resolve("gameplay-cases.json"))).getAsJsonObject();ids=new ArrayList<>(cases.keySet());started=true;mc.createWorldOpenFlows().createFreshLevel("sise-gameplay-"+System.currentTimeMillis(),new LevelSettings("SISE actual gameplay verification",GameType.CREATIVE,new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),new WorldOptions(604,false,false),WorldPresets::createFlatWorldDimensions,mc.screen);return;}
            if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null||mc.player.tickCount<100)return;tick++;var s=mc.getSingleplayerServer();
            if(!ready){s.submit(()->setup(s)).join();ready=true;report();}
            if(tick%10==0 && Files.isRegularFile(dir.resolve("command.json"))) {var cmd=JsonParser.parseString(Files.readString(dir.resolve("command.json"))).getAsJsonObject();Files.delete(dir.resolve("command.json"));command(mc,s,cmd);}
            if(batch)batchStep(mc,s);
            if(tick%40==0) {s.submit(()->{for(var m:machines)m.poll();}).join();report();}
        } catch(Throwable e) {e.printStackTrace();errors.add(Map.of("case",current,"error",e.toString()));if(dir!=null)report();}
    }
}
