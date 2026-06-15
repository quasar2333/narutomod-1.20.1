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
import net.narutomod.block.LightSourceBlock;
import net.narutomod.event.SpecialEvent;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureSync;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class RasenshurikenEntity extends Entity {
    private static final float BASE_WIDTH = 2.5F;
    private static final float BASE_HEIGHT = 0.5F;
    private static final float IMPACT_BASE_SIZE = 0.5F;
    private static final int GROW_TIME = 20;
    private static final int MAX_AIR_TICKS = 200;
    private static final int MAX_IMPACT_TICKS = 200;
    private static final float PROJECTILE_SPEED = 0.95F;
    private static final float WATER_SLOWDOWN = 0.8F;
    private static final float DEFAULT_IMPACT_DAMAGE_MULTIPLIER = 2.0F;
    private static final float TSB_IMPACT_DAMAGE_MULTIPLIER = 8.0F;
    private static final int TSB_BALL_COLOR = 0xE0101010;
    private static final int DEFAULT_BALL_COLOR = 0x20A9DEFF;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(RasenshurikenEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> FULL_SCALE = SynchedEntityData.defineId(RasenshurikenEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> CURRENT_SCALE = SynchedEntityData.defineId(RasenshurikenEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> IMPACT_TICKS = SynchedEntityData.defineId(RasenshurikenEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BALL_COLOR = SynchedEntityData.defineId(RasenshurikenEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private Vec3 impactPoint;
    private boolean forceBowPoseSynced;
    private boolean launched;
    private Vec3 acceleration = Vec3.ZERO;
    private float motionFactor;
    private int ticksInAir;
    private boolean impactDimensions;
    private float dimensionsScale = 0.1F;
    private float impactDamageMultiplier = DEFAULT_IMPACT_DAMAGE_MULTIPLIER;

    public RasenshurikenEntity(EntityType<? extends RasenshurikenEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    public void configure(LivingEntity owner, float fullScale) {
        setOwner(owner);
        setFullScale(fullScale);
        setCurrentScale(0.1F);
        setImpactTicks(0);
        this.entityData.set(BALL_COLOR, DEFAULT_BALL_COLOR);
        this.impactDamageMultiplier = DEFAULT_IMPACT_DAMAGE_MULTIPLIER;
        this.forceBowPoseSynced = false;
        this.launched = false;
        this.acceleration = Vec3.ZERO;
        this.motionFactor = 0.0F;
        this.ticksInAir = 0;
        this.impactDimensions = false;
        this.impactPoint = null;
        setDeltaMovement(Vec3.ZERO);
        moveAboveOwner(owner);
    }

    public static boolean spawnFrom(LivingEntity owner, float power) {
        if (power < 0.1F) {
            return false;
        }
        RasenshurikenEntity entity = ModEntityTypes.RASENSHURIKEN.get().create(owner.level());
        if (entity == null) {
            return false;
        }
        entity.configure(owner, power);
        owner.level().addFreshEntity(entity);
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                ModSounds.SOUND_RASENSHURIKEN.get(), SoundSource.PLAYERS, 5.0F, 1.0F);
        return true;
    }

    public static boolean spawnTruthSeekingVariantFrom(LivingEntity owner) {
        RasenshurikenEntity entity = ModEntityTypes.RASENSHURIKEN.get().create(owner.level());
        if (entity == null) {
            return false;
        }
        entity.configure(owner, 4.0F);
        entity.entityData.set(BALL_COLOR, TSB_BALL_COLOR);
        entity.impactDamageMultiplier = TSB_IMPACT_DAMAGE_MULTIPLIER;
        owner.level().addFreshEntity(entity);
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                ModSounds.SOUND_RASENSHURIKEN.get(), SoundSource.PLAYERS, 5.0F, 1.0F);
        return true;
    }

    public float getCurrentScale() {
        return this.entityData.get(CURRENT_SCALE);
    }

    public float getFullScale() {
        return this.entityData.get(FULL_SCALE);
    }

    public int getImpactTicks() {
        return this.entityData.get(IMPACT_TICKS);
    }

    public int getBallColor() {
        return this.entityData.get(BALL_COLOR);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float width = this.impactDimensions ? IMPACT_BASE_SIZE : BASE_WIDTH;
        return EntityDimensions.fixed(width * this.dimensionsScale, BASE_HEIGHT * this.dimensionsScale);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (CURRENT_SCALE.equals(key)) {
            this.dimensionsScale = getCurrentScale();
            refreshDimensions();
        } else if (IMPACT_TICKS.equals(key)) {
            this.impactDimensions = getImpactTicks() > 0;
            refreshDimensions();
        }
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(FULL_SCALE, 1.0F);
        this.entityData.define(CURRENT_SCALE, 0.1F);
        this.entityData.define(IMPACT_TICKS, 0);
        this.entityData.define(BALL_COLOR, DEFAULT_BALL_COLOR);
    }

    @Override
    public void tick() {
        super.tick();
        this.clearFire();
        if (this.level().isClientSide) {
            return;
        }
        if (getImpactTicks() > 0) {
            tickImpact();
            return;
        }
        LivingEntity owner = getOwner();
        if (owner != null && !owner.isAlive()) {
            discardRasenshuriken();
            return;
        }
        if (owner == null && !this.launched) {
            discardRasenshuriken();
            return;
        }
        if (owner != null) {
            syncForceBowPose(owner);
        }

        if (this.tickCount < GROW_TIME && owner != null) {
            growTowardFullScale();
            moveAboveOwner(owner);
        } else {
            setCurrentScale(getFullScale());
            if (this.launched) {
                travelAndImpact();
                if (getImpactTicks() > 0) {
                    return;
                }
            }
            if (owner != null && this.distanceTo(owner) < 48.0F) {
                guideTowardOwnerLook(owner);
            }
        }

        refreshLegacyLightSource();
        playWindLoop();
        spawnChargingSmoke();
        if (this.ticksInAir > MAX_AIR_TICKS) {
            discardRasenshuriken();
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        clearForceBowPose();
        super.remove(reason);
    }

    @Override
    public boolean fireImmune() {
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
        setFullScale(tag.contains("FullScale") ? tag.getFloat("FullScale") : 1.0F);
        setCurrentScale(tag.contains("CurrentScale") ? tag.getFloat("CurrentScale") : 0.1F);
        setImpactTicks(tag.getInt("ImpactTicks"));
        this.entityData.set(BALL_COLOR, tag.contains("BallColor") ? tag.getInt("BallColor") : DEFAULT_BALL_COLOR);
        this.impactDamageMultiplier = tag.contains("ImpactDamageMultiplier")
                ? tag.getFloat("ImpactDamageMultiplier")
                : DEFAULT_IMPACT_DAMAGE_MULTIPLIER;
        this.forceBowPoseSynced = tag.getBoolean("ForceBowPoseSynced");
        this.launched = tag.getBoolean("Launched");
        this.ticksInAir = tag.getInt("TicksInAir");
        this.motionFactor = tag.getFloat("MotionFactor");
        this.acceleration = tag.contains("AccelerationX")
                ? new Vec3(tag.getDouble("AccelerationX"), tag.getDouble("AccelerationY"), tag.getDouble("AccelerationZ"))
                : Vec3.ZERO;
        setDeltaMovement(tag.getDouble("MotionX"), tag.getDouble("MotionY"), tag.getDouble("MotionZ"));
        if (tag.contains("ImpactX") && tag.contains("ImpactY") && tag.contains("ImpactZ")) {
            this.impactPoint = new Vec3(tag.getDouble("ImpactX"), tag.getDouble("ImpactY"), tag.getDouble("ImpactZ"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putFloat("FullScale", getFullScale());
        tag.putFloat("CurrentScale", getCurrentScale());
        tag.putInt("ImpactTicks", getImpactTicks());
        tag.putInt("BallColor", this.entityData.get(BALL_COLOR));
        tag.putFloat("ImpactDamageMultiplier", this.impactDamageMultiplier);
        tag.putBoolean("ForceBowPoseSynced", this.forceBowPoseSynced);
        tag.putBoolean("Launched", this.launched);
        tag.putInt("TicksInAir", this.ticksInAir);
        tag.putFloat("MotionFactor", this.motionFactor);
        tag.putDouble("AccelerationX", this.acceleration.x());
        tag.putDouble("AccelerationY", this.acceleration.y());
        tag.putDouble("AccelerationZ", this.acceleration.z());
        Vec3 motion = getDeltaMovement();
        tag.putDouble("MotionX", motion.x());
        tag.putDouble("MotionY", motion.y());
        tag.putDouble("MotionZ", motion.z());
        if (this.impactPoint != null) {
            tag.putDouble("ImpactX", this.impactPoint.x());
            tag.putDouble("ImpactY", this.impactPoint.y());
            tag.putDouble("ImpactZ", this.impactPoint.z());
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

    private void setFullScale(float fullScale) {
        this.entityData.set(FULL_SCALE, Mth.clamp(fullScale, 0.1F, 32.0F));
    }

    private void setCurrentScale(float currentScale) {
        this.dimensionsScale = Mth.clamp(currentScale, 0.1F, 64.0F);
        this.entityData.set(CURRENT_SCALE, this.dimensionsScale);
        refreshDimensions();
    }

    private void setImpactTicks(int ticks) {
        int clampedTicks = Math.max(ticks, 0);
        this.entityData.set(IMPACT_TICKS, clampedTicks);
        boolean useImpactDimensions = clampedTicks > 0;
        if (this.impactDimensions != useImpactDimensions) {
            this.impactDimensions = useImpactDimensions;
            refreshDimensions();
        }
    }

    private void growTowardFullScale() {
        float fullScale = getFullScale();
        float scale = Mth.clamp(fullScale * (this.tickCount + 1) / (float)GROW_TIME, 0.1F, fullScale);
        setCurrentScale(scale);
    }

    private void moveAboveOwner(LivingEntity owner) {
        this.setDeltaMovement(Vec3.ZERO);
        this.moveTo(owner.getX(), owner.getY() + owner.getBbHeight() + 0.5D, owner.getZ(), owner.getYRot(), owner.getXRot());
    }

    private void guideTowardOwnerLook(LivingEntity owner) {
        HitResult hit = ProcedureUtils.objectEntityLookingAt(owner, 50.0D, 0.0D, false, false,
                target -> target != this && target != owner);
        Vec3 target = hit.getType() == HitResult.Type.MISS
                ? owner.getEyePosition().add(owner.getLookAngle().scale(50.0D))
                : hit.getLocation();
        shootTowards(target, PROJECTILE_SPEED, owner);
    }

    private void shootTowards(Vec3 target, float speed, LivingEntity owner) {
        Vec3 direction = target.subtract(position());
        if (direction.lengthSqr() <= 1.0E-8D) {
            direction = owner.getLookAngle();
        }
        if (direction.lengthSqr() <= 1.0E-8D) {
            direction = Vec3.directionFromRotation(owner.getXRot(), owner.getYHeadRot());
        }
        if (direction.lengthSqr() <= 1.0E-8D) {
            direction = new Vec3(0.0D, 0.0D, 1.0D);
        }
        direction = direction.normalize();
        this.acceleration = direction.scale(0.1D);
        this.motionFactor = speed;
        faceMotion(direction);
        this.launched = true;
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
            if (hit.getType() != HitResult.Type.MISS && !isIgnoredImpact(hit)) {
                beginImpact(hit.getLocation());
                tickImpact();
                return;
            }
            setPos(getX() + motion.x(), getY() + motion.y(), getZ() + motion.z());
        }
        updateLegacyNoGravityMotion();
    }

    private void updateLegacyNoGravityMotion() {
        if (this.motionFactor > 0.0F) {
            float factor = this.isInWater() ? this.motionFactor * WATER_SLOWDOWN : this.motionFactor;
            if (this.isInWater() && this.level() instanceof ServerLevel serverLevel) {
                Vec3 motion = getDeltaMovement();
                serverLevel.sendParticles(ParticleTypes.BUBBLE,
                        getX() - motion.x() * 0.25D,
                        getY() + getBbHeight() * 0.5D - motion.y() * 0.25D,
                        getZ() - motion.z() * 0.25D,
                        4,
                        0.0D,
                        0.0D,
                        0.0D,
                        0.0D);
            }
            Vec3 motion = getDeltaMovement().add(this.acceleration).scale(factor);
            setDeltaMovement(motion);
        }
    }

    private Vec3 centerPosition() {
        return position().add(0.0D, getBbHeight() * 0.5D, 0.0D);
    }

    private HitResult findImpact(Vec3 start, Vec3 end) {
        BlockHitResult blockHit = findLegacyBlockImpact(start, end);
        double maxDistance = blockHit.getType() == HitResult.Type.MISS ? start.distanceTo(end) : start.distanceTo(blockHit.getLocation());
        LivingEntity owner = getOwner();
        Vec3 travel = end.subtract(start);
        EntityHitResult entityHit = this.level().getEntities(this, getBoundingBox().expandTowards(travel).inflate(1.0D),
                        target -> canImpact(owner, target))
                .stream()
                .map(entity -> entity.getBoundingBox().inflate(activeRadius()).clip(start, end)
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

    private boolean isIgnoredImpact(HitResult hit) {
        if (hit instanceof BlockHitResult blockHit
                && this.level().getBlockState(blockHit.getBlockPos()).getBlock() instanceof LightSourceBlock) {
            return true;
        }
        return false;
    }

    private boolean canImpact(@Nullable LivingEntity owner, Entity target) {
        return target.isAlive()
                && target != owner
                && target != this
                && target.getRootVehicle() != (owner == null ? null : owner.getRootVehicle())
                && !(target instanceof RasenshurikenEntity);
    }

    private void beginImpact(Vec3 point) {
        clearForceBowPose();
        this.impactPoint = point;
        setImpactTicks(1);
        setDeltaMovement(Vec3.ZERO);
        this.acceleration = Vec3.ZERO;
        this.motionFactor = 0.0F;
        moveTo(point.x(), point.y(), point.z(), getYRot(), getXRot());
    }

    private void tickImpact() {
        Vec3 point = this.impactPoint == null ? position() : this.impactPoint;
        int ticks = getImpactTicks() + 1;
        setImpactTicks(ticks);
        double yOffset = growImpactScale(ticks);
        moveTo(point.x(), getY() - yOffset, point.z(), getYRot(), getXRot());
        if (ticks % 4 == 0) {
            this.level().playSound(null, point.x(), point.y(), point.z(),
                    ModSounds.SOUND_RASENSHURIKEN_EXPLODE.get(), SoundSource.NEUTRAL, 5.0F, 1.0F);
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            spawnImpactParticles(serverLevel, point);
            damageImpactTargets(serverLevel, point);
            affectTerrain(serverLevel, point);
        }
        if (ticks >= MAX_IMPACT_TICKS) {
            discardRasenshuriken();
        }
    }

    private double growImpactScale(int ticks) {
        double oldHeight = getBbHeight();
        float multiplier = ticks <= 20 ? 1.15F : 1.001F;
        setCurrentScale(getCurrentScale() * multiplier);
        return (getBbHeight() - oldHeight) * 0.5D;
    }

    private void damageImpactTargets(ServerLevel level, Vec3 point) {
        LivingEntity owner = getOwner();
        DamageSource source = ModDamageTypes.ninjutsu(level, this, owner);
        float damage = getFullScale() * this.impactDamageMultiplier;
        AABB area = new AABB(point, point).inflate(impactRadius());
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, target -> canImpact(owner, target))) {
            target.invulnerableTime = 0;
            target.hurt(source, damage);
            target.setDeltaMovement(Vec3.ZERO);
        }
    }

    private void affectTerrain(ServerLevel level, Vec3 point) {
        int radius = (int)Math.ceil(impactRadius()) + 1;
        SpecialEvent.setSphericalExplosionEvent(
                level,
                null,
                Mth.floor(point.x()),
                Mth.floor(point.y()),
                Mth.floor(point.z()),
                radius,
                level.getServer().overworld().getGameTime(),
                true,
                0.0F,
                true,
                false,
                ForgeEventFactory.getMobGriefingEvent(level, null));
    }

    private void spawnChargingSmoke() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int count = Math.min(Math.max(this.tickCount, 1), GROW_TIME) * 10;
        serverLevel.sendParticles(
                ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0x10FFFFFF, (int)(getFullScale() * 12.0F), 0, 0, -1, 4),
                getX(),
                getY() + getBbHeight() * 0.5D,
                getZ(),
                count,
                activeRadius() * 0.25D,
                activeRadius() * 0.1D,
                activeRadius() * 0.25D,
                0.01D);
    }

    private void spawnImpactParticles(ServerLevel level, Vec3 point) {
        int color = this.entityData.get(BALL_COLOR);
        int count = Mth.clamp((int)(impactRadius() * 8.0D), 8, 80);
        level.sendParticles(
                ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, color, 10, 30, 0xF0, -1, 4),
                point.x(),
                point.y(),
                point.z(),
                count,
                impactRadius() * 0.35D,
                impactRadius() * 0.35D,
                impactRadius() * 0.35D,
                0.02D);
    }

    private void playWindLoop() {
        if (this.tickCount % 80 == 79) {
            this.level().playSound(null, getX(), getY(), getZ(),
                    ModSounds.SOUND_WIND.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
    }

    private void refreshLegacyLightSource() {
        if (getFullScale() >= 4.0F) {
            LightSourceBlock.setOrRefresh(this.level(), BlockPos.containing(getX(), getY(), getZ()));
        }
    }

    private void syncForceBowPose(LivingEntity owner) {
        if (!this.forceBowPoseSynced) {
            ProcedureSync.EntityNBTTag.setAndSync(owner, NarutomodModVariables.FORCE_BOW_POSE, true);
            this.forceBowPoseSynced = true;
        }
    }

    private void clearForceBowPose() {
        LivingEntity owner = getOwner();
        if (this.forceBowPoseSynced && owner != null) {
            ProcedureSync.EntityNBTTag.removeAndSync(owner, NarutomodModVariables.FORCE_BOW_POSE);
        }
        this.forceBowPoseSynced = false;
    }

    private void discardRasenshuriken() {
        clearForceBowPose();
        discard();
    }

    private double activeRadius() {
        return getBbWidth() * 0.5D;
    }

    private double impactRadius() {
        return getBbWidth() * 0.5D;
    }

    private void faceMotion(Vec3 motion) {
        if (motion.lengthSqr() <= 1.0E-8D) {
            return;
        }
        setYRot((float)(-Mth.atan2(motion.x(), motion.z()) * Mth.RAD_TO_DEG));
        setXRot((float)(-Mth.atan2(motion.y(), Math.sqrt(motion.x() * motion.x() + motion.z() * motion.z())) * Mth.RAD_TO_DEG));
    }
}
