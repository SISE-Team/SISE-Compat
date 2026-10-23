package local.sise.compat.probe;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import com.tacz.guns.api.*;
import com.tacz.guns.api.entity.*;
import com.tacz.guns.api.item.*;
import com.tacz.guns.api.item.builder.*;
import com.tacz.guns.crafting.*;
import com.tacz.guns.inventory.GunSmithTableMenu;
import com.tacz.guns.resource.CommonAssetsManager;
import io.netty.buffer.Unpooled;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Real server inventory/crafting/reload/shoot/persistence checks in a marked disposable client. */
@Mod("sise_flow_probe")
public final class TaczFlowProbe {
    private static final Path DIR = Path.of(System.getProperty("sise.flow.fixture"));
    private static final boolean RELOAD = Boolean.getBoolean("sise.flow.reload");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String,Object> REPORT = new LinkedHashMap<>();
    private static final List<Map<String,Object>> GUNS = new ArrayList<>();
    private static final Set<Object> BACKUP_SCREENS = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<String> RENDERED_GUNS = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final List<ItemEntity> CRAFTED = new ArrayList<>();
    private static final Map<Identifier,ItemStack> CRAFTED_GUNS = new HashMap<>();
    private static boolean crafting;
    private static List<Identifier> guns;
    private static boolean prepared, done, clientSeen;
    private static int ticks, gunIndex, phase, age, reloadBefore, shotBefore, shots, assertions;
    private static ShootResult lastShot;
    private static Villager target;
    private static int attachmentsInstalled, clientStopTicks;
    private static final BlockPos CHEST = new BlockPos(48,90,48);
    public TaczFlowProbe() {
        if (!Files.isRegularFile(DIR.resolve("MOD_UPDATE_AUTOTEST_ONLY"))) throw new IllegalStateException("Disposable fixture marker required");
        NeoForge.EVENT_BUS.addListener(TaczFlowProbe::tick);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) -> {
            if(crafting && !event.getLevel().isClientSide() && event.getEntity() instanceof ItemEntity item && !event.isCanceled()) CRAFTED.add(item);
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            var mc = Minecraft.getInstance();
            if (mc.level == null && mc.screen instanceof net.minecraft.client.gui.screens.BackupConfirmScreen backup && BACKUP_SCREENS.add(backup)) {
                try {var f=backup.getClass().getDeclaredField("onProceed");f.setAccessible(true);((net.minecraft.client.gui.screens.BackupConfirmScreen.Listener)f.get(backup)).proceed(false,false);}
                catch(ReflectiveOperationException error){throw new RuntimeException(error);}
            }
            if (mc.level != null && mc.player != null) clientSeen = true;
            if (mc.player != null && IGun.getIGunOrNull(mc.player.getMainHandItem()) instanceof IGun gun) {
                var stack=mc.player.getMainHandItem();
                if(TimelessAPI.getGunDisplay(stack).map(display -> display.getGunModel()!=null).orElse(false))RENDERED_GUNS.add(gun.getGunId(stack).toString());
            }
            if (done && ++clientStopTicks>60 && mc.getOverlay() == null) mc.stop();
        });
    }
    private static void require(boolean value, String message) { assertions++; if (!value) throw new AssertionError(message); }
    private static void write(String name, Object data) {
        try { Files.writeString(DIR.resolve(name),GSON.toJson(data)); } catch (Exception e) { throw new RuntimeException(e); }
    }
    private static void tick(ServerTickEvent.Post event) {
        if (done || !clientSeen) return;
        var server = event.getServer();
        if (server.getPlayerList().getPlayers().isEmpty()) return;
        var player = server.getPlayerList().getPlayers().getFirst();
        ticks++;
        try {
            if (!prepared && ticks >= 30) {
                prepared = true;
                if (RELOAD) { reloadCheck(server); finish(server); return; }
                player.setGameMode(GameType.SURVIVAL);
                player.setInvulnerable(true);player.setNoGravity(true);
                player.teleportTo(server.overworld(),64.5,91,60.5,Set.of(),0,0,true);
                for (int x=60;x<=75;x++) for (int z=58;z<=74;z++) server.overworld().setBlock(new BlockPos(x,89,z),Blocks.STONE.defaultBlockState(),3);
                server.overworld().getEntitiesOfClass(Villager.class,new net.minecraft.world.phys.AABB(55,85,55,80,110,80),Villager::isNoAi).forEach(Villager::discard);
                RENDERED_GUNS.clear();
                codecChecks(server);
                craftChecks(player);
                guns = TimelessAPI.getAllCommonGunIndex().stream().map(Map.Entry::getKey).sorted(Comparator.comparing(Object::toString)).toList();
                require(!guns.isEmpty(),"No guns loaded");
                REPORT.put("gun_index_count",guns.size());
            }
            if (!prepared) return;
            if (gunIndex >= guns.size()) { saveChecks(server); finish(server); return; }
            var operator = IGunOperator.fromLivingEntity(player);
            if (phase == 0) {
                var id=guns.get(gunIndex); var gun=CRAFTED_GUNS.getOrDefault(id,ItemStack.EMPTY).copy();
                if(gun.isEmpty()) {
                    // The native pack exposes some experimental guns only in its creative catalogue.
                    gun=com.tacz.guns.crafting.result.RawGunTableResult.init(new com.tacz.guns.crafting.result.RawGunTableResult("gun",id,1)).getResult().copy();
                    @SuppressWarnings("unchecked") var creativeOnly=(List<String>)REPORT.computeIfAbsent("native_catalogue_guns_without_workbench_recipe",unused->new ArrayList<String>());
                    creativeOnly.add(id.toString());
                }
                require(!gun.isEmpty(),"Native gun catalogue product is empty: "+id);
                player.getInventory().clearContent(); player.getInventory().setSelectedSlot(0);
                player.setItemInHand(InteractionHand.MAIN_HAND,gun);
                var gunApi=IGun.getIGunOrNull(gun);
                var originalData=TimelessAPI.getCommonGunIndex(gunApi.getGunId(gun)).orElseThrow().getGunData();
                require(originalData.getFireModeSet().contains(gunApi.getFireMode(gun)),"Crafted gun has invalid fire mode: "+id);
                gunApi.setCurrentAmmoCount(gun,0);gunApi.setBulletInBarrel(gun,false);
                var installed=EnumSet.noneOf(com.tacz.guns.api.item.attachment.AttachmentType.class);
                for(var candidate:TimelessAPI.getAllCommonAttachmentIndex()) {
                    var attachment=AttachmentItemBuilder.create().setId(candidate.getKey()).build();
                    var attachmentApi=IAttachment.getIAttachmentOrNull(attachment);
                    var type=attachmentApi.getType(attachment);if(installed.contains(type))continue;
                    var expectedAttachment=attachmentApi.getAttachmentId(attachment);
                    if(!gunApi.allowAttachmentType(gun,type)||!gunApi.allowAttachment(gun,attachment)||gunApi.hasAttachmentLock(gun))continue;
                    gunApi.installAttachment(gun,attachment);
                    require(gunApi.getAttachmentId(gun,type).equals(expectedAttachment),"Attachment installation failed: "+candidate.getKey()+" canonical="+expectedAttachment+" actual="+gunApi.getAttachmentId(gun,type));
                    var copy=gun.copy();gunApi.unloadAttachment(copy,type);
                    require(gunApi.getAttachmentId(gun,type).equals(expectedAttachment),"Unloading a copied stack mutated the equipped gun");
                    installed.add(type);attachmentsInstalled++;
                }
                if(target!=null)target.discard();
                target=EntityType.VILLAGER.create(server.overworld(),EntitySpawnReason.COMMAND);
                target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10000);target.getAttribute(Attributes.SCALE).setBaseValue(2.5);
                target.setHealth(10000);target.setNoAi(true);target.setNoGravity(true);target.setSilent(true);target.setPos(64.5,91,68.5);server.overworld().addFreshEntity(target);
                var canonical=gunApi.getGunId(gun);
                var data=TimelessAPI.getCommonGunIndex(canonical).orElseThrow().getGunData();
                if(!canonical.equals(id)) {
                    @SuppressWarnings("unchecked") var aliases=(Map<String,String>)REPORT.computeIfAbsent("native_legacy_gun_id_migrations",unused->new TreeMap<String,String>());
                    aliases.put(id.toString(),canonical.toString());
                }
                var ammo=AmmoItemBuilder.create().setId(data.getAmmoId()).setCount(1).build();
                for(int remaining=512,slot=9;remaining>0;slot++) {int size=Math.min(remaining,ammo.getMaxStackSize());player.getInventory().setItem(slot,ammo.copyWithCount(size));remaining-=size;}
                operator.initialData();operator.draw(player::getMainHandItem);operator.aim(true);
                age=0;phase=1;lastShot=null;
                player.inventoryMenu.broadcastFullState();
            } else if (phase == 1 && ++age >= 50 && operator.getSynDrawCoolDown() <= 0) {
                reloadBefore=ammoCount(player);operator.reload();age=0;phase=2;
            } else if (phase == 2) {
                age++;
                var gun=IGun.getIGunOrNull(player.getMainHandItem());
                int loaded=gun.getCurrentAmmoCount(player.getMainHandItem())+(gun.hasBulletInBarrel(player.getMainHandItem())?1:0);
                if(gun.useInventoryAmmo(player.getMainHandItem())&&age>=10) {
                    require(ammoCount(player)==reloadBefore&&!operator.getSynReloadState().getStateType().isReloading(),"Inventory-fed gun incorrectly reloads a magazine");
                    shotBefore=ammoCount(player);age=0;phase=3;
                } else if (loaded>0 && !operator.getSynReloadState().getStateType().isReloading()) {
                    require(ammoCount(player)<reloadBefore,"Reload did not consume survival inventory ammo: "+guns.get(gunIndex));
                    shotBefore=loaded;age=0;phase=3;
                } else if (age>600) throw new AssertionError("Reload timed out: "+guns.get(gunIndex)+" loaded="+loaded+" state="+operator.getSynReloadState());
            } else if (phase == 3) {
                age++;
                if (age%10==0) { lastShot=operator.shoot(()->0f,()->0f); if (lastShot==ShootResult.SUCCESS) shots++;if(lastShot==ShootResult.NEED_BOLT)operator.bolt(); }
                var gun=IGun.getIGunOrNull(player.getMainHandItem());
                boolean inventoryFeed=gun.useInventoryAmmo(player.getMainHandItem());
                int after=inventoryFeed?ammoCount(player):gun.getCurrentAmmoCount(player.getMainHandItem())+(gun.hasBulletInBarrel(player.getMainHandItem())?1:0);
                if (after<shotBefore && target.getHealth()<10000) {
                    require(shots>0,"Ammo changed without a successful shot");
                    var row=new LinkedHashMap<String,Object>();row.put("id",guns.get(gunIndex).toString());row.put("ammo_source",inventoryFeed?"survival_inventory":"reloaded_magazine_and_chamber");row.put("ammo_before_shot",shotBefore);row.put("ammo_after_shot",after);row.put("damage",10000-target.getHealth());row.put("result",String.valueOf(lastShot));GUNS.add(row);
                    var pos=CHEST.offset(gunIndex/27*2,0,0);server.overworld().setBlock(pos,Blocks.CHEST.defaultBlockState(),3);
                    ((ChestBlockEntity)server.overworld().getBlockEntity(pos)).setItem(gunIndex%27,player.getMainHandItem().copy());
                    gunIndex++;phase=0;
                    write("tacz-flow-progress.json",Map.of("completed",gunIndex,"total",guns.size(),"guns",GUNS));
                } else if (age>240) throw new AssertionError("Shooting timed out: "+guns.get(gunIndex)+" result="+lastShot+" before="+shotBefore+" after="+after+" target="+target.getHealth());
            }
        } catch (Throwable error) {
            REPORT.put("passed",false);REPORT.put("error",error.toString());error.printStackTrace();complete();
        }
    }
    private static int ammoCount(ServerPlayer player) {
        int total=0;for(var stack:player.getInventory().getNonEquipmentItems()) if(stack.getItem() instanceof IAmmo) total+=stack.getCount();return total;
    }
    private static void codecChecks(MinecraftServer server) {
        var ops=server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        int count=0;
        for(var entry:TimelessAPI.getAllRecipes().entrySet()) {
            var recipe=entry.getValue();recipe.resolveIngredients(server.registryAccess());
            var expected=recipe.getOutput().copy();require(!expected.isEmpty(),"Empty product: "+entry.getKey());
            var json=GunSmithTableSerializer.CODEC.codec().encodeStart(ops,recipe).getOrThrow();
            var decoded=GunSmithTableSerializer.CODEC.codec().parse(ops,json).getOrThrow();
            require(ItemStack.matches(expected,decoded.getOutput()),"JSON product changed: "+entry.getKey());
            require(json.equals(GunSmithTableSerializer.CODEC.codec().encodeStart(ops,decoded).getOrThrow()),"JSON recipe changed");
            var buf=new RegistryFriendlyByteBuf(Unpooled.buffer(),server.registryAccess());
            try {
                GunSmithTableSerializer.STREAM_CODEC.encode(buf,decoded);
                var network=GunSmithTableSerializer.STREAM_CODEC.decode(buf);
                require(buf.readableBytes()==0 && ItemStack.matches(expected,network.getOutput()),"Network product changed: "+entry.getKey());
                require(recipe.getId().equals(network.getId()) && recipe.getTab().equals(network.getTab()),"Network identity changed");
            } finally { buf.release(); }
            count++;
        }
        REPORT.put("gunpack_recipe_json_and_network_cases",count);
        var custom=new ItemStack(Items.DIAMOND,7);custom.set(DataComponents.CUSTOM_NAME,Component.literal("Codec custom component verification"));
        var value=new GunSmithTableRecipe(Identifier.parse("sise_compat:custom_probe"),new com.tacz.guns.crafting.result.GunSmithTableResult(custom,Identifier.parse("tacz:custom")),List.of(new GunSmithTableIngredient(net.minecraft.world.item.crafting.Ingredient.of(Items.IRON_INGOT),3)));
        var encoded=GunSmithTableSerializer.CODEC.codec().encodeStart(ops,value).getOrThrow();
        var customDecoded=GunSmithTableSerializer.CODEC.codec().parse(ops,encoded).getOrThrow();
        require(ItemStack.matches(custom,customDecoded.getOutput())&&customDecoded.getId().equals(value.getId()),"Custom component product or recipe identity lost");
        var customBuf=new RegistryFriendlyByteBuf(Unpooled.buffer(),server.registryAccess());
        try {GunSmithTableSerializer.STREAM_CODEC.encode(customBuf,customDecoded);var network=GunSmithTableSerializer.STREAM_CODEC.decode(customBuf);require(ItemStack.matches(custom,network.getOutput())&&network.getId().equals(value.getId())&&network.getTab().equals(value.getTab()),"Custom components or group lost over the network");}finally{customBuf.release();}
        REPORT.put("custom_component_result",encoded);
        for(String type:List.of("gun","ammo","attachment")) {
            var id=type.equals("gun")?TimelessAPI.getAllCommonGunIndex().iterator().next().getKey():type.equals("ammo")?TimelessAPI.getAllCommonAmmoIndex().iterator().next().getKey():TimelessAPI.getAllCommonAttachmentIndex().iterator().next().getKey();
            var raw=JsonParser.parseString("{\"result\":{\"type\":\""+type+"\",\"id\":\""+id+"\",\"count\":2,\"group\":\"sise_compat:probe\",\"ammo_count\":7},\"materials\":[{\"item\":\"minecraft:iron_ingot\",\"count\":3}]}");
            var product=GunSmithTableSerializer.CODEC.codec().parse(ops,raw).getOrThrow();
            var first=GunSmithTableSerializer.CODEC.codec().encodeStart(ops,product).getOrThrow();
            var next=GunSmithTableSerializer.CODEC.codec().parse(ops,first).getOrThrow();
            require(ItemStack.matches(product.getOutput(),next.getOutput()),"Raw result changed: "+type);
            require(first.equals(GunSmithTableSerializer.CODEC.codec().encodeStart(ops,next).getOrThrow()),"Raw definition changed: "+type);
            require(next.getTab().equals(Identifier.parse("sise_compat:probe")),"Group override lost");
            if(type.equals("gun"))require(IGun.getIGunOrNull(next.getOutput()).getCurrentAmmoCount(next.getOutput())==7,"Preloaded ammunition lost");
            var tagged=raw.deepCopy().getAsJsonObject();tagged.getAsJsonObject("result").addProperty("nbt","{SiseCodecProbe:1}");
            var taggedRecipe=GunSmithTableSerializer.CODEC.codec().parse(ops,tagged).getOrThrow();
            var taggedAgain=GunSmithTableSerializer.CODEC.codec().parse(ops,GunSmithTableSerializer.CODEC.codec().encodeStart(ops,taggedRecipe).getOrThrow()).getOrThrow();
            require(ItemStack.matches(taggedRecipe.getOutput(),taggedAgain.getOutput())&&taggedAgain.getOutput().getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getIntOr("SiseCodecProbe",0)==1,"Legacy result NBT lost: "+type);
            for(int invalid:List.of(0,-1)) {
                var bad=raw.deepCopy().getAsJsonObject();bad.getAsJsonObject("result").addProperty("count",invalid);
                require(GunSmithTableSerializer.CODEC.codec().parse(ops,bad).isError(),"Invalid output quantity accepted: "+invalid);
                bad=raw.deepCopy().getAsJsonObject();bad.getAsJsonArray("materials").get(0).getAsJsonObject().addProperty("count",invalid);
                require(GunSmithTableSerializer.CODEC.codec().parse(ops,bad).isError(),"Invalid material quantity accepted: "+invalid);
            }
        }
        REPORT.put("codec_additional_edge_cases",16);
    }
    private static List<ItemEntity> drops(ServerPlayer player) { return player.level().getEntitiesOfClass(ItemEntity.class,player.getBoundingBox().inflate(5)); }
    private static void craftChecks(ServerPlayer player) throws Exception {
        var menus=new ArrayList<GunSmithTableMenu>();
        for(var block:TimelessAPI.getAllCommonBlockIndex())menus.add(new GunSmithTableMenu(42,player.getInventory(),block.getKey()));
        var permission=GunSmithTableMenu.class.getDeclaredMethod("getRecipe",Identifier.class);permission.setAccessible(true);
        var benches=new TreeMap<String,Integer>();
        int cases=0,denied=0;
        for(var entry:TimelessAPI.getAllRecipes().entrySet()) {
            var recipe=entry.getValue();recipe.resolveIngredients(player.level().registryAccess());
            GunSmithTableMenu menu=null;
            for(var candidate:menus)if(permission.invoke(candidate,entry.getKey())!=null){menu=candidate;break;}
            require(menu!=null,"Recipe has no permitted workbench: "+entry.getKey()+" tab="+recipe.getTab());
            benches.merge(menu.getBlockId().toString(),1,Integer::sum);
            var required=new LinkedHashMap<net.minecraft.world.item.Item,Integer>();
            for(var input:recipe.getInputs()) {
                var sample=input.getIngredientOrThrow().items().findFirst().orElseThrow().value();
                required.merge(sample,input.getCount(),Integer::sum);
            }
            for(int test=0;test<3;test++) {
                drops(player).forEach(ItemEntity::discard);player.getInventory().clearContent();
                int slot=0;boolean shortage=false;
                for(var input:required.entrySet()) {
                    int remaining=test==0?0:input.getValue();
                    if(test==1&&!shortage){remaining--;shortage=true;}
                    while(remaining>0){int take=Math.min(remaining,new ItemStack(input.getKey()).getMaxStackSize());player.getInventory().setItem(slot++,new ItemStack(input.getKey(),take));remaining-=take;}
                }
                require(slot<=36,"Recipe materials do not fit ordinary inventory: "+entry.getKey());
                var before=new ArrayList<ItemStack>();for(int s=0;s<36;s++)before.add(player.getInventory().getItem(s).copy());
                CRAFTED.clear();crafting=true;
                try {menu.doCraft(entry.getKey(),player);} finally {crafting=false;}
                var result=List.copyOf(CRAFTED);
                if(test<2){require(result.isEmpty(),"Insufficient materials produced an item: "+entry.getKey());for(int s=0;s<36;s++)require(ItemStack.matches(before.get(s),player.getInventory().getItem(s)),"Rejected craft consumed materials");denied++;}
                else {require(result.size()==1 && ItemStack.matches(result.getFirst().getItem(),recipe.getOutput()),"Craft product incorrect: "+entry.getKey()+" drops="+result.size());for(int s=0;s<36;s++)require(player.getInventory().getItem(s).isEmpty(),"Craft did not charge exact materials: "+entry.getKey());var product=result.getFirst().getItem();if(IGun.getIGunOrNull(product) instanceof IGun gun)CRAFTED_GUNS.put(gun.getGunId(product),product.copy());cases++;}
                result.forEach(ItemEntity::discard);
            }
        }
        REPORT.put("paid_survival_workbench_crafts",cases);REPORT.put("insufficient_material_denials",denied);
        REPORT.put("paid_crafts_by_native_workbench",benches);
        REPORT.put("native_workbench_gun_product_ids",new TreeSet<>(CRAFTED_GUNS.keySet()).stream().map(Object::toString).toList());
    }
    private static void saveChecks(MinecraftServer server) {
        var ops=server.registryAccess().createSerializationContext(JsonOps.INSTANCE);var expected=new JsonArray();
        for(int i=0;i<guns.size();i++) {
            var chest=(ChestBlockEntity)server.overworld().getBlockEntity(CHEST.offset(i/27*2,0,0));
            expected.add(ItemStack.CODEC.encodeStart(ops,chest.getItem(i%27)).getOrThrow());chest.setChanged();
        }
        write("tacz-flow-persisted.json",expected);server.saveEverything(false,true,true);
        REPORT.put("guns_saved_in_chests",guns.size());
    }
    private static void reloadCheck(MinecraftServer server) throws Exception {
        var expected=JsonParser.parseString(Files.readString(DIR.resolve("tacz-flow-persisted.json"))).getAsJsonArray();
        var ops=server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        for(int i=0;i<expected.size();i++) {
            var chest=(ChestBlockEntity)server.overworld().getBlockEntity(CHEST.offset(i/27*2,0,0));
            require(chest!=null,"Persisted gun chest absent");var stack=ItemStack.CODEC.parse(ops,expected.get(i)).getOrThrow();
            var actual=chest.getItem(i%27);
            // Use the same JSON representation on both sides. Gson otherwise compares a live
            // Float(1.922f) with a parsed decimal(1.922) through different double expansions.
            var actualJson=JsonParser.parseString(ItemStack.CODEC.encodeStart(ops,actual).getOrThrow().toString());
            if(!expected.get(i).equals(actualJson))write("tacz-flow-disk-difference.json",Map.of("index",i,"expected",expected.get(i),"actual",actualJson));
            // JSON snapshots have no NBT numeric widths: parsing a small integer may produce a
            // ByteTag instead of the original IntTag. Compare the original JSON, then native APIs.
            require(expected.get(i).equals(actualJson)&&ItemStack.isSameItem(stack,actual)&&stack.getCount()==actual.getCount(),"Saved gun/ammo/components changed after restart: "+i);
            var gun=IGun.getIGunOrNull(actual);var reference=IGun.getIGunOrNull(stack);
            require(gun!=null&&reference!=null&&gun.getGunId(actual).equals(reference.getGunId(stack)),"Saved gun identity changed");
            require(gun.getCurrentAmmoCount(actual)==reference.getCurrentAmmoCount(stack)&&gun.hasBulletInBarrel(actual)==reference.hasBulletInBarrel(stack)&&gun.getFireMode(actual)==reference.getFireMode(stack),"Saved gun ammunition or fire mode changed");
            for(var type:com.tacz.guns.api.item.attachment.AttachmentType.values())require(gun.getAttachmentId(actual,type).equals(reference.getAttachmentId(stack,type)),"Saved attachment changed: "+type);
        }
        codecChecks(server);REPORT.put("persisted_guns_verified_after_restart",expected.size());
        REPORT.put("persistence_comparison","Every original source JSON field, item identity/count, ammunition, chamber, fire mode and attachment is checked against the reloaded native stack");
    }
    private static void finish(MinecraftServer server) { REPORT.put("passed",true);complete(); }
    private static void complete() { REPORT.put("guns",GUNS);REPORT.put("attachments_installed_and_copy_isolation_checked",attachmentsInstalled);REPORT.put("client_gun_models_resolved",RENDERED_GUNS);REPORT.put("assertions",assertions);REPORT.put("ticks",ticks);write(RELOAD?"tacz-flow-reload-result.json":"tacz-flow-result.json",REPORT);done=true; }
}
