package net.narutomod.entity;

import java.util.Comparator;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.registry.ModEntityTypes;

public final class SpikeEntity extends Entity {
    private static final float BASE_WIDTH = 0.5F;
    private static final float BASE_HEIGHT = 1.82F;
    private static final int MAX_IN_GROUND_TIME = 1200;
    private static final float WATER_SLOWDOWN = 0.8F;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(SpikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(SpikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(SpikeEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUuid;
    private float dimensionsScale = 1.0F;
    private int ticksInAir;
    private int ticksInGround;
    private boolean inGround;
    private Vec3 acceleration = Vec3.ZERO;
    private float motionFactor;

    public SpikeEntity(EntityType<? extends SpikeEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static boolean spawnFrom(LivingEntity owner, int color, float scale, float speed) {
        if (!(owner.level() instanceof ServerLevel level)) {
            return false;
        }
        SpikeEntity spike = ModEntityTypes.SPIKE.get().create(level);
        if (spike == null) {
            return false;
        }
        spike.configure(owner, color, scale, speed);
        return level.addFreshEntity(spike);
    }

    public void configure(LivingEntity owner, int color, float scale, float speed) {
        setOwner(owner);
        setColor(color);
        setEntityScale(scale);
        setNoGravity(true);
        Vec3 look = owner.getLookAngle();
        moveTo(owner.getX(), owner.getEyeY() - 0.15D, owner.getZ(), owner.getYRot(), owner.getXRot() + 90.0F);
        this.yRotO = getYRot();
        this.xRotO = getXRot();
        shoot(look.x(), look.y(), look.z(), speed, 0.0F);
    }

    public int getColor() {
        return this.entityData.get(COLOR);
    }

    public float getEntityScale() {
        return this.entityData.get(SCALE);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(COLOR, 0xFFFFFFFF);
        this.entityData.define(SCALE, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && ownerUuid != null && getOwner() == null) {
            discard();
            return;
        }
        if (this.inGround) {
            if (!this.level().isClientSide && ++this.ticksInGround > MAX_IN_GROUND_TIME) {
                discard();
            }
            return;
        }

        this.ticksInAir++;
        checkTipCollision();
        if (this.inGround) {
            return;
        }
        travel();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        this.ticksInAir = tag.getInt("TicksInAir");
        this.ticksInGround = tag.getInt("TicksInGround");
        this.inGround = tag.getBoolean("InGround");
        setColor(tag.contains("Color") ? tag.getInt("Color") : 0xFFFFFFFF);
        setEntityScale(tag.contains("Scale") ? tag.getFloat("Scale") : 1.0F);
        setDeltaMovement(tag.getDouble("MotionX"), tag.getDouble("MotionY"), tag.getDouble("MotionZ"));
        setNoGravity(tag.getBoolean("NoGravity"));
        this.motionFactor = tag.getFloat("MotionFactor");
        this.acceleration = tag.contains("AccelerationX")
                ? new Vec3(tag.getDouble("AccelerationX"), tag.getDouble("AccelerationY"), tag.getDouble("AccelerationZ"))
                : Vec3.ZERO;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putInt("TicksInAir", this.ticksInAir);
        tag.putInt("TicksInGround", this.ticksInGround);
        tag.putBoolean("InGround", this.inGround);
        tag.putInt("Color", getColor());
        tag.putFloat("Scale", getEntityScale());
        Vec3 motion = getDeltaMovement();
        tag.putDouble("MotionX", motion.x());
        tag.putDouble("MotionY", motion.y());
        tag.putDouble("MotionZ", motion.z());
        tag.putBoolean("NoGravity", isNoGravity());
        tag.putFloat("MotionFactor", this.motionFactor);
        tag.putDouble("AccelerationX", this.acceleration.x());
        tag.putDouble("AccelerationY", this.acceleration.y());
        tag.putDouble("AccelerationZ", this.acceleration.z());
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(this.dimensionsScale);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SCALE.equals(key)) {
            this.dimensionsScale = Mth.clamp(getEntityScale(), 0.05F, 32.0F);
            refreshDimensions();
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
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

    private void setColor(int color) {
        this.entityData.set(COLOR, color);
    }

    private void setEntityScale(float scale) {
        this.dimensionsScale = Mth.clamp(scale, 0.05F, 32.0F);
        this.entityData.set(SCALE, this.dimensionsScale);
        refreshDimensions();
    }

    private void shoot(double x, double y, double z, float speed, float inaccuracy) {
        Vec3 direction = new Vec3(
                x + this.random.nextGaussian() * inaccuracy,
                y + this.random.nextGaussian() * inaccuracy,
                z + this.random.nextGaussian() * inaccuracy).normalize();
        if (direction.lengthSqr() <= 1.0E-8D) {
            direction = Vec3.directionFromRotation(getXRot(), getYRot());
        }
        if (isNoGravity()) {
            this.acceleration = direction.normalize().scale(0.1D);
            this.motionFactor = speed;
        } else {
            setDeltaMovement(direction.scale(speed));
            this.motionFactor = speed;
        }
        faceMotion(direction);
    }

    private void travel() {
        Vec3 motion = getDeltaMovement();
        if (motion.lengthSqr() <= 1.0E-8D) {
            return;
        }
        Vec3 start = tipPosition();
        Vec3 end = start.add(motion);
        HitResult hit = findImpact(start, end);
        if (hit.getType() == HitResult.Type.BLOCK) {
            moveToImpact(hit.getLocation());
            setInGround();
            return;
        }
        setPos(getX() + motion.x(), getY() + motion.y(), getZ() + motion.z());
        if (isNoGravity()) {
            updateLegacyNoGravityMotion();
        } else if (this.motionFactor > 0.0F) {
            setDeltaMovement(motion.x() * 0.98D, motion.y() * 0.98D - 0.04D, motion.z() * 0.98D);
            faceMotion(getDeltaMovement());
        }
    }

    private HitResult findImpact(Vec3 start, Vec3 end) {
        BlockHitResult blockHit = this.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        double maxDistance = blockHit.getType() == HitResult.Type.MISS ? start.distanceTo(end) : start.distanceTo(blockHit.getLocation());
        LivingEntity owner = getOwner();
        EntityHitResult entityHit = this.level().getEntities(this, getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D),
                        target -> canCollideWithTarget(owner, target))
                .stream()
                .map(entity -> entity.getBoundingBox().inflate(BASE_WIDTH * this.dimensionsScale).clip(start, end)
                        .map(location -> new EntityHitResult(entity, location))
                        .orElse(null))
                .filter(candidate -> candidate != null && start.distanceTo(candidate.getLocation()) <= maxDistance)
                .min(Comparator.comparingDouble(candidate -> start.distanceTo(candidate.getLocation())))
                .orElse(null);
        return entityHit != null ? entityHit : blockHit;
    }

    private boolean canCollideWithTarget(@Nullable LivingEntity owner, Entity target) {
        return target.isAlive()
                && target.isPickable()
                && target != this
                && !(target instanceof SpikeEntity)
                && (this.ticksInAir >= 25 || target != owner);
    }

    private void checkTipCollision() {
        BlockPos pos = BlockPos.containing(tipPosition());
        if (!this.level().getBlockState(pos).getCollisionShape(this.level(), pos).isEmpty()) {
            setInGround();
        }
    }

    private Vec3 tipPosition() {
        Vec3 localTip = new Vec3(0.0D, BASE_HEIGHT * this.dimensionsScale, 0.0D);
        float pitch = -getXRot() * Mth.DEG_TO_RAD;
        float yaw = -getYRot() * Mth.DEG_TO_RAD;
        return localTip.xRot(pitch).yRot(yaw).add(position());
    }

    private void moveToImpact(Vec3 location) {
        Vec3 tip = tipPosition();
        Vec3 offset = location.subtract(tip);
        setPos(getX() + offset.x(), getY() + offset.y(), getZ() + offset.z());
    }

    private void setInGround() {
        this.inGround = true;
        this.ticksInGround = 0;
        setDeltaMovement(Vec3.ZERO);
        this.acceleration = Vec3.ZERO;
        this.motionFactor = 0.0F;
        setNoGravity(false);
    }

    private void updateLegacyNoGravityMotion() {
        if (this.motionFactor <= 0.0F) {
            return;
        }
        float factor = this.isInWater() ? this.motionFactor * WATER_SLOWDOWN : this.motionFactor;
        if (this.isInWater() && this.level() instanceof ServerLevel serverLevel) {
            Vec3 motion = getDeltaMovement();
            serverLevel.sendParticles(ParticleTypes.BUBBLE,
                    getX() - motion.x() * 0.25D,
                    getY() - motion.y() * 0.25D,
                    getZ() - motion.z() * 0.25D,
                    4,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D);
        }
        setDeltaMovement(getDeltaMovement().add(this.acceleration).scale(factor));
    }

    private void faceMotion(Vec3 motion) {
        if (motion.lengthSqr() <= 1.0E-8D) {
            return;
        }
        setYRot((float)(-Mth.atan2(motion.x(), motion.z()) * Mth.RAD_TO_DEG));
        setXRot((float)(-Mth.atan2(motion.y(), Math.sqrt(motion.x() * motion.x() + motion.z() * motion.z())) * Mth.RAD_TO_DEG) + 90.0F);
    }
}
