package dev.viking602.sise.compat.tacz;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.model.BedrockAmmoModel;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.IFunctionalSubmitter;
import com.tacz.guns.client.model.functional.ShellRender;
import com.tacz.guns.client.renderer.snapshot.BedrockRenderSnapshot;
import com.tacz.guns.compat.iris.IrisCompat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientResourceLoadFinishedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Reviewed TaCZ Renovated R3-hotfix2: shells belong to the world depth pass. */
public final class TaczWorldShells {
    static final int MAX_SHELLS = 256;
    private static final float EJECTION_SPEED_SCALE = 0.25f;
    private static final double MAX_HORIZONTAL_SPEED = 2.0;
    private static final double MAX_VERTICAL_SPEED = 1.5;
    private static final Vec3 GRAVITY = new Vec3(0, -9.81, 0);
    static final ArrayDeque<ShellBody> SHELLS = new ArrayDeque<>();
    private static final Map<BedrockAmmoModel, ShellShape> SHAPES = new IdentityHashMap<>();
    private static final ContextKey<List<Draw>> DRAWS = new ContextKey<>(
            Identifier.fromNamespaceAndPath("sise_compat", "tacz_shells"));
    private static ClientLevel currentLevel;
    private static boolean registered;

    private TaczWorldShells() { }

    public static void register() {
        if (registered) return;
        registered = true;
        NeoForge.EVENT_BUS.addListener(TaczWorldShells::tick);
        NeoForge.EVENT_BUS.addListener(TaczWorldShells::extract);
        NeoForge.EVENT_BUS.addListener(TaczWorldShells::submit);
        NeoForge.EVENT_BUS.addListener((ClientResourceLoadFinishedEvent event) -> {
            SHELLS.clear(); SHAPES.clear();
        });
    }

    public static boolean takeShells(BedrockGunModel gunModel, ConcurrentLinkedDeque<ShellRender.Data> queue,
            IFunctionalSubmitter.ExtractionContext context) {
        var mc = Minecraft.getInstance();
        var displayContext = context.displayContext();
        boolean hand = displayContext.firstPerson();
        boolean thirdPerson = displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        if (!ShellRender.isSelf || (!hand && !thirdPerson) || IrisCompat.isRenderShadow()
                || mc.options.getCameraType().isFirstPerson() != hand || mc.level == null) return false;
        ensureLevel(mc.level);
        if (queue.isEmpty()) return true;
        var stack = gunModel.getCurrentGunItem();
        var gun = IGun.getIGunOrNull(stack);
        if (gun == null) { queue.clear(); return true; }
        var data = TimelessAPI.getClientGunIndex(gun.getGunId(stack)).map(index -> index.getGunData()).orElse(null);
        var display = TimelessAPI.getGunDisplay(stack).orElse(null);
        if (data == null || display == null || display.getShellEjection() == null) { queue.clear(); return true; }
        var ammo = TimelessAPI.getClientAmmoIndex(data.getAmmoId()).orElse(null);
        if (ammo == null || ammo.getShellModel() == null || ammo.getShellTextureLocation() == null) {
            queue.clear(); return true;
        }
        ShellShape shape = SHAPES.computeIfAbsent(ammo.getShellModel(), ShellShape::new);
        var camera = mc.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState;
        Matrix4f origin = new Matrix4f(context.poseStack().last().pose());
        // Vanilla 26.1 hands already contain inverse model-view. Iris's hand pass uses camera-local poses.
        if (hand && IrisCompat.isHandRendererActive()) origin = new Matrix4f().rotation(camera.orientation).mul(origin);
        Vector3f scale = origin.getScale(new Vector3f());
        Quaternionf rotation = origin.getUnnormalizedRotation(new Quaternionf()).normalize();
        Vector3f center = origin.transformPosition(shape.center, new Vector3f());
        Vec3 position = camera.pos.add(center.x, center.y, center.z);
        var eject = display.getShellEjection();
        Vector3f angular = new Vector3f(eject.getAngularVelocity()).mul(
                -(float) Math.PI / 180, -(float) Math.PI / 180, (float) Math.PI / 180);
        int lifetime = (int) (Math.max(8, Math.min(30, eject.getLivingTime())) * 20);
        long now = System.currentTimeMillis();
        ShellRender.Data pending;
        while ((pending = queue.pollFirst()) != null) {
            if (now - pending.timeStamp > Math.max(1000, eject.getLivingTime() * 1000)) continue;
            Vector3f speed = new Vector3f(eject.getInitialVelocity()).add(pending.randomOffset);
            Vector3f worldSpeed = origin.transformDirection(new Vector3f(-speed.x, -speed.y, speed.z));
            // Gun-pack values describe a short hand animation. They are too fast for long-lived world casings.
            Vec3 velocity = ejectionVelocity(worldSpeed);
            ShellBody body = new ShellBody(shape, ammo.getShellTextureLocation(), position,
                    velocity, GRAVITY, rotation, scale, angular, lifetime);
            // The gun can be pushed into a wall; do not create a casing inside solid terrain.
            if (mc.level.getBlockCollisions(null, body.box()).iterator().hasNext()) continue;
            if (SHELLS.size() >= MAX_SHELLS) SHELLS.removeFirst();
            SHELLS.addLast(body);
        }
        return true;
    }

    private static Vec3 ejectionVelocity(Vector3f worldSpeed) {
        Vec3 velocity = new Vec3(worldSpeed.x, worldSpeed.y, worldSpeed.z).scale(EJECTION_SPEED_SCALE);
        double horizontalSpeed = velocity.horizontalDistance();
        double factor = horizontalSpeed > MAX_HORIZONTAL_SPEED ? MAX_HORIZONTAL_SPEED / horizontalSpeed : 1;
        return new Vec3(velocity.x * factor, Math.clamp(velocity.y, -MAX_VERTICAL_SPEED, MAX_VERTICAL_SPEED),
                velocity.z * factor);
    }

    private static void ensureLevel(ClientLevel level) {
        if (level != currentLevel) { SHELLS.clear(); currentLevel = level; }
    }

    private static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        ensureLevel(mc.level);
        if (mc.level == null || mc.isPaused()) return;
        var iterator = SHELLS.iterator();
        while (iterator.hasNext()) {
            ShellBody body = iterator.next();
            if (body.age >= body.lifetime || !mc.level.hasChunkAt(BlockPos.containing(body.position))
                    || body.position.y < mc.level.getMinY() - 1) { iterator.remove(); continue; }
            body.tick(mc.level);
        }
    }

    private static void extract(ExtractLevelRenderStateEvent event) {
        ensureLevel(event.getLevel());
        var draws = new ArrayList<Draw>();
        float partial = event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getRenderState().cameraRenderState.pos;
        for (ShellBody body : SHELLS) {
            Vec3 position = body.previousPosition.lerp(body.position, partial);
            if (position.distanceToSqr(camera) > 64 * 64) continue;
            PoseStack pose = new PoseStack();
            pose.translate(position.x - camera.x, position.y - camera.y, position.z - camera.z);
            pose.mulPose(new Quaternionf(body.previousRotation).slerp(body.rotation, partial));
            pose.scale(body.scale.x, body.scale.y, body.scale.z);
            int light = net.minecraft.client.renderer.LevelRenderer.getLightCoords(event.getLevel(), BlockPos.containing(position));
            draws.add(new Draw(body.shape.snapshot(pose, light), body.texture));
        }
        event.getRenderState().setRenderData(DRAWS, List.copyOf(draws));
    }

    private static void submit(SubmitCustomGeometryEvent event) {
        if (IrisCompat.isRenderShadow()) return;
        var draws = event.getLevelRenderState().getRenderData(DRAWS);
        if (draws == null) return;
        for (Draw draw : draws) {
            // Extraction freezes every vertex/normal/light; submission never reads a live world or model.
            event.getSubmitNodeCollector().submitCustomGeometry(new PoseStack(), RenderTypes.entityCutout(draw.texture),
                    (pose, buffer) -> draw.snapshot.write(buffer));
        }
    }

    private record Draw(BedrockRenderSnapshot snapshot, Identifier texture) { }
}
