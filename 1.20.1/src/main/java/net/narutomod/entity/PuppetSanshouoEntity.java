package net.narutomod.entity;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.registry.ModEntityTypes;

public final class PuppetSanshouoEntity extends AbstractPuppetEntity {
    public static final float MAX_HEALTH = 200.0F;

    public PuppetSanshouoEntity(EntityType<? extends PuppetSanshouoEntity> entityType, Level level) {
        super(entityType, level);
        this.setMaxUpStep(2.5F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return puppetAttributes(MAX_HEALTH, 6.0D, 0.6D, 8.0D);
    }

    public static PuppetSanshouoEntity spawnNear(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        PuppetSanshouoEntity puppet = ModEntityTypes.PUPPET_SANSHOUO.get().create(serverLevel);
        if (puppet == null) {
            return null;
        }
        Vec3 look = owner.getLookAngle().scale(2.0D);
        puppet.moveTo(owner.getX() + look.x(), owner.getY(), owner.getZ() + look.z(), owner.getYRot(), 0.0F);
        puppet.setHealth(puppet.getMaxHealth());
        puppet.bindTo(owner);
        serverLevel.addFreshEntity(puppet);
        return puppet;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
