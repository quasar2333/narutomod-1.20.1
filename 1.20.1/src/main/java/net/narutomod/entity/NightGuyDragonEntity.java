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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
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
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class NightGuyDragonEntity extends Entity {
    private static final int SUSPEND_TICKS = 20;
    private static final int MAX_AIR_TICKS = 30;
    private static final float FULL_SCALE = 6.0F;
    private static final float PROJECTILE_SPEED = 1.2F;
    private static final float DIRECT_DAMAGE_MULTIPLIER = 64.0F;
    private static final float AOE_DAMAGE_MULTIPLIER = 16.0F;
    private static final float AOE_RADIUS = 5.0F;
    private static final float EXPLOSION_STRENGTH = 10.0F;
    private static final float EXPLOSION_RESISTANCE_REDUCTION = 1999.0F;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(NightGuyDragonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(NightGuyDragonEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> LAUNCHED = SynchedEntityData.defineId(NightGuyDragonEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID ownerUuid;
    private float dimensionsScale = 1.0F;
    private Vec3 acceleration = Vec3.ZERO;
    private float motionFactor;
    private int ticksInAir;
    private float prevLimbSwingAmount;
    private float limbSwingAmount;
    private float limbSwing;

    public NightGuyDragonEntity(EntityType<? extends NightGuyDragonEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    public static boolean spawnFrom(LivingEntity owner) {
        NightGuyDragonEntity entity = ModEntityTypes.ENTITYNGDRAGON.get().create(owner.level());
        if (entity == null) {
            return false;
        }
        entity.configure(owner);
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                ModSounds.SOUND_YAGAI.get(), SoundSource.NEUTRAL, 2.0F, 1.0F);
        return owner.level().addFreshEntity(entity);
    }

    public float getDragonScale() {
        return this.entityData.get(SCALE);
    }

    public boolean isLaunched() {
        return this.entityData.get(LAUNCHED);
    }

    public float getPrevLimbSwingAmount() {
        return this.prevLimbSwingAmount;
    }

    public float getLimbSwingAmount() {
        return this.limbSwingAmount;
    }

    public float getLimbSwing() {
        return this.limbSwing;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(SCALE, 1.0F);
        this.entityData.define(LAUNCHED, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            updateLimbSwing();
            return;
        }
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive() || !this.level().hasChunkAt(blockPosition())) {
            discard();
            return;
        }
        owner.getPersistentData().putDouble(NarutomodModVariables.INVULNERABLE_TIME, 40.0D);
        if (this.tickCount <= SUSPEND_TICKS) {
            setWaitPosition(owner);
            setScale(FULL_SCALE * Mth.clamp(this.tickCount / (float) SUSPEND_TICKS, 1.0F / FULL_SCALE, 1.0F));
            spawnChargeParticles();
            updateLimbSwing();
            return;
        }
        if (!isLaunched()) {
            spawnChargeParticles();
            launch(owner);
            updateLimbSwing();
            return;
        }
        travelAndImpact(owner);
        if (!isRemoved()) {
            dragOwner(owner);
            spawnTrailParticles();
            updateLimbSwing();
            if (this.ticksInAir > MAX_AIR_TICKS) {
                discard();
            }
        }
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean ignoreExplosion() {
        return true;
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
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(this.dimensionsScale);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SCALE.equals(key)) {
            this.dimensionsScale = Math.max(getDragonScale(), 0.1F);
            refreshDimensions();
        }
    }

    @Override
    public float getBlockExplosionResistance(Explosion explosion, BlockGetter blockGetter, BlockPos pos,
            BlockState state, FluidState fluid, float resistance) {
        float original = super.getBlockExplosionResistance(explosion, blockGetter, pos, state, fluid, resistance);
        return Math.max(original - EXPLOSION_RESISTANCE_REDUCTION, 0.1F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        setScale(tag.contains("Scale") ? tag.getFloat("Scale") : 1.0F);
        setLaunched(tag.getBoolean("Launched"));
        this.ticksInAir = readFlightTicks(tag);
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
        tag.putFloat("Scale", getDragonScale());
        tag.putBoolean("Launched", isLaunched());
        tag.putInt("FlightTicks", this.ticksInAir);
        tag.putInt("TicksInAir", this.ticksInAir);
        tag.putInt("LaunchedTicks", this.ticksInAir);
        tag.putInt("flighttime", this.ticksInAir);
        tag.putFloat("MotionFactor", this.motionFactor);
        tag.putDouble("AccelerationX", this.acceleration.x());
        tag.putDouble("AccelerationY", this.acceleration.y());
        tag.putDouble("AccelerationZ", this.acceleration.z());
        Vec3 motion = getDeltaMovement();
        tag.putDouble("MotionX", motion.x());
        tag.putDouble("MotionY", motion.y());
        tag.putDouble("MotionZ", motion.z());
    }

    private int readFlightTicks(CompoundTag tag) {
        if (tag.contains("TicksInAir")) {
            return tag.getInt("TicksInAir");
        }
        if (tag.contains("FlightTicks")) {
            return tag.getInt("FlightTicks");
        }
        if (tag.contains("LaunchedTicks")) {
            return tag.getInt("LaunchedTicks");
        }
        return tag.getInt("flighttime");
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void configure(LivingEntity owner) {
        setOwner(owner);
        setScale(1.0F);
        setLaunched(false);
        setWaitPosition(owner);
        setDeltaMovement(Vec3.ZERO);
        this.acceleration = Vec3.ZERO;
        this.motionFactor = 0.0F;
        this.ticksInAir = 0;
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
        this.dimensionsScale = Mth.clamp(scale, 0.1F, 32.0F);
        this.entityData.set(SCALE, this.dimensionsScale);
        refreshDimensions();
    }

    private void setLaunched(boolean launched) {
        this.entityData.set(LAUNCHED, launched);
    }

    private void setWaitPosition(LivingEntity owner) {
        moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
        setDeltaMovement(Vec3.ZERO);
    }

    private void launch(LivingEntity owner) {
        Vec3 direction = owner instanceof Mob mob && mob.getTarget() != null
                ? mob.getTarget().position().subtract(position())
                : owner.getLookAngle();
        if (direction.lengthSqr() <= 1.0E-8D) {
            direction = Vec3.directionFromRotation(owner.getXRot(), owner.getYHeadRot());
        }
        if (direction.lengthSqr() <= 1.0E-8D) {
            direction = new Vec3(0.0D, 0.0D, 1.0D);
        }
        direction = direction.normalize();
        this.acceleration = direction.scale(0.1D);
        this.motionFactor = PROJECTILE_SPEED;
        faceDirection(direction, owner);
        setLaunched(true);
    }

    private void travelAndImpact(LivingEntity owner) {
        Vec3 start = centerPosition();
        Vec3 motion = getDeltaMovement();
        if (this.motionFactor > 0.0F) {
            this.ticksInAir++;
        }
        if (motion.lengthSqr() > 1.0E-8D) {
            Vec3 end = start.add(motion);
            HitResult hit = findImpact(owner, start, end);
            if (hit.getType() != HitResult.Type.MISS && impact(owner, hit, start, end)) {
                return;
            }
            motion = getDeltaMovement();
            setPos(getX() + motion.x(), getY() + motion.y(), getZ() + motion.z());
        }
        updateLegacyNoGravityMotion();
    }

    private HitResult findImpact(LivingEntity owner, Vec3 start, Vec3 end) {
        BlockHitResult blockHit = findLegacyBlockImpact(start, end);
        double maxDistance = blockHit.getType() == HitResult.Type.MISS ? start.distanceTo(end) : start.distanceTo(blockHit.getLocation());
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

    private boolean canImpact(LivingEntity owner, Entity target) {
        if (!target.isAlive() || !target.isPickable() || target.noPhysics || target == this) {
            return false;
        }
        return target != owner || this.ticksInAir >= 25;
    }

    private boolean impact(LivingEntity owner, HitResult hit, Vec3 start, Vec3 end) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            discard();
            return true;
        }
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() == owner) {
            return false;
        }
        DamageSource source = ModDamageTypes.nightGuy(serverLevel, this, owner);
        float baseDamage = (float) ProcedureUtils.getModifiedAttackDamage(owner);
        Vec3 point = position();
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target) {
            target.hurt(source, baseDamage * DIRECT_DAMAGE_MULTIPLIER);
            ProcedureUtils.pushEntity(point, target, 30.0D, 2.0F);
        }
        for (Entity target : serverLevel.getEntities(this, getBoundingBox().inflate(AOE_RADIUS),
                target -> canLegacyAoeDamage(owner, target))) {
            target.hurt(source, baseDamage * AOE_DAMAGE_MULTIPLIER);
        }
        boolean damagesTerrain = ForgeEventFactory.getMobGriefingEvent(serverLevel, owner);
        serverLevel.explode(this, point.x(), point.y(), point.z(), EXPLOSION_STRENGTH, false,
                damagesTerrain ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE);
        if (hit.getType() == HitResult.Type.BLOCK || findLegacyBlockImpact(start, end).getType() != HitResult.Type.MISS) {
            this.acceleration = Vec3.ZERO;
            this.motionFactor = 0.0F;
            discard();
            return true;
        } else {
            setDeltaMovement(getDeltaMovement().scale(0.4D));
        }
        return false;
    }

    private void updateLegacyNoGravityMotion() {
        if (this.motionFactor <= 0.0F) {
            return;
        }
        float factor = isInWater() ? this.motionFactor * 0.8F : this.motionFactor;
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

    private boolean canLegacyAoeDamage(LivingEntity owner, Entity target) {
        return target.isAlive()
                && !target.isSpectator()
                && target != owner
                && !target.getPersistentData().getBoolean("kamui_intangible");
    }

    private void dragOwner(LivingEntity owner) {
        owner.teleportTo(getX(), getY(), getZ());
        owner.setDeltaMovement(getDeltaMovement());
    }

    private void updateLimbSwing() {
        this.prevLimbSwingAmount = this.limbSwingAmount;
        double dx = getX() - xo;
        double dy = getY() - yo;
        double dz = getZ() - zo;
        float amount = Mth.sqrt((float)(dx * dx + dy * dy + dz * dz)) * 4.0F;
        if (amount > 1.0F) {
            amount = 1.0F;
        }
        this.limbSwingAmount += (amount - this.limbSwingAmount) * 0.4F;
        this.limbSwing += this.limbSwingAmount;
    }

    private void spawnChargeParticles() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.sendParticles(
                ModParticleTypes.options(NarutoParticleKind.FLAME_COLORED, 0x80FF0000, 60),
                getX(),
                getY() + getBbHeight() * 0.5D,
                getZ(),
                100,
                0.0D,
                0.0D,
                0.0D,
                0.5D);
    }

    private void spawnTrailParticles() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.sendParticles(
                ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0x80800000, 40, 0, 0xF0, -1, 4),
                getX(),
                getY() + getBbHeight() * 0.5D,
                getZ(),
                200,
                Math.max(getBbWidth() * 0.5D, 0.1D),
                Math.max(getBbHeight() * 0.5D, 0.1D),
                Math.max(getBbWidth() * 0.5D, 0.1D),
                0.0D);
    }

    private double collisionRadius() {
        return Math.max(0.5D, getDragonScale() * 0.5D);
    }

    private Vec3 centerPosition() {
        return position().add(0.0D, getBbHeight() * 0.5D, 0.0D);
    }

    private void faceDirection(Vec3 direction, LivingEntity owner) {
        if (direction.lengthSqr() <= 1.0E-8D) {
            setYRot(owner.getYRot());
            setXRot(owner.getXRot());
            return;
        }
        setYRot((float)(-Mth.atan2(direction.x(), direction.z()) * Mth.RAD_TO_DEG));
        setXRot((float)(-Mth.atan2(direction.y(), Math.sqrt(direction.x() * direction.x() + direction.z() * direction.z())) * Mth.RAD_TO_DEG));
    }
}
