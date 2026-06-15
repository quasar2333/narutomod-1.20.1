package net.narutomod.entity;

import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.narutomod.NarutomodMod;

@Mod.EventBusSubscriber(modid = NarutomodMod.MODID)
final class CloneTargetingEvents {
    private static final int RETARGET_INTERVAL = 20;

    private CloneTargetingEvents() {
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !(entity instanceof Mob mob) || !(mob instanceof Enemy)) {
            return;
        }
        if (mob.tickCount % RETARGET_INTERVAL != 0 || isLegacyTargetableClone(mob.getTarget())) {
            return;
        }
        LivingEntity clone = findNearestTargetableClone(mob);
        if (clone != null && canSwitchTarget(mob.getTarget(), clone)) {
            mob.setTarget(clone);
        }
    }

    @Nullable
    private static LivingEntity findNearestTargetableClone(Mob mob) {
        Level level = mob.level();
        double range = getFollowRange(mob);
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (KageBunshinEntity clone : level.getEntitiesOfClass(KageBunshinEntity.class, mob.getBoundingBox().inflate(range),
                clone -> canBeMobTarget(mob, clone))) {
            double distance = mob.distanceToSqr(clone);
            if (distance < bestDistance) {
                best = clone;
                bestDistance = distance;
            }
        }
        for (ExplosiveCloneEntity clone : level.getEntitiesOfClass(ExplosiveCloneEntity.class, mob.getBoundingBox().inflate(range),
                clone -> canBeMobTarget(mob, clone))) {
            double distance = mob.distanceToSqr(clone);
            if (distance < bestDistance) {
                best = clone;
                bestDistance = distance;
            }
        }
        for (JinchurikiCloneEntity clone : level.getEntitiesOfClass(JinchurikiCloneEntity.class, mob.getBoundingBox().inflate(range),
                clone -> canBeMobTarget(mob, clone))) {
            double distance = mob.distanceToSqr(clone);
            if (distance < bestDistance) {
                best = clone;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static double getFollowRange(Mob mob) {
        return mob.getAttribute(Attributes.FOLLOW_RANGE) != null
                ? Math.max(8.0D, mob.getAttributeValue(Attributes.FOLLOW_RANGE))
                : 16.0D;
    }

    private static boolean canBeMobTarget(Mob mob, LivingEntity clone) {
        if (!clone.isAlive() || clone.isSpectator() || !mob.canAttack(clone) || mob.isAlliedTo(clone)) {
            return false;
        }
        LivingEntity owner = ownerOf(clone);
        return (owner == null || !mob.isAlliedTo(owner)) && mob.getSensing().hasLineOfSight(clone);
    }

    private static boolean canSwitchTarget(@Nullable LivingEntity currentTarget, LivingEntity clone) {
        if (currentTarget == null || !currentTarget.isAlive()) {
            return true;
        }
        return currentTarget == ownerOf(clone);
    }

    private static boolean isLegacyTargetableClone(@Nullable LivingEntity entity) {
        return entity instanceof KageBunshinEntity
                || entity instanceof ExplosiveCloneEntity
                || entity instanceof JinchurikiCloneEntity;
    }

    @Nullable
    private static LivingEntity ownerOf(LivingEntity clone) {
        if (clone instanceof KageBunshinEntity kageBunshin) {
            return kageBunshin.getOwner();
        }
        if (clone instanceof ExplosiveCloneEntity explosiveClone) {
            return explosiveClone.getOwner();
        }
        if (clone instanceof JinchurikiCloneEntity jinchurikiClone) {
            return jinchurikiClone.getOwner();
        }
        return null;
    }
}
