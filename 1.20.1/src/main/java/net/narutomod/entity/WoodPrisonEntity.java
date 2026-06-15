package net.narutomod.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class WoodPrisonEntity extends Entity {
    private static final double RANGE = 20.0D;
    private static final int WOOD_LIFETIME_TICKS = 1200;
    private static final int BLOCKS_PER_TICK = 128;
    private static final int BLOCKS_SCANNED_PER_TICK = BLOCKS_PER_TICK * 4;

    @Nullable
    private BlockPos origin;
    private int radius;
    private int targetHeight;
    private int buildIndex;
    private final List<BlockPos> buildPlan = new ArrayList<>();
    private final Map<BlockPos, PlacedWood> placedWood = new HashMap<>();
    private final Map<UUID, Vec3> capturedPositions = new HashMap<>();

    public WoodPrisonEntity(EntityType<? extends WoodPrisonEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public void configure(LivingEntity owner, Vec3 targetVec, float power) {
        moveTo(targetVec.x(), targetVec.y(), targetVec.z(), owner.getYRot(), owner.getXRot());
        this.origin = BlockPos.containing(targetVec);
        float clampedPower = Mth.clamp(power, 1.0F, 50.0F);
        this.radius = Math.max(Mth.ceil(clampedPower * 0.5F), 1);
        this.targetHeight = Math.max(Mth.ceil(clampedPower - 0.5F), 1);
        this.buildIndex = 0;
        captureEntities(owner);
        rebuildBuildPlan();
    }

    public static boolean spawnFrom(LivingEntity owner, float power) {
        BlockHitResult hit = findTarget(owner);
        if (hit == null) {
            return false;
        }
        WoodPrisonEntity prison = ModEntityTypes.WOOD_PRISON.get().create(owner.level());
        if (prison == null) {
            return false;
        }
        prison.configure(owner, hit.getLocation(), power);
        owner.level().playSound(null, hit.getLocation().x(), hit.getLocation().y(), hit.getLocation().z(),
                ModSounds.SOUND_WOODSPAWN.get(), SoundSource.BLOCKS, 1.0F, owner.getRandom().nextFloat() * 0.4F + 0.8F);
        owner.level().addFreshEntity(prison);
        return true;
    }

    @Nullable
    public static BlockHitResult findTarget(LivingEntity owner) {
        BlockHitResult hit = ProcedureUtils.raytraceBlocks(owner, RANGE);
        return hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (this.origin == null || this.radius <= 0 || this.targetHeight <= 0) {
            discard();
            return;
        }
        expirePlacedWood();
        if (this.buildIndex < this.buildPlan.size()) {
            holdCapturedEntities();
            placeNextWoodBlocks();
            return;
        }
        if (this.placedWood.isEmpty()) {
            discard();
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && reason.shouldDestroy()) {
            clearPlacedWood();
        }
        super.remove(reason);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Origin", 10)) {
            this.origin = readBlockPos(tag.getCompound("Origin"));
        }
        this.radius = tag.getInt("Radius");
        this.targetHeight = tag.getInt("TargetHeight");
        this.buildIndex = tag.getInt("BuildIndex");
        rebuildBuildPlan();
        this.placedWood.clear();
        if (tag.contains("PlacedWood", 9)) {
            ListTag list = tag.getList("PlacedWood", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag woodTag = list.getCompound(i);
                this.placedWood.put(readBlockPos(woodTag), new PlacedWood(woodTag.getInt("ExpiresAt"), woodTag.getBoolean("Slab")));
            }
        }
        this.capturedPositions.clear();
        if (tag.contains("Captured", 9)) {
            ListTag list = tag.getList("Captured", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag capturedTag = list.getCompound(i);
                if (capturedTag.hasUUID("UUID")) {
                    this.capturedPositions.put(capturedTag.getUUID("UUID"),
                            new Vec3(capturedTag.getDouble("X"), capturedTag.getDouble("Y"), capturedTag.getDouble("Z")));
                }
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.origin != null) {
            tag.put("Origin", writeBlockPos(this.origin));
        }
        tag.putInt("Radius", this.radius);
        tag.putInt("TargetHeight", this.targetHeight);
        tag.putInt("BuildIndex", this.buildIndex);
        ListTag placedList = new ListTag();
        for (Map.Entry<BlockPos, PlacedWood> entry : this.placedWood.entrySet()) {
            CompoundTag woodTag = writeBlockPos(entry.getKey());
            woodTag.putInt("ExpiresAt", entry.getValue().expiresAt());
            woodTag.putBoolean("Slab", entry.getValue().slab());
            placedList.add(woodTag);
        }
        tag.put("PlacedWood", placedList);
        ListTag capturedList = new ListTag();
        for (Map.Entry<UUID, Vec3> entry : this.capturedPositions.entrySet()) {
            CompoundTag capturedTag = new CompoundTag();
            capturedTag.putUUID("UUID", entry.getKey());
            capturedTag.putDouble("X", entry.getValue().x());
            capturedTag.putDouble("Y", entry.getValue().y());
            capturedTag.putDouble("Z", entry.getValue().z());
            capturedList.add(capturedTag);
        }
        tag.put("Captured", capturedList);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void captureEntities(LivingEntity owner) {
        if (this.origin == null) {
            return;
        }
        AABB box = new AABB(
                this.origin.getX() - this.radius,
                this.origin.getY(),
                this.origin.getZ() - this.radius,
                this.origin.getX() + this.radius + 1.0D,
                this.origin.getY() + this.targetHeight + 1.0D,
                this.origin.getZ() + this.radius + 1.0D);
        for (LivingEntity entity : owner.level().getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            this.capturedPositions.put(entity.getUUID(), entity.position());
        }
    }

    private void holdCapturedEntities() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        for (Map.Entry<UUID, Vec3> entry : this.capturedPositions.entrySet()) {
            if (level.getEntity(entry.getKey()) instanceof LivingEntity entity && entity.isAlive()) {
                Vec3 pos = entry.getValue();
                entity.teleportTo(pos.x(), pos.y(), pos.z());
                if (this.tickCount % 20 == 1) {
                    entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, WOOD_LIFETIME_TICKS, 2, false, false));
                }
            }
        }
    }

    private void placeNextWoodBlocks() {
        if (!(this.level() instanceof ServerLevel level) || this.origin == null) {
            return;
        }
        int placedThisTick = 0;
        int scannedThisTick = 0;
        while (placedThisTick < BLOCKS_PER_TICK
                && scannedThisTick < BLOCKS_SCANNED_PER_TICK
                && this.buildIndex < this.buildPlan.size()) {
            BlockPos pos = this.buildPlan.get(this.buildIndex++);
            scannedThisTick++;
            if (level.hasChunkAt(pos) && level.getBlockState(pos).isAir()) {
                BlockState state = woodStateFor(pos);
                level.setBlock(pos, state, 3);
                this.placedWood.put(pos, new PlacedWood(this.tickCount + WOOD_LIFETIME_TICKS, state.is(Blocks.OAK_SLAB)));
                spawnWoodParticles(level, pos, state);
                placedThisTick++;
            }
        }
        if (placedThisTick > 0 && this.tickCount % 20 == 1) {
            level.playSound(null, getX(), getY(), getZ(), ModSounds.SOUND_WOODSPAWN.get(),
                    SoundSource.BLOCKS, 1.0F, this.random.nextFloat() * 0.4F + 0.8F);
        }
    }

    private void expirePlacedWood() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        this.placedWood.entrySet().removeIf(entry -> {
            if (entry.getValue().expiresAt() > this.tickCount) {
                return false;
            }
            removeWoodBlock(level, entry.getKey(), entry.getValue());
            return true;
        });
    }

    private void clearPlacedWood() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        for (Map.Entry<BlockPos, PlacedWood> entry : this.placedWood.entrySet()) {
            removeWoodBlock(level, entry.getKey(), entry.getValue());
        }
        this.placedWood.clear();
    }

    private void removeWoodBlock(ServerLevel level, BlockPos pos, PlacedWood placed) {
        BlockState state = level.getBlockState(pos);
        if (placed.slab() && state.is(Blocks.OAK_SLAB) || !placed.slab() && state.is(Blocks.OAK_FENCE)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private void rebuildBuildPlan() {
        this.buildPlan.clear();
        if (this.origin == null || this.radius <= 0 || this.targetHeight <= 0) {
            return;
        }
        for (int y = 0; y <= this.targetHeight; y++) {
            for (int x = -this.radius; x <= this.radius; x++) {
                for (int z = -this.radius; z <= this.radius; z++) {
                    this.buildPlan.add(this.origin.offset(x, y, z));
                }
            }
        }
        if (this.buildIndex > this.buildPlan.size()) {
            this.buildIndex = this.buildPlan.size();
        }
    }

    private BlockState woodStateFor(BlockPos pos) {
        if (this.origin != null && pos.getY() - this.origin.getY() == this.targetHeight) {
            return Blocks.OAK_SLAB.defaultBlockState();
        }
        return Blocks.OAK_FENCE.defaultBlockState();
    }

    private void spawnWoodParticles(ServerLevel level, BlockPos pos, BlockState state) {
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

    private record PlacedWood(int expiresAt, boolean slab) {
    }
}
