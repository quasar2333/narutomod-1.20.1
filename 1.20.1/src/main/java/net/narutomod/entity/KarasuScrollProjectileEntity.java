package net.narutomod.entity;

import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.narutomod.registry.ModEntityTypes;

public final class KarasuScrollProjectileEntity extends AbstractPuppetScrollEntity {
    public KarasuScrollProjectileEntity(EntityType<? extends KarasuScrollProjectileEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    @Nullable
    protected AbstractPuppetEntity createPuppet(ServerLevel level) {
        return ModEntityTypes.PUPPET_KARASU.get().create(level);
    }
}
