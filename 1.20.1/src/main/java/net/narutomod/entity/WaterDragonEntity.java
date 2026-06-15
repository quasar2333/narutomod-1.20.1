package net.narutomod.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class WaterDragonEntity extends Entity {
    public static final int WAIT_TICKS = 60;
    public static final int SPINE_SEGMENTS = 100;
    private static final int MAX_LIFE = 100;
    private static final int TEMPORARY_WATER_LIFE_TICKS = 10;
    private static final float PROJECTILE_SPEED = 0.95F;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(WaterDragonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(WaterDragonEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> LAUNCHED = SynchedEntityData.defineId(WaterDragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IMPACTED = SynchedEntityData.defineId(WaterDragonEntity.class, EntityDataSerializers.BOOLEAN);

    private final List<ProcedureUtils.Vec2f> partRotations = new ArrayList<>();
    @Nullable
    private UUID ownerUuid;
    private double yOrigin;
    @Nullable
    private Vec3 lastSegmentPosition;
    private float previousHeadYaw;
    private float previousHeadPitch;
    private float dimensionsScale = 1.0F;
    private Vec3 acceleration = Vec3.ZERO;
    private float motionFactor;
    private int ticksInAir;
    private final Map<BlockPos, Integer> temporaryWater = new HashMap<>();

    public WaterDragonEntity(EntityType<? extends WaterDragonEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        seedPartRotations();
    }

    public void configure(LivingEntity owner, float power) {
        setOwner(owner);
        setScale(power);
        this.yOrigin = owner.getY();
        this.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
        this.setDeltaMovement(Vec3.ZERO);
    }

    public static boolean spawnFrom(LivingEntity owner, float power) {
        WaterDragonEntity entity = ModEntityTypes.WATER_DRAGON.get().create(owner.level());
        if (entity == null) {
            return false;
        }
        entity.configure(owner, power);
        owner.level().addFreshEntity(entity);
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                ModSounds.SOUND_SUITON_SUIRYUUDAN.get(), SoundSource.NEUTRAL, 5.0F, 1.0F);
        return true;
    }

    public float getScale() {
        return this.entityData.get(SCALE);
    }

    public List<ProcedureUtils.Vec2f> getPartRotations() {
        return this.partRotations;
    }

    public boolean isWaiting() {
        return this.tickCount <= WAIT_TICKS;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(SCALE, 1.0F);
        this.entityData.define(LAUNCHED, false);
        this.entityData.define(IMPACTED, false);
    }

    @Override
    public void tick() {
        super.tick();
        updateVisualSegments();
        if (this.level().isClientSide) {
            return;
        }
        expireTemporaryWater();
        if (hasImpacted()) {
            if (this.temporaryWater.isEmpty()) {
                discard();
            }
            return;
        }
        LivingEntity owner = getOwner();
        if (this.tickCount > MAX_LIFE || owner == null || !owner.isAlive()) {
            discardDragon();
            return;
        }

        if (this.tickCount <= WAIT_TICKS) {
            waitAndAim(owner);
            spawnWaitingParticles();
            return;
        }
        if (!isLaunched()) {
            launch(owner);
            return;
        }
        travelAndImpact();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && reason.shouldDestroy()) {
            clearTemporaryWater();
        }
        super.remove(reason);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 96.0D * getViewScale();
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
            this.dimensionsScale = getScale();
            refreshDimensions();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        setScale(tag.contains("Scale") ? tag.getFloat("Scale") : 1.0F);
        setLaunched(tag.getBoolean("Launched"));
        setImpacted(tag.getBoolean("Impacted"));
        this.yOrigin = tag.getDouble("YOrigin");
        this.motionFactor = tag.getFloat("MotionFactor");
        this.ticksInAir = tag.getInt("TicksInAir");
        this.acceleration = tag.contains("AccelerationX")
                ? new Vec3(tag.getDouble("AccelerationX"), tag.getDouble("AccelerationY"), tag.getDouble("AccelerationZ"))
                : Vec3.ZERO;
        if (tag.contains("MotionX")) {
            setDeltaMovement(tag.getDouble("MotionX"), tag.getDouble("MotionY"), tag.getDouble("MotionZ"));
        }
        this.temporaryWater.clear();
        if (tag.contains("TemporaryWater", 9)) {
            ListTag list = tag.getList("TemporaryWater", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag waterTag = list.getCompound(i);
                BlockPos pos = new BlockPos(waterTag.getInt("X"), waterTag.getInt("Y"), waterTag.getInt("Z"));
                this.temporaryWater.put(pos, waterTag.getInt("ExpiresAt"));
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putFloat("Scale", getScale());
        tag.putBoolean("Launched", isLaunched());
        tag.putBoolean("Impacted", hasImpacted());
        tag.putDouble("YOrigin", this.yOrigin);
        tag.putFloat("MotionFactor", this.motionFactor);
        tag.putInt("TicksInAir", this.ticksInAir);
        tag.putDouble("AccelerationX", this.acceleration.x());
        tag.putDouble("AccelerationY", this.acceleration.y());
        tag.putDouble("AccelerationZ", this.acceleration.z());
        Vec3 motion = getDeltaMovement();
        tag.putDouble("MotionX", motion.x());
        tag.putDouble("MotionY", motion.y());
        tag.putDouble("MotionZ", motion.z());
        ListTag list = new ListTag();
        for (Map.Entry<BlockPos, Integer> entry : this.temporaryWater.entrySet()) {
            CompoundTag waterTag = new CompoundTag();
            waterTag.putInt("X", entry.getKey().getX());
            waterTag.putInt("Y", entry.getKey().getY());
            waterTag.putInt("Z", entry.getKey().getZ());
            waterTag.putInt("ExpiresAt", entry.getValue());
            list.add(waterTag);
        }
        tag.put("TemporaryWater", list);
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

    private void setScale(float scale) {
        this.dimensionsScale = Mth.clamp(scale, 0.1F, 16.0F);
        this.entityData.set(SCALE, this.dimensionsScale);
        refreshDimensions();
    }

    private void seedPartRotations() {
        this.partRotations.add(new ProcedureUtils.Vec2f(0.0F, 0.0F));
        this.partRotations.add(new ProcedureUtils.Vec2f(0.0F, 30.0F));
        this.partRotations.add(new ProcedureUtils.Vec2f(0.0F, 30.0F));
        this.partRotations.add(new ProcedureUtils.Vec2f(0.0F, 30.0F));
        this.partRotations.add(new ProcedureUtils.Vec2f(0.0F, 30.0F));
        this.partRotations.add(new ProcedureUtils.Vec2f(0.0F, -15.0F));
        this.partRotations.add(new ProcedureUtils.Vec2f(0.0F, -15.0F));
        this.partRotations.add(new ProcedureUtils.Vec2f(0.0F, 0.0F));
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
        float segmentLength = Math.max(getScale() * 11.0F * 0.0625F, 0.05F);
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

    private boolean isLaunched() {
        return this.entityData.get(LAUNCHED);
    }

    private void setLaunched(boolean launched) {
        this.entityData.set(LAUNCHED, launched);
    }

    private boolean hasImpacted() {
        return this.entityData.get(IMPACTED);
    }

    private void setImpacted(boolean impacted) {
        this.entityData.set(IMPACTED, impacted);
    }

    private void waitAndAim(LivingEntity owner) {
        Vec3 target = findAimPoint(owner);
        Vec3 direction = target.subtract(position());
        faceDirection(direction, owner);
        if (this.tickCount <= WAIT_TICKS / 2) {
            setPos(getX(), getY() + 3.0D * getScale() / WAIT_TICKS * 2.0D, getZ());
        }
        setDeltaMovement(Vec3.ZERO);
    }

    private void launch(LivingEntity owner) {
        Vec3 target = findAimPoint(owner);
        Vec3 direction = target.subtract(position());
        if (direction.lengthSqr() <= 1.0E-8D) {
            direction = owner.getLookAngle();
        }
        this.acceleration = direction.normalize().scale(0.1D);
        this.motionFactor = PROJECTILE_SPEED;
        faceDirection(direction, owner);
        setLaunched(true);
    }

    private Vec3 findAimPoint(LivingEntity owner) {
        if (owner instanceof Mob mob && mob.getTarget() != null) {
            return mob.getTarget().position().add(0.0D, mob.getTarget().getBbHeight() * 0.5D, 0.0D);
        }
        HitResult hit = ProcedureUtils.objectEntityLookingAt(owner, 50.0D, 0.0D, true, false, target -> target != this && target != owner);
        return hit.getType() == HitResult.Type.MISS
                ? owner.getEyePosition().add(owner.getLookAngle().scale(50.0D))
                : hit.getLocation();
    }

    private void travelAndImpact() {
        if (this.motionFactor > 0.0F) {
            this.ticksInAir++;
        }
        Vec3 start = centerPosition();
        Vec3 motion = getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-8D) {
            Vec3 end = start.add(motion);
            HitResult hit = findImpact(start, end);
            if (hit.getType() != HitResult.Type.MISS) {
                impact();
                return;
            }
            setPos(getX() + motion.x(), getY() + motion.y(), getZ() + motion.z());
        }
        updateLegacyNoGravityMotion();
        spawnLaunchedParticles();
    }

    private void updateLegacyNoGravityMotion() {
        if (this.motionFactor > 0.0F) {
            float factor = this.isInWater() ? this.motionFactor * 0.8F : this.motionFactor;
            if (this.isInWater() && this.level() instanceof ServerLevel serverLevel) {
                Vec3 motion = getDeltaMovement();
                serverLevel.sendParticles(
                        ParticleTypes.BUBBLE,
                        getX() - motion.x() * 0.25D,
                        getY() + getBbHeight() * 0.5D - motion.y() * 0.25D,
                        getZ() - motion.z() * 0.25D,
                        4,
                        0.0D,
                        0.0D,
                        0.0D,
                        0.0D);
            }
            setDeltaMovement(getDeltaMovement().add(this.acceleration).scale(factor));
        }
    }

    private HitResult findImpact(Vec3 start, Vec3 end) {
        BlockHitResult blockHit = findLegacyBlockImpact(start, end);
        double maxDistance = blockHit.getType() == HitResult.Type.MISS ? start.distanceTo(end) : start.distanceTo(blockHit.getLocation());
        LivingEntity owner = getOwner();
        Vec3 travel = end.subtract(start);
        EntityHitResult entityHit = this.level().getEntities(this, getBoundingBox().expandTowards(travel).inflate(1.0D),
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

    private boolean canImpact(@Nullable LivingEntity owner, Entity target) {
        return target.isAlive()
                && target.isPickable()
                && !target.noPhysics
                && target != owner
                && target != this
                && target.getRootVehicle() != (owner == null ? null : owner.getRootVehicle())
                && !(target instanceof WaterDragonEntity);
    }

    private void impact() {
        Vec3 point = position();
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            discardDragon();
            return;
        }
        LivingEntity owner = getOwner();
        float scale = getScale();
        serverLevel.explode(owner, point.x(), point.y(), point.z(), 5.0F * scale, Level.ExplosionInteraction.MOB);
        DamageSource source = ModDamageTypes.ninjutsu(serverLevel, this, owner);
        AABB damageBox = new AABB(point, point).inflate(3.0D);
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, damageBox, target -> canImpact(owner, target))) {
            target.hurt(source, 20.0F * scale);
        }
        setImpacted(true);
        setDeltaMovement(Vec3.ZERO);
        this.acceleration = Vec3.ZERO;
        this.motionFactor = 0.0F;
        setPos(point.x(), point.y(), point.z());
        placeTemporaryImpactWater(serverLevel, point);
        if (this.temporaryWater.isEmpty()) {
            discard();
        }
    }

    private void placeTemporaryImpactWater(ServerLevel level, Vec3 point) {
        AABB box = this.getBoundingBox().move(point.subtract(position())).contract(0.0D, Math.max(getBbHeight() - 1.0D, 0.0D), 0.0D);
        BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
        BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
        for (BlockPos mutablePos : BlockPos.betweenClosed(min, max)) {
            BlockPos pos = mutablePos.immutable();
            if (level.getBlockState(pos).isAir()) {
                level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
                this.temporaryWater.put(pos, this.tickCount + TEMPORARY_WATER_LIFE_TICKS);
            }
        }
    }

    private void expireTemporaryWater() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        this.temporaryWater.entrySet().removeIf(entry -> {
            if (entry.getValue() > this.tickCount) {
                return false;
            }
            removeTemporaryWater(level, entry.getKey());
            return true;
        });
    }

    private void clearTemporaryWater() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        for (BlockPos pos : this.temporaryWater.keySet()) {
            removeTemporaryWater(level, pos);
        }
        this.temporaryWater.clear();
    }

    private void removeTemporaryWater(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).is(Blocks.WATER)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private void spawnWaitingParticles() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        double yaw = Math.toRadians(-getYRot());
        double x = Math.sin(yaw) * -1.75D * getScale() + getX();
        double z = Math.cos(yaw) * -1.75D * getScale() + getZ();
        int count = Math.max(this.tickCount * 5, 1);
        level.sendParticles(ParticleTypes.SPLASH, x, this.yOrigin, z, count,
                getBbWidth() * 0.5D, getBbHeight() * 0.5D, getBbWidth() * 0.5D, 0.05D);
    }

    private void spawnLaunchedParticles() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        level.sendParticles(ParticleTypes.FALLING_WATER, getX(), getY() + getBbHeight() * 0.5D, getZ(), 200,
                getBbWidth() * 0.5D, getBbHeight() * 0.5D, getBbWidth() * 0.5D, 0.0D);
    }

    private double collisionRadius() {
        return Math.max(0.5D, getScale() * 0.5D);
    }

    private void faceDirection(Vec3 direction, @Nullable LivingEntity owner) {
        if (direction.lengthSqr() <= 1.0E-8D) {
            if (owner != null) {
                setYRot(owner.getYRot());
                setXRot(owner.getXRot());
            }
            return;
        }
        setYRot((float)(-Mth.atan2(direction.x(), direction.z()) * Mth.RAD_TO_DEG));
        setXRot((float)(-Mth.atan2(direction.y(), Math.sqrt(direction.x() * direction.x() + direction.z() * direction.z())) * Mth.RAD_TO_DEG));
    }

    private Vec3 centerPosition() {
        return position().add(0.0D, getBbHeight() * 0.5D, 0.0D);
    }

    private void discardDragon() {
        clearTemporaryWater();
        discard();
    }
}
