package dev.viking602.sise.compat.tacz;

import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Client-only simulation: no entities, packets, dropped items or saved world data. */
final class ShellBody {
    private static final double STEP = 1.0 / 80.0;
    private static final double EPSILON = 0.0005;
    final ShellShape shape;
    final Identifier texture;
    final Vector3f scale;
    final Vec3 acceleration;
    final Vector3f angularVelocity;
    final Quaternionf restingRotation;
    final Vector3f restingExtent;
    final float flightRadius;
    final int lifetime;
    Vec3 position, previousPosition, velocity;
    Quaternionf rotation, previousRotation;
    boolean grounded, flattened;
    int age, bounces;

    ShellBody(ShellShape shape, Identifier texture, Vec3 position, Vec3 velocity, Vec3 acceleration,
            Quaternionf rotation, Vector3f scale, Vector3f angularVelocity, int lifetime) {
        this.shape = shape; this.texture = texture; this.position = position;
        this.previousPosition = position; this.velocity = velocity; this.acceleration = acceleration;
        this.rotation = new Quaternionf(rotation); previousRotation = new Quaternionf(rotation);
        this.scale = new Vector3f(scale); this.angularVelocity = new Vector3f(angularVelocity);
        this.lifetime = lifetime;
        flightRadius = Math.max(0.005f, new Vector3f(shape.halfSize).mul(scale).length());
        restingRotation = shape.restingRotation((float) Math.atan2(velocity.x, velocity.z), scale);
        restingExtent = shape.extent(restingRotation, scale);
    }

    AABB box() {
        Vector3f extent = flattened ? restingExtent : new Vector3f(flightRadius);
        return new AABB(position.x - extent.x, position.y - extent.y, position.z - extent.z,
                position.x + extent.x, position.y + extent.y, position.z + extent.z);
    }

    void tick(ClientLevel level) {
        previousPosition = position;
        previousRotation.set(rotation);
        age++;
        for (int step = 0; step < 4; step++) advance(level);
    }

    private void advance(ClientLevel level) {
        Vec3 wanted = velocity.scale(STEP).add(acceleration.scale(STEP * STEP * 0.5));
        velocity = velocity.add(acceleration.scale(STEP));
        Vec3 moved = Entity.collideBoundingBox(null, wanted, box(), level, List.of());
        position = position.add(moved);
        if (Math.abs(moved.x - wanted.x) > 1e-7) velocity = new Vec3(-velocity.x * 0.28, velocity.y, velocity.z);
        if (Math.abs(moved.z - wanted.z) > 1e-7) velocity = new Vec3(velocity.x, velocity.y, -velocity.z * 0.28);
        if (Math.abs(moved.y - wanted.y) > 1e-7) {
            if (wanted.y < 0) {
                if (grounded || Math.abs(velocity.y) < 0.8 || bounces >= 2) settle(level);
                else {
                    bounces++;
                    velocity = new Vec3(velocity.x * 0.65, -velocity.y * 0.28, velocity.z * 0.65);
                    angularVelocity.mul(0.35f);
                }
            } else velocity = new Vec3(velocity.x, -velocity.y * 0.28, velocity.z);
        } else if (grounded) {
            grounded = false; // Removing the supporting block lets the casing fall again.
            // Keep the flattened collider. Expanding it can intersect the side of a nearby stair.
        }
        if (grounded) {
            velocity = new Vec3(velocity.x * 0.70, 0, velocity.z * 0.70);
            if (velocity.horizontalDistanceSqr() < 0.0004) velocity = Vec3.ZERO;
        } else {
            velocity = velocity.scale(Math.exp(-0.35 * STEP));
            rotation.rotateXYZ(angularVelocity.x * (float) STEP, angularVelocity.y * (float) STEP,
                    angularVelocity.z * (float) STEP).normalize();
        }
    }

    private void settle(ClientLevel level) {
        if (!flattened) {
            Vec3 oldPosition = position;
            double floor = position.y - flightRadius;
            position = new Vec3(position.x, floor + restingExtent.y + EPSILON, position.z);
            grounded = true;
            flattened = true;
            if (level.getBlockCollisions(null, box()).iterator().hasNext()) {
                // A tight corner may not have room for the mesh's preferred flat orientation.
                position = oldPosition;
                grounded = false;
                flattened = false;
                velocity = new Vec3(velocity.x * 0.3, 0.25, velocity.z * 0.3);
                return;
            }
            rotation.set(restingRotation);
            angularVelocity.zero();
        }
        grounded = true;
        velocity = new Vec3(velocity.x, 0, velocity.z);
    }
}
