package net.narutomod.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.narutomod.registry.ModDamageTypes;

public final class CorrosionEffect extends MobEffect {
    public CorrosionEffect() {
        super(MobEffectCategory.HARMFUL, 0xCCCCCC);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            return;
        }
        entity.invulnerableTime = 0;
        entity.hurt(ModDamageTypes.ninjutsu(entity.level(), null, null), amplifier + 1.0F);
        entity.invulnerableTime = 0;
        entity.hurt(entity.damageSources().generic(), amplifier);
        if (entity.level() instanceof ServerLevel level && entity.getRandom().nextDouble() <= 0.5D) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.8F,
                    entity.getRandom().nextFloat() * 0.6F + 0.6F);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
