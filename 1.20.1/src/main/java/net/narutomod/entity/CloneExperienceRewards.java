package net.narutomod.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

final class CloneExperienceRewards {
    private CloneExperienceRewards() {
    }

    static int collectFromKill(LivingEntity clone, Entity killed) {
        if (!(killed instanceof Mob mob) || clone.level().isClientSide) {
            return 0;
        }
        return Math.max(mob.getExperienceReward(), 0);
    }

    static void transferToOwner(LivingEntity owner, int amount) {
        if (amount > 0 && owner instanceof Player player && !player.level().isClientSide) {
            player.giveExperiencePoints(amount);
        }
    }
}
