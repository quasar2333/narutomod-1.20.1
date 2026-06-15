package net.narutomod.effect;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.phys.Vec3;
import net.narutomod.entity.LightningArcEntity;
import net.narutomod.registry.ModEntityTypes;

public final class ParalysisEffect extends MobEffect {
    private static final String TEMPORARY_DISABLE_AI_KEY = "temporaryDisableAI";
    private static final String FEAR_EFFECT_KEY = "FearEffect";

    public ParalysisEffect() {
        super(MobEffectCategory.HARMFUL, 0x00CCDC);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            return;
        }
        if (amplifier == 1) {
            entity.getPersistentData().putInt(FEAR_EFFECT_KEY, 2);
        } else if (amplifier >= 2 && entity.level() instanceof ServerLevel serverLevel) {
            spawnParalysisArc(entity, serverLevel, amplifier);
        }
        if (entity instanceof ServerPlayer player) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        float bodyYaw = entity.yBodyRot;
        entity.setYRot(bodyYaw);
        entity.setYHeadRot(bodyYaw);
        entity.setXRot(0.0F);
        if (entity.onGround()) {
            if (entity instanceof Mob mob) {
                if (entity.getPersistentData().getBoolean(TEMPORARY_DISABLE_AI_KEY)) {
                    entity.teleportTo(entity.xOld, entity.yOld, entity.zOld);
                } else if (!mob.isNoAi()) {
                    mob.setNoAi(true);
                    entity.getPersistentData().putBoolean(TEMPORARY_DISABLE_AI_KEY, true);
                }
            } else {
                entity.teleportTo(entity.xOld, entity.yOld, entity.zOld);
            }
            entity.setDeltaMovement(Vec3.ZERO);
        } else {
            entity.setDeltaMovement(0.0D, entity.getDeltaMovement().y() - 0.1D, 0.0D);
            entity.teleportTo(entity.xOld, entity.getY() + entity.getDeltaMovement().y(), entity.zOld);
        }
        entity.hurtMarked = true;
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        super.removeAttributeModifiers(entity, attributes, amplifier);
        if (!entity.level().isClientSide
                && entity instanceof Mob mob
                && entity.getPersistentData().getBoolean(TEMPORARY_DISABLE_AI_KEY)) {
            mob.setNoAi(false);
            entity.getPersistentData().putBoolean(TEMPORARY_DISABLE_AI_KEY, false);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    private static void spawnParalysisArc(LivingEntity entity, ServerLevel level, int amplifier) {
        double chance = 0.4D + 0.05D * (amplifier - 1);
        if (entity.getRandom().nextDouble() > chance) {
            return;
        }
        LightningArcEntity arc = ModEntityTypes.LIGHTNING_ARC.get().create(level);
        if (arc == null) {
            return;
        }
        Vec3 center = new Vec3(
                entity.getX() + (entity.getRandom().nextDouble() - 0.5D) * 0.4D,
                entity.getY() + entity.getRandom().nextDouble() * 1.3D,
                entity.getZ() + (entity.getRandom().nextDouble() - 0.5D) * 0.4D);
        arc.configureRandom(center, 0.3D * Math.min(amplifier - 1, 12), new Vec3(0.0D, 0.15D, 0.0D),
                0xC00000FF, 1, 0.0F, 0.1F);
        level.addFreshEntity(arc);
    }
}
