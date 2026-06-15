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
import net.narutomod.Chakra;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class TenseiganOrbEntity extends Entity {
    private static final int MAX_LIFE = 100;
    private static final int EXPLOSION_SIZE = 5;
    private static final double AOE_RADIUS = 3.0D;
    private static final float DAMAGE = 30.0F;
    private static final float SCALE = 0.5F;
    private static final float SPEED = 0.95F;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(TenseiganOrbEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;
    private Vec3 acceleration = Vec3.ZERO;
    private float motionFactor;
    private int ticksInAir;

    public TenseiganOrbEntity(EntityType<? extends TenseiganOrbEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        refreshDimensions();
    }

    public static boolean spawnFrom(LivingEntity owner) {
        TenseiganOrbEntity entity = ModEntityTypes.TENSEIGANGUN.get().create(owner.level());
        if (entity == null) {
            return false;
        }
        entity.configure(owner);
        return owner.level().addFreshEntity(entity);
    }

    private void configure(LivingEntity owner) {
        setOwner(owner);
        Vec3 look = owner.getLookAngle();
        if (look.lengthSqr() <= 1.0E-8D) {
            look = Vec3.directionFromRotation(owner.getXRot(), owner.getYHeadRot());
        }
        if (look.lengthSqr() <= 1.0E-8D) {
            look = new Vec3(0.0D, 0.0D, 1.0D);
        }
        Vec3 spawnOffset = look.yRot((this.random.nextFloat() - 0.5F) * 60.0F);
        moveTo(
                owner.getX() + spawnOffset.x(),
                owner.getY() + 1.2D + spawnOffset.y(),
                owner.getZ() + spawnOffset.z(),
                owner.getYRot(),
                owner.getXRot());
        shoot(look.x(), look.y(), look.z(), SPEED);
    }

    public float getOrbScale() {
        return SCALE;
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
        if (this.tickCount == 5) {
            this.level().playSound(null, getX(), getY(), getZ(),
                    ModSounds.SOUND_THROWPUNCH.get(), SoundSource.NEUTRAL, 0.1F, this.random.nextFloat() * 0.6F + 0.5F);
        }
        travelAndImpact();
        if (isRemoved()) {
            return;
        }
        if (this.ticksInAir > MAX_LIFE) {
            discard();
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(SCALE);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        this.ticksInAir = tag.contains("TicksInAir") ? tag.getInt("TicksInAir") : tag.getInt("FlightTicks");
        this.motionFactor = tag.getFloat("MotionFactor");
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
        tag.putInt("FlightTicks", this.ticksInAir);
        tag.putInt("TicksInAir", this.ticksInAir);
        tag.putFloat("MotionFactor", this.motionFactor);
        tag.putDouble("AccelerationX", this.acceleration.x());
        tag.putDouble("AccelerationY", this.acceleration.y());
        tag.putDouble("AccelerationZ", this.acceleration.z());
        Vec3 motion = getDeltaMovement();
        tag.putDouble("MotionX", motion.x());
        tag.putDouble("MotionY", motion.y());
        tag.putDouble("MotionZ", motion.z());
    }

    @Override
    public boolean fireImmune() {
        return true;
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

    private void shoot(double x, double y, double z, float speed) {
        Vec3 direction = new Vec3(x, y, z);
        if (direction.lengthSqr() <= 1.0E-8D) {
            direction = Vec3.directionFromRotation(getXRot(), getYRot());
        }
        direction = direction.normalize();
        this.acceleration = direction.scale(0.1D);
        this.motionFactor = speed;
        faceMotion(direction);
    }

    private void travelAndImpact() {
        Vec3 start = centerPosition();
        Vec3 motion = getDeltaMovement();
        if (this.motionFactor > 0.0F) {
            this.ticksInAir++;
        }
        Vec3 end = start.add(motion);
        HitResult hit = findImpact(start, end);
        if (hit.getType() != HitResult.Type.MISS) {
            impact(hit);
            return;
        }
        setPos(getX() + motion.x(), getY() + motion.y(), getZ() + motion.z());
        updateLegacyNoGravityMotion();
    }

    private HitResult findImpact(Vec3 start, Vec3 end) {
        BlockHitResult blockHit = findLegacyBlockImpact(start, end);
        double maxDistance = blockHit.getType() == HitResult.Type.MISS ? start.distanceTo(end) : start.distanceTo(blockHit.getLocation());
        LivingEntity owner = getOwner();
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

    private boolean canImpact(@Nullable LivingEntity owner, Entity target) {
        if (!target.isAlive() || !target.isPickable() || target.noPhysics || target == owner || target == this) {
            return false;
        }
        return !(target instanceof TenseiganOrbEntity orb && this.ownerUuid != null && this.ownerUuid.equals(orb.ownerUuid));
    }

    private void impact(HitResult hit) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LivingEntity owner = getOwner();
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living) {
            Chakra.pathway(living).consume(1.0D);
        }
        Vec3 center = position();
        serverLevel.explode(owner, center.x(), center.y(), center.z(), EXPLOSION_SIZE, false, Level.ExplosionInteraction.MOB);
        DamageSource source = this.damageSources().indirectMagic(this, owner);
        AABB area = new AABB(center, center).inflate(AOE_RADIUS);
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                target -> target.isAlive() && target != owner && target.distanceToSqr(center) <= AOE_RADIUS * AOE_RADIUS)) {
            target.invulnerableTime = 0;
            target.hurt(source, DAMAGE);
        }
        this.acceleration = Vec3.ZERO;
        this.motionFactor = 0.0F;
        discard();
    }

    private void updateLegacyNoGravityMotion() {
        if (this.motionFactor <= 0.0F) {
            return;
        }
        float factor = this.isInWater() ? this.motionFactor * 0.8F : this.motionFactor;
        setDeltaMovement(getDeltaMovement().add(this.acceleration).scale(factor));
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
