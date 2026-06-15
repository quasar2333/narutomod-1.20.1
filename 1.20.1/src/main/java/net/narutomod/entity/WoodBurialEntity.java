package net.narutomod.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class WoodBurialEntity extends Entity {
    private static final double RANGE = 20.0D;
    private static final int LIFETIME_TICKS = 1200;
    private static final int ROOT_BLOCKS_PER_TICK = 4;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(WoodBurialEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(WoodBurialEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private UUID targetUuid;
    @Nullable
    private Vec3 capturedPosition;
    private int rootIndex;
    private final List<BlockPos> rootPlan = new ArrayList<>();
    private final List<PlacedRootBlock> placedRoots = new ArrayList<>();

    public WoodBurialEntity(EntityType<? extends WoodBurialEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public static boolean spawnFrom(LivingEntity owner) {
        LivingEntity target = findTarget(owner);
        if (target == null) {
            return false;
        }
        WoodBurialEntity burial = ModEntityTypes.WOOD_BURIAL.get().create(owner.level());
        if (burial == null) {
            return false;
        }
        burial.configure(owner, target);
        owner.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                ModSounds.SOUND_WOODGROW.get(), SoundSource.BLOCKS, 1.0F, owner.getRandom().nextFloat() * 0.4F + 0.6F);
        owner.level().addFreshEntity(burial);
        return true;
    }

    @Nullable
    public static LivingEntity findTarget(LivingEntity owner) {
        HitResult hit = ProcedureUtils.objectEntityLookingAt(owner, RANGE, 1.0D, true, false,
                target -> target instanceof LivingEntity living && living != owner && living.isAlive()
                        && !(target instanceof WoodBurialEntity));
        return hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living ? living : null;
    }

    public void configure(LivingEntity owner, LivingEntity target) {
        setOwner(owner);
        setTarget(target);
        this.capturedPosition = target.position();
        moveTo(target.getX(), target.getY() - 0.5D, target.getZ(), 0.0F, 0.0F);
        rebuildRootPlan(target);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(TARGET_ID, -1);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || this.tickCount >= LIFETIME_TICKS) {
            discard();
            return;
        }
        holdAndDamageTarget(target);
        placeNextRootBlocks();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && reason.shouldDestroy()) {
            clearPlacedRoots();
        }
        super.remove(reason);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        if (tag.hasUUID("Target")) {
            this.targetUuid = tag.getUUID("Target");
        }
        if (tag.contains("CapturedX") && tag.contains("CapturedY") && tag.contains("CapturedZ")) {
            this.capturedPosition = new Vec3(tag.getDouble("CapturedX"), tag.getDouble("CapturedY"), tag.getDouble("CapturedZ"));
            rebuildRootPlan(this.capturedPosition);
        }
        this.rootIndex = tag.getInt("RootIndex");
        this.placedRoots.clear();
        if (tag.contains("PlacedRoots", 9)) {
            ListTag list = tag.getList("PlacedRoots", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag rootTag = list.getCompound(i);
                this.placedRoots.add(new PlacedRootBlock(readBlockPos(rootTag), rootTag.getBoolean("Log")));
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
        if (this.capturedPosition != null) {
            tag.putDouble("CapturedX", this.capturedPosition.x());
            tag.putDouble("CapturedY", this.capturedPosition.y());
            tag.putDouble("CapturedZ", this.capturedPosition.z());
        }
        tag.putInt("RootIndex", this.rootIndex);
        ListTag list = new ListTag();
        for (PlacedRootBlock placedRoot : this.placedRoots) {
            CompoundTag rootTag = writeBlockPos(placedRoot.pos());
            rootTag.putBoolean("Log", placedRoot.log());
            list.add(rootTag);
        }
        tag.put("PlacedRoots", list);
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

    @Nullable
    public LivingEntity getTargetForRender() {
        return getTarget();
    }

    private void holdAndDamageTarget(LivingEntity target) {
        Vec3 position = this.capturedPosition;
        if (position == null) {
            position = target.position();
            this.capturedPosition = position;
            rebuildRootPlan(position);
        }
        target.hurt(this.damageSources().inWall(), 1.0F);
        target.teleportTo(position.x(), position.y(), position.z());
        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
        moveTo(position.x(), position.y() - 0.5D, position.z(), getYRot(), getXRot());
    }

    private void placeNextRootBlocks() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        int placedThisTick = 0;
        while (placedThisTick < ROOT_BLOCKS_PER_TICK && this.rootIndex < this.rootPlan.size()) {
            BlockPos pos = this.rootPlan.get(this.rootIndex++);
            if (level.hasChunkAt(pos) && level.getBlockState(pos).isAir()) {
                boolean log = shouldPlaceLog(pos);
                BlockState state = log ? Blocks.OAK_LOG.defaultBlockState() : rootLeaves();
                level.setBlock(pos, state, 3);
                this.placedRoots.add(new PlacedRootBlock(pos, log));
                spawnRootParticles(level, pos, state);
                placedThisTick++;
            }
        }
    }

    private void clearPlacedRoots() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        for (PlacedRootBlock placedRoot : this.placedRoots) {
            BlockState state = level.getBlockState(placedRoot.pos());
            if (placedRoot.log() && state.is(Blocks.OAK_LOG) || !placedRoot.log() && state.is(Blocks.OAK_LEAVES)) {
                level.setBlock(placedRoot.pos(), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        this.placedRoots.clear();
    }

    private void rebuildRootPlan(LivingEntity target) {
        rebuildRootPlan(target.position());
    }

    private void rebuildRootPlan(Vec3 targetPosition) {
        this.rootPlan.clear();
        BlockPos center = BlockPos.containing(targetPosition);
        for (int y = -1; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (Math.abs(x) + Math.abs(z) <= 2) {
                        this.rootPlan.add(center.offset(x, y, z));
                    }
                }
            }
        }
        for (int y = 0; y <= 4; y++) {
            this.rootPlan.add(center.offset(2, y, 0));
            this.rootPlan.add(center.offset(-2, y, 0));
            this.rootPlan.add(center.offset(0, y, 2));
            this.rootPlan.add(center.offset(0, y, -2));
        }
        if (this.rootIndex > this.rootPlan.size()) {
            this.rootIndex = this.rootPlan.size();
        }
    }

    private boolean shouldPlaceLog(BlockPos pos) {
        return this.capturedPosition != null
                && BlockPos.containing(this.capturedPosition).getX() == pos.getX()
                && BlockPos.containing(this.capturedPosition).getZ() == pos.getZ();
    }

    private static BlockState rootLeaves() {
        return Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    private static void spawnRootParticles(ServerLevel level, BlockPos pos, BlockState state) {
        level.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, state),
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                3,
                0.2D,
                0.2D,
                0.2D,
                0.04D);
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

    private record PlacedRootBlock(BlockPos pos, boolean log) {
    }
}
