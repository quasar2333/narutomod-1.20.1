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

public final class PuppetKarasuEntity extends AbstractPuppetEntity {
    public static final float MAX_HEALTH = 40.0F;

    public PuppetKarasuEntity(EntityType<? extends PuppetKarasuEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return puppetAttributes(MAX_HEALTH, 10.0D, 0.4D, 10.0D);
    }

    public static PuppetKarasuEntity spawnNear(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        PuppetKarasuEntity puppet = ModEntityTypes.PUPPET_KARASU.get().create(serverLevel);
        if (puppet == null) {
            return null;
        }
        Vec3 look = owner.getLookAngle();
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
