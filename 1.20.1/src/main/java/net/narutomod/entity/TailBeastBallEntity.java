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
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.NarutomodModVariables;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class TailBeastBallEntity extends Entity {
    private static final int BUILDUP_TIME = 100;
    private static final float PROJECTILE_SPEED = 1.05F;
    private static final float WATER_SLOWDOWN = 0.98F;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(TailBeastBallEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> MAX_SCALE = SynchedEntityData.defineId(TailBeastBallEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> CURRENT_SCALE = SynchedEntityData.defineId(TailBeastBallEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(TailBeastBallEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> LAUNCHED = SynchedEntityData.defineId(TailBeastBallEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID ownerUuid;
    private int flightTicks;
    private Vec3 acceleration = Vec3.ZERO;
    private float motionFactor;
    private float dimensionsScale = 0.01F;
    private boolean shooterAiDisabled;
    private boolean ownerAiChanged;

    public TailBeastBallEntity(EntityType<? extends TailBeastBallEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public static boolean spawnFrom(LivingEntity owner, float maxScale, float maxDamage) {
        TailBeastBallEntity entity = ModEntityTypes.TAILBEASTBALL.get().create(owner.level());
        if (entity == null) {
            return false;
        }
        entity.configure(owner, maxScale, maxDamage);
        owner.level().addFreshEntity(entity);
        return true;
    }

    public void configure(LivingEntity owner, float maxScale, float maxDamage) {
        setOwner(owner);
        setMaxScale(maxScale);
        setCurrentScale(0.01F);
        this.entityData.set(DAMAGE, Math.max(maxDamage, 1.0F));
        this.entityData.set(LAUNCHED, false);
        this.flightTicks = 0;
        this.acceleration = Vec3.ZERO;
        this.motionFactor = 0.0F;
        this.shooterAiDisabled = false;
        this.ownerAiChanged = false;
        if (owner instanceof Mob mob) {
            this.shooterAiDisabled = mob.isNoAi();
            if (!this.shooterAiDisabled) {
                mob.setNoAi(true);
                this.ownerAiChanged = true;
            }
        }
        moveToBuildupPosition(owner);
    }

    public float getCurrentScale() {
        return this.entityData.get(CURRENT_SCALE);
    }

    public float getMaxScale() {
        return this.entityData.get(MAX_SCALE);
    }

    public boolean isLaunched() {
        return this.entityData.get(LAUNCHED);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(this.dimensionsScale);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (CURRENT_SCALE.equals(key)) {
            this.dimensionsScale = getCurrentScale();
            refreshDimensions();
        }
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(MAX_SCALE, 1.0F);
        this.entityData.define(CURRENT_SCALE, 0.01F);
        this.entityData.define(DAMAGE, 1.0F);
        this.entityData.define(LAUNCHED, false);
    }

    @Override
    public void tick() {
        super.tick();
        this.clearFire();
        if (this.level().isClientSide) {
            return;
        }

        LivingEntity owner = getOwner();
        if (owner != null && !owner.isAlive()) {
            discard();
            return;
        }
        if (!isLaunched()) {
            tickBuildup(owner);
            return;
        }
        if (!this.level().hasChunkAt(blockPosition())) {
            restoreOwnerAi(owner);
            discard();
            return;
        }
        travelAndImpact();
        spawnFlightParticles();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide) {
            restoreOwnerAi(getOwner());
        }
        super.remove(reason);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        setMaxScale(tag.contains("MaxScale") ? tag.getFloat("MaxScale") : 1.0F);
        setCurrentScale(tag.contains("CurrentScale") ? tag.getFloat("CurrentScale") : 0.01F);
        this.entityData.set(DAMAGE, tag.contains("Damage") ? tag.getFloat("Damage") : 1.0F);
        this.entityData.set(LAUNCHED, tag.getBoolean("Launched"));
        this.flightTicks = tag.contains("TicksInAir") ? tag.getInt("TicksInAir") : tag.getInt("FlightTicks");
        this.motionFactor = tag.getFloat("MotionFactor");
        this.acceleration = tag.contains("AccelerationX")
                ? new Vec3(tag.getDouble("AccelerationX"), tag.getDouble("AccelerationY"), tag.getDouble("AccelerationZ"))
                : Vec3.ZERO;
        if (tag.contains("MotionX")) {
            setDeltaMovement(tag.getDouble("MotionX"), tag.getDouble("MotionY"), tag.getDouble("MotionZ"));
        }
        this.shooterAiDisabled = tag.getBoolean("ShooterAiDisabled");
        this.ownerAiChanged = tag.getBoolean("OwnerAiChanged");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putFloat("MaxScale", getMaxScale());
        tag.putFloat("CurrentScale", getCurrentScale());
        tag.putFloat("Damage", this.entityData.get(DAMAGE));
        tag.putBoolean("Launched", isLaunched());
        tag.putInt("FlightTicks", this.flightTicks);
        tag.putInt("TicksInAir", this.flightTicks);
        tag.putFloat("MotionFactor", this.motionFactor);
        tag.putDouble("AccelerationX", this.acceleration.x());
        tag.putDouble("AccelerationY", this.acceleration.y());
        tag.putDouble("AccelerationZ", this.acceleration.z());
        Vec3 motion = getDeltaMovement();
        tag.putDouble("MotionX", motion.x());
        tag.putDouble("MotionY", motion.y());
        tag.putDouble("MotionZ", motion.z());
        tag.putBoolean("ShooterAiDisabled", this.shooterAiDisabled);
        tag.putBoolean("OwnerAiChanged", this.ownerAiChanged);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void tickBuildup(@Nullable LivingEntity owner) {
        if (owner == null) {
            discard();
            return;
        }
        if (this.tickCount == 1) {
            this.level().playSound(null, getX(), getY(), getZ(),
                    ModSounds.SOUND_BIJUDAMA.get(), SoundSource.NEUTRAL, 10.0F, 1.0F);
        }
        if (this.tickCount <= BUILDUP_TIME) {
            moveToBuildupPosition(owner);
            float scale = Mth.clamp(getMaxScale() * this.tickCount / (float)BUILDUP_TIME, 0.01F, getMaxScale());
            setCurrentScale(scale);
            spawnBuildupParticles();
            return;
        }
        launch(owner);
    }

    private void launch(LivingEntity owner) {
        restoreOwnerAi(owner);
        Vec3 direction;
        if (owner instanceof Mob mob && !this.shooterAiDisabled) {
            LivingEntity target = mob.getTarget();
            if (target == null) {
                discard();
                return;
            }
            direction = new Vec3(
                    target.getX() - getX(),
                    target.getY() - getY() - getBbHeight() * 0.5D,
                    target.getZ() - getZ());
        } else {
            direction = owner.getLookAngle();
            if (direction.lengthSqr() <= 1.0E-8D) {
                direction = Vec3.directionFromRotation(owner.getXRot(), owner.getYRot());
            }
        }
        shoot(direction, PROJECTILE_SPEED);
        this.entityData.set(LAUNCHED, true);
        this.flightTicks = 0;
        this.level().playSound(null, getX(), getY(), getZ(),
                ModSounds.SOUND_NAGIHARAI.get(), SoundSource.NEUTRAL, 10.0F, 1.0F);
    }

    private void moveToBuildupPosition(LivingEntity owner) {
        Vec3 look = Vec3.directionFromRotation(owner.getXRot(), owner.getYHeadRot());
        Vec3 point = owner.getEyePosition().add(look.scale(owner.getBbWidth() * 1.5D));
        setDeltaMovement(Vec3.ZERO);
        moveTo(point.x(), point.y(), point.z(), owner.getYRot(), owner.getXRot());
        updateBoundingBox();
    }

    private void travelAndImpact() {
        Vec3 start = position().add(0.0D, activeRadius(), 0.0D);
        Vec3 motion = getDeltaMovement();
        if (this.motionFactor > 0.0F) {
            this.flightTicks++;
        }
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
                && !(target instanceof TailBeastBallEntity);
    }

    private void impact() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 point = position();
        LivingEntity owner = getOwner();
        if (owner != null) {
            owner.getPersistentData().putDouble(NarutomodModVariables.INVULNERABLE_TIME, 40.0D);
        }
        float radius = impactRadius();
        DamageSource source = ModDamageTypes.ninjutsu(serverLevel, this, owner);
        AABB area = new AABB(point, point).inflate(radius + 2.0D);
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, area, target -> canImpact(owner, target))) {
            target.invulnerableTime = 0;
            target.hurt(source, this.entityData.get(DAMAGE));
        }
        spawnImpactParticles(serverLevel, point, radius);
        serverLevel.explode(owner, point.x(), point.y(), point.z(), Mth.clamp(radius * 0.35F, 1.0F, 8.0F), Level.ExplosionInteraction.MOB);
        this.acceleration = Vec3.ZERO;
        this.motionFactor = 0.0F;
        discard();
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

    private void setMaxScale(float scale) {
        this.entityData.set(MAX_SCALE, Mth.clamp(scale, 0.1F, 32.0F));
    }

    private void setCurrentScale(float scale) {
        this.dimensionsScale = Mth.clamp(scale, 0.01F, 32.0F);
        this.entityData.set(CURRENT_SCALE, this.dimensionsScale);
        refreshDimensions();
        updateBoundingBox();
    }

    private float activeRadius() {
        return Math.max(getCurrentScale() * 0.125F, 0.05F);
    }

    private float impactRadius() {
        return Mth.sqrt(getCurrentScale()) * 6.0F;
    }

    private void updateBoundingBox() {
        float radius = activeRadius();
        setBoundingBox(new AABB(getX() - radius, getY(), getZ() - radius, getX() + radius, getY() + radius * 2.0D, getZ() + radius));
    }

    private void faceMotion(Vec3 motion) {
        if (motion.lengthSqr() <= 1.0E-8D) {
            return;
        }
        Vec3 normalized = motion.normalize();
        setYRot((float)(-Mth.atan2(normalized.x(), normalized.z()) * Mth.RAD_TO_DEG));
        setXRot((float)(Mth.atan2(normalized.y(), Math.sqrt(normalized.x() * normalized.x() + normalized.z() * normalized.z())) * -Mth.RAD_TO_DEG));
    }

    private void shoot(Vec3 direction, float speed) {
        if (direction.lengthSqr() <= 1.0E-8D) {
            return;
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
                    getY() + activeRadius() - motion.y() * 0.25D,
                    getZ() - motion.z() * 0.25D,
                    4,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D);
        }
        setDeltaMovement(getDeltaMovement().add(this.acceleration).scale(factor));
    }

    private void restoreOwnerAi(@Nullable LivingEntity owner) {
        if (this.ownerAiChanged && owner instanceof Mob mob && mob.isNoAi()) {
            mob.setNoAi(false);
        }
        this.ownerAiChanged = false;
    }

    private void spawnBuildupParticles() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (this.tickCount > BUILDUP_TIME - 40) {
            return;
        }
        serverLevel.sendParticles(
                ModParticleTypes.options(
                        NarutoParticleKind.HOMING_ORB,
                        Mth.ceil(getMaxScale() * 0.35F),
                        Math.max(1, (int)(getMaxScale() * 2.2F))),
                getX(),
                getY() + activeRadius(),
                getZ(),
                2,
                0.0D,
                0.0D,
                0.0D,
                0.0D);
    }

    private void spawnFlightParticles() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.sendParticles(
                ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0x90181428, 16, 10, 0xF0, -1, 4),
                getX(),
                getY() + activeRadius(),
                getZ(),
                Mth.clamp((int)(getCurrentScale() * 3.0F), 4, 40),
                activeRadius(),
                activeRadius(),
                activeRadius(),
                0.02D);
    }

    private void spawnImpactParticles(ServerLevel level, Vec3 point, float radius) {
        level.sendParticles(
                ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0xD0101020, 16, 40, 0xF0, -1, 4),
                point.x(),
                point.y(),
                point.z(),
                Mth.clamp((int)(radius * 12.0F), 24, 160),
                radius * 0.35D,
                radius * 0.35D,
                radius * 0.35D,
                0.04D);
    }
}
