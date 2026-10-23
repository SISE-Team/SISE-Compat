package dev.viking602.sise.compat.tacz;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tacz.guns.client.model.BedrockAmmoModel;
import com.tacz.guns.client.renderer.snapshot.BedrockRenderSnapshot;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Bounds of drawable gun-pack faces, including rotated cubes and bone offsets. */
final class ShellShape {
    final BedrockAmmoModel model;
    final Vector3f center;
    final Vector3f halfSize;

    ShellShape(BedrockAmmoModel model) {
        this.model = model;
        PoseStack centered = new PoseStack();
        centered.translate(0, -1.5, 0); // Bedrock roots use a 24-pixel Y origin.
        Bounds bounds = new Bounds();
        BedrockRenderSnapshot.capture(model, centered, ItemDisplayContext.NONE, 0,
                OverlayTexture.NO_OVERLAY, 1, 1, 1, 1).write(bounds);
        if (bounds.count == 0) throw new IllegalArgumentException("Empty shell model");
        center = new Vector3f(bounds.min).add(bounds.max).mul(0.5f);
        halfSize = new Vector3f(bounds.max).sub(bounds.min).mul(0.5f);
    }

    Quaternionf restingRotation(float yaw, Vector3f scale) {
        Vector3f size = new Vector3f(halfSize).mul(scale);
        Quaternionf rotation = new Quaternionf().rotationY(yaw);
        // The thinnest mesh dimension becomes vertical, so tall casings lie flat too.
        if (size.x < size.y && size.x < size.z) rotation.rotateZ((float) Math.PI / 2);
        else if (size.z < size.y) rotation.rotateX((float) Math.PI / 2);
        return rotation;
    }

    Vector3f extent(Quaternionf rotation, Vector3f scale) {
        Vector3f x = rotation.transform(new Vector3f(halfSize.x * scale.x, 0, 0));
        Vector3f y = rotation.transform(new Vector3f(0, halfSize.y * scale.y, 0));
        Vector3f z = rotation.transform(new Vector3f(0, 0, halfSize.z * scale.z));
        return new Vector3f(Math.abs(x.x) + Math.abs(y.x) + Math.abs(z.x),
                Math.abs(x.y) + Math.abs(y.y) + Math.abs(z.y),
                Math.abs(x.z) + Math.abs(y.z) + Math.abs(z.z));
    }

    BedrockRenderSnapshot snapshot(PoseStack pose, int light) {
        pose.translate(-center.x, -1.5 - center.y, -center.z);
        return BedrockRenderSnapshot.capture(model, pose, ItemDisplayContext.NONE, light,
                OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
    }

    private static final class Bounds implements VertexConsumer {
        final Vector3f min = new Vector3f(Float.POSITIVE_INFINITY);
        final Vector3f max = new Vector3f(Float.NEGATIVE_INFINITY);
        final Vector3f[] quad = {new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
        int index, count;
        @Override public VertexConsumer addVertex(float x, float y, float z) {
            quad[index++].set(x, y, z);
            if (index == 4) {
                index = 0;
                Vector3f edge = new Vector3f(quad[1]).sub(quad[0]);
                // TaCZ represents omitted per-face UVs as four coincident vertices at the bone pivot.
                // Those quads draw nothing, but measuring them can inflate a 3 cm casing to 40 cm.
                boolean drawable = edge.cross(new Vector3f(quad[2]).sub(quad[0]), new Vector3f()).lengthSquared() > 1e-18f
                        || new Vector3f(quad[2]).sub(quad[0]).cross(new Vector3f(quad[3]).sub(quad[0])).lengthSquared() > 1e-18f;
                if (drawable) {
                    for (Vector3f vertex : quad) { min.min(vertex); max.max(vertex); }
                    count += 4;
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
}
