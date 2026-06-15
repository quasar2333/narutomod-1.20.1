package net.narutomod.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

final class CloneOwnerMovement {
    private CloneOwnerMovement() {
    }

    static void followWithSpacing(PathfinderMob clone, LivingEntity owner, double speed, double stopDistance, double teleportDistance) {
        double distanceSqr = clone.distanceToSqr(owner);
        if (distanceSqr > teleportDistance * teleportDistance) {
            clone.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
            clone.getNavigation().stop();
            return;
        }
        if (distanceSqr > stopDistance * stopDistance) {
            clone.getNavigation().moveTo(owner, speed);
            return;
        }
        clone.getNavigation().stop();
        if (distanceSqr <= stopDistance * stopDistance * 0.25D) {
            double offsetX = owner.getX() - clone.getX();
            double offsetZ = owner.getZ() - clone.getZ();
            clone.getNavigation().moveTo(clone.getX() - offsetX, clone.getY(), clone.getZ() - offsetZ, speed);
        }
    }
}
