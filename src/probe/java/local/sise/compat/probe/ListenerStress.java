package local.sise.compat.probe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicIntegerArray;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.registries.holdersets.NotHolderSet;

/** Calls the actual NeoForge-patched HolderSet and NotHolderSet, never a list substitute. */
public final class ListenerStress {
    private static final int THREADS = 32, PER_THREAD = 32, ROUNDS = 128;
    private static final Field CALLBACKS;
    private static final Method BIND;
    private static final Constructor<?> CONSTRUCTOR;
    static {
        try {
            CALLBACKS = HolderSet.Named.class.getDeclaredField("invalidationCallbacks");
            BIND = HolderSet.Named.class.getDeclaredMethod("bind", List.class);
            CONSTRUCTOR = HolderSet.Named.class.getDeclaredConstructor(HolderOwner.class, TagKey.class);
            CALLBACKS.setAccessible(true); BIND.setAccessible(true); CONSTRUCTOR.setAccessible(true);
        } catch (ReflectiveOperationException e) { throw new ExceptionInInitializerError(e); }
    }
    @SuppressWarnings("unchecked")
    static HolderSet.Named<String> fresh() throws ReflectiveOperationException {
        return (HolderSet.Named<String>) CONSTRUCTOR.newInstance(new HolderOwner<String>() {},
                TagKey.create(ResourceKey.<String>createRegistryKey(Identifier.parse("registry_smoke:registry")),
                        Identifier.parse("registry_smoke:tag")));
    }
    static void bind(HolderSet.Named<?> named) throws ReflectiveOperationException { BIND.invoke(named, List.of()); }
    public static void main(String[] args) throws Exception { run(false); }
    public static void run(boolean patched) throws Exception {
        int badRounds = 0, registrationErrors = 0, missingCallbacks = 0, listErrors = 0;
        Throwable firstError = null;
        Throwable firstRegistrationError = null;
        String listType = CALLBACKS.get(fresh()).getClass().getName();
        if (patched && !listType.equals("java.util.concurrent.CopyOnWriteArrayList")) throw new AssertionError("Mixin did not apply: " + listType);
        try (var pool = Executors.newFixedThreadPool(THREADS)) {
            for (int round = 0; round < ROUNDS; round++) {
                var named = fresh();
                var delivered = new AtomicIntegerArray(THREADS * PER_THREAD);
                var start = new CyclicBarrier(THREADS);
                List<Future<?>> tasks = new ArrayList<>();
                for (int t = 0; t < THREADS; t++) {
                    final int thread = t;
                    tasks.add(pool.submit(() -> {
                        start.await(30, TimeUnit.SECONDS);
                        for (int i = 0; i < PER_THREAD; i++) {
                            final int index = thread * PER_THREAD + i;
                            // Matches the crash: every NotHolderSet registers against one shared tag.
                            var not = new NotHolderSet<String>(null, named);
                            not.addInvalidationListener(() -> delivered.incrementAndGet(index));
                        }
                        return null;
                    }));
                }
                boolean bad = false;
                for (var task : tasks) try { task.get(30, TimeUnit.SECONDS); }
                catch (ExecutionException e) {
                    registrationErrors++; bad = true; if (firstError == null) firstError = e.getCause();
                    if (firstRegistrationError == null) firstRegistrationError = e.getCause();
                }
                try { bind(named); }
                catch (ReflectiveOperationException e) {
                    listErrors++; bad = true; if (firstError == null) firstError = e.getCause();
                }
                for (int i = 0; i < delivered.length(); i++) if (delivered.get(i) != 1) { missingCallbacks++; bad = true; }
                if (((List<?>) CALLBACKS.get(named)).size() != delivered.length()) bad = true;
                if (bad) badRounds++;
                if (patched && bad) throw new AssertionError("listener corruption in round " + round, firstError);
            }
        }
        System.out.printf("REGISTRY_STRESS %s: list=%s rounds=%d registrations=%d badRounds=%d registrationErrors=%d notificationErrors=%d missingOrDuplicateCallbacks=%d%n",
                patched ? "PATCHED" : "BASELINE", listType, ROUNDS, ROUNDS * THREADS * PER_THREAD,
                badRounds, registrationErrors, listErrors, missingCallbacks);
        if (firstError != null) firstError.printStackTrace(System.out);
        if (firstRegistrationError != null && firstRegistrationError != firstError) firstRegistrationError.printStackTrace(System.out);
        if (!patched && badRounds == 0) throw new AssertionError("Baseline did not reproduce the race");
        if (patched) snapshotNotification();
    }
    private static void snapshotNotification() throws Exception {
        var named = fresh();
        var calls = new AtomicIntegerArray(2);
        named.addInvalidationListener(() -> {
            if (calls.incrementAndGet(0) == 1) named.addInvalidationListener(() -> calls.incrementAndGet(1));
        });
        bind(named);
        if (calls.get(0) != 1 || calls.get(1) != 0) throw new AssertionError("first notification snapshot");
        bind(named);
        if (calls.get(0) != 2 || calls.get(1) != 1) throw new AssertionError("second notification snapshot");
        System.out.println("REGISTRY_STRESS PASS: callback registered during notification is retained for next rebind");
    }
}
