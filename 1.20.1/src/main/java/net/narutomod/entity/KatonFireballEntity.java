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
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.NarutomodModVariables;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class KatonFireballEntity extends Entity {
    private static final int TIME_TO_FULL_SCALE = 20;
    private static final int MAX_LIFE = 100;
    private static final float BASE_SIZE = 0.8F;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(KatonFireballEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> FULL_SCALE = SynchedEntityData.defineId(KatonFireballEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> CURRENT_SCALE = SynchedEntityData.defineId(KatonFireballEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(KatonFireballEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUuid;
    private Vec3 acceleration = Vec3.ZERO;
    private float motionFactor;
    private int ticksInAir;

    public KatonFireballEntity(EntityType<? extends KatonFireballEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    public void configure(LivingEntity owner, float fullScale) {
        configure(owner, owner.getLookAngle(), fullScale);
    }

    public void configure(LivingEntity owner, Vec3 direction, float fullScale) {
        setOwner(owner);
        float safeScale = Math.max(fullScale, 0.1F);
        setFullScale(safeScale);
        setCurrentScale(1.0F);
        this.entityData.set(DAMAGE, safeScale * 10.0F);
        Vec3 look = direction.lengthSqr() <= 1.0E-8D ? owner.getLookAngle() : direction.normalize();
        this.moveTo(
                owner.getX() + look.x(),
                owner.getY() + 1.2D + look.y(),
                owner.getZ() + look.z(),
                owner.getYRot(),
                owner.getXRot());
        shoot(look.x(), look.y(), look.z(), 0.95F);
    }

    public float getCurrentScale() {
        return this.entityData.get(CURRENT_SCALE);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(FULL_SCALE, 1.0F);
        this.entityData.define(CURRENT_SCALE, 1.0F);
        this.entityData.define(DAMAGE, 10.0F);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (CURRENT_SCALE.equals(key)) {
            refreshDimensions();
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float size = BASE_SIZE * getCurrentScale();
        return EntityDimensions.fixed(size, size);
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
        if (isRemoved()) {
            return;
        }
        if (this.ticksInAir > MAX_LIFE || isInWater()) {
            discard();
            return;
        }
        if (this.tickCount <= TIME_TO_FULL_SCALE) {
            float progress = this.tickCount / (float)TIME_TO_FULL_SCALE;
            setCurrentScale(1.0F + (getFullScale() - 1.0F) * progress);
        }
        playFireSound();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        setFullScale(tag.contains("FullScale") ? tag.getFloat("FullScale") : 1.0F);
        setCurrentScale(tag.contains("CurrentScale") ? tag.getFloat("CurrentScale") : 1.0F);
        this.entityData.set(DAMAGE, tag.contains("Damage") ? tag.getFloat("Damage") : getFullScale() * 10.0F);
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
        tag.putFloat("FullScale", getFullScale());
        tag.putFloat("CurrentScale", getCurrentScale());
        tag.putFloat("Damage", this.entityData.get(DAMAGE));
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

    private float getFullScale() {
        return this.entityData.get(FULL_SCALE);
    }

    private void setFullScale(float fullScale) {
        this.entityData.set(FULL_SCALE, Math.max(fullScale, 0.1F));
    }

    private void setCurrentScale(float currentScale) {
        this.entityData.set(CURRENT_SCALE, Math.max(currentScale, 0.1F));
        refreshDimensions();
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
        spawnFlameParticles();
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
        return target.isAlive()
                && target.isPickable()
                && !target.noPhysics
                && target != this
                && target != owner
                && !(target instanceof KatonFireballEntity);
    }

    private void impact(HitResult hit) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LivingEntity owner = getOwner();
        if (owner != null) {
            owner.getPersistentData().putDouble(NarutomodModVariables.INVULNERABLE_TIME, 40.0D);
        }
        if (hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            DamageSource source = ModDamageTypes.ninjutsuFire(this.level(), this, owner);
            target.hurt(source, this.entityData.get(DAMAGE));
            target.setSecondsOnFire(10);
        }
        int explosionSize = Math.max((int)getFullScale() - 1, 0);
        boolean mobGriefing = ForgeEventFactory.getMobGriefingEvent(serverLevel, owner);
        serverLevel.explode(owner, getX(), getY(), getZ(), explosionSize, mobGriefing, Level.ExplosionInteraction.NONE);
        this.acceleration = Vec3.ZERO;
        this.motionFactor = 0.0F;
        discard();
    }

    private void updateLegacyNoGravityMotion() {
        if (this.motionFactor <= 0.0F) {
            return;
        }
        float factor = this.motionFactor;
        if (isInWater()) {
            factor *= 0.8F;
        }
        setDeltaMovement(getDeltaMovement().add(this.acceleration).scale(factor));
    }

    private void spawnFlameParticles() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int count = Math.max((int)getFullScale() * 2, 1);
        int color = 0xFFFF0000 | ((0x40 + this.random.nextInt(0x80)) << 8);
        serverLevel.sendParticles(
                ModParticleTypes.options(NarutoParticleKind.FLAME_COLORED, color, 30),
                getX(),
                getY() + getBbHeight() * 0.5D,
                getZ(),
                count,
                0.3D * getBbWidth(),
                0.3D * getBbHeight(),
                0.3D * getBbWidth(),
                0.0D);
    }

    private void playFireSound() {
        if (this.random.nextFloat() <= 0.2F) {
            this.level().playSound(null, getX(), getY(), getZ(),
                    ModSounds.SOUND_FLAMETHROW.get(), SoundSource.NEUTRAL, 1.0F, this.random.nextFloat() * 0.5F + 0.6F);
        }
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
