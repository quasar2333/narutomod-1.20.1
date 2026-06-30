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
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class YasakaMagatamaEntity extends Entity {
    private static final int MAX_FLIGHT_TICKS = 100;
    private static final double AOE_RADIUS = 3.0D;
    private static final float SPEED = 0.99F;
    private static final float WATER_SLOWDOWN = 0.8F;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(YasakaMagatamaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(YasakaMagatamaEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(YasakaMagatamaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(YasakaMagatamaEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> LAUNCHED = SynchedEntityData.defineId(YasakaMagatamaEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID ownerUuid;
    private int flightTicks;
    private Vec3 acceleration = Vec3.ZERO;
    private float motionFactor;
    private float dimensionsScale = 1.0F;

    public YasakaMagatamaEntity(EntityType<? extends YasakaMagatamaEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public static boolean spawnFrom(LivingEntity owner, int color, float scale, boolean launchImmediately) {
        return spawnFrom(owner, color, scale, launchImmediately, null);
    }

    public static boolean spawnFrom(LivingEntity owner, int color, float scale, boolean launchImmediately, @Nullable Vec3 direction) {
        YasakaMagatamaEntity entity = ModEntityTypes.YASAKA_MAGATAMA.get().create(owner.level());
        if (entity == null) {
            return false;
        }
        entity.configure(owner, color, scale);
        if (launchImmediately) {
            if (direction == null) {
                entity.launchFromOwnerLook(owner);
            } else {
                entity.shoot(direction.x(), direction.y(), direction.z(), SPEED);
            }
        }
        return owner.level().addFreshEntity(entity);
    }

    public void configure(LivingEntity owner, int color, float scale) {
        setOwner(owner);
        setMagatamaScale(scale);
        setColor(color);
        this.entityData.set(DAMAGE, getMagatamaScale() * 20.0F);
        this.entityData.set(LAUNCHED, false);
        this.flightTicks = 0;
        this.acceleration = Vec3.ZERO;
        this.motionFactor = 0.0F;
        moveToIdlePosition(owner);
    }

    public boolean launchFromOwnerLook() {
        LivingEntity owner = getOwner();
        return owner != null && launchFromOwnerLook(owner);
    }

    public boolean launchFromOwnerLook(LivingEntity owner) {
        Vec3 look = owner.getLookAngle();
        if (look.lengthSqr() <= 1.0E-8D) {
            look = Vec3.directionFromRotation(owner.getXRot(), owner.getYRot());
        }
        return shoot(look.x(), look.y(), look.z(), SPEED);
    }

    public boolean shoot(double x, double y, double z, float speed) {
        Vec3 direction = new Vec3(x, y, z);
        if (direction.lengthSqr() <= 1.0E-8D) {
            direction = Vec3.directionFromRotation(getXRot(), getYRot());
        }
        if (direction.lengthSqr() <= 1.0E-8D) {
            return false;
        }
        this.acceleration = direction.normalize().scale(0.1D);
        this.motionFactor = speed;
        faceMotion(direction);
        this.entityData.set(LAUNCHED, true);
        this.flightTicks = 0;
        return true;
    }

    public float getMagatamaScale() {
        return this.entityData.get(SCALE);
    }

    public int getColor() {
        return this.entityData.get(COLOR);
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
        if (SCALE.equals(key)) {
            this.dimensionsScale = getMagatamaScale();
            refreshDimensions();
        }
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(SCALE, 1.0F);
        this.entityData.define(COLOR, 0xFFFFFFFF);
        this.entityData.define(DAMAGE, 20.0F);
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
        if (!isLaunched()) {
            if (owner == null || !owner.isAlive()) {
                discard();
                return;
            }
            moveToIdlePosition(owner);
        } else {
            this.flightTicks++;
            if (this.flightTicks > MAX_FLIGHT_TICKS) {
                discard();
                return;
            }
            travelAndImpact(owner);
        }
        if (this.tickCount % 12 == 1) {
            this.level().playSound(null, getX(), getY(), getZ(),
                    ModSounds.SOUND_MAGATAMA_SPIN.get(), SoundSource.NEUTRAL, 1.0F, this.random.nextFloat() * 0.4F + 0.8F);
        }
        spawnSmokeParticles();
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
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide) {
            clearSusanooSwingingArms();
        }
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        setMagatamaScale(tag.contains("Scale") ? tag.getFloat("Scale") : 1.0F);
        setColor(tag.contains("Color") ? tag.getInt("Color") : 0xFFFFFFFF);
        this.entityData.set(DAMAGE, tag.contains("Damage") ? tag.getFloat("Damage") : getMagatamaScale() * 20.0F);
        this.entityData.set(LAUNCHED, tag.getBoolean("Launched"));
        this.flightTicks = tag.contains("TicksInAir") ? tag.getInt("TicksInAir") : tag.getInt("FlightTicks");
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
        tag.putFloat("Scale", getMagatamaScale());
        tag.putInt("Color", getColor());
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

    private void setMagatamaScale(float scale) {
        this.dimensionsScale = Mth.clamp(scale, 0.1F, 8.0F);
        this.entityData.set(SCALE, this.dimensionsScale);
        refreshDimensions();
        updateBoundingBox();
    }

    private void setColor(int color) {
        this.entityData.set(COLOR, color);
    }

    private void moveToIdlePosition(LivingEntity owner) {
        Vec3 look = owner.getLookAngle();
        if (look.lengthSqr() <= 1.0E-8D) {
            look = Vec3.directionFromRotation(owner.getXRot(), owner.getYRot());
        }
        Vec3 offset = look.normalize().scale(getMagatamaScale() + 0.5D);
        Vec3 point = owner.getEyePosition().subtract(0.0D, getBbHeight(), 0.0D).add(offset);
        setDeltaMovement(Vec3.ZERO);
        moveTo(point.x(), point.y(), point.z(), yawFromVector(offset), pitchFromVector(offset));
        updateBoundingBox();
    }

    private void travelAndImpact(@Nullable LivingEntity owner) {
        Vec3 start = position().add(0.0D, activeRadius(), 0.0D);
        Vec3 motion = getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-8D) {
            Vec3 end = start.add(motion);
            HitResult hit = findImpact(owner, start, end);
            if (hit.getType() != HitResult.Type.MISS) {
                impact(owner);
                return;
            }
            setPos(getX() + motion.x(), getY() + motion.y(), getZ() + motion.z());
            updateBoundingBox();
        }
        updateLegacyNoGravityMotion();
    }

    private HitResult findImpact(@Nullable LivingEntity owner, Vec3 start, Vec3 end) {
        BlockHitResult blockHit = findLegacyBlockImpact(start, end);
        double maxDistance = blockHit.getType() == HitResult.Type.MISS ? start.distanceTo(end) : start.distanceTo(blockHit.getLocation());
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
                && !(target instanceof YasakaMagatamaEntity);
    }

    private void impact(@Nullable LivingEntity owner) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 point = position();
        if (owner != null) {
            owner.getPersistentData().putDouble(NarutomodModVariables.INVULNERABLE_TIME, 40.0D);
        }
        DamageSource source = ModDamageTypes.ninjutsu(serverLevel, this, owner);
        AABB area = new AABB(point, point).inflate(AOE_RADIUS);
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                target -> target.isAlive() && target != owner && target.distanceToSqr(point) <= AOE_RADIUS * AOE_RADIUS)) {
            target.invulnerableTime = 0;
            target.hurt(source, this.entityData.get(DAMAGE));
        }
        boolean mobGriefing = ForgeEventFactory.getMobGriefingEvent(serverLevel, owner);
        serverLevel.explode(owner, point.x(), point.y(), point.z(), Mth.clamp((int)(getMagatamaScale() * 3.0F), 0, 16),
                mobGriefing, Level.ExplosionInteraction.MOB);
        this.acceleration = Vec3.ZERO;
        this.motionFactor = 0.0F;
        discard();
    }

    private void spawnSmokeParticles() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        float scale = getMagatamaScale();
        serverLevel.sendParticles(
                ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, getColor(), 10 + (int)(scale * 10.0F),
                        Math.max(1, (int)(4.0D / (this.random.nextDouble() * 0.8D + 0.2D))), 0xF0, -1, 4),
                getX(),
                getY() + getBbHeight() * 0.5D,
                getZ(),
                Math.max((int)(scale * 10.0F), 1),
                0.3D * getBbWidth(),
                0.3D * getBbHeight(),
                0.3D * getBbWidth(),
                0.0D);
    }

    private float activeRadius() {
        return Math.max(getMagatamaScale() * 0.5F, 0.1F);
    }

    private void updateBoundingBox() {
        float radius = activeRadius();
        setBoundingBox(new AABB(getX() - radius, getY(), getZ() - radius, getX() + radius, getY() + radius * 2.0D, getZ() + radius));
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

    private void clearSusanooSwingingArms() {
        LivingEntity owner = getOwner();
        if (owner instanceof SusanooClothedEntity susanoo) {
            susanoo.setSwingingArms(false);
        } else if (owner instanceof SusanooWingedEntity susanoo) {
            susanoo.setSwingingArms(false);
        }
    }

    private void faceMotion(Vec3 motion) {
        if (motion.lengthSqr() <= 1.0E-8D) {
            return;
        }
        setYRot(yawFromVector(motion));
        setXRot(pitchFromVector(motion));
    }

    private static float yawFromVector(Vec3 vector) {
        return (float)(-Mth.atan2(vector.x(), vector.z()) * Mth.RAD_TO_DEG);
    }

    private static float pitchFromVector(Vec3 vector) {
        return (float)(-Mth.atan2(vector.y(), Math.sqrt(vector.x() * vector.x() + vector.z() * vector.z())) * Mth.RAD_TO_DEG);
    }
}
