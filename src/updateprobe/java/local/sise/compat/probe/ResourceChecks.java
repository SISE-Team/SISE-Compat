package local.sise.compat.probe;

import com.google.gson.*;
import java.io.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.block.RenderShape;

/** Validate the actual baked models and loaded audio, independently of the source repair rules. */
public final class ResourceChecks {
    public static Map<String,Object> client(Minecraft mc) throws Exception {
        var report=new LinkedHashMap<String,Object>();
        var missing=new ArrayList<String>();int states=0,fluids=0,sounds=0;
        var blocks=mc.getModelManager().getBlockStateModelSet();
        for(var block:BuiltInRegistries.BLOCK) for(var state:block.getStateDefinition().getPossibleStates()) {
            if(state.getRenderShape()!=RenderShape.MODEL)continue;
            states++;
            if(blocks.get(state)==blocks.missingModel())missing.add(state.toString());
        }
        report.put("ordinary_block_states_with_baked_models",states);
        if(!missing.isEmpty())throw new AssertionError("Missing baked block states: "+missing);
        for(var fluid:BuiltInRegistries.FLUID) {
            if(fluid==Fluids.EMPTY)continue;
            var model=mc.getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState());
            if(model.stillMaterial().sprite().contents().name().toString().equals("minecraft:missingno")
                    ||model.flowingMaterial().sprite().contents().name().toString().equals("minecraft:missingno"))
                throw new AssertionError("Fluid uses a missing sprite: "+BuiltInRegistries.FLUID.getKey(fluid));
            fluids++;
        }
        report.put("registered_fluid_models_with_real_sprites",fluids);
        var resources=mc.getResourceManager();
        try(var in=local.sise.compat.SiseCompat.class.getResourceAsStream("/sise_compat/resource_repairs.json")) {
            var manifest=JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(in))).getAsJsonObject();
            for(var entry:manifest.entrySet()) {
                String path=entry.getKey();
                if(!path.endsWith(".ogg"))continue;
                String[] parts=path.split("/",3);var resource=resources.getResourceOrThrow(Identifier.fromNamespaceAndPath(parts[1],parts[2]));
                try(var stream=resource.open()) {
                    if(!Arrays.equals(stream.readNBytes(4),new byte[]{'O','g','g','S'}))throw new AssertionError("Invalid restored sound: "+path);
                }
                sounds++;
            }
        }
        report.put("official_restored_ogg_files_loaded",sounds);
        for(String id:List.of("twilightforest:entity.twilightforest.squirrel.ambient","twilightforest:entity.twilightforest.fire_beetle.death","twilightforest:entity.twilightforest.deer.eat","tofucraft:music.tofu_dungeon")) {
            var event=mc.getSoundManager().getSoundEvent(Identifier.parse(id));
            if(event==null || event.getWeight()==0)throw new AssertionError("Sound event is silent: "+id);
        }
        report.put("repaired_sound_registrations",local.sise.compat.ReviewedResources.repairedSoundReads);
        report.put("tacz_audio_resources",gunAudio(mc));
        var manuals=new TreeMap<String,Integer>();int manualRecipes=0;
        var missingManualRecipes=new ArrayList<String>();
        var doubleRecipe=vazkii.patchouli.client.book.page.abstr.PageDoubleRecipe.class;
        var recipeId=doubleRecipe.getDeclaredField("recipeId");recipeId.setAccessible(true);
        var recipe2Id=doubleRecipe.getDeclaredField("recipe2Id");recipe2Id.setAccessible(true);
        var recipe1=doubleRecipe.getDeclaredField("recipe1");recipe1.setAccessible(true);
        var recipe2=doubleRecipe.getDeclaredField("recipe2");recipe2.setAccessible(true);
        for(var entry:vazkii.patchouli.common.book.BookRegistry.INSTANCE.books.entrySet()) {
            var contents=entry.getValue().getContents();
            if(contents==null||contents.isErrored())throw new AssertionError("Manual failed to compile: "+entry.getKey()+" cause="+(contents==null?"absent":contents.getException()));
            manuals.put(entry.getKey().toString(),contents.entries.size());
            for(var bookEntry:contents.entries.values()) {
                if(!bookEntry.canAdd())continue;
                for(var page:bookEntry.getPages())if(page instanceof vazkii.patchouli.client.book.page.abstr.PageDoubleRecipe<?>) {
                    for(var pair:List.of(new java.lang.reflect.Field[]{recipeId,recipe1},new java.lang.reflect.Field[]{recipe2Id,recipe2})) {
                        var id=pair[0].get(page);if(id==null)continue;
                        if(pair[1].get(page)==null)missingManualRecipes.add(entry.getKey()+"/"+bookEntry.getId()+" -> "+id);
                        else manualRecipes++;
                    }
                }
            }
        }
        if(manuals.getOrDefault("justdirethings:justdirethingsbook",0)==0)throw new AssertionError("Just Dire Things manual is empty");
        report.put("compiled_manual_entries",manuals);
        if(!missingManualRecipes.isEmpty())throw new AssertionError("Unresolved manual recipe pages: "+missingManualRecipes);
        report.put("resolved_manual_recipe_displays",manualRecipes);
        report.put("patchouli_runtime_recipe_keys",manualKeys(mc));
        var uncrafting=twilightforest.compat.RecipeViewerConstants.getAllUncraftingRecipes(mc.getSingleplayerServer().getRecipeManager());
        if(uncrafting.isEmpty())throw new AssertionError("Twilight uncrafting recipes are empty");
        report.put("twilight_uncrafting_recipe_previews",uncrafting.size());
        report.put("twilight_uncrafting_jei_layouts",uncraftingLayouts());
        report.put("passed",true);return report;
    }
    private static Map<String,Object> gunAudio(Minecraft mc) throws Exception {
        JsonObject audit;
        try(var stream=local.sise.compat.SiseCompat.class.getResourceAsStream("/sise_compat/tacz_sound_audit.json")) {
            audit=JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(stream))).getAsJsonObject();
        }
        var ids=new TreeSet<String>();int references=0;
        for(String kind:List.of("displays","animations"))for(var file:audit.getAsJsonArray(kind)) {
            String[] parts=file.getAsString().split("/",3);JsonObject json;
            try(var reader=mc.getResourceManager().getResourceOrThrow(Identifier.fromNamespaceAndPath(parts[1],parts[2])).openAsReader()) {
                json=com.tacz.guns.resource.CommonAssetsManager.GSON.fromJson(reader,JsonObject.class);
            }
            if(kind.equals("displays")) {
                for(var value:json.getAsJsonObject("sounds").entrySet()) {String id=value.getValue().getAsString();if(!id.isEmpty()){ids.add(id);references++;}}
            } else for(var state:json.getAsJsonObject("animations").entrySet()) {
                var effects=state.getValue().getAsJsonObject().getAsJsonObject("sound_effects");if(effects==null)continue;
                for(var effect:effects.entrySet()) {
                    var nodes=effect.getValue().isJsonArray()?effect.getValue().getAsJsonArray():new JsonArray();
                    if(!effect.getValue().isJsonArray())nodes.add(effect.getValue());
                    for(var node:nodes)if(node.isJsonObject()&&node.getAsJsonObject().has("effect")){ids.add(node.getAsJsonObject().get("effect").getAsString());references++;}
                }
            }
        }
        for(String value:ids) {
            var id=Identifier.parse(value);var resource=Identifier.fromNamespaceAndPath(id.getNamespace(),"tacz_sounds/"+id.getPath()+".ogg");
            try(var stream=mc.getResourceManager().getResourceOrThrow(resource).open()) {
                if(!Arrays.equals(stream.readNBytes(4),new byte[]{'O','g','g','S'}))throw new AssertionError("Invalid TaCZ audio: "+value);
            }
        }
        return Map.of("display_files",audit.getAsJsonArray("displays").size(),"animation_files",audit.getAsJsonArray("animations").size(),"valid_audio_references",references,"real_ogg_assets",ids.size(),"dangling_references",0);
    }
    private static int manualKeys(Minecraft mc) {
        var runtime=local.sise.ores.compat.OresJeiPlugin.runtime;var before=mc.screen;
        try {
            vazkii.patchouli.api.PatchouliAPI.get().openBookGUI(Identifier.parse("justdirethings:justdirethingsbook"));
            if(mc.screen==null||!mc.screen.getClass().getName().contains("patchouli"))throw new AssertionError("The compiled manual did not open");
            for(var mapping:List.of(runtime.getKeyMappings().getShowRecipe(),runtime.getKeyMappings().getShowUses())) {
                net.minecraft.client.input.KeyEvent event=null;
                for(int key=32;key<=348;key++) {
                    var candidate=new net.minecraft.client.input.KeyEvent(key,0,0);
                    if(mapping.isActiveAndMatches(com.mojang.blaze3d.platform.InputConstants.getKey(candidate))){event=candidate;break;}
                }
                if(event==null||!vazkii.patchouli.client.jei.PatchouliJeiPlugin.handleRecipeKeybind(event,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT)))throw new AssertionError("Patchouli JEI recipe/usage key did not resolve the live binding");
                if(mc.screen==null||!mc.screen.getClass().getName().contains("jei"))throw new AssertionError("Patchouli recipe key did not open JEI");
            }
        } finally {mc.setScreen(before);}
        return 2;
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private static int uncraftingLayouts() {
        var runtime=local.sise.ores.compat.OresJeiPlugin.runtime;
        var manager=runtime.getRecipeManager();
        var category=manager.createRecipeCategoryLookup().includeHidden().get().filter(c->c instanceof twilightforest.compat.jei.categories.JEIUncraftingCategory).findFirst().orElseThrow();
        var recipes=manager.createRecipeLookup(category.getRecipeType()).includeHidden().get().toList();
        if(recipes.isEmpty())throw new AssertionError("No JEI uncrafting recipes registered");
        var focus=runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup();
        for(Object recipe:recipes)if(manager.createRecipeLayoutDrawable((mezz.jei.api.recipe.category.IRecipeCategory)category,recipe,focus).isEmpty())throw new AssertionError("Rejected uncrafting layout: "+recipe);
        return recipes.size();
    }
    private ResourceChecks() { }
}
