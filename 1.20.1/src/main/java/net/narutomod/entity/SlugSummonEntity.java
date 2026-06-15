package net.narutomod.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.narutomod.registry.ModEntityTypes;

public final class SlugSummonEntity extends AbstractSummonAnimalEntity {
    public SlugSummonEntity(EntityType<? extends SlugSummonEntity> entityType, Level level) {
        super(entityType, level);
        this.fireImmune();
    }

    public static boolean spawnFrom(LivingEntity owner, float scale) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        SlugSummonEntity entity = ModEntityTypes.SLUG.get().create(serverLevel);
        if (entity == null) {
            return false;
        }
        entity.configure(owner, scale);
        return scheduleDelayedSpawn(serverLevel, entity);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public double baseRenderWidth() {
        return 0.75D;
    }

    @Override
    public double baseRenderHeight() {
        return 0.75D;
    }

    @Override
    public double baseRenderDepth() {
        return 1.4D;
    }

    @Override
    protected void applyScaledAttributes(float scale) {
        setAttributeBaseValue(Attributes.ARMOR, 5.0D * scale);
        setAttributeBaseValue(Attributes.ATTACK_DAMAGE, 3.0D * scale);
        setAttributeBaseValue(Attributes.FOLLOW_RANGE, 13.0D + 3.0D * scale);
        setAttributeBaseValue(Attributes.MAX_HEALTH, 10.0D * scale * scale);
        setAttributeBaseValue(Attributes.MOVEMENT_SPEED, 0.25D + scale * 0.05D);
    }
}
