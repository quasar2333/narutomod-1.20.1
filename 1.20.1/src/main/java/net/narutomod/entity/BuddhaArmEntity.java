package net.narutomod.entity;

import java.util.Comparator;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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

public final class BuddhaArmEntity extends Entity {
    private static final float MODEL_SCALE = 20.0F;
    private static final float WIDTH = 0.25F * MODEL_SCALE;
    private static final float IMPACT_RADIUS = 0.25F * MODEL_SCALE;
    private static final float EXPLOSION_STRENGTH = 6.0F;
    private static final float WATER_SLOWDOWN = 0.8F;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(BuddhaArmEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> GROW = SynchedEntityData.defineId(BuddhaArmEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(BuddhaArmEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUuid;
    private Vec3 acceleration = Vec3.ZERO;
    private float motionFactor;

    public BuddhaArmEntity(EntityType<? extends BuddhaArmEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public static boolean spawnFrom(LivingEntity owner, float damage, boolean grow) {
        Vec3 look = owner.getLookAngle();
        if (look.lengthSqr() <= 1.0E-8D) {
            look = Vec3.directionFromRotation(owner.getXRot(), owner.getYRot());
        }
        Vec3 start = owner.getEyePosition().add(look.normalize().scale(owner.getBbWidth() + WIDTH * 0.5D));
        return spawnFrom(owner, start, look, damage, grow, grow ? 1.15F : 1.2F, true);
    }

    public static boolean spawnFrom(
            LivingEntity owner,
            Vec3 start,
            Vec3 direction,
            float damage,
            boolean grow,
            float speed,
            boolean playSound) {
        BuddhaArmEntity entity = ModEntityTypes.BUDDHA_ARM.get().create(owner.level());
        if (entity == null) {
            return false;
        }
        if (direction.lengthSqr() <= 1.0E-8D) {
            direction = Vec3.directionFromRotation(owner.getXRot(), owner.getYRot());
        }
        entity.configure(owner, start, damage, grow);
        entity.shoot(direction.x(), direction.y(), direction.z(), speed);
        if (playSound) {
            owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                    ModSounds.SOUND_WOODSPAWN.get(), SoundSource.NEUTRAL, 2.0F, owner.getRandom().nextFloat() * 0.6F + 0.6F);
        }
        return owner.level().addFreshEntity(entity);
    }

    public void configure(LivingEntity owner, Vec3 position, float damage, boolean grow) {
        setOwner(owner);
        this.entityData.set(GROW, grow);
        this.entityData.set(DAMAGE, Math.max(damage, 1.0F));
        this.acceleration = Vec3.ZERO;
        this.motionFactor = 0.0F;
        moveTo(position.x(), position.y() - WIDTH * 0.5D, position.z(), owner.getYRot(), owner.getXRot());
        updateBoundingBox();
    }

    public boolean shouldGrow() {
        return this.entityData.get(GROW);
    }

    public float getRenderLength() {
        float speedLength = (float)getDeltaMovement().length() * 10.0F;
        float oldModelLen = shouldGrow() ? Math.max(speedLength, 8.0F) : 8.0F;
        return oldModelLen * 0.0625F * MODEL_SCALE;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(GROW, true);
        this.entityData.define(DAMAGE, 100.0F);
    }

    @Override
    public void tick() {
        super.tick();
        this.clearFire();
        if (this.level().isClientSide) {
            return;
        }
        if (this.tickCount > (shouldGrow() ? 100 : 20)) {
            discard();
            return;
        }
        travelAndImpact();
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean ignoreExplosion() {
        return !shouldGrow();
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
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        this.entityData.set(GROW, !tag.contains("Grow") || tag.getBoolean("Grow"));
        this.entityData.set(DAMAGE, tag.contains("Damage") ? tag.getFloat("Damage") : 100.0F);
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
        tag.putBoolean("Grow", shouldGrow());
        tag.putFloat("Damage", this.entityData.get(DAMAGE));
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
        this.acceleration = direction.normalize().scale(0.1D);
        this.motionFactor = speed;
        faceMotion(direction);
    }

    private void travelAndImpact() {
        Vec3 start = position().add(0.0D, WIDTH * 0.5D, 0.0D);
        Vec3 motion = getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-8D) {
            Vec3 end = start.add(motion);
            HitResult hit = findImpact(start, end);
            if (hit.getType() != HitResult.Type.MISS) {
                impact();
                return;
            }
            setPos(getX() + motion.x(), getY() + motion.y(), getZ() + motion.z());
            updateBoundingBox();
        }
        updateLegacyNoGravityMotion();
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
                && target != this
                && target != owner
                && (owner == null || target.getRootVehicle() != owner)
                && !(target instanceof BuddhaArmEntity arm && arm.ownerUuid != null && arm.ownerUuid.equals(this.ownerUuid));
    }

    private void impact() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 point = position();
        LivingEntity owner = getOwner();
        DamageSource source = ModDamageTypes.senjutsu(serverLevel, this, owner);
        AABB area = new AABB(point, point).inflate(IMPACT_RADIUS);
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                target -> target.isAlive() && target != owner && target.distanceToSqr(point) <= IMPACT_RADIUS * IMPACT_RADIUS)) {
            target.invulnerableTime = 0;
            target.hurt(source, this.entityData.get(DAMAGE));
        }
        serverLevel.explode(owner, point.x(), point.y(), point.z(), EXPLOSION_STRENGTH, Level.ExplosionInteraction.MOB);
        this.acceleration = Vec3.ZERO;
        this.motionFactor = 0.0F;
        discard();
    }

    private void updateBoundingBox() {
        float radius = WIDTH * 0.5F;
        setBoundingBox(new AABB(getX() - radius, getY(), getZ() - radius, getX() + radius, getY() + WIDTH, getZ() + radius));
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
                    getY() + WIDTH * 0.5D - motion.y() * 0.25D,
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
        setXRot((float)(-Mth.atan2(motion.y(), Math.sqrt(motion.x() * motion.x() + motion.z() * motion.z())) * Mth.RAD_TO_DEG));
    }
}
