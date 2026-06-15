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

public final class PuppetHirukoEntity extends AbstractPuppetEntity {
    public static final float MAX_HEALTH = 10.0F;

    public PuppetHirukoEntity(EntityType<? extends PuppetHirukoEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return puppetAttributes(MAX_HEALTH, 0.0D, 0.3D, 3.0D);
    }

    public static PuppetHirukoEntity spawnNear(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        PuppetHirukoEntity puppet = ModEntityTypes.PUPPET_HIRUKO.get().create(serverLevel);
        if (puppet == null) {
            return null;
        }
        Vec3 look = owner.getLookAngle();
        puppet.moveTo(owner.getX() - look.x(), owner.getY(), owner.getZ() - look.z(), owner.getYRot(), 0.0F);
        puppet.setHealth(puppet.getMaxHealth());
        serverLevel.addFreshEntity(puppet);
        return puppet;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
