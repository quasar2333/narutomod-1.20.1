package net.narutomod.entity;

import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

final class CloneFamilyAlliances {
    private CloneFamilyAlliances() {
    }

    static boolean hasSameOwner(Entity entity, @Nullable LivingEntity owner) {
        if (owner == null) {
            return false;
        }
        if (entity == owner || entity.getUUID().equals(owner.getUUID())) {
            return true;
        }
        if (entity instanceof KageBunshinEntity clone) {
            return clone.isOwnedBy(owner);
        }
        if (entity instanceof ExplosiveCloneEntity clone) {
            return clone.isOwnedBy(owner);
        }
        if (entity instanceof JinchurikiCloneEntity clone) {
            return clone.isOwnedBy(owner);
        }
        if (entity instanceof LimboCloneEntity clone) {
            return clone.isOwnedBy(owner);
        }
        if (entity instanceof BiggerMeEntity biggerMe && owner instanceof Player player) {
            return biggerMe.isOwnedBy(player);
        }
        if (entity instanceof MindTransferSelfEntity body) {
            return body.isOwnedBy(owner);
        }
        return false;
    }
}
