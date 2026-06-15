package net.narutomod.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class MandaEntity extends AbstractSummonAnimalEntity {
    public static final float SUMMON_SCALE = 18.0F;

    public MandaEntity(EntityType<? extends MandaEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static boolean spawnFrom(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        MandaEntity entity = ModEntityTypes.MANDA.get().create(serverLevel);
        if (entity == null) {
            return false;
        }
        entity.configure(owner, SUMMON_SCALE);
        return scheduleDelayedSpawn(serverLevel, entity);
    }

    @Override
    public double baseRenderWidth() {
        return 0.3D;
    }

    @Override
    public double baseRenderHeight() {
        return 0.25D;
    }

    @Override
    public double baseRenderDepth() {
        return 3.0D;
    }

    @Override
    protected void applyScaledAttributes(float scale) {
        setAttributeBaseValue(Attributes.ARMOR, 5.0D * scale);
        setAttributeBaseValue(Attributes.ATTACK_DAMAGE, 2.6667D * scale);
        setAttributeBaseValue(Attributes.FOLLOW_RANGE, 13.0D + 3.0D * scale);
        setAttributeBaseValue(Attributes.MAX_HEALTH, 8.0D * scale * scale);
        setAttributeBaseValue(Attributes.MOVEMENT_SPEED, 0.25D + scale * 0.05D);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SOUND_SNAKE_HISS.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return net.minecraft.sounds.SoundEvents.GENERIC_HURT;
    }
}
