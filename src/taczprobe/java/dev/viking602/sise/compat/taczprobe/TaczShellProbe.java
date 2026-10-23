package dev.viking602.sise.compat.taczprobe;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.client.model.IFunctionalSubmitter;
import com.tacz.guns.client.model.functional.ShellRender;
import com.tacz.guns.client.renderer.snapshot.BedrockRenderSnapshot;
import dev.viking602.sise.compat.tacz.TaczWorldShells;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientResourceLoadFinishedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Test fixture only: actual client collision shapes, mixed ammo meshes and the hand extraction hook. */
@Mod(value = "sise_compat_tacz_probe", dist = Dist.CLIENT)
public final class TaczShellProbe {
    private boolean worldRequested, prepared, tested, captureRequested, standingCaptured, lowCamera, lowCaptureRequested, finished;
    private int resources, ticks, assertionCount, nativeEmissions, worldSubmissions;
    private BlockPos base;
    private Path fixture;
    private final LinkedHashMap<String, Object> report = new LinkedHashMap<>();
    private final List<Body> displayed = new ArrayList<>();
    private final List<Vec3> anchors = new ArrayList<>();
    private final IdentityHashMap<Object, Vec3> nativeStarts = new IdentityHashMap<>();
    private double nativeMaxDistance;

    private static Object read(Object object, String name) {
        try {
            var field = (object instanceof Class<?> type ? type : object.getClass()).getDeclaredField(name);
            field.setAccessible(true); return field.get(object instanceof Class<?> ? null : object);
        } catch (Exception error) { throw new IllegalStateException(error); }
    }

    @SuppressWarnings("unchecked")
    private static Deque<Object> shells() { return (Deque<Object>) read(TaczWorldShells.class, "SHELLS"); }

    private static final class Shape {
        final Object value;
        final Vector3f center, halfSize;
        Shape(com.tacz.guns.client.model.BedrockAmmoModel model) {
            try {
                var constructor = Class.forName("dev.viking602.sise.compat.tacz.ShellShape").getDeclaredConstructors()[0];
                constructor.setAccessible(true); value = constructor.newInstance(model);
            } catch (Exception error) { throw new IllegalStateException(error); }
            center = (Vector3f) read(value, "center"); halfSize = (Vector3f) read(value, "halfSize");
        }
        Shape(Object value) {
            this.value = value; center = (Vector3f) read(value, "center"); halfSize = (Vector3f) read(value, "halfSize");
        }
    }

    private static final class Body {
        final Object value;
        final Shape shape;
        final Identifier texture;
        Body(Object value) { this.value = value; shape = new Shape(read(value, "shape")); texture = (Identifier) read(value, "texture"); }
        Vec3 position() { return (Vec3) read(value, "position"); }
        Vec3 velocity() { return (Vec3) read(value, "velocity"); }
        Quaternionf rotation() { return (Quaternionf) read(value, "rotation"); }
        boolean grounded() { return (boolean) read(value, "grounded"); }
        Vector3f restingExtent() { return (Vector3f) read(value, "restingExtent"); }
        AABBCall boxCall() { return new AABBCall(value); }
        net.minecraft.world.phys.AABB box() { return boxCall().box(); }
        void tick(ClientLevel level) {
            try { var method = value.getClass().getDeclaredMethod("tick", ClientLevel.class); method.setAccessible(true); method.invoke(value, level); }
            catch (Exception error) { throw new IllegalStateException(error); }
        }
        void resetAge() {
            try { var field = value.getClass().getDeclaredField("age"); field.setAccessible(true); field.setInt(value, 0); }
            catch (Exception error) { throw new IllegalStateException(error); }
        }
    }

    private record AABBCall(Object value) {
        net.minecraft.world.phys.AABB box() {
            try { var method = value.getClass().getDeclaredMethod("box"); method.setAccessible(true); return (net.minecraft.world.phys.AABB) method.invoke(value); }
            catch (Exception error) { throw new IllegalStateException(error); }
        }
    }

    /** Measure drawable quads independently of the physics collider, excluding zero-area placeholder faces. */
    private static final class VisibleBounds implements VertexConsumer {
        final Vector3f min = new Vector3f(Float.POSITIVE_INFINITY);
        final Vector3f max = new Vector3f(Float.NEGATIVE_INFINITY);
        final Vector3f[] quad = {new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
        int index, faces;
        @Override public VertexConsumer addVertex(float x, float y, float z) {
            quad[index++].set(x, y, z);
            if (index == 4) {
                index = 0;
                Vector3f edge = new Vector3f(quad[1]).sub(quad[0]);
                if (edge.cross(new Vector3f(quad[2]).sub(quad[0]), new Vector3f()).lengthSquared() > 1e-18f
                        || new Vector3f(quad[2]).sub(quad[0]).cross(new Vector3f(quad[3]).sub(quad[0])).lengthSquared() > 1e-18f) {
                    for (var vertex : quad) { min.min(vertex); max.max(vertex); }
                    faces++;
                }
            }
            return this;
        }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer setColor(int color) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
        @Override public VertexConsumer setLineWidth(float width) { return this; }
    }

    private static VisibleBounds renderedBounds(Body body) throws Exception {
        var pose = new PoseStack();
        var position = body.position();
        pose.translate(position.x, position.y, position.z);
        pose.mulPose(body.rotation());
        var scale = (Vector3f) read(body.value, "scale");
        pose.scale(scale.x, scale.y, scale.z);
        var method = body.shape.value.getClass().getDeclaredMethod("snapshot", PoseStack.class, int.class);
        method.setAccessible(true);
        var bounds = new VisibleBounds();
        ((BedrockRenderSnapshot) method.invoke(body.shape.value, pose, 15728880)).write(bounds);
        return bounds;
    }

    public TaczShellProbe() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Pre event) -> {
            if (worldRequested) net.minecraft.client.KeyMapping.releaseAll();
        });
        NeoForge.EVENT_BUS.addListener((ClientResourceLoadFinishedEvent event) -> {
            if (++resources == 2) Minecraft.getInstance().execute(this::createWorld);
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> runTick());
        NeoForge.EVENT_BUS.addListener((SubmitCustomGeometryEvent event) -> {
            if (tested && !shells().isEmpty()) worldSubmissions++;
        });
        NeoForge.EVENT_BUS.addListener((RenderFrameEvent.Post event) -> {
            if (tested) for (var shell : shells()) {
                if (displayed.stream().noneMatch(body -> body.value == shell))
                    nativeStarts.putIfAbsent(shell, new Body(shell).position());
            }
            if (tested && ticks >= 110 && !captureRequested) {
                captureRequested = true;
                Screenshot.grab(fixture.toFile(), "tacz-shells-world.png", Minecraft.getInstance().getMainRenderTarget(), 1,
                        message -> Minecraft.getInstance().execute(() -> standingCaptured = true));
            }
            if (lowCamera && ticks >= 150 && !lowCaptureRequested) {
                lowCaptureRequested = true;
                Screenshot.grab(fixture.toFile(), "tacz-shells-ground-contact.png", Minecraft.getInstance().getMainRenderTarget(), 1,
                        message -> Minecraft.getInstance().execute(this::finish));
            }
        });
    }

    private void createWorld() {
        if (worldRequested) return;
        var mc = Minecraft.getInstance();
        fixture = mc.gameDirectory.toPath().toAbsolutePath().normalize();
        if (!fixture.equals(Path.of(System.getProperty("sise.compat.tacz.fixture", "")).toAbsolutePath().normalize())
                || !Files.isRegularFile(fixture.resolve("SISE_COMPAT_AUTOTEST_ONLY")))
            throw new IllegalStateException("TACZ probe requires a marked disposable fixture");
        worldRequested = true;
        var settings = new LevelSettings("TACZ shell collision verification", GameType.CREATIVE,
                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel("sise-tacz-shell-" + System.currentTimeMillis(), settings,
                new WorldOptions(602, false, false), WorldPresets::createFlatWorldDimensions, mc.screen);
    }

    private void runTick() {
        if (finished) return;
        var mc = Minecraft.getInstance();
        try {
            if (worldRequested) net.minecraft.client.KeyMapping.releaseAll();
            if (!worldRequested && mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen && mc.getOverlay() == null)
                createWorld();
            if (mc.level == null || mc.player == null) return;
            ticks++;
            if (!prepared && ticks >= 35) {
                prepared = true;
                base = new BlockPos(mc.player.getBlockX(), mc.player.getBlockY() - 1, mc.player.getBlockZ());
                var gun = GunItemBuilder.create().setId(Identifier.parse("tacz:minigun")).setAmmoCount(100).build();
                mc.getSingleplayerServer().execute(() -> {
                    var server = mc.getSingleplayerServer();
                    var level = server.overworld();
                    for (int x = -12; x <= 12; x++) for (int z = -8; z <= 14; z++)
                        level.setBlock(base.offset(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                    for (int i = 0; i < 4; i++) level.setBlock(base.offset(-8 + i * 4, 2, 6), surface(i), 3);
                    for (int y = 1; y <= 5; y++) for (int z = -1; z <= 1; z++)
                        level.setBlock(base.offset(9, y, 10 + z), Blocks.STONE.defaultBlockState(), 3);
                    var player = server.getPlayerList().getPlayers().getFirst();
                    player.setItemInHand(InteractionHand.MAIN_HAND, gun);
                    player.teleportTo(level, base.getX() + 0.5, base.getY() + 1, base.getZ() - 1.5, Set.of(), 0, 15, true);
                });
            }
            if (prepared && !tested && ticks >= 65) runChecks();
            if (tested && ticks >= 66 && ticks <= 82 && ticks % 2 == 0) {
                var display = TimelessAPI.getGunDisplay(mc.player.getMainHandItem()).orElseThrow();
                display.getGunModel().getShellRender(0).addShell(display.getShellEjection().getRandomVelocity());
                nativeEmissions++;
            }
            if (tested && ticks == 95) {
                int nativeShells = shells().size() - displayed.size();
                require(nativeShells == nativeEmissions, "Native hand renderer did not transfer every casing to the world: "
                        + nativeShells + " / " + nativeEmissions);
                report.put("native_world_shells", nativeShells);
                // World casings must keep rendering when the gun is no longer in the hand.
                var paper = new net.minecraft.world.item.ItemStack(Items.PAPER);
                mc.player.setItemInHand(InteractionHand.MAIN_HAND, paper);
                mc.getSingleplayerServer().execute(() -> mc.getSingleplayerServer().getPlayerList()
                        .getPlayers().getFirst().setItemInHand(InteractionHand.MAIN_HAND, paper.copy()));
                mc.player.setXRot(55); mc.player.xRotO = 55;
                mc.player.setYRot(15); mc.player.yRotO = 15;
            }
            for (var entry : nativeStarts.entrySet()) {
                var difference = new Body(entry.getKey()).position().subtract(entry.getValue());
                nativeMaxDistance = Math.max(nativeMaxDistance, difference.horizontalDistance());
            }
            if (tested && ticks >= 125 && standingCaptured && !lowCamera) {
                Vec3 middle = Vec3.ZERO;
                for (var shell : nativeStarts.keySet()) middle = middle.add(new Body(shell).position());
                middle = middle.scale(1.0 / nativeStarts.size());
                var camera = new net.minecraft.world.entity.decoration.ArmorStand(mc.level, middle.x + 1.6,
                        base.getY() + 1.16, middle.z + 0.8);
                camera.setPos(middle.x + 1.6, base.getY() + 1.16 - camera.getEyeHeight(), middle.z + 0.8);
                camera.setYRot(116.565f); camera.setYHeadRot(116.565f); camera.setXRot(4); camera.setOldPosAndRot();
                mc.player.setInvisible(true);
                mc.setCameraEntity(camera);
                mc.options.hideGui = true; mc.options.fov().set(35);
                lowCamera = true;
            }
            if (ticks > 230 && !finished) throw new IllegalStateException("Screenshot callback timed out");
        } catch (Throwable failure) { fail(failure); }
    }

    private static BlockState surface(int index) {
        return switch (index) {
            case 0 -> Blocks.GRASS_BLOCK.defaultBlockState();
            case 1 -> Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
            case 2 -> Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
            default -> Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST)
                    .setValue(StairBlock.HALF, Half.BOTTOM).setValue(StairBlock.SHAPE, StairsShape.STRAIGHT);
        };
    }

    private void require(boolean value, String message) {
        assertionCount++;
        if (!value) throw new AssertionError(message);
    }

    private Body body(Shape shape, Identifier texture, Vec3 pos, Vec3 speed) {
        try {
            var constructor = Class.forName("dev.viking602.sise.compat.tacz.ShellBody").getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            return new Body(constructor.newInstance(shape.value, texture, pos, speed, new Vec3(0, -20, 0),
                    new Quaternionf().rotationXYZ(0.4f, 0.2f, 0.5f), new Vector3f(1), new Vector3f(7, 5, 3), 160));
        } catch (Exception error) { throw new IllegalStateException(error); }
    }

    private void runChecks() throws Exception {
        var mc = Minecraft.getInstance();
        TaczWorldShells.register();
        var shapes = new ArrayList<Object>();
        int models = 0, cases = 0;
        for (var entry : TimelessAPI.getAllClientAmmoIndex()) {
            var index = entry.getValue();
            if (index.getShellModel() == null || index.getShellTextureLocation() == null) continue;
            Shape shape = new Shape(index.getShellModel());
            require(shape.halfSize.isFinite() && shape.halfSize.length() > 0, "Invalid mesh " + entry.getKey());
            shapes.add(java.util.Map.of("ammo", entry.getKey().toString(), "half_size",
                    List.of(shape.halfSize.x, shape.halfSize.y, shape.halfSize.z)));
            for (int surface = 0; surface < 4; surface++) {
                BlockPos block = base.offset(-8 + surface * 4, 2, 6);
                require(mc.level.getBlockState(block).equals(surface(surface)), "Server surface not synchronized");
                for (int side = 0; side < (surface == 3 ? 2 : 1); side++) {
                    double x = surface == 3 ? (side == 0 ? 0.2 : 0.8) : 0.5;
                    double top = 0;
                    for (var part : mc.level.getBlockState(block).getCollisionShape(mc.level, block).toAabbs())
                        if (part.minX <= x && part.maxX >= x && part.minZ <= 0.5 && part.maxZ >= 0.5)
                            top = Math.max(top, part.maxY);
                    double floor = block.getY() + top;
                    Body shell = body(shape, index.getShellTextureLocation(),
                            new Vec3(block.getX() + x, floor + 2, block.getZ() + 0.5), Vec3.ZERO);
                    for (int tick = 0; tick < 110; tick++) {
                        shell.tick(mc.level);
                        require(!mc.level.getBlockCollisions(null, shell.box().deflate(1e-6)).iterator().hasNext(),
                                "Terrain penetration: " + entry.getKey() + " / " + surface + " / " + side
                                + " tick=" + tick + " pos=" + shell.position() + " box=" + shell.box()
                                + " grounded=" + shell.grounded() + " size=" + shape.halfSize);
                    }
                    require(shell.grounded(), "Did not settle: " + entry.getKey());
                    require(Math.abs(shell.box().minY - floor) < 0.002, "Wrong floor height: " + entry.getKey());
                    var visible = renderedBounds(shell);
                    require(visible.faces > 0 && Math.abs(visible.min.y - floor) < 0.002,
                            "Visible mesh floating: " + entry.getKey() + " / " + surface + " / " + side
                            + " gap=" + (visible.min.y - floor) + " collider=" + shell.box().minY);
                    Vec3 resting = shell.position();
                    var rotation = new Quaternionf(shell.rotation());
                    for (int tick = 0; tick < 20; tick++) shell.tick(mc.level);
                    require(resting.distanceToSqr(shell.position()) < 1e-8 && Math.abs(rotation.dot(shell.rotation())) > 0.99999,
                            "Resting shell kept moving or rotating");
                    cases++;
                }
            }
            Body shown = body(shape, index.getShellTextureLocation(),
                    new Vec3(base.getX() - 1.1 + (models % 5) * 0.45, base.getY() + 3,
                            base.getZ() + 0.1 + (models / 5) * 0.35), Vec3.ZERO);
            for (int tick = 0; tick < 100; tick++) shown.tick(mc.level);
            shown.resetAge();
            displayed.add(shown);
            anchors.add(shown.position());
            models++;
        }
        require(models >= 15, "Ammo pack missing shell models");
        var first = displayed.getFirst();
        var fast = body(first.shape, first.texture,
                new Vec3(base.getX() + 7.5, base.getY() + 3, base.getZ() + 10.5), new Vec3(100, 0, 0));
        fast.tick(mc.level);
        require(fast.box().maxX <= base.getX() + 9.0001 && fast.velocity().x < 0, "Fast casing tunneled through wall");
        var drop = body(first.shape, first.texture,
                new Vec3(base.getX() - 6.5, base.getY() + 3, base.getZ() - 4.5), Vec3.ZERO);
        for (int tick = 0; tick < 100; tick++) drop.tick(mc.level);
        require(drop.grounded(), "Block removal setup did not settle");
        Vec3 before = drop.position();
        BlockPos support = BlockPos.containing(before.add(0, -drop.restingExtent().y - 0.1, 0));
        var saved = mc.level.getBlockState(support);
        mc.level.setBlock(support, Blocks.AIR.defaultBlockState(), 3);
        for (int tick = 0; tick < 12; tick++) drop.tick(mc.level);
        require(drop.position().y < before.y - 0.25, "Casing floated after removing supporting block");
        mc.level.setBlock(support, saved, 3);

        var display = TimelessAPI.getGunDisplay(mc.player.getMainHandItem()).orElseThrow();
        var model = display.getGunModel();
        require(!model.getCurrentGunItem().isEmpty(), "Native gun render did not initialize");
        var shellRender = model.getShellRender(0);
        require(shellRender != null, "Missing native shell node");
        shells().clear();
        PoseStack origin = new PoseStack(); origin.translate(1, 1, 1);
        int[] handTasks = {0};
        var context = new IFunctionalSubmitter.ExtractionContext(origin, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                15728880, 0, task -> handTasks[0]++);
        try {
            ShellRender.isSelf = true;
            for (int i = 0; i < 300; i++) {
                shellRender.addShell(new Vector3f()); shellRender.extract(context);
            }
        } finally { ShellRender.isSelf = false; }
        require(handTasks[0] == 0, "Shell still submitted to hand depth pass");
        require(shells().size() == 256, "World shell pool exceeded cap or hook did not run");
        Vec3 camera = mc.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState.pos;
        var emitted = new Body(shells().getLast());
        Vector3f center = emitted.shape.center;
        require(emitted.position().distanceToSqr(camera.add(1 + center.x, 1 + center.y, 1 + center.z)) < 1e-8,
                "Emission origin did not become a world position");
        require((int) read(emitted.value, "lifetime") == 160, "Default shell lifetime must allow visible resting casings");
        report.put("models", models); report.put("collision_cases", cases);
        report.put("mesh_bounds", shapes); report.put("fast_wall_collision", true);
        report.put("support_removal", true); report.put("native_mixin_hook", true);
        report.put("no_hand_render_tasks", true); report.put("bounded_pool", 256);
        checkGunTrajectories();
        shells().clear();
        displayed.forEach(body -> shells().addLast(body.value));
        tested = true;
        System.out.println("TACZ_SHELL_PHYSICS_PASSED: " + models + " models, " + cases + " collision cases");
    }

    private void checkGunTrajectories() throws Exception {
        var mc = Minecraft.getInstance();
        int trajectories = 0;
        double maxDistance = 0;
        var camera = mc.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState.pos;
        for (var gunEntry : TimelessAPI.getAllClientGunIndex()) {
            var stack = GunItemBuilder.create().setId(gunEntry.getKey()).setAmmoCount(100).build();
            var display = TimelessAPI.getGunDisplay(stack).orElse(null);
            if (display == null || display.getShellEjection() == null || display.getGunModel().getShellRender(0) == null) continue;
            var ammo = TimelessAPI.getClientAmmoIndex(gunEntry.getValue().getGunData().getAmmoId()).orElse(null);
            if (ammo == null || ammo.getShellModel() == null) continue;
            var gunModel = display.getGunModel();
            var currentGun = gunModel.getClass().getDeclaredField("currentGunItem"); currentGun.setAccessible(true);
            var saved = currentGun.get(gunModel); currentGun.set(gunModel, stack);
            try {
                for (float pitch : new float[]{-80, 0, 80}) for (int random = 0; random < 2; random++) {
                    shells().clear();
                    var origin = new PoseStack();
                    origin.translate(base.getX() + 0.5 - camera.x, base.getY() + 2.4 - camera.y,
                            base.getZ() - 3.5 - camera.z);
                    origin.mulPose(new Quaternionf().rotationYXZ(0.7f, pitch * (float) Math.PI / 180, 0));
                    var queue = new java.util.concurrent.ConcurrentLinkedDeque<ShellRender.Data>();
                    queue.add(new ShellRender.Data(System.currentTimeMillis(), random == 0 ? new Vector3f()
                            : new Vector3f(display.getShellEjection().getRandomVelocity())));
                    ShellRender.isSelf = true;
                    var context = new IFunctionalSubmitter.ExtractionContext(origin, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                            15728880, 0, task -> { throw new AssertionError("Trajectory entered hand renderer"); });
                    require(TaczWorldShells.takeShells(gunModel, queue, context) && shells().size() == 1,
                            "Trajectory did not spawn: " + gunEntry.getKey());
                    var body = new Body(shells().getFirst());
                    Vec3 start = body.position();
                    for (int tick = 0; tick < 110; tick++) {
                        body.tick(mc.level);
                        double distance = body.position().subtract(start).horizontalDistance();
                        maxDistance = Math.max(maxDistance, distance);
                        require(distance < 1.75, "Casing flew too far: " + gunEntry.getKey() + " / " + pitch + " / " + distance);
                        require(body.position().y < start.y + 0.13, "Ejection height too high: " + gunEntry.getKey());
                    }
                    require(body.grounded(), "Gun trajectory did not settle: " + gunEntry.getKey());
                    require(Math.abs(renderedBounds(body).min.y - (base.getY() + 1)) < 0.002,
                            "Gun trajectory's visible casing floated: " + gunEntry.getKey());
                    trajectories++;
                }
            } finally { currentGun.set(gunModel, saved); ShellRender.isSelf = false; }
        }
        require(trajectories >= 100, "Insufficient gun trajectory coverage");
        report.put("gun_trajectories", trajectories); report.put("max_horizontal_travel_blocks", maxDistance);
        report.put("visible_mesh_ground_contact", true);
    }

    private void finish() {
        if (finished) return;
        try {
            require(Files.isRegularFile(fixture.resolve("screenshots/tacz-shells-world.png")), "GPU screenshot missing");
            require(Files.isRegularFile(fixture.resolve("screenshots/tacz-shells-ground-contact.png")), "Low camera screenshot missing");
            require(nativeEmissions >= 8 && worldSubmissions > 5, "Native emission/world rendering did not run");
            require(displayed.stream().allMatch(Body::grounded), "Displayed shell left the ground");
            for (int i = 0; i < displayed.size(); i++) require(displayed.get(i).position().distanceToSqr(anchors.get(i)) < 1e-8,
                    "Camera movement changed a resting shell's world position");
            require(Minecraft.getInstance().player.getMainHandItem().is(Items.PAPER), "Weapon switch was not tested");
            require(nativeStarts.size() == nativeEmissions && nativeMaxDistance < 1.75,
                    "Native minigun ejection distance exceeded the local drop area: " + nativeMaxDistance);
            for (var value : nativeStarts.keySet()) {
                var shell = new Body(value);
                require(shell.grounded(), "Native minigun casing did not settle");
                require(Math.abs(renderedBounds(shell).min.y - (base.getY() + 1)) < 0.002,
                        "Native minigun visible casing floated");
                var camera = Minecraft.getInstance().gameRenderer.getGameRenderState().levelRenderState.cameraRenderState;
                var relative = shell.position().subtract(camera.pos);
                var clip = new org.joml.Vector4f((float) relative.x, (float) relative.y, (float) relative.z, 1)
                        .mul(camera.viewRotationMatrix).mul(camera.projectionMatrix);
                require(clip.w > 0 && Math.abs(clip.x / clip.w) < 0.95 && Math.abs(clip.y / clip.w) < 0.95,
                        "Low camera did not frame the native casing");
            }
            report.put("native_minigun_max_travel_blocks", nativeMaxDistance);
            report.put("low_camera_height_above_ground", Minecraft.getInstance().gameRenderer.getMainCamera().position().y - (base.getY() + 1));
            report.put("native_minigun_visible_ground_contact", true); report.put("low_camera_screenshot", true);
            report.put("native_emissions", nativeEmissions); report.put("world_submit_frames", worldSubmissions);
            report.put("weapon_switch_and_camera_turn", true); report.put("assertions", assertionCount);
            report.put("passed", true);
            writeReport(); finished = true; Minecraft.getInstance().stop();
        } catch (Throwable failure) { fail(failure); }
    }

    private void writeReport() throws Exception {
        Files.writeString(fixture.resolve("tacz-shell-result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
    }

    private void fail(Throwable failure) {
        failure.printStackTrace(); report.put("passed", false); report.put("error", failure.toString());
        report.put("assertions", assertionCount);
        try { writeReport(); } catch (Exception ignored) { }
        finished = true; Minecraft.getInstance().stop();
    }
}
