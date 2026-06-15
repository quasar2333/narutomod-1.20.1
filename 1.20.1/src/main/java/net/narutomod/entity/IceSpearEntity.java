package net.narutomod.entity;

import java.util.Comparator;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class IceSpearEntity extends Entity {
    private static final float BASE_WIDTH = 0.5F;
    private static final float BASE_HEIGHT = 1.82F;
    private static final float DEFAULT_SCALE = 1.0F;
    private static final float JUTSU_SCALE = 0.5F;
    private static final float DAMAGE = 10.0F;
    private static final int MAX_IN_GROUND_TIME = 1200;
    private static final float WATER_SLOWDOWN = 0.8F;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(IceSpearEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(IceSpearEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> RAND_YAW = SynchedEntityData.defineId(IceSpearEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> RAND_PITCH = SynchedEntityData.defineId(IceSpearEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUuid;
    private float dimensionsScale = DEFAULT_SCALE;
    private int ticksInAir;
    private int ticksInGround;
    private boolean inGround;
    private Vec3 acceleration = Vec3.ZERO;
    private float motionFactor;

    public IceSpearEntity(EntityType<? extends IceSpearEntity> entityType, Level level) {
        super(entityType, level);
        setRandYawPitch();
    }

    public void configure(LivingEntity owner, Vec3 from, Vec3 to, float speed, float inaccuracy) {
        setOwner(owner);
        setEntityScale(JUTSU_SCALE);
        setNoGravity(true);
        moveTo(from.x(), from.y(), from.z(), owner.getYRot(), owner.getXRot());
        shoot(to.x() - from.x(), to.y() - from.y(), to.z() - from.z(), speed, inaccuracy);
    }

    public static int spawnFrom(LivingEntity owner, float power) {
        if (!(owner.level() instanceof ServerLevel level)) {
            return 0;
        }
        Vec3 look = owner.getLookAngle();
        if (look.lengthSqr() <= 1.0E-8D) {
            return 0;
        }
        look = look.normalize();
        Vec3 origin = owner.getEyePosition().add(look.scale(1.5D));
        int count = Math.max((int)(power * 3.0F), 1);
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            IceSpearEntity spear = ModEntityTypes.ICE_SPEAR.get().create(level);
            if (spear == null) {
                continue;
            }
            Vec3 from = origin.add(
                    owner.getRandom().nextDouble() - 0.5D,
                    owner.getRandom().nextDouble() - 0.5D,
                    owner.getRandom().nextDouble() - 0.5D);
            spear.configure(owner, from, from.add(look), 0.95F, 0.05F);
            level.addFreshEntity(spear);
            level.playSound(null, from.x(), from.y(), from.z(), ModSounds.SOUND_ICE_SHOOT_SMALL.get(),
                    SoundSource.NEUTRAL, 0.8F, level.random.nextFloat() * 0.4F + 0.8F);
            spawned++;
        }
        return spawned;
    }

    public static int spawnAtTarget(LivingEntity owner, LivingEntity target, float power) {
        if (!(owner.level() instanceof ServerLevel level) || !target.isAlive()) {
            return 0;
        }
        Vec3 look = owner.getLookAngle();
        if (look.lengthSqr() <= 1.0E-8D) {
            look = target.getEyePosition().subtract(owner.getEyePosition());
        }
        if (look.lengthSqr() <= 1.0E-8D) {
            return 0;
        }
        look = look.normalize();
        Vec3 origin = owner.getEyePosition().add(look.scale(1.5D));
        Vec3 targetCenter = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ());
        int count = Math.max((int)(power * 3.0F), 1);
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            IceSpearEntity spear = ModEntityTypes.ICE_SPEAR.get().create(level);
            if (spear == null) {
                continue;
            }
            Vec3 from = origin.add(
                    owner.getRandom().nextDouble() - 0.5D,
                    owner.getRandom().nextDouble() - 0.5D,
                    owner.getRandom().nextDouble() - 0.5D);
            spear.configure(owner, from, targetCenter, 0.95F, 0.05F);
            level.addFreshEntity(spear);
            level.playSound(null, from.x(), from.y(), from.z(), ModSounds.SOUND_ICE_SHOOT_SMALL.get(),
                    SoundSource.NEUTRAL, 0.8F, level.random.nextFloat() * 0.4F + 0.8F);
            spawned++;
        }
        return spawned;
    }

    public static boolean spawnShatteredShard(ServerLevel level, double x, double y, double z, double motionX, double motionY, double motionZ) {
        IceSpearEntity shard = ModEntityTypes.ICE_SPEAR.get().create(level);
        if (shard == null) {
            return false;
        }
        shard.setEntityScale(level.random.nextFloat() * 0.5F + 0.05F);
        shard.moveTo(x, y, z, shard.getRandYaw(), shard.getRandPitch());
        shard.setDeltaMovement(motionX, motionY, motionZ);
        return level.addFreshEntity(shard);
    }

    public float getScale() {
        return this.entityData.get(SCALE);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(SCALE, DEFAULT_SCALE);
        this.entityData.define(RAND_YAW, 0.0F);
        this.entityData.define(RAND_PITCH, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.ownerUuid != null && getOwner() == null) {
            discard();
            return;
        }
        if (!this.level().hasChunkAt(blockPosition())) {
            discard();
            return;
        }
        if (this.inGround) {
            if (!this.level().isClientSide && ++this.ticksInGround > MAX_IN_GROUND_TIME) {
                discard();
            }
            return;
        }

        checkTipCollision();
        if (!this.inGround) {
            travelLegacy();
        }
        if (!this.inGround && !isLaunched() && !isNoGravity()) {
            setYRot(getYRot() + getRandYaw());
            setXRot(getXRot() + getRandPitch());
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        this.ticksInAir = tag.getInt("TicksInAir");
        this.ticksInGround = tag.getInt("TicksInGround");
        this.inGround = tag.getBoolean("InGround");
        setEntityScale(tag.contains("Scale") ? tag.getFloat("Scale") : DEFAULT_SCALE);
        this.entityData.set(RAND_YAW, tag.contains("RandYaw") ? tag.getFloat("RandYaw") : 0.0F);
        this.entityData.set(RAND_PITCH, tag.contains("RandPitch") ? tag.getFloat("RandPitch") : 0.0F);
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
        tag.putFloat("Scale", getScale());
        tag.putFloat("RandYaw", getRandYaw());
        tag.putFloat("RandPitch", getRandPitch());
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
            this.dimensionsScale = Mth.clamp(getScale(), 0.05F, 32.0F);
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

    private void setEntityScale(float scale) {
        this.dimensionsScale = Mth.clamp(scale, 0.05F, 32.0F);
        this.entityData.set(SCALE, this.dimensionsScale);
        refreshDimensions();
    }

    private float getRandYaw() {
        return this.entityData.get(RAND_YAW);
    }

    private float getRandPitch() {
        return this.entityData.get(RAND_PITCH);
    }

    private void setRandYawPitch() {
        this.entityData.set(RAND_YAW, (this.random.nextFloat() - 0.5F) * 90.0F);
        this.entityData.set(RAND_PITCH, (this.random.nextFloat() - 0.5F) * 60.0F);
    }

    private boolean isLaunched() {
        return this.motionFactor > 0.0F;
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
        if (isNoGravity()) {
            this.acceleration = direction.scale(0.1D);
        } else {
            setDeltaMovement(direction.scale(speed));
        }
        this.motionFactor = speed;
        faceMotion(direction);
    }

    private void travelLegacy() {
        float factor = this.motionFactor;
        if (factor > 0.0F) {
            this.ticksInAir++;
            Vec3 start = tipPosition();
            Vec3 end = start.add(getDeltaMovement());
            HitResult hit = findImpact(start, end, this.ticksInAir >= 25);
            if (hit.getType() == HitResult.Type.BLOCK) {
                moveToImpact(hit.getLocation());
                setInGround();
                return;
            }
            if (hit.getType() == HitResult.Type.ENTITY && impact(hit)) {
                return;
            }
        }

        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x(), getY() + motion.y(), getZ() + motion.z());
        if (factor > 0.0F && isInWater()) {
            spawnWaterBubbles(motion);
            factor *= WATER_SLOWDOWN;
        }
        if (factor > 0.0F && isNoGravity()) {
            setDeltaMovement(motion.add(this.acceleration).scale(factor));
        }
        if (!isNoGravity()) {
            setDeltaMovement(motion.x() * 0.98D, motion.y() * 0.98D - 0.04D, motion.z() * 0.98D);
            if (factor > 0.0F) {
                faceMotion(getDeltaMovement());
            }
        }
        spawnIceTrail();
    }

    private HitResult findImpact(Vec3 start, Vec3 end, boolean includeOwner) {
        BlockHitResult blockHit = this.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        double maxDistance = blockHit.getType() == HitResult.Type.MISS ? start.distanceTo(end) : start.distanceTo(blockHit.getLocation());
        LivingEntity owner = getOwner();
        AABB search = getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        EntityHitResult entityHit = this.level().getEntities(this, search,
                        target -> target.isAlive() && target.isPickable() && !target.noPhysics
                                && (includeOwner || target != owner))
                .stream()
                .map(entity -> entity.getBoundingBox().clip(start, end)
                        .map(location -> new EntityHitResult(entity, location))
                        .orElse(null))
                .filter(candidate -> candidate != null && start.distanceTo(candidate.getLocation()) <= maxDistance)
                .min(Comparator.comparingDouble(candidate -> start.distanceTo(candidate.getLocation())))
                .orElse(null);
        return entityHit != null ? entityHit : blockHit;
    }

    private boolean impact(HitResult hit) {
        if (!(this.level() instanceof ServerLevel)) {
            return false;
        }
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target) {
            LivingEntity owner = getOwner();
            if (target == owner) {
                return false;
            }
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1, false, false));
            target.invulnerableTime = 0;
            DamageSource source = ModDamageTypes.ninjutsu(this.level(), this, owner);
            target.hurt(source, DAMAGE);
            discard();
            return true;
        }
        return false;
    }

    private Vec3 tipPosition() {
        Vec3 localTip = new Vec3(0.0D, BASE_HEIGHT * this.dimensionsScale, 0.0D);
        float pitch = -getXRot() * Mth.DEG_TO_RAD;
        float yaw = -getYRot() * Mth.DEG_TO_RAD;
        return localTip.xRot(pitch).yRot(yaw).add(position());
    }

    private void checkTipCollision() {
        Vec3 tip = tipPosition();
        BlockPos pos = BlockPos.containing(tip);
        if (!this.level().getBlockState(pos).getCollisionShape(this.level(), pos).isEmpty()) {
            setInGround();
        }
    }

    private void moveToImpact(Vec3 location) {
        Vec3 offset = location.subtract(tipPosition());
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

    private void spawnIceTrail() {
        if (this.motionFactor > 0.0F && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0x70C8F4FF, 8, 8, 0xF0, -1, 4),
                    getX(),
                    getY() + getBbHeight() * 0.5D,
                    getZ(),
                    1,
                    0.08D,
                    0.08D,
                    0.08D,
                    0.0D);
        }
    }

    private void spawnWaterBubbles(Vec3 motion) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.sendParticles(
                net.minecraft.core.particles.ParticleTypes.BUBBLE,
                getX() - motion.x() * 0.25D,
                getY() - motion.y() * 0.25D,
                getZ() - motion.z() * 0.25D,
                4,
                0.0D,
                0.0D,
                0.0D,
                0.0D);
    }

    private void faceMotion(Vec3 motion) {
        if (motion.lengthSqr() <= 1.0E-8D) {
            return;
        }
        setYRot((float)(-Mth.atan2(motion.x(), motion.z()) * Mth.RAD_TO_DEG));
        setXRot((float)(-Mth.atan2(motion.y(), Math.sqrt(motion.x() * motion.x() + motion.z() * motion.z())) * Mth.RAD_TO_DEG) + 90.0F);
        if (this.tickCount == 0) {
            this.yRotO = getYRot();
            this.xRotO = getXRot();
        }
    }
}
