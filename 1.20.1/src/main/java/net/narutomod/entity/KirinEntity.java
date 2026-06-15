package net.narutomod.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class KirinEntity extends Entity {
    public static final int WAIT_TICKS = 60;
    public static final int MAX_LIFE = 100;
    public static final float SCALE = 10.0F;
    public static final int SPINE_SEGMENTS = 100;
    private static final float SPINE_SEGMENT_LENGTH = SCALE * 11.0F * 0.0625F;
    private static final float WATER_SLOWDOWN = 0.8F;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(KirinEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> LAUNCHED = SynchedEntityData.defineId(KirinEntity.class, EntityDataSerializers.BOOLEAN);

    private final List<ProcedureUtils.Vec2f> partRotations = new ArrayList<>();
    @Nullable
    private UUID ownerUuid;
    @Nullable
    private Vec3 targetPoint;
    @Nullable
    private Vec3 lastSegmentPosition;
    private Vec3 acceleration = Vec3.ZERO;
    private float previousHeadYaw;
    private float previousHeadPitch;
    private float motionFactor;
    private int ticksInAir;

    public KirinEntity(EntityType<? extends KirinEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        seedPartRotations();
    }

    public void configure(LivingEntity owner) {
        setOwner(owner);
        this.moveTo(owner.getX(), owner.getY() + 100.0D, owner.getZ(), owner.getYRot(), 80.0F);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSounds.SOUND_DRAGON_ROAR.get(), SoundSource.WEATHER, 100.0F, this.random.nextFloat() * 0.4F + 0.8F);
    }

    public boolean isOwnedBy(LivingEntity entity) {
        return entity.getUUID().equals(this.ownerUuid) || getOwner() == entity;
    }

    public List<ProcedureUtils.Vec2f> getPartRotations() {
        return this.partRotations;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(LAUNCHED, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            updateVisualSegments();
            return;
        }
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive() || !this.level().hasChunkAt(blockPosition()) || this.tickCount > MAX_LIFE) {
            discard();
            return;
        }
        if (!isLaunched()) {
            updateWaiting(owner);
        } else {
            updateLaunched(owner);
        }
        spawnFlightArcs();
        updateVisualSegments();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        setLaunched(tag.getBoolean("Launched"));
        this.ticksInAir = readFlightTicks(tag);
        this.motionFactor = tag.getFloat("MotionFactor");
        this.acceleration = tag.contains("AccelerationX")
                ? new Vec3(tag.getDouble("AccelerationX"), tag.getDouble("AccelerationY"), tag.getDouble("AccelerationZ"))
                : Vec3.ZERO;
        if (tag.contains("MotionX")) {
            setDeltaMovement(tag.getDouble("MotionX"), tag.getDouble("MotionY"), tag.getDouble("MotionZ"));
        }
        if (tag.contains("TargetX")) {
            this.targetPoint = new Vec3(tag.getDouble("TargetX"), tag.getDouble("TargetY"), tag.getDouble("TargetZ"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putBoolean("Launched", isLaunched());
        tag.putInt("FlightTicks", this.ticksInAir);
        tag.putInt("TicksInAir", this.ticksInAir);
        tag.putInt("flighttime", this.ticksInAir);
        tag.putFloat("MotionFactor", this.motionFactor);
        tag.putDouble("AccelerationX", this.acceleration.x());
        tag.putDouble("AccelerationY", this.acceleration.y());
        tag.putDouble("AccelerationZ", this.acceleration.z());
        Vec3 motion = getDeltaMovement();
        tag.putDouble("MotionX", motion.x());
        tag.putDouble("MotionY", motion.y());
        tag.putDouble("MotionZ", motion.z());
        if (this.targetPoint != null) {
            tag.putDouble("TargetX", this.targetPoint.x());
            tag.putDouble("TargetY", this.targetPoint.y());
            tag.putDouble("TargetZ", this.targetPoint.z());
        }
    }

    private int readFlightTicks(CompoundTag tag) {
        if (tag.contains("TicksInAir")) {
            return tag.getInt("TicksInAir");
        }
        if (tag.contains("FlightTicks")) {
            return tag.getInt("FlightTicks");
        }
        return tag.getInt("flighttime");
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
        return !isRemoved();
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public static void chargingEffects(LivingEntity player, float power) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (power >= 0.8F && power < 0.81F) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.SOUND_KIRIN_DIALOG.get(), SoundSource.PLAYERS, 5.0F, 1.0F);
        }
        int count = 10 + player.getRandom().nextInt(21);
        for (int i = 0; i < count; i++) {
            Vec3 center = new Vec3(
                    player.getX() + (player.getRandom().nextDouble() - 0.5D) * 100.0D,
                    player.getY() + 95.0D + player.getRandom().nextDouble() * 10.0D,
                    player.getZ() + (player.getRandom().nextDouble() - 0.5D) * 100.0D);
            LightningArcEntity arc = ModEntityTypes.LIGHTNING_ARC.get().create(serverLevel);
            if (arc != null) {
                arc.configureRandom(center, player.getRandom().nextDouble() * 40.0D + 10.0D, Vec3.ZERO,
                        0xC00000FF, 1, 5.0F, 0.1F);
                serverLevel.addFreshEntity(arc);
            }
        }
    }

    public static void startWeatherThunder(Entity entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.setWeatherParameters(0, 600, true, true);
        serverLevel.playSound(null, entity.getX(), entity.getY() + 100.0D, entity.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1000.0F, 0.8F + entity.level().random.nextFloat() * 0.2F);
    }

    private void setOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
    }

    @Nullable
    private LivingEntity getOwner() {
        int ownerId = this.entityData.get(OWNER_ID);
        if (ownerId >= 0) {
            Entity entity = this.level().getEntity(ownerId);
            if (entity instanceof LivingEntity living) {
                return living;
            }
        }
        if (this.ownerUuid != null && this.level() instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(this.ownerUuid);
            if (entity instanceof LivingEntity living) {
                setOwner(living);
                return living;
            }
        }
        return null;
    }

    private boolean isLaunched() {
        return this.entityData.get(LAUNCHED);
    }

    private void setLaunched(boolean launched) {
        this.entityData.set(LAUNCHED, launched);
    }

    private void updateWaiting(LivingEntity owner) {
        this.targetPoint = resolveTargetPoint(owner);
        faceTarget(this.targetPoint);
        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x(), getY() + motion.y(), getZ() + motion.z());
        if (this.tickCount <= WAIT_TICKS / 2) {
            motion = motion.add(0.0D, -0.03D, 0.0D);
        }
        setDeltaMovement(motion);
        if (this.tickCount > WAIT_TICKS) {
            launchAtTarget();
        }
    }

    private void launchAtTarget() {
        Vec3 target = this.targetPoint == null ? position().add(getLookAngle().scale(50.0D)) : this.targetPoint;
        Vec3 direction = target.subtract(position());
        if (direction.lengthSqr() <= 1.0E-8D) {
            direction = getLookAngle();
        }
        Vec3 motion = getDeltaMovement();
        setDeltaMovement(motion.x(), 0.0D, motion.z());
        shoot(direction, 1.2F);
        faceTarget(position().add(direction));
        setLaunched(true);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSounds.SOUND_LIGHTNING_SHOOT.get(), SoundSource.WEATHER, 100.0F, this.random.nextFloat() * 0.4F + 0.8F);
    }

    private void updateLaunched(LivingEntity owner) {
        Vec3 start = centerPosition();
        Vec3 motion = getDeltaMovement();
        if (this.motionFactor > 0.0F) {
            this.ticksInAir++;
        }
        if (motion.lengthSqr() > 1.0E-8D) {
            Vec3 end = start.add(motion);
            HitResult hit = findImpact(start, end, owner);
            if (hit.getType() != HitResult.Type.MISS && impact(hit, owner)) {
                discard();
                return;
            }
            setPos(getX() + motion.x(), getY() + motion.y(), getZ() + motion.z());
        }
        updateLegacyNoGravityMotion();
    }

    private Vec3 resolveTargetPoint(LivingEntity owner) {
        if (owner instanceof Mob mob && mob.getTarget() != null) {
            return mob.getTarget().position();
        }
        HitResult hit = ProcedureUtils.objectEntityLookingAt(owner, 50.0D, 0.0D, true, false, target -> target != this);
        return hit.getType() == HitResult.Type.MISS ? owner.getEyePosition().add(owner.getLookAngle().scale(50.0D)) : hit.getLocation();
    }

    private void seedPartRotations() {
        this.partRotations.add(new ProcedureUtils.Vec2f(45.0F, 0.0F));
        this.partRotations.add(new ProcedureUtils.Vec2f(-45.0F, 0.0F));
        this.partRotations.add(new ProcedureUtils.Vec2f(-45.0F, 0.0F));
        this.partRotations.add(new ProcedureUtils.Vec2f(-22.5F, 0.0F));
        this.partRotations.add(new ProcedureUtils.Vec2f(45.0F, 0.0F));
        this.partRotations.add(new ProcedureUtils.Vec2f(45.0F, 0.0F));
        this.partRotations.add(new ProcedureUtils.Vec2f(45.0F, 0.0F));
        this.partRotations.add(new ProcedureUtils.Vec2f(-45.0F, 0.0F));
    }

    private void updateVisualSegments() {
        Vec3 currentPosition = position();
        if (this.lastSegmentPosition == null) {
            this.lastSegmentPosition = currentPosition;
            this.previousHeadYaw = getYRot();
            this.previousHeadPitch = getXRot();
            return;
        }

        ProcedureUtils.Vec2f rotationDelta = new ProcedureUtils.Vec2f(getYRot(), getXRot())
                .subtract(this.previousHeadYaw, this.previousHeadPitch);
        Vec3 movement = currentPosition.subtract(this.lastSegmentPosition);
        double distance = movement.length();
        if (distance >= SPINE_SEGMENT_LENGTH) {
            this.partRotations.add(0, rotationDelta);
            int steps = 1;
            int totalSteps = (int)(distance / SPINE_SEGMENT_LENGTH);
            for (; steps < totalSteps; steps++) {
                this.partRotations.add(0, ProcedureUtils.Vec2f.ZERO);
            }
            this.lastSegmentPosition = movement.normalize().scale(SPINE_SEGMENT_LENGTH * steps).add(this.lastSegmentPosition);
        } else if (!this.partRotations.isEmpty()) {
            this.partRotations.set(0, this.partRotations.get(0).add(rotationDelta));
        }
        while (this.partRotations.size() > SPINE_SEGMENTS) {
            this.partRotations.remove(this.partRotations.size() - 1);
        }
        this.previousHeadYaw = getYRot();
        this.previousHeadPitch = getXRot();
    }

    private HitResult findImpact(Vec3 start, Vec3 end, LivingEntity owner) {
        BlockHitResult blockHit = findLegacyBlockImpact(start, end);
        double maxDistance = blockHit.getType() == HitResult.Type.MISS ? start.distanceTo(end) : start.distanceTo(blockHit.getLocation());
        EntityHitResult entityHit = this.level().getEntities(this, getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D),
                        target -> canImpact(owner, target))
                .stream()
                .map(entity -> {
                    AABB box = entity.getBoundingBox().inflate(getBbWidth() * 0.5D, getBbHeight() * 0.5D, getBbWidth() * 0.5D);
                    return box.clip(start, end).map(location -> new EntityHitResult(entity, location)).orElse(null);
                })
                .filter(hit -> hit != null && start.distanceTo(hit.getLocation()) <= maxDistance)
                .min(Comparator.comparingDouble(hit -> start.distanceTo(hit.getLocation())))
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
        if (target == owner && this.ticksInAir < 25) {
            return false;
        }
        return !(target instanceof KirinEntity kirin && isSameOwnerKirin(kirin));
    }

    private boolean isSameOwnerKirin(KirinEntity kirin) {
        return this.ownerUuid != null && this.ownerUuid.equals(kirin.ownerUuid);
    }

    private boolean impact(HitResult hit, LivingEntity owner) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() == owner) {
            return false;
        }
        Vec3 point = hit.getLocation();
        if (hit instanceof EntityHitResult entityHit) {
            point = entityHit.getEntity().position();
        }
        this.level().playSound(null, point.x(), point.y(), point.z(),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 5.0F, 0.5F + this.random.nextFloat() * 0.2F);
        LightningArcEntity column = ModEntityTypes.LIGHTNING_ARC.get().create(serverLevel);
        if (column != null) {
            column.configureBetween(point.subtract(0.0D, 5.0D, 0.0D), point.add(0.0D, 150.0D, 0.0D),
                    0xC00000FF, 40, 0.0F, 6.0F, 4);
            serverLevel.addFreshEntity(column);
        }
        boolean mobGriefing = ForgeEventFactory.getMobGriefingEvent(serverLevel, owner);
        serverLevel.explode(owner, point.x(), point.y(), point.z(), SCALE, mobGriefing, Level.ExplosionInteraction.MOB);
        DamageSource source = ModDamageTypes.ninjutsu(this.level(), this, owner);
        for (Entity target : serverLevel.getEntities(this, new AABB(point, point).inflate(10.0D),
                entity -> entity != owner && entity.isAlive())) {
            target.setSecondsOnFire(15);
            target.hurt(source, 100.0F * SCALE);
        }
        serverLevel.gameEvent(owner, GameEvent.EXPLODE, point);
        return true;
    }

    private void spawnFlightArcs() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int i = 0; i < 8; i++) {
            Vec3 center = new Vec3(
                    getX() + (this.random.nextDouble() - 0.5D) * getBbWidth(),
                    getY() + this.random.nextDouble() * getBbHeight() - 2.0D,
                    getZ() + (this.random.nextDouble() - 0.5D) * getBbWidth());
            LightningArcEntity arc = ModEntityTypes.LIGHTNING_ARC.get().create(serverLevel);
            if (arc != null) {
                arc.configureRandom(center, this.random.nextDouble() * 15.0D + 15.0D, getDeltaMovement(),
                        0xC00000FF, 1, getBbWidth() * 0.5F, 0.1F);
                serverLevel.addFreshEntity(arc);
            }
        }
    }

    private void faceTarget(Vec3 target) {
        Vec3 direction = target.subtract(position());
        if (direction.lengthSqr() <= 1.0E-8D) {
            return;
        }
        float yaw = (float)(-Mth.atan2(direction.x(), direction.z()) * Mth.RAD_TO_DEG);
        float pitch = (float)(-Mth.atan2(direction.y(), Math.sqrt(direction.x() * direction.x() + direction.z() * direction.z())) * Mth.RAD_TO_DEG);
        setYRot(yaw);
        setXRot(pitch);
    }

    private void shoot(Vec3 direction, float speed) {
        if (direction.lengthSqr() <= 1.0E-8D) {
            direction = new Vec3(0.0D, -1.0D, 0.0D);
        }
        this.acceleration = direction.normalize().scale(0.1D);
        this.motionFactor = speed;
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
