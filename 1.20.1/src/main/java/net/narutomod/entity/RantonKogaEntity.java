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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class RantonKogaEntity extends Entity {
    private static final int DURATION_TICKS = 20;
    private static final int LIGHTNING_COLOR = 0x80FF00FF;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(RantonKogaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> POWER = SynchedEntityData.defineId(RantonKogaEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUuid;

    public RantonKogaEntity(EntityType<? extends RantonKogaEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public static boolean spawnFrom(LivingEntity owner, float power) {
        RantonKogaEntity entity = ModEntityTypes.RANTON_KOGA.get().create(owner.level());
        if (entity == null) {
            return false;
        }
        entity.configure(owner, power);
        return owner.level().addFreshEntity(entity);
    }

    private void configure(LivingEntity owner, float power) {
        setOwner(owner);
        setPower(power);
        moveToOwner(owner);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(POWER, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        moveToOwner(owner);
        if (this.level().isClientSide) {
            return;
        }
        if (this.tickCount == 1) {
            this.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                    ModSounds.SOUND_LASER_SHORT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        spawnKogaArc((ServerLevel)this.level(), owner);
        if (this.tickCount > DURATION_TICKS) {
            discard();
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 96.0D * getViewScale();
        return distance < range * range;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        this.tickCount = tag.getInt("Life");
        setPower(tag.contains("Power") ? tag.getFloat("Power") : 1.0F);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putInt("Life", this.tickCount);
        tag.putFloat("Power", getPower());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void spawnKogaArc(ServerLevel level, LivingEntity owner) {
        Vec3 look = owner.getLookAngle();
        if (look.lengthSqr() <= 1.0E-8D) {
            look = Vec3.directionFromRotation(owner.getXRot(), owner.getYHeadRot());
        }
        if (look.lengthSqr() <= 1.0E-8D) {
            look = new Vec3(0.0D, 0.0D, 1.0D);
        }
        look = look.normalize();
        Vec3 eye = owner.getEyePosition();
        Vec3 start = eye.subtract(0.0D, 0.15D, 0.0D).add(look);
        Vec3 end = eye.add(look.scale(getPower() * 4.0F));
        LightningArcEntity arc = ModEntityTypes.LIGHTNING_ARC.get().create(level);
        if (arc != null) {
            arc.configureBetween(start, end, LIGHTNING_COLOR, 1, 0.0F, 0.0F, 0);
            arc.setSenjutsuDamage(20.0F * getPower(), true, owner);
            level.addFreshEntity(arc);
        }
    }

    private void moveToOwner(LivingEntity owner) {
        this.setDeltaMovement(Vec3.ZERO);
        this.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
    }

    private void setOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
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

    private void setPower(float power) {
        this.entityData.set(POWER, Math.max(power, 1.0F));
    }

    private float getPower() {
        return this.entityData.get(POWER);
    }
}
