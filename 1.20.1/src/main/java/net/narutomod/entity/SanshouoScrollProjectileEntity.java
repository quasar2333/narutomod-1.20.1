package net.narutomod.entity;

import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.narutomod.registry.ModEntityTypes;

public final class SanshouoScrollProjectileEntity extends AbstractPuppetScrollEntity {
    public SanshouoScrollProjectileEntity(EntityType<? extends SanshouoScrollProjectileEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    @Nullable
    protected AbstractPuppetEntity createPuppet(ServerLevel level) {
        return ModEntityTypes.PUPPET_SANSHOUO.get().create(level);
    }
}
