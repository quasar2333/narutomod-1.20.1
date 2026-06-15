package net.narutomod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.player.Player;

public final class FlightEffect extends MobEffect {
    private static final String GRANTED_FLIGHT_TAG = "NarutomodFlightGranted";

    public FlightEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x8FF8F0);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide || !(entity instanceof Player player)) {
            return;
        }
        if (!player.getAbilities().mayfly) {
            player.getPersistentData().putBoolean(GRANTED_FLIGHT_TAG, true);
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        super.removeAttributeModifiers(entity, attributes, amplifier);
        if (entity.level().isClientSide || !(entity instanceof Player player)) {
            return;
        }
        if (player.getPersistentData().getBoolean(GRANTED_FLIGHT_TAG) && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, false, false));
        }
        player.getPersistentData().remove(GRANTED_FLIGHT_TAG);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
