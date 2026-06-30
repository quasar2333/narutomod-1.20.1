package net.narutomod.procedure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.narutomod.entity.EarthBlocksEntity;

public final class ProcedurePullAndHold {
    @Nullable
    private Entity grabbedEntity;
    private final List<EarthBlocksEntity> grabbedEarthBlocks = new ArrayList<>();

    public boolean hasGrabbedEntity() {
        return this.grabbedEntity != null && this.grabbedEntity.isAlive();
    }

    public void addEarthBlock(EarthBlocksEntity entity) {
        if (entity.isAlive() && !this.grabbedEarthBlocks.contains(entity)) {
            this.grabbedEarthBlocks.add(entity);
            entity.setMovementEnabled(true);
            this.grabbedEntity = entity;
        }
    }

    public List<EarthBlocksEntity> getGrabbedEarthBlocks() {
        this.grabbedEarthBlocks.removeIf(entity -> !entity.isAlive());
        return this.grabbedEarthBlocks.isEmpty()
                ? Collections.emptyList()
                : Collections.unmodifiableList(this.grabbedEarthBlocks);
    }

    public boolean execute(boolean pressed, Entity puller, @Nullable Entity target) {
        if (!pressed) {
            boolean hadGrabbed = hasGrabbedEntity() || !getGrabbedEarthBlocks().isEmpty();
            reset();
            return hadGrabbed;
        }
        if (!hasGrabbedEntity() && target != null && target.isAlive()) {
            this.grabbedEntity = target;
            if (target instanceof EarthBlocksEntity earthBlocks) {
                addEarthBlock(earthBlocks);
            }
        }
        if (!hasGrabbedEntity()) {
            return false;
        }
        holdEntity(puller, this.grabbedEntity);
        if (!(this.grabbedEntity instanceof EarthBlocksEntity)) {
            for (EarthBlocksEntity earthBlocks : getGrabbedEarthBlocks()) {
                holdEntity(puller, earthBlocks);
            }
        }
        return true;
    }

    public void reset() {
        if (this.grabbedEntity != null && this.grabbedEntity.isAlive()) {
            this.grabbedEntity.setNoGravity(false);
        }
        for (EarthBlocksEntity earthBlocks : getGrabbedEarthBlocks()) {
            earthBlocks.setNoGravity(false);
        }
        this.grabbedEntity = null;
        this.grabbedEarthBlocks.clear();
    }

    private static void holdEntity(Entity puller, Entity target) {
        target.setNoGravity(true);
        if (target instanceof EarthBlocksEntity earthBlocks) {
            earthBlocks.setMovementEnabled(true);
        }
        Vec3 destination = holdDestination(puller, target);
        Vec3 delta = destination.subtract(target.getBoundingBox().getCenter());
        if (delta.lengthSqr() < 0.01D) {
            target.setDeltaMovement(Vec3.ZERO);
        } else {
            target.setDeltaMovement(delta.scale(0.3D));
        }
        target.hasImpulse = true;
        target.hurtMarked = true;
    }

    private static Vec3 holdDestination(Entity puller, Entity target) {
        if (target instanceof ItemEntity || target instanceof ExperienceOrb) {
            return puller.position().add(0.0D, puller.getBbHeight() * 0.5D, 0.0D);
        }
        double distance = target instanceof EarthBlocksEntity ? 5.0D : 3.0D;
        return puller.getEyePosition().add(puller.getLookAngle().scale(distance));
    }
}
