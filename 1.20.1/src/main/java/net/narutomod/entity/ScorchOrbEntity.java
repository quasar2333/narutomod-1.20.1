package net.narutomod.entity;

import java.util.Comparator;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.event.SpecialEvent;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModItems;
import net.narutomod.registry.ModParticleTypes;

public final class ScorchOrbEntity extends Entity {
    private static final float INITIAL_SCALE = 0.5F;
    private static final double ORBIT_SPEED = 9.0D;
    private static final double LAUNCH_SPEED = 0.95D;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(ScorchOrbEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(ScorchOrbEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_TIME = SynchedEntityData.defineId(ScorchOrbEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(ScorchOrbEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> MAX_SCALE = SynchedEntityData.defineId(ScorchOrbEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> LAUNCHED = SynchedEntityData.defineId(ScorchOrbEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private UUID targetUuid;
    private double idleHeight = 1.62D;
    private Vec3 acceleration = Vec3.ZERO;
    private float motionFactor;
    private int ticksInAir;
    private float dimensionsScale = INITIAL_SCALE;

    public ScorchOrbEntity(EntityType<? extends ScorchOrbEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    public void configure(LivingEntity owner) {
        setOwner(owner);
        this.idleHeight = owner.getEyeHeight();
        setScorchScale(INITIAL_SCALE);
        setMaxScale(INITIAL_SCALE);
        moveTo(owner.getX(), owner.getY() + owner.getBbHeight(), owner.getZ(), owner.getYRot(), owner.getXRot());
    }

    public static ScorchOrbEntity spawnFrom(LivingEntity owner) {
        ScorchOrbEntity entity = ModEntityTypes.SCORCHORB.get().create(owner.level());
        if (entity == null) {
            return null;
        }
        entity.configure(owner);
        owner.level().addFreshEntity(entity);
        return entity;
    }

    public float getScorchScale() {
        return this.entityData.get(SCALE);
    }

    public void setTarget(@Nullable Entity target) {
        this.targetUuid = target == null ? null : target.getUUID();
        this.entityData.set(TARGET_ID, target == null ? -1 : target.getId());
        this.entityData.set(TARGET_TIME, target == null ? -1 : 60);
    }

    public void setMaxScale(float scale) {
        this.entityData.set(MAX_SCALE, Math.max(scale, 0.0F));
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(TARGET_ID, -1);
        this.entityData.define(TARGET_TIME, -1);
        this.entityData.define(SCALE, INITIAL_SCALE);
        this.entityData.define(MAX_SCALE, INITIAL_SCALE);
        this.entityData.define(LAUNCHED, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive() || !this.level().hasChunkAt(blockPosition())
                || !isOwnerHoldingShakuton(owner) || isInWater()) {
            discard();
            return;
        }
        spawnIdleParticles();
        if (isLaunched()) {
            travelAndImpact(owner);
            return;
        }
        updateUnlaunched(owner);
        moveByDelta();
        scorchTouchedEntities(owner);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 128.0D * getViewScale();
        return distance < range * range;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        if (tag.hasUUID("Target")) {
            this.targetUuid = tag.getUUID("Target");
        }
        setScorchScale(tag.contains("Scale") ? tag.getFloat("Scale") : INITIAL_SCALE);
        setMaxScale(tag.contains("MaxScale") ? tag.getFloat("MaxScale") : INITIAL_SCALE);
        this.entityData.set(TARGET_TIME, tag.contains("TargetTime") ? tag.getInt("TargetTime") : -1);
        this.entityData.set(LAUNCHED, tag.getBoolean("Launched"));
        this.idleHeight = tag.contains("IdleHeight") ? tag.getDouble("IdleHeight") : 1.62D;
        this.motionFactor = tag.getFloat("MotionFactor");
        this.ticksInAir = readFlightTicks(tag);
        this.acceleration = tag.contains("AccelerationX")
                ? new Vec3(tag.getDouble("AccelerationX"), tag.getDouble("AccelerationY"), tag.getDouble("AccelerationZ"))
                : Vec3.ZERO;
        if (tag.contains("MotionX")) {
            setDeltaMovement(tag.getDouble("MotionX"), tag.getDouble("MotionY"), tag.getDouble("MotionZ"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        if (this.targetUuid != null) {
            tag.putUUID("Target", this.targetUuid);
        }
        tag.putFloat("Scale", getScorchScale());
        tag.putFloat("MaxScale", getMaxScale());
        tag.putInt("TargetTime", this.entityData.get(TARGET_TIME));
        tag.putBoolean("Launched", isLaunched());
        tag.putDouble("IdleHeight", this.idleHeight);
        tag.putInt("FlightTicks", this.ticksInAir);
        tag.putInt("TicksInAir", this.ticksInAir);
        tag.putInt("flighttime", this.ticksInAir);
        tag.putFloat("MotionFactor", this.motionFactor);
        tag.putDouble("AccelerationX", this.acceleration.x());
        tag.putDouble("AccelerationY", this.acceleration.y());
        tag.putDouble("AccelerationZ", this.acceleration.z());
        Vec3 motion = getDeltaMovement();
        tag.putDouble("MotionX", motion.x());
        tag.putDouble("MotionY", motion.y());
        tag.putDouble("MotionZ", motion.z());
    }

    private int readFlightTicks(CompoundTag tag) {
        if (tag.contains("TicksInAir")) {
            return tag.getInt("TicksInAir");
        }
        if (tag.contains("FlightTicks")) {
            return tag.getInt("FlightTicks");
        }
        return tag.getInt("flighttime");
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(this.dimensionsScale);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SCALE.equals(key)) {
            this.dimensionsScale = Math.max(getScorchScale(), 0.1F);
            refreshDimensions();
        }
    }

    private void updateUnlaunched(LivingEntity owner) {
        float scale = getScorchScale();
        float maxScale = getMaxScale();
        if (maxScale != scale) {
            moveGrowAndShoot(owner, scale, maxScale);
            return;
        }
        int targetTime = this.entityData.get(TARGET_TIME);
        Entity target = getTarget();
        if (target != null && targetTime > 0) {
            if (target.isAlive()) {
                setNextPosition(target.getEyePosition());
                this.entityData.set(TARGET_TIME, targetTime - 1);
            } else {
                this.entityData.set(TARGET_TIME, 0);
                returnToIdlePosition(owner);
            }
        } else {
            returnToIdlePosition(owner);
        }
    }

    private void returnToIdlePosition(LivingEntity owner) {
        Vec3 idlePosition = getIdlePosition(owner);
        boolean clearTargetOnArrival = this.entityData.get(TARGET_TIME) >= 0;
        setNextPosition(idlePosition);
        if (clearTargetOnArrival && this.position().distanceTo(idlePosition) <= 0.5D) {
            setTarget(null);
        }
    }

    private void moveGrowAndShoot(LivingEntity owner, float scale, float maxScale) {
        Vec3 launchPoint = owner.position().add(0.0D, owner.getBbHeight() + 1.5D, 0.0D);
        if (this.position().distanceTo(launchPoint) > 0.2D) {
            setDeltaMovement(launchPoint.subtract(position()).normalize().scale(0.1D));
            return;
        }
        if (maxScale <= 0.0F) {
            discard();
            return;
        }
        setDeltaMovement(Vec3.ZERO);
        if (scale < maxScale) {
            setScorchScale(scale * 1.03F);
            return;
        }
        Vec3 look = owner.getLookAngle();
        if (look.lengthSqr() <= 1.0E-8D) {
            look = new Vec3(0.0D, 0.0D, 1.0D);
        }
        shoot(look, LAUNCH_SPEED);
    }

    private void setNextPosition(Vec3 vec) {
        if (this.position().distanceTo(vec) > 0.5D && this.entityData.get(TARGET_TIME) >= 0) {
            setDeltaMovement(vec.subtract(position()).normalize().scale(0.4D));
        } else {
            setDeltaMovement(vec.subtract(position()));
        }
    }

    private void moveByDelta() {
        Vec3 movement = getDeltaMovement();
        setPos(getX() + movement.x(), getY() + movement.y(), getZ() + movement.z());
    }

    private void travelAndImpact(LivingEntity owner) {
        Vec3 start = centerPosition();
        Vec3 motion = getDeltaMovement();
        if (this.motionFactor > 0.0F) {
            this.ticksInAir++;
        }
        if (motion.lengthSqr() > 1.0E-8D) {
            Vec3 end = start.add(motion);
            HitResult hit = findImpact(owner, start, end);
            if (hit.getType() != HitResult.Type.MISS) {
                if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() == owner) {
                    setPos(getX() + motion.x(), getY() + motion.y(), getZ() + motion.z());
                    updateLegacyNoGravityMotion();
                    return;
                }
                impact(owner);
                return;
            }
            setPos(getX() + motion.x(), getY() + motion.y(), getZ() + motion.z());
        }
        updateLegacyNoGravityMotion();
    }

    private HitResult findImpact(LivingEntity owner, Vec3 start, Vec3 end) {
        BlockHitResult blockHit = findLegacyBlockImpact(start, end);
        double maxDistance = blockHit.getType() == HitResult.Type.MISS ? start.distanceTo(end) : start.distanceTo(blockHit.getLocation());
        EntityHitResult entityHit = this.level().getEntities(this, getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D),
                        target -> canImpact(owner, target))
                .stream()
                .map(entity -> entity.getBoundingBox().inflate(getBbWidth() * 0.5D, getBbHeight() * 0.5D, getBbWidth() * 0.5D).clip(start, end)
                        .map(location -> new EntityHitResult(entity, location))
                        .orElse(null))
                .filter(candidate -> candidate != null && start.distanceTo(candidate.getLocation()) <= maxDistance)
                .min(Comparator.comparingDouble(candidate -> start.distanceTo(candidate.getLocation())))
                .orElse(null);
        return entityHit != null ? entityHit : blockHit;
    }

    private BlockHitResult findLegacyBlockImpact(Vec3 start, Vec3 end) {
        Vec3 motion = end.subtract(start);
        AABB search = getBoundingBox().expandTowards(motion).inflate(1.0D);
        Vec3 bestLocation = null;
        BlockPos bestPos = null;
        double bestDistance = 0.0D;
        for (BlockPos pos : BlockPos.betweenClosed(
                Mth.floor(search.minX),
                Mth.floor(search.minY),
                Mth.floor(search.minZ),
                Mth.floor(search.maxX),
                Mth.floor(search.maxY),
                Mth.floor(search.maxZ))) {
            VoxelShape shape = this.level().getBlockState(pos).getCollisionShape(this.level(), pos);
            if (shape.isEmpty()) {
                continue;
            }
            for (AABB box : shape.toAabbs()) {
                AABB sweptBox = box.move(pos).inflate(getBbWidth() * 0.5D, getBbHeight() * 0.5D, getBbWidth() * 0.5D);
                Vec3 location = sweptBox.clip(start, end).orElse(null);
                if (location == null) {
                    continue;
                }
                double distance = start.distanceTo(location);
                if (bestLocation == null || distance < bestDistance) {
                    bestLocation = location;
                    bestPos = pos.immutable();
                    bestDistance = distance;
                }
            }
        }
        if (bestLocation == null || bestPos == null) {
            return BlockHitResult.miss(end, Direction.getNearest(motion.x(), motion.y(), motion.z()), BlockPos.containing(end));
        }
        return new BlockHitResult(bestLocation, Direction.getNearest(-motion.x(), -motion.y(), -motion.z()), bestPos, false);
    }

    private boolean canImpact(LivingEntity owner, Entity target) {
        return target.isAlive()
                && target.isPickable()
                && !target.noPhysics
                && target != this
                && (target != owner || this.ticksInAir >= 25);
    }

    private void impact(LivingEntity owner) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 point = position();
        float scale = getMaxScale();
        float radius = Math.max(scale, 0.5F);
        DamageSource source = ModDamageTypes.ninjutsu(this.level(), this, owner);
        SpecialEvent.setSphericalExplosionEvent(serverLevel, owner, (int)point.x(), (int)point.y() + 5, (int)point.z(),
                (int)scale, serverLevel.getServer().overworld().getGameTime(), true, 0.3333F, true, true,
                ForgeEventFactory.getMobGriefingEvent(serverLevel, owner));
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, new AABB(point, point).inflate(radius),
                target -> target.isAlive() && target != owner && target.distanceToSqr(point) <= radius * radius)) {
            target.invulnerableTime = 0;
            target.hurt(source, Math.max(scale * 60.0F, 1.0F));
        }
        scorchEffects(point.x(), point.y(), point.z(), 2.5D * scale, 1.0D);
        discard();
    }

    private void scorchTouchedEntities(LivingEntity owner) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        DamageSource source = ModDamageTypes.ninjutsu(this.level(), this, owner);
        AABB box = getBoundingBox();
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, box,
                target -> target.isAlive() && target != owner)) {
            target.invulnerableTime = 0;
            target.hurt(source, 1.0F);
            scorchEffects(target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    target.getBbWidth() * 0.5D, target.getBbHeight() * 0.5D);
        }
    }

    private void scorchEffects(double x, double y, double z, double dx, double dy) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        this.level().playSound(null, x, y, z, SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL,
                1.0F, this.random.nextFloat() * 0.6F + 0.7F);
        serverLevel.sendParticles(
                ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0x40FFFFFF, 15, 0, 0, -1, 4),
                x,
                y,
                z,
                Math.max((int)(dx * dy * 100.0D), 1),
                dx,
                dy,
                dx,
                0.0D);
    }

    private void spawnIdleParticles() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        float scale = getScorchScale();
        serverLevel.sendParticles(
                ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0x40FF4E83, 10, 0, 0xF0, -1, 4),
                getX(),
                getY() + getBbHeight() * 0.5D,
                getZ(),
                Math.max((int)(scale * 25.0F), 1),
                scale * 0.5D,
                0.0D,
                scale * 0.5D,
                0.0D);
    }

    private Vec3 getIdlePosition(LivingEntity owner) {
        Vec3 orbit = Vec3.directionFromRotation(0.0F, (float)(this.tickCount * ORBIT_SPEED));
        return owner.position().add(orbit.x(), this.idleHeight, orbit.z());
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

    @Nullable
    private Entity getTarget() {
        int targetId = this.entityData.get(TARGET_ID);
        if (targetId >= 0) {
            Entity target = this.level().getEntity(targetId);
            if (target != null) {
                return target;
            }
        }
        if (this.targetUuid != null && this.level() instanceof ServerLevel serverLevel) {
            Entity target = serverLevel.getEntity(this.targetUuid);
            if (target != null) {
                this.entityData.set(TARGET_ID, target.getId());
                return target;
            }
        }
        return null;
    }

    private float getMaxScale() {
        return this.entityData.get(MAX_SCALE);
    }

    private void setScorchScale(float scale) {
        this.dimensionsScale = Math.max(scale, 0.1F);
        this.entityData.set(SCALE, this.dimensionsScale);
        refreshDimensions();
    }

    private boolean isLaunched() {
        return this.entityData.get(LAUNCHED);
    }

    private boolean isOwnerHoldingShakuton(LivingEntity owner) {
        return owner.getMainHandItem().is(ModItems.SHAKUTON.get()) || owner.getOffhandItem().is(ModItems.SHAKUTON.get());
    }

    private void shoot(Vec3 direction, double speed) {
        if (direction.lengthSqr() <= 1.0E-8D) {
            direction = new Vec3(0.0D, 0.0D, 1.0D);
        }
        this.acceleration = direction.normalize().scale(0.1D);
        this.motionFactor = (float)speed;
        this.entityData.set(LAUNCHED, true);
        faceMotion(direction);
    }

    private void updateLegacyNoGravityMotion() {
        if (this.motionFactor <= 0.0F) {
            return;
        }
        setDeltaMovement(getDeltaMovement().add(this.acceleration).scale(this.motionFactor));
    }

    private void faceMotion(Vec3 motion) {
        if (motion.lengthSqr() <= 1.0E-8D) {
            return;
        }
        setYRot((float)(-Mth.atan2(motion.x(), motion.z()) * Mth.RAD_TO_DEG));
        setXRot((float)(-Mth.atan2(motion.y(), Math.sqrt(motion.x() * motion.x() + motion.z() * motion.z())) * Mth.RAD_TO_DEG));
    }

    private Vec3 centerPosition() {
        return position().add(0.0D, getBbHeight() * 0.5D, 0.0D);
    }
}
