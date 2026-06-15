package net.narutomod.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.narutomod.registry.ModEntityTypes;

public final class GamabuntaEntity extends AbstractSummonAnimalEntity {
    public static final float SUMMON_SCALE = 16.0F;

    public GamabuntaEntity(EntityType<? extends GamabuntaEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static boolean spawnFrom(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        GamabuntaEntity entity = ModEntityTypes.GAMABUNTA.get().create(serverLevel);
        if (entity == null) {
            return false;
        }
        entity.configure(owner, SUMMON_SCALE);
        return scheduleDelayedSpawn(serverLevel, entity);
    }

    @Override
    public double baseRenderWidth() {
        return 0.8D;
    }

    @Override
    public double baseRenderHeight() {
        return 1.125D;
    }

    @Override
    public double baseRenderDepth() {
        return 1.15D;
    }

    @Override
    protected void applyScaledAttributes(float scale) {
        setAttributeBaseValue(Attributes.ARMOR, 5.0D * scale);
        setAttributeBaseValue(Attributes.ATTACK_DAMAGE, 3.0D * scale);
        setAttributeBaseValue(Attributes.FOLLOW_RANGE, 13.0D + 3.0D * scale);
        setAttributeBaseValue(Attributes.MAX_HEALTH, 10.0D * scale * scale);
        setAttributeBaseValue(Attributes.MOVEMENT_SPEED, 0.3D);
    }
}
