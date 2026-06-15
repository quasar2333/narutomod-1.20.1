package net.narutomod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public final class HeavinessEffect extends MobEffect {
    public HeavinessEffect() {
        super(MobEffectCategory.HARMFUL, 0x666666);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            return;
        }
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 2, amplifier, false, false));
        if (entity.hasEffect(MobEffects.JUMP)) {
            entity.removeEffect(MobEffects.JUMP);
        }
        entity.addEffect(new MobEffectInstance(MobEffects.JUMP, 2, -2 - amplifier, false, false));
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
