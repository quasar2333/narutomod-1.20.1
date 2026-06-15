package net.narutomod.entity;

import java.util.Comparator;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class MeltingJutsuEntity extends Entity {
    private static final int GROW_TIME = 20;
    private static final int MAX_LIFE = 100;
    private static final double GRAVITY = 0.04D;
    private static final double DRAG = 0.98D;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(MeltingJutsuEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(MeltingJutsuEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> EMISSION_TICKS = SynchedEntityData.defineId(MeltingJutsuEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private BlockPos dripPos;
    private int deathTicks;
    private int deathTime;
    private float motionFactor;
    private int ticksInAir;
    private float dimensionsScale = 0.5F;

    public MeltingJutsuEntity(EntityType<? extends MeltingJutsuEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(false);
    }

    public void configureEmitter(LivingEntity owner, float power) {
        setOwner(owner);
        setNoGravity(power > 0.0F);
        setScale(0.5F);
        setYRot(this.random.nextFloat() * 360.0F);
        setIdlePosition(owner);
        setEmissionTicks(Math.max((int)(power * 20.0F), 0));
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(SCALE, 0.5F);
        this.entityData.define(EMISSION_TICKS, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (this.deathTicks > 0) {
            tickDeath();
            return;
        }
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive() || !this.level().hasChunkAt(blockPosition())) {
            discard();
            return;
        }
        if (isInWater()) {
            discard();
            return;
        }
        if (getEmissionTicks() > 0) {
            tickEmitter();
            if (!isRemoved() && this.tickCount > MAX_LIFE) {
                discard();
            }
            return;
        }
        tickDroplet();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        this.deathTicks = tag.getInt("DeathTicks");
        this.deathTime = tag.getInt("DeathTime");
        setScale(tag.contains("Scale") ? tag.getFloat("Scale") : 0.5F);
        setEmissionTicks(tag.getInt("EmissionTicks"));
        if (tag.contains("DripPos")) {
            this.dripPos = NbtUtils.readBlockPos(tag.getCompound("DripPos"));
        }
        this.ticksInAir = readFlightTicks(tag);
        this.motionFactor = tag.getFloat("MotionFactor");
        if (tag.contains("MotionX")) {
            setDeltaMovement(tag.getDouble("MotionX"), tag.getDouble("MotionY"), tag.getDouble("MotionZ"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putInt("DeathTicks", this.deathTicks);
        tag.putInt("DeathTime", this.deathTime);
        tag.putFloat("Scale", getScale());
        tag.putInt("EmissionTicks", getEmissionTicks());
        if (this.dripPos != null) {
            tag.put("DripPos", NbtUtils.writeBlockPos(this.dripPos));
        }
        tag.putInt("FlightTicks", this.ticksInAir);
        tag.putInt("TicksInAir", this.ticksInAir);
        tag.putInt("flighttime", this.ticksInAir);
        tag.putFloat("MotionFactor", this.motionFactor);
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
        return tag.getInt("flighttime");
    }

    @Override
    public boolean fireImmune() {
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

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(this.dimensionsScale);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SCALE.equals(key)) {
            this.dimensionsScale = Math.max(getScale(), 0.1F);
            refreshDimensions();
        }
    }

    public float getScale() {
        return this.entityData.get(SCALE);
    }

    public float getRenderVOffset(float partialTick) {
        return ((this.tickCount + partialTick) * -0.2F) % 1.0F;
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

    private int getEmissionTicks() {
        return this.entityData.get(EMISSION_TICKS);
    }

    private void setEmissionTicks(int ticks) {
        this.entityData.set(EMISSION_TICKS, Math.max(ticks, 0));
    }

    private void setScale(float scale) {
        this.dimensionsScale = Math.max(scale, 0.1F);
        this.entityData.set(SCALE, this.dimensionsScale);
        refreshDimensions();
    }

    private void setIdlePosition(LivingEntity owner) {
        Vec3 look = owner.getLookAngle();
        Vec3 point = owner.getEyePosition().add(look.scale(0.4D)).subtract(0.0D, 0.1D, 0.0D);
        moveTo(point.x(), point.y(), point.z(), owner.getYRot(), owner.getXRot());
    }

    private void tickEmitter() {
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        setIdlePosition(owner);
        int emissionTicks = getEmissionTicks();
        if (emissionTicks > 1) {
            Vec3 look = owner.getLookAngle();
            for (int i = 0; i < 10; i++) {
                MeltingJutsuEntity droplet = ModEntityTypes.MELTING_JUTSU.get().create(this.level());
                if (droplet != null) {
                    droplet.configureDroplet(owner, look);
                    this.level().addFreshEntity(droplet);
                }
            }
            setEmissionTicks(emissionTicks - 1);
        }
    }

    private void configureDroplet(LivingEntity owner, Vec3 direction) {
        setOwner(owner);
        setNoGravity(false);
        setScale(0.5F);
        setYRot(this.random.nextFloat() * 360.0F);
        setIdlePosition(owner);
        shootWithInaccuracy(direction, 0.85F, 0.1F);
        setEmissionTicks(0);
    }

    private void shootWithInaccuracy(Vec3 direction, float speed, float inaccuracy) {
        Vec3 safeDirection = direction.lengthSqr() <= 1.0E-8D ? Vec3.directionFromRotation(getXRot(), getYRot()) : direction;
        safeDirection = safeDirection.add(
                this.random.nextGaussian() * inaccuracy,
                this.random.nextGaussian() * inaccuracy,
                this.random.nextGaussian() * inaccuracy);
        Vec3 motion = safeDirection.normalize().scale(speed);
        setDeltaMovement(motion);
        this.motionFactor = speed;
        faceShootDirection(motion);
    }

    private void tickDroplet() {
        if (onGround()) {
            discard();
            return;
        }
        travelAndImpact(getDeltaMovement());
        if (isRemoved()) {
            return;
        }
        if (this.ticksInAir <= GROW_TIME) {
            setScale(0.5F + 3.5F * this.ticksInAir / GROW_TIME);
        }
        if (this.ticksInAir == this.random.nextInt(99) + 1) {
            this.level().playSound(null, getX(), getY(), getZ(), ModSounds.SOUND_MOVEMENT.get(),
                    SoundSource.NEUTRAL, 0.8F, this.random.nextFloat() * 0.4F + 0.8F);
        }
        if (this.tickCount > MAX_LIFE) {
            discard();
        }
    }

    private void travelAndImpact(Vec3 motion) {
        Vec3 start = centerPosition();
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
        if (isNoGravity()) {
            setDeltaMovement(motion);
        } else {
            setDeltaMovement(motion.x() * DRAG, motion.y() * DRAG - GRAVITY, motion.z() * DRAG);
        }
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
        if (!target.isAlive() || !target.isPickable() || target.noPhysics || target == this) {
            return false;
        }
        if (target == owner && this.ticksInAir < 25) {
            return false;
        }
        return !(target instanceof MeltingJutsuEntity melting && isSameOwnerMelting(melting));
    }

    private boolean isSameOwnerMelting(MeltingJutsuEntity melting) {
        return this.ownerUuid != null && this.ownerUuid.equals(melting.ownerUuid);
    }

    private boolean impact(HitResult hit) {
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof MeltingJutsuEntity) {
            return false;
        }
        BlockPos pos = impactBlockPos(hit);
        if (this.level().isEmptyBlock(pos)) {
            this.level().setBlock(pos, Blocks.LAVA.defaultBlockState(), 3);
            this.dripPos = pos;
        }
        startDeath();
        return true;
    }

    private static BlockPos impactBlockPos(HitResult hit) {
        if (hit instanceof BlockHitResult blockHit) {
            return blockHit.getBlockPos().relative(blockHit.getDirection());
        }
        return BlockPos.containing(hit.getLocation());
    }

    private void startDeath() {
        this.deathTicks = 1;
        this.deathTime = 120 + this.random.nextInt(80);
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
    }

    private void tickDeath() {
        if (this.deathTicks >= this.deathTime) {
            if (this.dripPos != null) {
                solidifyLava(this.dripPos);
            }
            discard();
            return;
        }
        this.level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(getScale() * 0.5D, 0.0D, getScale() * 0.5D).expandTowards(0.0D, -1.0D, 0.0D),
                Entity::isAlive).forEach(entity -> {
            ProcedureUtils.multiplyVelocity(entity, 0.4D);
            entity.setDeltaMovement(entity.getDeltaMovement().add(0.0D, -0.04D, 0.0D));
            entity.hasImpulse = true;
        });
        this.deathTicks++;
    }

    private void solidifyLava(BlockPos start) {
        BlockPos.MutableBlockPos cursor = start.mutable();
        while (cursor.getY() >= this.level().getMinBuildHeight() && isLava(cursor)) {
            this.level().setBlock(cursor, Blocks.OBSIDIAN.defaultBlockState(), 3);
            cursor.move(0, -1, 0);
        }
    }

    private boolean isLava(BlockPos pos) {
        BlockState state = this.level().getBlockState(pos);
        return state.is(Blocks.LAVA) || state.getFluidState().is(FluidTags.LAVA);
    }

    private void faceShootDirection(Vec3 motion) {
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
