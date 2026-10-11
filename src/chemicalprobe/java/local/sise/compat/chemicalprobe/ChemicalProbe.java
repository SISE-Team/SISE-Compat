package local.sise.compat.chemicalprobe;

import local.sise.compat.chemical.*;

import com.enderio.enderio.api.EnderIODataComponents;
import com.enderio.enderio.api.EnderIORegistries;
import com.enderio.enderio.api.conduits.Conduit;
import com.enderio.enderio.api.io.RedstoneControl;
import com.enderio.enderio.client.content.conduits.gui.screen_type.ConduitScreenTypes;
import com.enderio.enderio.content.conduits.ConduitBlockItem;
import com.enderio.enderio.content.conduits.bundle.ConduitBundleBlockEntity;
import com.enderio.enderio.init.EIOBlocks;
import com.google.gson.GsonBuilder;
import mekanism.api.RelativeSide;
import mekanism.api.chemical.ChemicalResource;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.registries.MekanismChemicals;
import mekanism.common.tile.TileEntityChemicalTank;
import mekanism.common.tile.component.config.DataType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientResourceLoadFinishedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Disposable test mod: never included in the installed compatibility mod. */
@Mod("sise_compat_chemical_probe")
public class ChemicalProbe {
    static final ChemicalResource HYDROGEN = ChemicalResource.of(MekanismChemicals.HYDROGEN);
    static final ChemicalResource OXYGEN = ChemicalResource.of(MekanismChemicals.OXYGEN);
    static final List<Rig> rigs = new ArrayList<>();
    static final Map<String,Object> report = new LinkedHashMap<>();
    static int ticks;
    static boolean started, finished;
    static final int SOURCE = 8_000_000;
    static final String WORLD = "chemical-conduit-verification";
    static final boolean RESUME = Boolean.getBoolean("sise.compat.chemical.resume");
    static String lastScreen="";
    public ChemicalProbe(IEventBus bus) {
        NeoForge.EVENT_BUS.addListener((ClientResourceLoadFinishedEvent event) -> Minecraft.getInstance().execute(ChemicalProbe::start));
        NeoForge.EVENT_BUS.addListener(ChemicalProbe::tick);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            var mc=Minecraft.getInstance();
            if(!started || finished || mc.screen==null) return;
            String screen=mc.screen.getClass().getName();
            if(!screen.equals(lastScreen)) {System.out.println("Chemical fixture screen: "+screen);lastScreen=screen;}
            if(RESUME && mc.screen instanceof BackupConfirmScreen backup) {
                // Only the marked, disposable fixture can reach this handler.
                // Take the offered backup before reopening its experimental modded world.
                for(var child:backup.children()) if(child instanceof Button button && button.getMessage().equals(BackupConfirmScreen.BACKUP_AND_JOIN)) {
                    button.onPress(null);break;
                }
            }
        });
    }
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    static void start() {
        if (started) return;
        started = true;
        var mc = Minecraft.getInstance();
        Path directory = mc.gameDirectory.toPath().toAbsolutePath().normalize();
        check(directory.equals(Path.of(System.getProperty("sise.compat.chemical.fixture")).toAbsolutePath().normalize()), "Wrong fixture directory");
        check(Files.isRegularFile(directory.resolve("CHEMICAL_AUTOTEST_ONLY")), "Fixture marker missing");
        check(ConduitScreenTypes.get(ChemicalCompat.CHEMICAL.get()) instanceof ChemicalScreenType, "Chemical screen missing");
        for (var name : List.of("chemical","pressurized_chemical","ender_chemical")) {
            check(mc.getResourceManager().getResource(Identifier.parse("enderio:textures/block/conduit/"+name+".png")).isPresent(), "Texture missing: "+name);
        }
        report.put("client_screen_and_textures", "passed");
        if (RESUME) {
            mc.createWorldOpenFlows().openWorld(WORLD,()->finish(new AssertionError("Fixture world opening cancelled")));
            return;
        }
        var settings = new LevelSettings("Chemical conduit verification", GameType.CREATIVE,
            new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(602,false,false), WorldPresets::createFlatWorldDimensions, mc.screen);
    }
    static void tick(ServerTickEvent.Post event) {
        if (finished || event.getServer().getPlayerList().getPlayers().isEmpty()) return;
        try {
            var level = event.getServer().overworld();
            ticks++;
            if (ticks == 30) {
                recipes(level);
                if (RESUME) { resume(level); return; }
                setup(level);
            }
            if (ticks < 30) return;
            for (var rig : rigs) rig.observe();
            if (ticks == 70 && !RESUME) {
                var channel = rigs.stream().filter(r->r.name.equals("channel")).findFirst().orElseThrow();
                check(channel.output() == 0, "Mismatched channels transferred");
                channel.bundle.setConnectionConfig(channel.holder,Direction.SOUTH,ChemicalConnectionConfig.DEFAULT.withIsInsert(true).withIsExtract(false));
                var redstone = rigs.stream().filter(r->r.name.equals("redstone")).findFirst().orElseThrow();
                check(redstone.output() == 0, "Disabled extraction transferred");
                redstone.bundle.setConnectionConfig(redstone.holder,Direction.NORTH,ChemicalConnectionConfig.DEFAULT.withExtractRedstoneControl(RedstoneControl.ALWAYS_ACTIVE));
            }
            if (ticks == 120) {
                var results = new ArrayList<Map<String,Object>>();
                for (var rig : rigs) {
                    long expected = switch(rig.name) { case "full", "incompatible" -> 0; case "partial" -> 123; default -> -1; };
                    if (expected >= 0) check(rig.output() == expected, rig.name+" wrong accepted amount: "+rig.output());
                    else check(rig.output()>0, rig.name+" did not transfer");
                    if (rig.name.startsWith("rate-") || rig.name.equals("multi-output") || rig.name.equals("mixed-tier")) check(rig.maxBurst == Math.min(rig.startingSource,rig.limit), rig.name+" failed advertised rate "+rig.maxBurst+" != "+rig.limit);
                    var data = rig.bundle.saveWithFullMetadata(level.registryAccess());
                    var restored = (ConduitBundleBlockEntity)BlockEntity.loadStatic(rig.pipe,rig.bundle.getBlockState(),data,level.registryAccess());
                    check(restored != null && restored.hasConduitStrict(rig.holder), "Conduit NBT failed");
                    check(restored.getConnectionConfig(rig.holder,Direction.NORTH).equals(rig.bundle.getConnectionConfig(rig.holder,Direction.NORTH)), "Config NBT failed");
                    results.add(Map.of("name",rig.name,"source_initial",rig.startingSource,"transferred",rig.output(),"maximum_burst",rig.maxBurst,"limit_per_five_ticks",rig.limit,"conservation","passed","config_round_trip","passed"));
                }
                report.put("transport",results);
                report.put("world",WORLD);
                finish(null);
            }
        } catch(Throwable t) { finish(t); }
    }
    static Holder<Conduit<?,?>> holder(ServerLevel level,String id) {
        return level.registryAccess().lookupOrThrow(EnderIORegistries.Keys.CONDUIT).getOrThrow(ResourceKey.create(EnderIORegistries.Keys.CONDUIT,Identifier.parse(id)));
    }
    static ItemStack item(String id) { return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id))); }
    static void craft(ServerLevel level,String recipeId,List<ItemStack> items,String output,int count) {
        var input = CraftingInput.of(3,3,items);
        var key = ResourceKey.create(Registries.RECIPE,Identifier.parse(recipeId));
        var raw = level.getServer().getRecipeManager().byKey(key).orElseThrow(()->new AssertionError("Missing recipe: "+recipeId));
        var recipe = (CraftingRecipe)raw.value();
        check(recipe.matches(input,level),"Recipe rejected proper inputs: "+recipeId);
        var stack = recipe.assemble(input);
        check(stack.getCount()==count && Objects.equals(stack.get(EnderIODataComponents.CONDUIT),holder(level,output)),"Wrong crafting result: "+recipeId);
        var found = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING,input,level).orElseThrow();
        check(found.id().equals(key),"Wrong recipe selected: "+recipeId);
    }
    static void recipes(ServerLevel level) {
        var binder = item("enderio:conduit_binder");
        String[] types={"chemical","pressurized_chemical","ender_chemical"};
        String[] tubes={"basic","advanced","elite"};
        for(int i=0;i<3;i++) {
            var tube=item("mekanism:"+tubes[i]+"_pressurized_tube");
            craft(level,"enderio:mek_"+tubes[i]+"_pressurized_tube",List.of(binder,binder,binder,tube,tube,tube,binder,binder,binder),"enderio:"+types[i],3);
        }
        for(int i=0;i<2;i++) {
            var previous = ConduitBlockItem.getStackFor(holder(level,"enderio:"+types[i]),1);
            var alloy=item("mekanism:alloy_"+(i==0?"infused":"reinforced"));
            craft(level,"enderio:mek_"+tubes[i]+"_pressurized_tube_upgrade",List.of(previous,previous,previous,previous,alloy,previous,previous,previous,previous),"enderio:"+types[i+1],8);
        }
        String previous="enderio:ender_chemical";
        for(var tier:List.of("melodic","stellar")) {
            var alloy=item("enderio_evolution:"+tier+"_alloy_ingot");
            var conduit=ConduitBlockItem.getStackFor(holder(level,previous),1);
            craft(level,"enderio_evolution:conduit/"+tier+"_chemical_conduit",List.of(binder,binder,binder,alloy,conduit,alloy,binder,binder,binder),"enderio_evolution:"+tier+"_chemical_conduit",tier.equals("melodic")?4:1);
            previous="enderio_evolution:"+tier+"_chemical_conduit";
        }
        report.put("crafting", "all seven recipes matched, assembled and selected correctly");
    }
    static ResourceHandler<ChemicalResource> tank(ServerLevel level,BlockPos pos,ChemicalResource chemical,long amount) {
        level.setBlockAndUpdate(pos,BuiltInRegistries.BLOCK.getValue(Identifier.parse("mekanism:ultimate_chemical_tank")).defaultBlockState());
        var tile=(TileEntityChemicalTank)level.getBlockEntity(pos);
        var config=tile.getConfig().getConfig(TransmissionType.CHEMICAL);
        for(var side:RelativeSide.values()) config.setDataType(DataType.INPUT_OUTPUT,side);
        config.setEjecting(false);
        level.invalidateCapabilities(pos);
        tile.setChanged();
        var handler=level.getCapability(Capabilities.CHEMICAL.block(),pos,Direction.UP);
        check(handler!=null,"Tank capability missing");
        try(var transaction=Transaction.openRoot()) {
            long remaining=amount;
            while(remaining>0) {
                int requested=(int)Math.min(Integer.MAX_VALUE,remaining);
                int inserted=handler.insert(chemical,requested,transaction);
                check(inserted==requested,"Could not fill test tank: "+inserted+"/"+requested+" at "+pos);
                remaining-=inserted;
            }
            transaction.commit();
        }
        return handler;
    }
    static Rig rig(ServerLevel level,String name,String id,int expectedRate,ChemicalResource targetChemical,int targetAmount) {
        var pipe=new BlockPos((rigs.size()%4)*5,6,(rigs.size()/4)*5);
        var source=tank(level,pipe.north(),HYDROGEN,SOURCE);
        var target=tank(level,pipe.south(),targetChemical,targetAmount);
        level.setBlockAndUpdate(pipe,EIOBlocks.CONDUIT_BUNDLE.get().defaultBlockState());
        var bundle=(ConduitBundleBlockEntity)level.getBlockEntity(pipe);
        var conduit=holder(level,id);
        bundle.addConduit(conduit,Direction.NORTH,null);
        bundle.setConnectionConfig(conduit,Direction.NORTH,ChemicalConnectionConfig.DEFAULT.withExtractRedstoneControl(RedstoneControl.ALWAYS_ACTIVE));
        bundle.setConnectionConfig(conduit,Direction.SOUTH,ChemicalConnectionConfig.DEFAULT.withIsInsert(true).withIsExtract(false));
        bundle.updateConnections(level,false);
        var rig=new Rig(name,pipe,bundle,conduit,source,new ArrayList<>(List.of(target)),targetAmount,(long)expectedRate*5);
        rigs.add(rig);
        return rig;
    }
    static void setup(ServerLevel level) {
        for(int x=0;x<2;x++) for(int z=0;z<2;z++) level.setChunkForced(x,z,true);
        rig(level,"rate-basic","enderio:chemical",750,HYDROGEN,0);
        rig(level,"rate-pressurized","enderio:pressurized_chemical",2000,HYDROGEN,0);
        rig(level,"rate-ender","enderio:ender_chemical",64000,HYDROGEN,0);
        rig(level,"rate-melodic","enderio_evolution:melodic_chemical_conduit",256000,HYDROGEN,0);
        rig(level,"rate-stellar","enderio_evolution:stellar_chemical_conduit",Integer.MAX_VALUE,HYDROGEN,0);
        var channel=rig(level,"channel","enderio_evolution:melodic_chemical_conduit",256000,HYDROGEN,0);
        channel.bundle.setConnectionConfig(channel.holder,Direction.SOUTH,ChemicalConnectionConfig.DEFAULT.withIsInsert(true).withIsExtract(false).withInsertChannel(DyeColor.RED));
        var redstone=rig(level,"redstone","enderio_evolution:stellar_chemical_conduit",Integer.MAX_VALUE,HYDROGEN,0);
        redstone.bundle.setConnectionConfig(redstone.holder,Direction.NORTH,ChemicalConnectionConfig.DEFAULT);
        rig(level,"incompatible","enderio_evolution:stellar_chemical_conduit",Integer.MAX_VALUE,OXYGEN,1000);
        rig(level,"full","enderio_evolution:stellar_chemical_conduit",Integer.MAX_VALUE,HYDROGEN,8192000);
        rig(level,"partial","enderio_evolution:stellar_chemical_conduit",Integer.MAX_VALUE,HYDROGEN,8192000-123);
        var multi=rig(level,"multi-output","enderio_evolution:melodic_chemical_conduit",256000,HYDROGEN,7192000);
        multi.targets.add(tank(level,multi.pipe.east(),HYDROGEN,0));
        multi.bundle.setConnectionConfig(multi.holder,Direction.SOUTH,ChemicalConnectionConfig.DEFAULT.withIsInsert(true).withIsExtract(false).withPriority(10));
        multi.bundle.setConnectionConfig(multi.holder,Direction.EAST,ChemicalConnectionConfig.DEFAULT.withIsInsert(true).withIsExtract(false).withPriority(-10));
        multi.bundle.updateConnections(level,false);
        var mixed=rig(level,"mixed-tier","enderio_evolution:stellar_chemical_conduit",750,HYDROGEN,0);
        var lower=mixed.pipe.south();
        mixed.targets.set(0,tank(level,lower.south(),HYDROGEN,0));
        level.setBlockAndUpdate(lower,EIOBlocks.CONDUIT_BUNDLE.get().defaultBlockState());
        var lowerBundle=(ConduitBundleBlockEntity)level.getBlockEntity(lower);
        var lowerHolder=holder(level,"enderio:chemical");
        lowerBundle.addConduit(lowerHolder,Direction.NORTH,null);
        lowerBundle.setConnectionConfig(lowerHolder,Direction.SOUTH,ChemicalConnectionConfig.DEFAULT.withIsInsert(true).withIsExtract(false));
        mixed.bundle.updateConnections(level,false);
        lowerBundle.updateConnections(level,false);
    }
    static void resume(ServerLevel level) {
        var pipe=new BlockPos(0,6,5); // stellar rate rig
        var conduit=holder(level,"enderio_evolution:stellar_chemical_conduit");
        var bundle=(ConduitBundleBlockEntity)level.getBlockEntity(pipe);
        check(bundle!=null && bundle.hasConduitStrict(conduit),"Saved conduit failed reload");
        check(((ChemicalConnectionConfig)bundle.getConnectionConfig(conduit,Direction.NORTH)).extractRedstoneControl()==RedstoneControl.ALWAYS_ACTIVE,"Saved redstone config failed reload");
        var source=level.getCapability(Capabilities.CHEMICAL.block(),pipe.north(),Direction.UP);
        var target=level.getCapability(Capabilities.CHEMICAL.block(),pipe.south(),Direction.UP);
        check(amount(source)+amount(target)==SOURCE,"Saved tank total changed");
        // Leave enough receiver space, then insert new source contents for another transfer.
        try(var t=Transaction.openRoot()) {
            check(target.extract(HYDROGEN,1_000_000,t)==1_000_000,"Saved receiver cannot extract");
            check(source.insert(HYDROGEN,1_000_000,t)==1_000_000,"Saved source cannot insert");
            t.commit();
        }
        rigs.add(new Rig("reload",pipe,bundle,conduit,source,new ArrayList<>(List.of(target)),amount(target),(long)Integer.MAX_VALUE*5));
        report.put("save_and_restart","conduit, side configuration and tank quantities restored");
        if(Boolean.getBoolean("sise.compat.chemical.deep")) {
            var largePipe=new BlockPos(0,6,20);
            var largeSource=tank(level,largePipe.north(),HYDROGEN,5_000_000_000L);
            var largeTarget=tank(level,largePipe.south(),HYDROGEN,0);
            level.setBlockAndUpdate(largePipe,EIOBlocks.CONDUIT_BUNDLE.get().defaultBlockState());
            var largeBundle=(ConduitBundleBlockEntity)level.getBlockEntity(largePipe);
            largeBundle.addConduit(conduit,Direction.NORTH,null);
            largeBundle.setConnectionConfig(conduit,Direction.NORTH,ChemicalConnectionConfig.DEFAULT.withExtractRedstoneControl(RedstoneControl.ALWAYS_ACTIVE));
            largeBundle.setConnectionConfig(conduit,Direction.SOUTH,ChemicalConnectionConfig.DEFAULT.withIsInsert(true).withIsExtract(false));
            largeBundle.updateConnections(level,false);
            rigs.add(new Rig("rate-stellar-large",largePipe,largeBundle,conduit,largeSource,new ArrayList<>(List.of(largeTarget)),0,(long)Integer.MAX_VALUE*5));
            report.put("large_batch_setup","Real Mekanism tanks; fixture-only ultimate capacity 12,000,000,000 mB; source 5,000,000,000 mB");
        }
    }
    static long amount(ResourceHandler<ChemicalResource> handler) {
        long total=0;for(int i=0;i<handler.size();i++) total+=handler.getAmountAsLong(i);return total;
    }
    static class Rig {
        final String name; final BlockPos pipe; final ConduitBundleBlockEntity bundle; final Holder<Conduit<?,?>> holder;
        final ResourceHandler<ChemicalResource> source; final List<ResourceHandler<ChemicalResource>> targets; final long initial; final long limit;
        final long startingSource;
        long previous, maxBurst;
        Rig(String name,BlockPos pipe,ConduitBundleBlockEntity bundle,Holder<Conduit<?,?>> holder,ResourceHandler<ChemicalResource> source,List<ResourceHandler<ChemicalResource>> targets,long initial,long limit) {
            this.name=name;this.pipe=pipe;this.bundle=bundle;this.holder=holder;this.source=source;this.targets=targets;this.initial=initial;this.limit=limit;
            this.startingSource=amount(source);
        }
        long output(){return targets.stream().mapToLong(ChemicalProbe::amount).sum()-initial;}
        void observe() {
            long transferred=output();
            check(amount(source)+transferred==startingSource,name+" chemical lost or duplicated");
            long delta=transferred-previous;
            check(delta>=0 && delta<=limit,name+" exceeded rate "+delta+" > "+limit);
            if(name.equals("multi-output") && transferred>0 && amount(targets.get(0))<8192000) check(amount(targets.get(1))==0,"Priority ordering ignored");
            maxBurst=Math.max(maxBurst,delta);previous=transferred;
        }
    }
    static void finish(Throwable failure) {
        finished=true;
        report.put("status",failure==null?"passed":"failed");
        report.put("resume",RESUME);
        report.put("ticks",ticks);
        if(failure!=null) {report.put("error",failure.toString());failure.printStackTrace();}
        try {
            var path=Path.of(System.getProperty("sise.compat.chemical.report"));Files.createDirectories(path.getParent());
            Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(report));
        } catch(Exception e) {throw new RuntimeException(e);}
        Minecraft.getInstance().execute(()->Minecraft.getInstance().stop());
    }
}
