package net.narutomod.entity;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class WaterSharkEntity extends Entity {
    private static final int WAIT_TICKS = 30;
    private static final int MOUTH_OPEN_TICKS = 20;
    private static final int TEMPORARY_WATER_LIFE_TICKS = 10;
    private static final float MIN_SCALE = 0.2F;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(WaterSharkEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(WaterSharkEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(WaterSharkEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FULL_SCALE = SynchedEntityData.defineId(WaterSharkEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> PREV_LIMB_SWING_AMOUNT = SynchedEntityData.defineId(WaterSharkEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LIMB_SWING_AMOUNT = SynchedEntityData.defineId(WaterSharkEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LIMB_SWING = SynchedEntityData.defineId(WaterSharkEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> MOUTH_OPEN_AMOUNT = SynchedEntityData.defineId(WaterSharkEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> LAUNCHED = SynchedEntityData.defineId(WaterSharkEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IMPACTED = SynchedEntityData.defineId(WaterSharkEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private UUID targetUuid;
    private float health;
    private int flightTicks;
    private float dimensionsScale = MIN_SCALE;
    private Vec3 acceleration = Vec3.ZERO;
    private float motionFactor;
    private final Map<BlockPos, Integer> temporaryWater = new HashMap<>();

    public WaterSharkEntity(EntityType<? extends WaterSharkEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.health = 20.0F;
    }

    public void configure(LivingEntity owner, float power) {
        setOwner(owner);
        setFullScale(power);
        setScale(power * MIN_SCALE);
        this.health = power * 20.0F;
        moveToWaitPosition(owner);
        this.setDeltaMovement(Vec3.ZERO);
    }

    public static boolean spawnFrom(LivingEntity owner, float power) {
        WaterSharkEntity entity = ModEntityTypes.SUITON_SHARK.get().create(owner.level());
        if (entity == null) {
            return false;
        }
        entity.configure(owner, power);
        owner.level().addFreshEntity(entity);
        if (!owner.isInWaterOrBubble()) {
            owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                    ModSounds.SOUND_SUIKODANNOJUTSU.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return true;
    }

    public float getScale() {
        return this.entityData.get(SCALE);
    }

    public float getFullScale() {
        return this.entityData.get(FULL_SCALE);
    }

    public float getPrevLimbSwingAmount() {
        return this.entityData.get(PREV_LIMB_SWING_AMOUNT);
    }

    public float getLimbSwingAmount() {
        return this.entityData.get(LIMB_SWING_AMOUNT);
    }

    public float getLegacyLimbSwing() {
        return this.entityData.get(LIMB_SWING);
    }

    public float getMouthOpenAmount() {
        return this.entityData.get(MOUTH_OPEN_AMOUNT);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(TARGET_ID, -1);
        this.entityData.define(SCALE, MIN_SCALE);
        this.entityData.define(FULL_SCALE, 1.0F);
        this.entityData.define(PREV_LIMB_SWING_AMOUNT, 0.0F);
        this.entityData.define(LIMB_SWING_AMOUNT, 0.0F);
        this.entityData.define(LIMB_SWING, 0.0F);
        this.entityData.define(MOUTH_OPEN_AMOUNT, 0.0F);
        this.entityData.define(LAUNCHED, false);
        this.entityData.define(IMPACTED, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        expireTemporaryWater();
        if (hasImpacted()) {
            if (this.temporaryWater.isEmpty()) {
                discard();
            }
            return;
        }
        LivingEntity owner = getOwner();
        if (this.flightTicks > 120 || owner == null || !owner.isAlive()) {
            discardShark();
            return;
        }

        if (this.tickCount <= WAIT_TICKS) {
            moveToWaitPosition(owner);
            setScale(getFullScale() * Mth.clamp((float)this.tickCount / WAIT_TICKS, MIN_SCALE, 1.0F));
            setMouthOpenAmount(0.0F);
            updateLegacyAnimationState();
            spawnWaitingParticles();
            return;
        }
        if (isLaunched()) {
            travelAndImpact();
            if (isRemoved() || hasImpacted()) {
                return;
            }
        }
        updateLaunchMotion(owner);
        this.flightTicks++;
        updateMouthOpenAmount();
        spawnLaunchedParticles();
        if (!isRemoved() && !hasImpacted()) {
            updateLegacyAnimationState();
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && reason.shouldDestroy()) {
            clearTemporaryWater();
        }
        super.remove(reason);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (getFullScale() >= 4.0F && ModDamageTypes.isNinjutsu(source)) {
            setScale(getScale() + amount * 0.013333F);
            Entity directEntity = source.getDirectEntity();
            if (directEntity != null && !(directEntity instanceof LivingEntity)) {
                directEntity.discard();
            }
            return false;
        }
        if (getFullScale() >= 1.0F) {
            this.health -= amount;
            if (this.health <= 0.0F) {
                discardShark();
            }
            return true;
        }
        return super.hurt(source, amount);
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
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 96.0D * getViewScale();
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
            this.dimensionsScale = getScale();
            refreshDimensions();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        if (tag.hasUUID("Target")) {
            this.targetUuid = tag.getUUID("Target");
        }
        setScale(tag.contains("Scale") ? tag.getFloat("Scale") : MIN_SCALE);
        setFullScale(tag.contains("FullScale") ? tag.getFloat("FullScale") : 1.0F);
        setPrevLimbSwingAmount(tag.contains("PrevLimbSwingAmount") ? tag.getFloat("PrevLimbSwingAmount") : 0.0F);
        setLimbSwingAmount(tag.contains("LimbSwingAmount") ? tag.getFloat("LimbSwingAmount") : 0.0F);
        setLegacyLimbSwing(tag.contains("LimbSwing") ? tag.getFloat("LimbSwing") : 0.0F);
        setMouthOpenAmount(tag.contains("MouthOpenAmount") ? tag.getFloat("MouthOpenAmount") : 0.0F);
        this.flightTicks = tag.contains("TicksInAir") ? tag.getInt("TicksInAir") : tag.getInt("FlightTicks");
        setLaunched(tag.getBoolean("Launched"));
        setImpacted(tag.getBoolean("Impacted"));
        this.health = tag.contains("Health") ? tag.getFloat("Health") : getFullScale() * 20.0F;
        this.motionFactor = tag.getFloat("MotionFactor");
        this.acceleration = tag.contains("AccelerationX")
                ? new Vec3(tag.getDouble("AccelerationX"), tag.getDouble("AccelerationY"), tag.getDouble("AccelerationZ"))
                : Vec3.ZERO;
        if (tag.contains("MotionX")) {
            setDeltaMovement(tag.getDouble("MotionX"), tag.getDouble("MotionY"), tag.getDouble("MotionZ"));
        }
        this.temporaryWater.clear();
        if (tag.contains("TemporaryWater", 9)) {
            ListTag list = tag.getList("TemporaryWater", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag waterTag = list.getCompound(i);
                BlockPos pos = new BlockPos(waterTag.getInt("X"), waterTag.getInt("Y"), waterTag.getInt("Z"));
                this.temporaryWater.put(pos, waterTag.getInt("ExpiresAt"));
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        if (this.targetUuid != null) {
            tag.putUUID("Target", this.targetUuid);
        }
        tag.putFloat("Scale", getScale());
        tag.putFloat("FullScale", getFullScale());
        tag.putFloat("PrevLimbSwingAmount", getPrevLimbSwingAmount());
        tag.putFloat("LimbSwingAmount", getLimbSwingAmount());
        tag.putFloat("LimbSwing", getLegacyLimbSwing());
        tag.putFloat("MouthOpenAmount", getMouthOpenAmount());
        tag.putInt("FlightTicks", this.flightTicks);
        tag.putInt("TicksInAir", this.flightTicks);
        tag.putBoolean("Launched", isLaunched());
        tag.putBoolean("Impacted", hasImpacted());
        tag.putFloat("Health", this.health);
        tag.putFloat("MotionFactor", this.motionFactor);
        tag.putDouble("AccelerationX", this.acceleration.x());
        tag.putDouble("AccelerationY", this.acceleration.y());
        tag.putDouble("AccelerationZ", this.acceleration.z());
        Vec3 motion = getDeltaMovement();
        tag.putDouble("MotionX", motion.x());
        tag.putDouble("MotionY", motion.y());
        tag.putDouble("MotionZ", motion.z());
        ListTag list = new ListTag();
        for (Map.Entry<BlockPos, Integer> entry : this.temporaryWater.entrySet()) {
            CompoundTag waterTag = new CompoundTag();
            waterTag.putInt("X", entry.getKey().getX());
            waterTag.putInt("Y", entry.getKey().getY());
            waterTag.putInt("Z", entry.getKey().getZ());
            waterTag.putInt("ExpiresAt", entry.getValue());
            list.add(waterTag);
        }
        tag.put("TemporaryWater", list);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && canRideLegacy(player)) {
            player.startRiding(this, true);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public boolean canBeCollidedWith() {
        return !isRemoved() && !hasImpacted();
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && canRideLegacy(passenger);
    }

    @Override
    public double getPassengersRidingOffset() {
        return Math.max(0.35D, getScale() * 0.35D);
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (!hasPassenger(passenger)) {
            return;
        }
        moveFunction.accept(passenger,
                getX(),
                getY() + getPassengersRidingOffset() + passenger.getMyRidingOffset(),
                getZ());
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

    private void setTarget(@Nullable Entity target) {
        if (target == null) {
            this.targetUuid = null;
            this.entityData.set(TARGET_ID, -1);
            return;
        }
        this.targetUuid = target.getUUID();
        this.entityData.set(TARGET_ID, target.getId());
    }

    @Nullable
    private Entity getTarget() {
        int targetId = this.entityData.get(TARGET_ID);
        if (targetId >= 0) {
            Entity entity = this.level().getEntity(targetId);
            if (entity != null) {
                return entity;
            }
        }
        if (this.targetUuid != null && this.level() instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(this.targetUuid);
            if (entity != null) {
                setTarget(entity);
                return entity;
            }
        }
        return null;
    }

    private void setScale(float scale) {
        this.dimensionsScale = Mth.clamp(scale, MIN_SCALE, 16.0F);
        this.entityData.set(SCALE, this.dimensionsScale);
        refreshDimensions();
    }

    private void setFullScale(float scale) {
        this.entityData.set(FULL_SCALE, Mth.clamp(scale, MIN_SCALE, 16.0F));
    }

    private void setPrevLimbSwingAmount(float amount) {
        this.entityData.set(PREV_LIMB_SWING_AMOUNT, amount);
    }

    private void setLimbSwingAmount(float amount) {
        this.entityData.set(LIMB_SWING_AMOUNT, amount);
    }

    private void setLegacyLimbSwing(float limbSwing) {
        this.entityData.set(LIMB_SWING, limbSwing);
    }

    private void setMouthOpenAmount(float amount) {
        this.entityData.set(MOUTH_OPEN_AMOUNT, Mth.clamp(amount, 0.0F, 1.0F));
    }

    private boolean isLaunched() {
        return this.entityData.get(LAUNCHED);
    }

    private void setLaunched(boolean launched) {
        this.entityData.set(LAUNCHED, launched);
    }

    private boolean hasImpacted() {
        return this.entityData.get(IMPACTED);
    }

    private void setImpacted(boolean impacted) {
        this.entityData.set(IMPACTED, impacted);
    }

    private boolean canRideLegacy(Entity passenger) {
        return passenger instanceof Player
                && !isRemoved()
                && !hasImpacted()
                && getFullScale() >= 2.0F
                && this.flightTicks <= 10;
    }

    private void moveToWaitPosition(LivingEntity owner) {
        Vec3 look = owner.getLookAngle();
        Vec3 origin = owner.getEyePosition().add(look.scale(2.0D));
        this.moveTo(origin.x(), origin.y(), origin.z(), owner.getYHeadRot(), owner.getXRot());
        this.setDeltaMovement(Vec3.ZERO);
    }

    private void updateLaunchMotion(LivingEntity owner) {
        boolean firstLaunchTick = !isLaunched();
        if (firstLaunchTick) {
            setTarget(findLaunchTarget(owner));
            setLaunched(true);
        }
        Entity target = getTarget();
        if (target != null && !target.isAlive()) {
            setTarget(null);
            target = null;
        }
        Vec3 direction = target != null ? targetCenter(target).subtract(position()) : owner.getLookAngle();
        if (direction.lengthSqr() <= 1.0E-8D) {
            direction = owner.getLookAngle();
        }
        float speed = getLaunchSpeed(target != null && !firstLaunchTick);
        this.acceleration = direction.normalize().scale(0.1D);
        this.motionFactor = speed;
        faceDirection(direction, owner);
    }

    @Nullable
    private Entity findLaunchTarget(LivingEntity owner) {
        if (owner instanceof Mob mob && mob.getTarget() != null && canTarget(owner, mob.getTarget())) {
            return mob.getTarget();
        }
        HitResult hit = ProcedureUtils.objectEntityLookingAt(owner, 50.0D, 0.0D, true, false, target -> canTarget(owner, target));
        return hit instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;
    }

    private boolean canTarget(LivingEntity owner, Entity target) {
        return target.isAlive()
                && target != owner
                && target != this
                && target.getRootVehicle() != owner.getRootVehicle()
                && !(target instanceof WaterSharkEntity);
    }

    private Vec3 targetCenter(Entity target) {
        return target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
    }

    private float getLaunchSpeed(boolean trackingExistingTarget) {
        if (trackingExistingTarget) {
            return isInWater() ? 0.85F : 0.8F;
        }
        return isInWater() ? 0.9F : 0.85F;
    }

    private void travelAndImpact() {
        Vec3 start = centerPosition();
        Vec3 motion = getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-8D) {
            Vec3 end = start.add(motion);
            HitResult hit = findImpact(start, end);
            if (hit.getType() != HitResult.Type.MISS) {
                impact();
                return;
            }
            setPos(getX() + motion.x(), getY() + motion.y(), getZ() + motion.z());
        }
        updateLegacyNoGravityMotion();
    }

    private void updateLegacyNoGravityMotion() {
        if (this.motionFactor > 0.0F) {
            if (this.isInWater() && this.level() instanceof ServerLevel serverLevel) {
                Vec3 motion = getDeltaMovement();
                serverLevel.sendParticles(
                        ParticleTypes.BUBBLE,
                        getX() - motion.x() * 0.25D,
                        getY() + getBbHeight() * 0.5D - motion.y() * 0.25D,
                        getZ() - motion.z() * 0.25D,
                        4,
                        0.0D,
                        0.0D,
                        0.0D,
                        0.0D);
            }
            setDeltaMovement(getDeltaMovement().add(this.acceleration).scale(this.motionFactor));
        }
    }

    private void updateMouthOpenAmount() {
        if (this.flightTicks <= 0) {
            setMouthOpenAmount(0.0F);
            return;
        }
        setMouthOpenAmount((float)this.flightTicks / MOUTH_OPEN_TICKS);
    }

    private void updateLegacyAnimationState() {
        float previousAmount = getLimbSwingAmount();
        setPrevLimbSwingAmount(previousAmount);
        double dx = getX() - this.xo;
        double dy = getY() - this.yo;
        double dz = getZ() - this.zo;
        float movement = Mth.sqrt((float)(dx * dx + dy * dy + dz * dz)) * 4.0F;
        if (movement > 1.0F) {
            movement = 1.0F;
        }
        float updatedAmount = previousAmount + (movement - previousAmount) * 0.4F;
        setLimbSwingAmount(updatedAmount);
        setLegacyLimbSwing(getLegacyLimbSwing() + updatedAmount);
    }

    private HitResult findImpact(Vec3 start, Vec3 end) {
        BlockHitResult blockHit = findLegacyBlockImpact(start, end);
        double maxDistance = blockHit.getType() == HitResult.Type.MISS ? start.distanceTo(end) : start.distanceTo(blockHit.getLocation());
        Entity target = getTarget();
        if (target == null || !canDamage(getOwner(), target)) {
            return blockHit;
        }
        Vec3 travel = end.subtract(start);
        return this.level().getEntities(this, getBoundingBox().expandTowards(travel).inflate(1.0D),
                        candidate -> candidate == target)
                .stream()
                .map(entity -> entity.getBoundingBox().inflate(getBbWidth() * 0.5D, getBbHeight() * 0.5D, getBbWidth() * 0.5D).clip(start, end)
                        .map(location -> new EntityHitResult(entity, location))
                        .orElse(null))
                .filter(candidate -> candidate != null && start.distanceTo(candidate.getLocation()) <= maxDistance)
                .min(Comparator.comparingDouble(candidate -> start.distanceTo(candidate.getLocation())))
                .map(HitResult.class::cast)
                .orElse(blockHit);
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

    private boolean canDamage(@Nullable LivingEntity owner, Entity target) {
        return target.isAlive()
                && target.isPickable()
                && !target.noPhysics
                && target != owner
                && target != this
                && target.getRootVehicle() != (owner == null ? null : owner.getRootVehicle())
                && !(target instanceof WaterSharkEntity);
    }

    private void impact() {
        Vec3 point = position();
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            discardShark();
            return;
        }
        LivingEntity owner = getOwner();
        float scale = getScale();
        serverLevel.explode(owner, point.x(), point.y(), point.z(), scale * 2.0F, Level.ExplosionInteraction.MOB);
        DamageSource source = ModDamageTypes.ninjutsu(serverLevel, this, owner);
        AABB damageBox = new AABB(point, point).inflate(Math.max(0.5D, scale));
        float damage = scale * (isInWater() ? 24.0F : 16.0F);
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, damageBox, target -> canDamage(owner, target))) {
            target.hurt(source, damage);
        }
        setImpacted(true);
        setDeltaMovement(Vec3.ZERO);
        this.acceleration = Vec3.ZERO;
        this.motionFactor = 0.0F;
        setPos(point.x(), point.y(), point.z());
        placeTemporaryImpactWater(serverLevel, point);
        if (this.temporaryWater.isEmpty()) {
            discard();
        }
    }

    private void placeTemporaryImpactWater(ServerLevel level, Vec3 point) {
        AABB box = this.getBoundingBox().move(point.subtract(position())).contract(0.0D, Math.max(getBbHeight() - 1.0D, 0.0D), 0.0D);
        BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
        BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
        for (BlockPos mutablePos : BlockPos.betweenClosed(min, max)) {
            BlockPos pos = mutablePos.immutable();
            if (level.getBlockState(pos).isAir()) {
                level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
                this.temporaryWater.put(pos, this.tickCount + TEMPORARY_WATER_LIFE_TICKS);
            }
        }
    }

    private void expireTemporaryWater() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        this.temporaryWater.entrySet().removeIf(entry -> {
            if (entry.getValue() > this.tickCount) {
                return false;
            }
            removeTemporaryWater(level, entry.getKey());
            return true;
        });
    }

    private void clearTemporaryWater() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        for (BlockPos pos : this.temporaryWater.keySet()) {
            removeTemporaryWater(level, pos);
        }
        this.temporaryWater.clear();
    }

    private void removeTemporaryWater(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).is(Blocks.WATER)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private void spawnWaitingParticles() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        level.sendParticles(ParticleTypes.SPLASH, getX(), getY() + getBbHeight() * 0.5D, getZ(), 10,
                collisionRadius(), Math.max(0.25D, getScale() * 0.25D), collisionRadius(), 0.0D);
    }

    private void spawnLaunchedParticles() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        level.sendParticles(ParticleTypes.FALLING_WATER, getX(), getY() + getBbHeight() * 0.5D, getZ(), 100,
                collisionRadius(), Math.max(0.25D, getScale() * 0.25D), collisionRadius(), 0.0D);
    }

    private double collisionRadius() {
        return Math.max(0.5D, getScale() * 0.5D);
    }

    private void faceDirection(Vec3 direction, @Nullable LivingEntity owner) {
        if (direction.lengthSqr() <= 1.0E-8D) {
            if (owner != null) {
                setYRot(owner.getYRot());
                setXRot(owner.getXRot());
            }
            return;
        }
        setYRot((float)(-Mth.atan2(direction.x(), direction.z()) * Mth.RAD_TO_DEG));
        setXRot((float)(-Mth.atan2(direction.y(), Math.sqrt(direction.x() * direction.x() + direction.z() * direction.z())) * Mth.RAD_TO_DEG));
    }

    private Vec3 centerPosition() {
        return position().add(0.0D, getBbHeight() * 0.5D, 0.0D);
    }

    private void discardShark() {
        clearTemporaryWater();
        discard();
    }
}
