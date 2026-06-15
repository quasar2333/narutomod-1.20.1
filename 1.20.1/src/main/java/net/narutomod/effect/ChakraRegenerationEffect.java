package net.narutomod.effect;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.narutomod.Chakra;

public final class ChakraRegenerationEffect extends MobEffect {
    public ChakraRegenerationEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x00CC4C);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof ServerPlayer player && player.tickCount % 20 == 0) {
            Chakra.pathway(player).consume(-0.05F, true);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
