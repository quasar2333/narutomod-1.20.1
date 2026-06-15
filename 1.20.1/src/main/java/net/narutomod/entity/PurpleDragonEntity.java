package net.narutomod.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class PurpleDragonEntity extends Entity {
    public static final float BASE_SIZE = 1.25F;
    public static final float DRAGON_SCALE = 1.5F;
    public static final int SPINE_SEGMENTS = 20;
    private static final int WAIT_TICKS = 40;
    private static final int MAX_ACTIVE_TICKS = WAIT_TICKS + 100;
    private static final double WAIT_ACCELERATION = 0.01D;
    private static final double FLIGHT_SPEED = 0.95D;
    private static final float WATER_SLOWDOWN = 0.8F;
    private static final double GEDO_DRAGON_HEIGHT_FACTOR = 1.4375D;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(PurpleDragonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(PurpleDragonEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUuid;
    private final List<UUID> targetUuids = new ArrayList<>();
    private final List<ProcedureUtils.Vec2f> partRotations = new ArrayList<>();
    @Nullable
    private Vec3 lastSegmentPosition;
    private Vec3 acceleration = Vec3.ZERO;
    private float previousHeadYaw;
    private float previousHeadPitch;
    private float dimensionsScale = DRAGON_SCALE;
    private float motionFactor;

    public PurpleDragonEntity(EntityType<? extends PurpleDragonEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
        this.noPhysics = true;
        seedPartRotations();
    }

    @Nullable
    public static PurpleDragonEntity spawnFrom(LivingEntity owner, List<? extends LivingEntity> targets) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        PurpleDragonEntity dragon = ModEntityTypes.PURPLE_DRAGON.get().create(serverLevel);
        if (dragon == null) {
            return null;
        }
        dragon.configure(owner, targets);
        serverLevel.addFreshEntity(dragon);
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                ModSounds.SOUND_DRAGON_ROAR.get(), SoundSource.HOSTILE, 100.0F, owner.getRandom().nextFloat() * 0.4F + 0.6F);
        return dragon;
    }

    public float getDragonScale() {
        return this.entityData.get(SCALE);
    }

    public float getWaitProgress(float partialTick) {
        return Mth.clamp((this.tickCount + partialTick) / (float)WAIT_TICKS, 0.0F, 1.0F);
    }

    public boolean isWaiting() {
        return this.tickCount <= WAIT_TICKS;
    }

    public List<ProcedureUtils.Vec2f> getPartRotations() {
        return this.partRotations;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(SCALE, DRAGON_SCALE);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            updateVisualSegments();
            return;
        }
        LivingEntity owner = getOwner();
        if (this.tickCount > MAX_ACTIVE_TICKS || owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        if (this.tickCount <= WAIT_TICKS) {
            tickWait(owner);
        } else {
            tickFlight();
        }
        hitLivingTargets(owner);
        updateVisualSegments();
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean ignoreExplosion() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 128.0D * getViewScale();
        return distance < range * range;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(this.dimensionsScale);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SCALE.equals(key)) {
            this.dimensionsScale = Mth.clamp(getDragonScale(), 0.1F, 16.0F);
            refreshDimensions();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        setScale(tag.contains("Scale") ? tag.getFloat("Scale") : DRAGON_SCALE);
        this.motionFactor = tag.getFloat("MotionFactor");
        this.acceleration = tag.contains("AccelerationX")
                ? new Vec3(tag.getDouble("AccelerationX"), tag.getDouble("AccelerationY"), tag.getDouble("AccelerationZ"))
                : Vec3.ZERO;
        this.targetUuids.clear();
        int count = tag.getInt("TargetCount");
        for (int i = 0; i < count; i++) {
            String key = "Target" + i;
            if (tag.hasUUID(key)) {
                this.targetUuids.add(tag.getUUID(key));
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putFloat("Scale", getDragonScale());
        tag.putFloat("MotionFactor", this.motionFactor);
        tag.putDouble("AccelerationX", this.acceleration.x());
        tag.putDouble("AccelerationY", this.acceleration.y());
        tag.putDouble("AccelerationZ", this.acceleration.z());
        tag.putInt("TargetCount", this.targetUuids.size());
        for (int i = 0; i < this.targetUuids.size(); i++) {
            tag.putUUID("Target" + i, this.targetUuids.get(i));
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void configure(LivingEntity owner, List<? extends LivingEntity> targets) {
        setOwner(owner);
        setScale(DRAGON_SCALE);
        this.targetUuids.clear();
        for (LivingEntity target : targets) {
            if (target.isAlive() && target != owner) {
                this.targetUuids.add(target.getUUID());
            }
        }
        Vec3 start = owner instanceof GedoStatueEntity
                ? owner.position().add(0.0D, GEDO_DRAGON_HEIGHT_FACTOR * GedoStatueEntity.MODEL_SCALE, 0.0D)
                : owner.getEyePosition().add(owner.getLookAngle().scale(1.0D));
        moveTo(start.x(), start.y(), start.z(), owner.getYHeadRot(), owner.getXRot());
        setDeltaMovement(Vec3.ZERO);
        this.acceleration = Vec3.ZERO;
        this.motionFactor = 0.0F;
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

    private void setScale(float scale) {
        this.dimensionsScale = Mth.clamp(scale, 0.1F, 16.0F);
        this.entityData.set(SCALE, this.dimensionsScale);
        refreshDimensions();
    }

    private void seedPartRotations() {
        this.partRotations.add(ProcedureUtils.Vec2f.ZERO);
    }

    private void updateVisualSegments() {
        Vec3 currentPosition = position();
        if (this.lastSegmentPosition == null) {
            this.lastSegmentPosition = currentPosition;
            this.previousHeadYaw = getYRot();
            this.previousHeadPitch = getXRot();
            return;
        }

        ProcedureUtils.Vec2f rotationDelta = new ProcedureUtils.Vec2f(getYRot(), getXRot())
                .subtract(this.previousHeadYaw, this.previousHeadPitch);
        Vec3 movement = currentPosition.subtract(this.lastSegmentPosition);
        double distance = movement.length();
        float segmentLength = Math.max(getDragonScale() * 11.0F * 0.0625F, 0.05F);
        if (distance >= segmentLength) {
            this.partRotations.add(0, rotationDelta);
            int steps = 1;
            int totalSteps = (int)(distance / segmentLength);
            for (; steps < totalSteps; steps++) {
                this.partRotations.add(0, ProcedureUtils.Vec2f.ZERO);
            }
            this.lastSegmentPosition = movement.normalize().scale(segmentLength * steps).add(this.lastSegmentPosition);
        } else if (!this.partRotations.isEmpty()) {
            this.partRotations.set(0, this.partRotations.get(0).add(rotationDelta));
        }
        while (this.partRotations.size() > SPINE_SEGMENTS) {
            this.partRotations.remove(this.partRotations.size() - 1);
        }
        this.previousHeadYaw = getYRot();
        this.previousHeadPitch = getXRot();
    }

    private void tickWait(LivingEntity owner) {
        Vec3 motion = getDeltaMovement();
        moveWithCurrentMotion(motion, headLook(owner));
        setDeltaMovement(getDeltaMovement().add(headLook(owner).scale(WAIT_ACCELERATION)));
    }

    private void tickFlight() {
        Vec3 motion = getDeltaMovement();
        moveWithCurrentMotion(motion, motion);
        updateLegacyNoGravityMotion();
        LivingEntity target = getNextTarget();
        if (target != null) {
            Vec3 delta = target.position().subtract(position());
            if (delta.lengthSqr() > 1.0E-8D) {
                shootToward(delta, FLIGHT_SPEED);
            }
        }
    }

    @Nullable
    private LivingEntity getNextTarget() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        this.targetUuids.removeIf(uuid -> {
            Entity entity = serverLevel.getEntity(uuid);
            return !(entity instanceof LivingEntity living) || !living.isAlive();
        });
        if (this.targetUuids.isEmpty()) {
            return null;
        }
        Entity entity = serverLevel.getEntity(this.targetUuids.get(0));
        return entity instanceof LivingEntity living ? living : null;
    }

    private void hitLivingTargets(@Nullable LivingEntity owner) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        AABB hitBox = getBoundingBox().inflate(getDragonScale() * 0.6D);
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, hitBox,
                living -> living.isAlive() && living != owner && !isAlliedToOwner(owner, living))) {
            target.kill();
            this.targetUuids.remove(target.getUUID());
        }
    }

    private boolean isAlliedToOwner(@Nullable LivingEntity owner, LivingEntity target) {
        return owner != null && (target == owner || target.isAlliedTo(owner) || owner.isAlliedTo(target));
    }

    private Vec3 headLook(LivingEntity owner) {
        Vec3 look = Vec3.directionFromRotation(owner.getXRot(), owner.getYHeadRot());
        return look.lengthSqr() > 1.0E-8D ? look : owner.getLookAngle();
    }

    private void moveWithCurrentMotion(Vec3 motion, Vec3 fallbackDirection) {
        if (motion.lengthSqr() > 1.0E-8D) {
            move(MoverType.SELF, motion);
            updateRotation(motion);
            return;
        }
        updateRotation(fallbackDirection);
    }

    private void shootToward(Vec3 direction, double speed) {
        if (direction.lengthSqr() <= 1.0E-8D) {
            return;
        }
        this.acceleration = direction.normalize().scale(0.1D);
        this.motionFactor = (float)speed;
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

    private void updateRotation(Vec3 motion) {
        if (motion.lengthSqr() <= 1.0E-8D) {
            return;
        }
        float yaw = ProcedureUtils.getYawFromVec(motion);
        float pitch = ProcedureUtils.getPitchFromVec(motion);
        while (yaw - this.yRotO < -180.0F) {
            this.yRotO -= 360.0F;
        }
        while (yaw - this.yRotO >= 180.0F) {
            this.yRotO += 360.0F;
        }
        while (pitch - this.xRotO < -180.0F) {
            this.xRotO -= 360.0F;
        }
        while (pitch - this.xRotO >= 180.0F) {
            this.xRotO += 360.0F;
        }
        setYRot(this.yRotO + (yaw - this.yRotO) * 0.2F);
        setXRot(this.xRotO + (pitch - this.xRotO) * 0.2F);
    }
}
