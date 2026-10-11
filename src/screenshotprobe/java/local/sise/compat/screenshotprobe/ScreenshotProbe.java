package local.sise.compat.screenshotprobe;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEItemKey;
import appeng.menu.me.crafting.CraftingPlanSummaryEntry;
import com.google.gson.GsonBuilder;
import com.xiaopiao.ae2ss.api.CraftingPlanScreenshotHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientResourceLoadFinishedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Disposable fixture only: exercise the addon's real GPU capture/readback/export pipeline. */
@Mod(value = "sise_compat_screenshot_probe", dist = Dist.CLIENT)
public final class ScreenshotProbe {
    private int resourcePass;
    private boolean worldRequested, started;
    private int worldTicks;
    public ScreenshotProbe() {
        NeoForge.EVENT_BUS.addListener((ClientResourceLoadFinishedEvent event) -> {
            if (++resourcePass == 2) Minecraft.getInstance().execute(this::createDisposableWorld);
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            var mc = Minecraft.getInstance();
            if (!worldRequested && mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen && mc.getOverlay() == null)
                createDisposableWorld();
            if (worldRequested && !started && mc.level != null && mc.player != null && ++worldTicks >= 30) {
                started = true;
                mc.execute(this::run);
            }
        });
    }

    private void createDisposableWorld() {
        if (worldRequested) return;
        var mc = Minecraft.getInstance();
        var directory = mc.gameDirectory.toPath().toAbsolutePath().normalize();
        if (!directory.equals(java.nio.file.Path.of(System.getProperty("sise.compat.screenshot.fixture", "")).toAbsolutePath().normalize()) || !Files.isRegularFile(directory.resolve("SISE_COMPAT_AUTOTEST_ONLY"))) throw new IllegalStateException("Probe must only run in disposable fixture");
        worldRequested = true;
        var settings = new LevelSettings("AE2 screenshot verification", GameType.CREATIVE,
                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel("sise-compat-screenshot-" + System.currentTimeMillis(), settings,
                new WorldOptions(602, false, false), WorldPresets::createFlatWorldDimensions, mc.screen);
    }

    private static Method method(Class<?> owner, String name, Class<?>... parameters) throws Exception {
        Method result = owner.getDeclaredMethod(name, parameters);
        result.setAccessible(true);
        return result;
    }

    @SuppressWarnings("unchecked")
    private void run() {
        var mc = Minecraft.getInstance();
        var entries = new ArrayList<CraftingPlanSummaryEntry>();
        var report = new LinkedHashMap<String, Object>();
        try {
            for (var item : BuiltInRegistries.ITEM) {
                if (item instanceof BlockItem && BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("minecraft")) {
                    entries.add(new CraftingPlanSummaryEntry(AEItemKey.of(item), entries.size() % 3 == 0 ? 7 : 0, 64, 32));
                    if (entries.size() == 80) break;
                }
            }
            if (entries.size() != 80) throw new IllegalStateException("Need 80 unique block items");
            var state = mc.gameRenderer.getGameRenderState().windowRenderState;
            int widthBefore = state.width, heightBefore = state.height, scaleBefore = state.guiScale;
            report.put("unique_items", entries.size());
            report.put("world", "new_disposable_fixture_world");
            report.put("minimum_gpu_batches", 2);
            var future = (CompletableFuture<Map<AEKey, BufferedImage>>) method(
                    CraftingPlanScreenshotHelper.class, "createItemImages", List.class).invoke(null, entries);
            future.whenComplete((icons, failure) -> mc.execute(() -> {
                try {
                    if (failure != null) throw new IllegalStateException("GPU capture failed", failure);
                    if (icons.size() != 80) throw new IllegalStateException("Wrong icon count: " + icons.size());
                    int visibleIcons = 0;
                    for (var icon : icons.values()) {
                        boolean visible = false;
                        for (int y = 0; y < icon.getHeight() && !visible; y++) {
                            for (int x = 0; x < icon.getWidth(); x++) {
                                if ((icon.getRGB(x, y) >>> 24) != 0) { visible = true; break; }
                            }
                        }
                        if (visible) visibleIcons++;
                    }
                    report.put("nonempty_gpu_icons", visibleIcons);
                    if (visibleIcons != 80) throw new IllegalStateException("Blank icons: " + (80 - visibleIcons));
                    Class<?> layoutClass = Class.forName("com.xiaopiao.ae2ss.api.CraftingPlanScreenshotHelper$PlanLayout");
                    Object layout = method(layoutClass, "forEntryCount", int.class).invoke(null, entries.size());
                    BufferedImage image = (BufferedImage) method(CraftingPlanScreenshotHelper.class,
                            "composePlan", List.class, Map.class, layoutClass, String.class)
                            .invoke(null, entries, icons, layout, "SISE Compat AE2 Screenshot verification");
                    if (image.getWidth() != 427 || image.getHeight() != 1423) throw new IllegalStateException("Wrong long-image dimensions");
                    java.io.File png = (java.io.File) method(CraftingPlanScreenshotHelper.class,
                            "writeImage", java.io.File.class, String.class, BufferedImage.class)
                            .invoke(null, mc.gameDirectory, "AE2-80-item-plan-verification.png", image);
                    boolean targetRestored = CraftingPlanScreenshotHelper.resolveRenderTarget(mc.getMainRenderTarget()) == mc.getMainRenderTarget();
                    boolean windowRestored = state.width == widthBefore && state.height == heightBefore && state.guiScale == scaleBefore;
                    report.put("normal_render_target_restored", targetRestored);
                    report.put("window_state_restored", windowRestored);
                    if (!targetRestored || !windowRestored) throw new IllegalStateException("Capture context was not restored");
                    report.put("png_width", image.getWidth());
                    report.put("png_height", image.getHeight());
                    report.put("png_path", png.getAbsolutePath());
                    report.put("passed", true);
                    finish(report);
                } catch (Throwable problem) { fail(report, problem); }
            }));
        } catch (Throwable problem) { fail(report, problem); }
    }

    private static void fail(Map<String, Object> report, Throwable problem) {
        report.put("passed", false);
        var stack = new java.io.StringWriter();
        problem.printStackTrace(new java.io.PrintWriter(stack));
        report.put("failure", stack.toString());
        finish(report);
    }

    private static void finish(Map<String, Object> report) {
        try {
            var path = Minecraft.getInstance().gameDirectory.toPath().resolve("screenshot-probe-result.json");
            Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(report));
            System.out.println("SISE_COMPAT_SCREENSHOT_PROBE " + report.get("passed"));
        } catch (Exception failure) { failure.printStackTrace(); }
        Minecraft.getInstance().stop();
    }
}
