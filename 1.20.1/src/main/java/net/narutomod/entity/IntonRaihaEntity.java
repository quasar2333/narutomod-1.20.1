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
import net.narutomod.NarutomodModVariables;
import net.narutomod.procedure.ProcedureSync;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class IntonRaihaEntity extends Entity {
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(IntonRaihaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> POWER = SynchedEntityData.defineId(IntonRaihaEntity.class, EntityDataSerializers.FLOAT);
    private static final int CHARGE_WAIT_TICKS = 50;

    @Nullable
    private UUID ownerUuid;
    private int waitTime;
    private boolean forceBowPoseSynced;

    public IntonRaihaEntity(EntityType<? extends IntonRaihaEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public static boolean spawnFrom(LivingEntity owner, float power) {
        IntonRaihaEntity entity = ModEntityTypes.INTON_RAIHA.get().create(owner.level());
        if (entity == null) {
            return false;
        }
        entity.configure(owner, power);
        owner.level().addFreshEntity(entity);
        return true;
    }

    private void configure(LivingEntity owner, float power) {
        setOwner(owner);
        setPower(power);
        this.waitTime = power >= 4.0F ? CHARGE_WAIT_TICKS : 0;
        moveTo(owner.getX(), owner.getY(), owner.getZ(), 0.0F, 0.0F);
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
            discardRaiha();
            return;
        }
        setPos(owner.getX(), owner.getY(), owner.getZ());
        if (this.level().isClientSide) {
            return;
        }
        float power = getPower();
        syncForceBowPose(owner);
        if (this.tickCount == 1 && this.waitTime > 0) {
            this.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                    ModSounds.SOUND_INTONRAIHA.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        float duration = this.waitTime + power * 10.0F;
        if (this.tickCount > this.waitTime) {
            spawnLightningArc((ServerLevel) this.level(), owner, power, duration);
        }
        if (this.tickCount > (int) duration) {
            discardRaiha();
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 68.5D * getViewScale();
        return distance < range * range;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        this.waitTime = tag.getInt("WaitTime");
        this.tickCount = tag.getInt("Life");
        this.forceBowPoseSynced = tag.getBoolean("ForceBowPoseSynced");
        setPower(tag.contains("Power") ? tag.getFloat("Power") : 1.0F);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putInt("WaitTime", this.waitTime);
        tag.putInt("Life", this.tickCount);
        tag.putBoolean("ForceBowPoseSynced", this.forceBowPoseSynced);
        tag.putFloat("Power", getPower());
    }

    @Override
    public void remove(RemovalReason reason) {
        clearForceBowPose();
        super.remove(reason);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void spawnLightningArc(ServerLevel level, LivingEntity owner, float power, float duration) {
        this.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                ModSounds.SOUND_ELECTRICITY.get(), SoundSource.PLAYERS, 0.6F, this.random.nextFloat() + 0.5F);
        Vec3 look = owner.getLookAngle();
        if (look.lengthSqr() <= 1.0E-8D) {
            look = Vec3.directionFromRotation(owner.getXRot(), owner.getYHeadRot());
        }
        if (look.lengthSqr() <= 1.0E-8D) {
            look = new Vec3(0.0D, 0.0D, 1.0D);
        }
        look = look.normalize();
        Vec3 start = owner.position()
                .add(0.0D, 1.3D, 0.0D)
                .add(look)
                .add((this.random.nextDouble() - 0.5D) * 0.2D,
                        (this.random.nextDouble() - 0.5D) * 0.2D,
                        (this.random.nextDouble() - 0.5D) * 0.2D);
        Vec3 direction = look
                .scale(power * 5.0F)
                .yRot((this.random.nextFloat() - 0.5F) * 1.0472F)
                .xRot((this.random.nextFloat() - 0.5F) * 1.0472F);
        Vec3 end = owner.getEyePosition().add(direction);
        float denominator = (float)this.tickCount - duration - 0.4F;
        int durationTicks = (int)(5.0F + power * 5.0F + 1.0F / denominator);
        LightningArcEntity arc = ModEntityTypes.LIGHTNING_ARC.get().create(level);
        if (arc != null) {
            arc.configureBetween(start, end, 0x80FF00FF, durationTicks, 0.4F, 0.0F, 4);
            arc.setSenjutsuDamage(power, true, owner);
            level.addFreshEntity(arc);
        }
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
        this.entityData.set(POWER, power);
    }

    private float getPower() {
        return this.entityData.get(POWER);
    }

    private void syncForceBowPose(LivingEntity owner) {
        if (!this.forceBowPoseSynced) {
            ProcedureSync.EntityNBTTag.setAndSync(owner, NarutomodModVariables.FORCE_BOW_POSE, true);
            this.forceBowPoseSynced = true;
        }
    }

    private void clearForceBowPose() {
        if (!this.level().isClientSide && this.forceBowPoseSynced) {
            LivingEntity owner = getOwner();
            if (owner != null) {
                ProcedureSync.EntityNBTTag.removeAndSync(owner, NarutomodModVariables.FORCE_BOW_POSE);
            }
            this.forceBowPoseSynced = false;
        }
    }

    private void discardRaiha() {
        clearForceBowPose();
        discard();
    }
}
