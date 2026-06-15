package net.narutomod.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModItems;
import net.narutomod.registry.ModParticleTypes;

public final class AmaterasuFlameEffect extends MobEffect {
    private static final int BLACK_FLAME_COLOR = 0xA0000000;
    private static final int BLACK_FLAME_SCALE = 20;

    public AmaterasuFlameEffect() {
        super(MobEffectCategory.HARMFUL, 0x000000);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            return;
        }
        if (isMangekyoProtected(entity)) {
            entity.removeEffect(ModEffects.AMATERASUFLAME.get());
            entity.clearFire();
            return;
        }
        entity.hurt(ModDamageTypes.ninjutsuFire(entity.level(), null, null), amplifier + 1.0F);
        if (entity.level() instanceof ServerLevel level) {
            double width = entity.getBbWidth() * 0.5D;
            double height = entity.getBbHeight();
            level.sendParticles(
                    ModParticleTypes.options(NarutoParticleKind.FLAME_COLORED, BLACK_FLAME_COLOR, BLACK_FLAME_SCALE),
                    entity.getX(),
                    entity.getY() + height * 0.5D,
                    entity.getZ(),
                    amplifier + 1,
                    width * 0.5D,
                    height * 0.2D,
                    width * 0.5D,
                    0.0D);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    private static boolean isMangekyoProtected(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        return head.is(ModItems.MANGEKYOSHARINGANHELMET.get())
                || head.is(ModItems.MANGEKYOSHARINGANETERNALHELMET.get());
    }
}
