package net.narutomod.entity;

import java.util.Comparator;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class EightyGodsEntity extends Entity {
    private static final int MAX_LIFE = 10;
    private static final float BASE_WIDTH = 0.5F;
    private static final float BASE_HEIGHT = 0.25F;
    private static final float INITIAL_SCALE = 2.0F;
    private static final float DAMAGE = 500.0F;
    private static final float EXPLOSION_STRENGTH = 6.0F;
    private static final float SPEED_FACTOR = 1.25F;
    private static final double ACCELERATION = 0.1D;
    private static final float WATER_SLOWDOWN = 0.8F;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(EightyGodsEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(EightyGodsEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUuid;
    private float dimensionsScale = INITIAL_SCALE;
    private Vec3 acceleration = Vec3.ZERO;
    private float motionFactor;
    private int ticksInAir;

    public EightyGodsEntity(EntityType<? extends EightyGodsEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    public static boolean spawnFrom(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        EightyGodsEntity entity = ModEntityTypes.ENTITY80GODS.get().create(serverLevel);
        if (entity == null) {
            return false;
        }
        entity.configure(owner);
        serverLevel.addFreshEntity(entity);
        serverLevel.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                ModSounds.SOUND_THROWPUNCH.get(), SoundSource.NEUTRAL, 1.0F, owner.getRandom().nextFloat() * 0.6F + 0.6F);
        return true;
    }

    public float getScale() {
        return this.entityData.get(SCALE);
    }

    private void configure(LivingEntity owner) {
        setOwner(owner);
        setScale(INITIAL_SCALE);
        Vec3 look = owner.getLookAngle();
        if (look.lengthSqr() <= 1.0E-8D) {
            look = Vec3.directionFromRotation(owner.getXRot(), owner.getYHeadRot());
        }
        Vec3 spawnOffset = look.normalize().scale(1.8D)
                .yRot((this.random.nextFloat() - 0.5F) * 90.0F * Mth.DEG_TO_RAD)
                .xRot((this.random.nextFloat() - 0.5F) * 60.0F * Mth.DEG_TO_RAD);
        moveTo(
                owner.getX() + spawnOffset.x(),
                owner.getY() + 1.2D + spawnOffset.y(),
                owner.getZ() + spawnOffset.z(),
                owner.getYHeadRot(),
                owner.getXRot());
        shoot(look.x(), look.y(), look.z(), SPEED_FACTOR, 0.1F);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(SCALE, INITIAL_SCALE);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        travelAndImpact();
        if (!isRemoved()) {
            setScale(getScale() + 1.0F);
            if (this.tickCount > MAX_LIFE) {
                discard();
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        setScale(tag.contains("Scale") ? tag.getFloat("Scale") : INITIAL_SCALE);
        if (tag.contains("AccelX")) {
            this.acceleration = new Vec3(tag.getDouble("AccelX"), tag.getDouble("AccelY"), tag.getDouble("AccelZ"));
        }
        this.motionFactor = tag.contains("MotionFactor") ? tag.getFloat("MotionFactor")
                : (this.acceleration.lengthSqr() > 1.0E-8D ? SPEED_FACTOR : 0.0F);
        this.ticksInAir = tag.getInt("TicksInAir");
        if (tag.contains("MotionX")) {
            setDeltaMovement(tag.getDouble("MotionX"), tag.getDouble("MotionY"), tag.getDouble("MotionZ"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putFloat("Scale", getScale());
        tag.putDouble("AccelX", this.acceleration.x());
        tag.putDouble("AccelY", this.acceleration.y());
        tag.putDouble("AccelZ", this.acceleration.z());
        tag.putFloat("MotionFactor", this.motionFactor);
        tag.putInt("TicksInAir", this.ticksInAir);
        Vec3 motion = getDeltaMovement();
        tag.putDouble("MotionX", motion.x());
        tag.putDouble("MotionY", motion.y());
        tag.putDouble("MotionZ", motion.z());
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
            this.dimensionsScale = getScale();
            refreshDimensions();
        }
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean ignoreExplosion() {
        return true;
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
        this.dimensionsScale = Math.max(scale, 0.1F);
        this.entityData.set(SCALE, this.dimensionsScale);
        refreshDimensions();
    }

    private void shoot(double x, double y, double z, float speed, float inaccuracy) {
        Vec3 direction = new Vec3(
                x + this.random.nextGaussian() * inaccuracy,
                y + this.random.nextGaussian() * inaccuracy,
                z + this.random.nextGaussian() * inaccuracy);
        if (direction.lengthSqr() <= 1.0E-8D) {
            direction = Vec3.directionFromRotation(getXRot(), getYRot());
        }
        direction = direction.normalize();
        this.acceleration = direction.scale(ACCELERATION);
        this.motionFactor = speed;
        setDeltaMovement(Vec3.ZERO);
        faceMotion(direction);
    }

    private void travelAndImpact() {
        if (this.motionFactor > 0.0F) {
            this.ticksInAir++;
        }
        Vec3 motion = getDeltaMovement();
        Vec3 start = centerPosition();
        if (motion.lengthSqr() > 1.0E-8D) {
            Vec3 end = start.add(motion);
            HitResult hit = findImpact(start, end);
            if (hit.getType() != HitResult.Type.MISS) {
                impact(hit);
                return;
            }
            setPos(getX() + motion.x(), getY() + motion.y(), getZ() + motion.z());
        }
        updateLegacyNoGravityMotion();
    }

    private HitResult findImpact(Vec3 start, Vec3 end) {
        BlockHitResult blockHit = findLegacyBlockImpact(start, end);
        double maxDistance = blockHit.getType() == HitResult.Type.MISS ? start.distanceTo(end) : start.distanceTo(blockHit.getLocation());
        LivingEntity owner = getOwner();
        Vec3 travel = end.subtract(start);
        AABB search = getBoundingBox().expandTowards(travel).inflate(1.0D);
        EntityHitResult entityHit = this.level().getEntities(this, search,
                        target -> canImpact(owner, target))
                .stream()
                .map(entity -> entity.getBoundingBox().inflate(getBbWidth() * 0.5D, getBbHeight() * 0.5D, getBbWidth() * 0.5D)
                        .clip(start, end)
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
                && !(target instanceof EightyGodsEntity);
    }

    private void impact(HitResult hit) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LivingEntity owner = getOwner();
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target) {
            target.invulnerableTime = 0;
            DamageSource source = ModDamageTypes.ninjutsu(this.level(), this, owner);
            target.hurt(source, DAMAGE);
        }
        serverLevel.explode(owner, getX(), getY(), getZ(), EXPLOSION_STRENGTH, Level.ExplosionInteraction.MOB);
        discard();
    }

    private void updateLegacyNoGravityMotion() {
        if (this.motionFactor <= 0.0F) {
            return;
        }
        float factor = this.isInWater() ? this.motionFactor * WATER_SLOWDOWN : this.motionFactor;
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

    private Vec3 centerPosition() {
        return position().add(0.0D, getBbHeight() * 0.5D, 0.0D);
    }

    private void faceMotion(Vec3 motion) {
        if (motion.lengthSqr() <= 1.0E-8D) {
            return;
        }
        setYRot((float)(-Mth.atan2(motion.x(), motion.z()) * Mth.RAD_TO_DEG));
        setXRot((float)(-Mth.atan2(motion.y(), Math.sqrt(motion.x() * motion.x() + motion.z() * motion.z())) * Mth.RAD_TO_DEG));
        if (this.tickCount == 0) {
            this.yRotO = getYRot();
            this.xRotO = getXRot();
        }
    }
}
