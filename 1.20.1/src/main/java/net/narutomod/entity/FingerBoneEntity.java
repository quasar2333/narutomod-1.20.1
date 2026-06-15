package net.narutomod.entity;

import java.util.Comparator;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.PlayerTracker;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class FingerBoneEntity extends Entity {
    private static final int MAX_LIFE = 100;
    private static final float DAMAGE = 8.0F;
    private static final double SPEED = 0.95D;
    private static final double INACCURACY = 0.05D;
    private static final float ENTITY_SCALE = 0.4F;
    private static final float WATER_SLOWDOWN = 0.8F;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(FingerBoneEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;
    private Vec3 acceleration = Vec3.ZERO;
    private float motionFactor;
    private int ticksInAir;

    public FingerBoneEntity(EntityType<? extends FingerBoneEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        refreshDimensions();
    }

    public static boolean spawnFrom(LivingEntity owner) {
        FingerBoneEntity entity = ModEntityTypes.FINGER_BONE.get().create(owner.level());
        if (entity == null) {
            return false;
        }
        entity.configure(owner);
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                ModSounds.SOUND_BONECRACK.get(), SoundSource.PLAYERS, 0.5F, owner.getRandom().nextFloat() * 0.6F + 0.6F);
        return owner.level().addFreshEntity(entity);
    }

    public void configure(LivingEntity owner) {
        setOwner(owner);
        Vec3 look = owner.getLookAngle();
        if (look.lengthSqr() <= 1.0E-8D) {
            look = Vec3.directionFromRotation(owner.getXRot(), owner.getYRot());
        }
        Vec3 start = owner.position().add(look).add(0.0D, 1.4D, 0.0D);
        moveTo(start.x(), start.y(), start.z(), owner.getYRot(), owner.getXRot());
        setDeltaMovement(look.normalize().scale(0.1D));
        shoot(look.x(), look.y(), look.z(), (float)SPEED, (float)INACCURACY);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(ENTITY_SCALE);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        LivingEntity owner = getOwner();
        if ((owner != null && !owner.isAlive()) || !this.level().hasChunkAt(blockPosition())) {
            discard();
            return;
        }
        travelAndImpact();
        if (!isRemoved() && this.tickCount > MAX_LIFE) {
            discard();
        }
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
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
        this.motionFactor = tag.getFloat("MotionFactor");
        this.acceleration = tag.contains("AccelerationX")
                ? new Vec3(tag.getDouble("AccelerationX"), tag.getDouble("AccelerationY"), tag.getDouble("AccelerationZ"))
                : Vec3.ZERO;
        this.ticksInAir = tag.contains("TicksInAir") ? tag.getInt("TicksInAir") : tag.getInt("FlightTicks");
        if (tag.contains("MotionX")) {
            setDeltaMovement(tag.getDouble("MotionX"), tag.getDouble("MotionY"), tag.getDouble("MotionZ"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putFloat("MotionFactor", this.motionFactor);
        tag.putDouble("AccelerationX", this.acceleration.x());
        tag.putDouble("AccelerationY", this.acceleration.y());
        tag.putDouble("AccelerationZ", this.acceleration.z());
        tag.putInt("FlightTicks", this.ticksInAir);
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

    private void travelAndImpact() {
        Vec3 start = centerPosition();
        Vec3 motion = getDeltaMovement();
        if (this.motionFactor > 0.0F) {
            this.ticksInAir++;
        }
        if (motion.lengthSqr() > 1.0E-8D) {
            Vec3 end = start.add(motion);
            HitResult hit = findImpact(start, end);
            if (hit.getType() != HitResult.Type.MISS && impact(hit)) {
                return;
            }
            setPos(getX() + motion.x(), getY() + motion.y(), getZ() + motion.z());
        }
        updateLegacyNoGravityMotion();
    }

    private HitResult findImpact(Vec3 start, Vec3 end) {
        BlockHitResult blockHit = findLegacyBlockImpact(start, end);
        double maxDistance = blockHit.getType() == HitResult.Type.MISS ? start.distanceTo(end) : start.distanceTo(blockHit.getLocation());
        Entity owner = getOwner();
        AABB search = getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        EntityHitResult entityHit = this.level().getEntities(this, search, target -> canImpact(target, owner))
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

    private boolean canImpact(Entity target, @Nullable Entity owner) {
        if (!target.isAlive() || !target.isPickable() || target.noPhysics || target == this) {
            return false;
        }
        if (target == owner && this.ticksInAir < 25) {
            return false;
        }
        return !(target instanceof FingerBoneEntity bone && isSameOwnerBone(bone));
    }

    private boolean isSameOwnerBone(FingerBoneEntity bone) {
        return this.ownerUuid != null && this.ownerUuid.equals(bone.ownerUuid);
    }

    private boolean impact(HitResult hit) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (hit instanceof BlockHitResult blockHit) {
            serverLevel.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.defaultBlockState()),
                    blockHit.getLocation().x(),
                    blockHit.getLocation().y(),
                    blockHit.getLocation().z(),
                    4,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.15D);
        } else if (hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            Entity owner = getOwner();
            if (target instanceof FingerBoneEntity || target == owner) {
                return false;
            }
            if (target instanceof LivingEntity livingTarget) {
                livingTarget.invulnerableTime = 0;
            }
            DamageSource source = this.damageSources().thrown(this, owner);
            if (target.hurt(source, DAMAGE) && owner instanceof Player player) {
                PlayerTracker.logBattleExp(player, 1.0D);
            }
        }
        this.level().playSound(null, getX(), getY(), getZ(),
                ModSounds.SOUND_BULLET_IMPACT.get(), SoundSource.NEUTRAL, 1.0F, 0.4F + this.random.nextFloat() * 0.6F);
        this.acceleration = Vec3.ZERO;
        this.motionFactor = 0.0F;
        discard();
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

    private void faceMotion(Vec3 motion) {
        if (motion.lengthSqr() <= 1.0E-8D) {
            return;
        }
        setYRot((float)(-Mth.atan2(motion.x(), motion.z()) * Mth.RAD_TO_DEG));
        setXRot(Mth.wrapDegrees((float)(-Mth.atan2(motion.y(), Math.sqrt(motion.x() * motion.x() + motion.z() * motion.z())) * Mth.RAD_TO_DEG + 90.0F)));
        yRotO = getYRot();
        xRotO = getXRot();
    }

    private void shoot(double x, double y, double z, float speed, float inaccuracy) {
        Vec3 direction = new Vec3(
                x + this.random.nextGaussian() * inaccuracy,
                y + this.random.nextGaussian() * inaccuracy,
                z + this.random.nextGaussian() * inaccuracy);
        if (direction.lengthSqr() <= 1.0E-8D) {
            direction = Vec3.directionFromRotation(getXRot(), getYRot());
        }
        if (direction.lengthSqr() <= 1.0E-8D) {
            direction = new Vec3(0.0D, 0.0D, 1.0D);
        }
        this.acceleration = direction.normalize().scale(0.1D);
        this.motionFactor = speed;
        faceMotion(direction);
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

    private Vec3 centerPosition() {
        return position().add(0.0D, getBbHeight() * 0.5D, 0.0D);
    }
}
