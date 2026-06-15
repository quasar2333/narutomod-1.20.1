package net.narutomod.entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.Chakra;
import net.narutomod.NarutomodModVariables;
import net.narutomod.procedure.ProcedureSync;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModBlocks;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class WaterPrisonEntity extends Entity {
    private static final int DEFAULT_DURATION_TICKS = 3600;
    private static final int TEMPORARY_WATER_LIFE_TICKS = 10;
    private static final double MAX_HOLD_DISTANCE = 4.0D;
    private static final double CHAKRA_BURN_PER_SECOND = 20.0D;
    private static final Map<UUID, UUID> TRAPPED_BY_TRAPPER = new HashMap<>();
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(WaterPrisonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(WaterPrisonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(WaterPrisonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RELEASED = SynchedEntityData.defineId(WaterPrisonEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private UUID targetUuid;
    private AABB prisonBox = new AABB(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
    private int totalWaterBlocks;
    private boolean forceBowPoseSynced;
    private final Set<BlockPos> prisonWater = new HashSet<>();
    private final Map<BlockPos, Integer> temporaryWater = new HashMap<>();

    public WaterPrisonEntity(EntityType<? extends WaterPrisonEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public void configure(LivingEntity owner, LivingEntity target, int duration) {
        setOwner(owner);
        setTarget(target);
        this.entityData.set(DURATION, duration);
        if (!this.level().getBlockState(target.blockPosition().below()).isAir()) {
            target.teleportTo(target.getX(), target.getY() + 0.5D, target.getZ());
        }
        moveTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
        this.prisonBox = createPrisonBox(target);
        placePrisonWater();
        TRAPPED_BY_TRAPPER.put(owner.getUUID(), target.getUUID());
    }

    public static boolean spawnFrom(LivingEntity owner, LivingEntity target) {
        return spawnFrom(owner, target, DEFAULT_DURATION_TICKS);
    }

    public static boolean spawnFrom(LivingEntity owner, LivingEntity target, int duration) {
        if (!canTrap(owner, target)) {
            return false;
        }
        WaterPrisonEntity entity = ModEntityTypes.WATER_PRISON.get().create(owner.level());
        if (entity == null) {
            return false;
        }
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                ModSounds.SOUND_SUIRONOJUTSU.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        entity.configure(owner, target, duration);
        owner.level().addFreshEntity(entity);
        return true;
    }

    @Nullable
    public static LivingEntity findTarget(LivingEntity owner) {
        if (owner instanceof Mob mob && mob.getTarget() != null && mob.distanceTo(mob.getTarget()) <= MAX_HOLD_DISTANCE) {
            return mob.getTarget();
        }
        HitResult hit = ProcedureUtils.objectEntityLookingAt(owner, MAX_HOLD_DISTANCE, 0.0D, true, false,
                target -> target instanceof LivingEntity living && canTrap(owner, living));
        return hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living ? living : null;
    }

    public static boolean isEntityTrapped(LivingEntity entity) {
        return TRAPPED_BY_TRAPPER.containsValue(entity.getUUID());
    }

    public static boolean isEntityTrappedBy(LivingEntity trapper, LivingEntity target) {
        return target.getUUID().equals(TRAPPED_BY_TRAPPER.get(trapper.getUUID()));
    }

    public static boolean isEntityTrapping(LivingEntity trapper) {
        return TRAPPED_BY_TRAPPER.containsKey(trapper.getUUID());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(TARGET_ID, -1);
        this.entityData.define(DURATION, DEFAULT_DURATION_TICKS);
        this.entityData.define(RELEASED, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        expireTemporaryWater();
        if (isReleased()) {
            if (this.temporaryWater.isEmpty()) {
                discard();
            }
            return;
        }

        LivingEntity owner = getOwner();
        LivingEntity target = getTarget();
        if (owner == null || target == null || !canContinue(owner, target)) {
            releasePrison();
            return;
        }

        TRAPPED_BY_TRAPPER.put(owner.getUUID(), target.getUUID());
        target.teleportTo(getX(), getY(), getZ());
        pushOtherEntities(target);
        spawnWaterParticles();
        syncForceBowPose(owner, true);
        if (this.tickCount > 0 && this.tickCount % 20 == 0) {
            Chakra.pathway(owner).consume(CHAKRA_BURN_PER_SECOND);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && reason.shouldDestroy()) {
            clearAllWater();
            clearTrapState();
        }
        super.remove(reason);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 64.0D * getViewScale();
        return distance < range * range;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        if (tag.hasUUID("Target")) {
            this.targetUuid = tag.getUUID("Target");
        }
        this.entityData.set(DURATION, tag.contains("Duration") ? tag.getInt("Duration") : DEFAULT_DURATION_TICKS);
        this.entityData.set(RELEASED, tag.getBoolean("Released"));
        this.forceBowPoseSynced = tag.getBoolean("ForceBowPoseSynced");
        this.totalWaterBlocks = tag.getInt("TotalWaterBlocks");
        this.prisonBox = new AABB(
                tag.getDouble("MinX"),
                tag.getDouble("MinY"),
                tag.getDouble("MinZ"),
                tag.getDouble("MaxX"),
                tag.getDouble("MaxY"),
                tag.getDouble("MaxZ"));
        this.prisonWater.clear();
        if (tag.contains("PrisonWater", 9)) {
            ListTag list = tag.getList("PrisonWater", 10);
            for (int i = 0; i < list.size(); i++) {
                this.prisonWater.add(readBlockPos(list.getCompound(i)));
            }
        }
        this.temporaryWater.clear();
        if (tag.contains("TemporaryWater", 9)) {
            ListTag list = tag.getList("TemporaryWater", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag waterTag = list.getCompound(i);
                this.temporaryWater.put(readBlockPos(waterTag), waterTag.getInt("ExpiresAt"));
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
        tag.putInt("Duration", this.entityData.get(DURATION));
        tag.putBoolean("Released", isReleased());
        tag.putBoolean("ForceBowPoseSynced", this.forceBowPoseSynced);
        tag.putInt("TotalWaterBlocks", this.totalWaterBlocks);
        tag.putDouble("MinX", this.prisonBox.minX);
        tag.putDouble("MinY", this.prisonBox.minY);
        tag.putDouble("MinZ", this.prisonBox.minZ);
        tag.putDouble("MaxX", this.prisonBox.maxX);
        tag.putDouble("MaxY", this.prisonBox.maxY);
        tag.putDouble("MaxZ", this.prisonBox.maxZ);
        ListTag prisonList = new ListTag();
        for (BlockPos pos : this.prisonWater) {
            prisonList.add(writeBlockPos(pos));
        }
        tag.put("PrisonWater", prisonList);
        ListTag tempList = new ListTag();
        for (Map.Entry<BlockPos, Integer> entry : this.temporaryWater.entrySet()) {
            CompoundTag waterTag = writeBlockPos(entry.getKey());
            waterTag.putInt("ExpiresAt", entry.getValue());
            tempList.add(waterTag);
        }
        tag.put("TemporaryWater", tempList);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private static boolean canTrap(LivingEntity owner, LivingEntity target) {
        return owner.isAlive()
                && target.isAlive()
                && owner != target
                && !isEntityTrapped(target)
                && !isEntityTrapping(owner)
                && owner.distanceTo(target) <= MAX_HOLD_DISTANCE;
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

    private void setTarget(LivingEntity target) {
        this.targetUuid = target.getUUID();
        this.entityData.set(TARGET_ID, target.getId());
    }

    @Nullable
    private LivingEntity getTarget() {
        int targetId = this.entityData.get(TARGET_ID);
        if (targetId >= 0 && this.level().getEntity(targetId) instanceof LivingEntity living) {
            return living;
        }
        if (this.targetUuid != null && this.level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(this.targetUuid) instanceof LivingEntity living) {
            setTarget(living);
            return living;
        }
        return null;
    }

    private boolean isReleased() {
        return this.entityData.get(RELEASED);
    }

    private void setReleased(boolean released) {
        this.entityData.set(RELEASED, released);
    }

    private boolean canContinue(LivingEntity owner, LivingEntity target) {
        return owner.isAlive()
                && target.isAlive()
                && owner.distanceTo(target) <= MAX_HOLD_DISTANCE
                && ProcedureUtils.isEntityInFOV(owner, target)
                && enoughWaterRemaining()
                && this.tickCount <= this.entityData.get(DURATION);
    }

    private AABB createPrisonBox(LivingEntity target) {
        AABB box = target.getBoundingBox();
        return new AABB(
                Math.floor(box.minX - 0.5D),
                Math.floor(box.minY - 0.5D),
                Math.floor(box.minZ - 0.5D),
                Math.ceil(box.maxX + 0.5D),
                Math.ceil(box.maxY + 0.5D),
                Math.ceil(box.maxZ + 0.5D));
    }

    private void placePrisonWater() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        this.prisonWater.clear();
        BlockPos min = BlockPos.containing(this.prisonBox.minX, this.prisonBox.minY, this.prisonBox.minZ);
        BlockPos max = BlockPos.containing(this.prisonBox.maxX, this.prisonBox.maxY, this.prisonBox.maxZ);
        for (BlockPos mutablePos : BlockPos.betweenClosed(min, max)) {
            BlockPos pos = mutablePos.immutable();
            if (level.getBlockState(pos).isAir()) {
                level.setBlock(pos, ModBlocks.WATER_STILL.get().defaultBlockState(), 3);
                this.prisonWater.add(pos);
            }
        }
        this.totalWaterBlocks = this.prisonWater.size();
    }

    private boolean enoughWaterRemaining() {
        if (!(this.level() instanceof ServerLevel level) || this.totalWaterBlocks <= 0) {
            return true;
        }
        int remaining = 0;
        for (BlockPos pos : this.prisonWater) {
            if (level.getBlockState(pos).is(ModBlocks.WATER_STILL.get())) {
                remaining++;
            }
        }
        return remaining >= this.totalWaterBlocks * 2 / 3;
    }

    private void pushOtherEntities(LivingEntity target) {
        for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, this.prisonBox, entity -> entity != target)) {
            Vec3 away = entity.position().subtract(position());
            if (away.lengthSqr() <= 1.0E-8D) {
                continue;
            }
            Vec3 push = away.normalize().scale(0.2D);
            ProcedureUtils.setVelocity(entity, entity.getDeltaMovement().x() + push.x(), entity.getDeltaMovement().y(), entity.getDeltaMovement().z() + push.z());
        }
    }

    private void releasePrison() {
        if (isReleased()) {
            return;
        }
        if (this.level() instanceof ServerLevel level) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL, 1.0F, 1.0F);
            clearPrisonWater(level);
            placeTemporaryFloorWater(level);
        }
        clearTrapState();
        setReleased(true);
        if (this.temporaryWater.isEmpty()) {
            discard();
        }
    }

    private void clearTrapState() {
        LivingEntity owner = getOwner();
        if (owner != null) {
            TRAPPED_BY_TRAPPER.remove(owner.getUUID());
            syncForceBowPose(owner, false);
        } else if (this.ownerUuid != null) {
            TRAPPED_BY_TRAPPER.remove(this.ownerUuid);
        }
    }

    private void clearPrisonWater(ServerLevel level) {
        for (BlockPos pos : this.prisonWater) {
            if (isOwnedWaterBlock(level, pos)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        this.prisonWater.clear();
    }

    private void placeTemporaryFloorWater(ServerLevel level) {
        int y = MthFloor(this.prisonBox.minY);
        for (int x = MthFloor(this.prisonBox.minX); x <= MthFloor(this.prisonBox.maxX); x++) {
            for (int z = MthFloor(this.prisonBox.minZ); z <= MthFloor(this.prisonBox.maxZ); z++) {
                BlockPos pos = new BlockPos(x, y, z);
                if (level.getBlockState(pos).isAir()) {
                    level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
                    this.temporaryWater.put(pos, this.tickCount + TEMPORARY_WATER_LIFE_TICKS);
                }
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

    private void clearAllWater() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        clearPrisonWater(level);
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

    private static boolean isOwnedWaterBlock(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(ModBlocks.WATER_STILL.get()) || level.getBlockState(pos).is(Blocks.WATER);
    }

    private void spawnWaterParticles() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        level.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 1.0D, getZ(), 8,
                Math.max(0.5D, this.prisonBox.getXsize() * 0.25D),
                Math.max(0.5D, this.prisonBox.getYsize() * 0.25D),
                Math.max(0.5D, this.prisonBox.getZsize() * 0.25D),
                0.01D);
    }

    private void syncForceBowPose(LivingEntity owner, boolean force) {
        if (force && !this.forceBowPoseSynced) {
            ProcedureSync.EntityNBTTag.setAndSync(owner, NarutomodModVariables.FORCE_BOW_POSE, true);
            this.forceBowPoseSynced = true;
        } else if (!force && this.forceBowPoseSynced) {
            ProcedureSync.EntityNBTTag.removeAndSync(owner, NarutomodModVariables.FORCE_BOW_POSE);
            this.forceBowPoseSynced = false;
        }
    }

    private static CompoundTag writeBlockPos(BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("X", pos.getX());
        tag.putInt("Y", pos.getY());
        tag.putInt("Z", pos.getZ());
        return tag;
    }

    private static BlockPos readBlockPos(CompoundTag tag) {
        return new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
    }

    private static int MthFloor(double value) {
        return (int)Math.floor(value);
    }
}
