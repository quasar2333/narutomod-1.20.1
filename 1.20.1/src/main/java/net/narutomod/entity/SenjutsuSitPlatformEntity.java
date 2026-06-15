package net.narutomod.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.registry.ModEntityTypes;

public final class SenjutsuSitPlatformEntity extends Entity {
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(SenjutsuSitPlatformEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;

    public SenjutsuSitPlatformEntity(EntityType<? extends SenjutsuSitPlatformEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = false;
    }

    public static boolean spawnFor(LivingEntity rider) {
        if (!(rider.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (rider.getVehicle() instanceof SenjutsuSitPlatformEntity) {
            return true;
        }
        SenjutsuSitPlatformEntity platform = ModEntityTypes.ENTITYBULLETSENJUTSU.get().create(serverLevel);
        if (platform == null) {
            return false;
        }
        platform.configure(rider);
        serverLevel.addFreshEntity(platform);
        return rider.startRiding(platform, true);
    }

    public static boolean isRidingPlatform(Entity entity) {
        return entity.getVehicle() instanceof SenjutsuSitPlatformEntity;
    }

    public static void stopRidingIfOnPlatform(Entity entity) {
        if (entity.getVehicle() instanceof SenjutsuSitPlatformEntity platform) {
            entity.stopRiding();
            platform.discard();
        }
    }

    private void configure(LivingEntity rider) {
        setOwner(rider);
        moveTo(rider.getX(), rider.getY() + 0.1D, rider.getZ(), rider.getYRot(), 0.0F);
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
    }

    @Override
    public void tick() {
        super.tick();
        move(MoverType.SELF, new Vec3(0.0D, getDeltaMovement().y(), 0.0D));
        double nextY = onGround() ? 0.0D : getDeltaMovement().y() - 0.08D;
        setDeltaMovement(0.0D, nextY * 0.98D, 0.0D);
        if (!this.level().isClientSide && !isVehicle()) {
            discard();
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof LivingEntity && getPassengers().isEmpty() && isOwnerOrUnbound(passenger);
    }

    @Override
    public double getPassengersRidingOffset() {
        return -0.25D;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (!hasPassenger(passenger)) {
            return;
        }
        moveFunction.accept(passenger,
                getX(),
                getY() + getPassengersRidingOffset() + passenger.getMyRidingOffset(),
                getZ());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private boolean isOwnerOrUnbound(Entity passenger) {
        return this.ownerUuid == null || passenger.getUUID().equals(this.ownerUuid) || getOwner() == passenger;
    }

    @Nullable
    private LivingEntity getOwner() {
        int ownerId = this.entityData.get(OWNER_ID);
        if (ownerId >= 0 && this.level().getEntity(ownerId) instanceof LivingEntity living) {
            return living;
        }
        if (this.ownerUuid != null && this.level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(this.ownerUuid) instanceof LivingEntity living) {
            setOwner(living);
            return living;
        }
        return null;
    }

    private void setOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
    }
}
