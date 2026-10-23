package dev.viking602.sise.compat.soundprobe;

import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundEngineExecutor;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Disposable full-client regression. No player world or live sound engine is modified. */
@Mod(value = "sise_sound_probe", dist = Dist.CLIENT)
public final class SoundRaceProbe {
    private boolean ran;
    private int assertions;
    private final Map<String, Object> report = new LinkedHashMap<>();

    public SoundRaceProbe() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            Minecraft mc = Minecraft.getInstance();
            if (ran || !(mc.screen instanceof TitleScreen) || mc.getOverlay() != null) return;
            ran = true;
            Path fixture = mc.gameDirectory.toPath().toAbsolutePath().normalize();
            try {
                check(fixture.equals(Path.of(System.getProperty("sise.sound.fixture")).toAbsolutePath().normalize())
                        && Files.isRegularFile(fixture.resolve("SISE_SOUND_TEST_ONLY")), "fixture boundary");
                run(mc);
                report.put("passed", true);
            } catch (Throwable failure) {
                failure.printStackTrace();
                report.put("passed", false);
                report.put("error", failure.toString());
            }
            report.put("assertions", assertions);
            try {
                Files.writeString(fixture.resolve("sound-result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
            } catch (Exception failure) { throw new IllegalStateException(failure); }
            mc.stop();
        });
    }

    private static Object read(Object owner, String name) throws Exception {
        var field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    private static void write(Object owner, String name, Object value) throws Exception {
        var field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(owner, value);
    }

    private void check(boolean passed, String message) {
        assertions++;
        if (!passed) throw new AssertionError(message);
    }

    @SuppressWarnings("unchecked")
    private void run(Minecraft mc) throws Exception {
        // A real transformed manager/engine, with its own audio executor but no OpenAL device.
        SoundManager manager = new SoundManager(mc.options);
        SoundEngine engine = (SoundEngine) read(manager, "soundEngine");
        SoundEngineExecutor executor = (SoundEngineExecutor) read(engine, "executor");
        Map<SoundInstance, Integer> deadlines = (Map<SoundInstance, Integer>) read(engine, "soundDeleteTime");
        Map<SoundInstance, Object> channels = (Map<SoundInstance, Object>) read(engine, "instanceToChannel");
        Set<SoundInstance> pending = (Set<SoundInstance>) read(engine, "rsls$pendingSounds");
        SoundInstance sound = SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F);
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicLong writes = new AtomicLong();
        CountDownLatch finished = new CountDownLatch(1);
        boolean workerStarted = false;
        try {
            report.put("map_type", deadlines.getClass().getName());
            report.put("query_thread", Thread.currentThread().getName());
            check(deadlines.getClass().getName().equals("java.util.Collections$SynchronizedMap"), "RSLS map wrapper");
            check(!manager.isActive(sound), "unloaded sound is inactive");
            write(engine, "loaded", true);
            write(engine, "tickCount", 100);
            check(!manager.isActive(sound), "absent sound is inactive");
            deadlines.put(sound, 99);
            check(manager.isActive(sound), "elapsed deadline retains upstream result");
            deadlines.put(sound, 100);
            check(manager.isActive(sound), "equal deadline retains upstream result");
            deadlines.put(sound, 101);
            check(!manager.isActive(sound), "future deadline without channel");
            channels.put(sound, null);
            check(manager.isActive(sound), "active channel is preserved");
            channels.clear(); deadlines.clear();
            pending.add(sound);
            check(manager.isActive(sound), "RSLS pending sound return modifier is preserved");
            pending.clear();
            check(!manager.isActive(sound), "removed pending sound is inactive");
            CountDownLatch started = new CountDownLatch(1);
            executor.execute(() -> {
                report.put("writer_thread", Thread.currentThread().getName());
                started.countDown();
                try {
                    while (running.get()) {
                        deadlines.put(sound, 99);
                        deadlines.remove(sound);
                        writes.incrementAndGet();
                    }
                } finally { finished.countDown(); }
            });
            workerStarted = true;
            check(started.await(5, TimeUnit.SECONDS), "audio writer started");
            int completed = 0;
            NullPointerException race = null;
            long start = System.nanoTime();
            try {
                for (; completed < 2_000_000; completed++) manager.isActive(sound);
            } catch (NullPointerException failure) {
                race = failure;
                report.put("reproduced_exception", failure.toString());
                report.put("reproduced_stack", java.util.Arrays.toString(failure.getStackTrace()));
            } finally {
                running.set(false);
                check(finished.await(5, TimeUnit.SECONDS), "audio writer exits without deadlock");
            }
            report.put("queries", completed);
            report.put("concurrent_removals", writes.get());
            report.put("duration_ms", (System.nanoTime() - start) / 1_000_000);
            boolean baseline = Boolean.getBoolean("sise.sound.expectRace");
            report.put("baseline", baseline);
            check(writes.get() > 0, "concurrent audio removal occurred");
            if (baseline) {
                check(race != null && race.toString().contains("asyncparticles"), "reproduce exact reported null unboxing");
            } else {
                check(race == null && completed == 2_000_000, "all concurrent state queries complete");
                check(writes.get() > 1000, "stress exercised sustained contention");
                check(!manager.isActive(sound), "finished sound is inactive after stress");
            }
        } finally {
            running.set(false);
            if (workerStarted) finished.await(5, TimeUnit.SECONDS);
            write(engine, "loaded", false);
            executor.shutDown();
        }
    }
}
