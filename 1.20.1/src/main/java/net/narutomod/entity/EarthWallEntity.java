package net.narutomod.entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
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
import net.narutomod.registry.ModSounds;

public final class EarthWallEntity extends Entity {
    private static final int BLOCKS_PER_TICK = 128;
    private static final int REMOVE_DELAY_TICKS = 1200;

    private final List<BlockPos> buildLayer = new ArrayList<>();
    private final List<BlockPos> nextLayer = new ArrayList<>();
    private final List<BlockPos> placedBlocks = new ArrayList<>();
    private int wallHeight;
    private boolean dieOnDone = true;
    private int builtLayers;
    private int doneTick = -1;
    private boolean cleanedUp;

    public EarthWallEntity(EntityType<? extends EarthWallEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public void configure(Vec3 center, float yaw, double width, boolean autoRemove) {
        double height = width * 0.6D;
        double thickness = Math.max(width * 0.25D, 1.0D);
        configure(center, yaw, width, height, thickness, autoRemove);
    }

    public void configure(Vec3 center, float yaw, double width, double height, double thickness, boolean autoRemove) {
        this.dieOnDone = autoRemove;
        this.wallHeight = Math.max((int)height, 1);
        this.builtLayers = 0;
        this.doneTick = -1;
        this.cleanedUp = false;
        this.buildLayer.clear();
        this.nextLayer.clear();
        this.placedBlocks.clear();
        moveTo(center.x(), center.y(), center.z(), yaw, 0.0F);
        collectBaseLayer(center, yaw, width, Math.max((thickness - 1.0D) * 0.5D, 0.5D), autoRemove);
    }

    public static boolean hasBlockTarget(LivingEntity owner) {
        HitResult result = ProcedureUtils.raytraceBlocks(owner, 30.0D);
        return result instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK;
    }

    public static boolean hasBuildableTarget(LivingEntity owner, float power) {
        return createConfiguredFrom(owner, power) != null;
    }

    public static boolean spawnFrom(LivingEntity owner, float power) {
        EarthWallEntity wall = createConfiguredFrom(owner, power);
        if (wall == null || !(owner.level() instanceof ServerLevel level)) {
            return false;
        }
        level.addFreshEntity(wall);
        return true;
    }

    @Nullable
    private static EarthWallEntity createConfiguredFrom(LivingEntity owner, float power) {
        HitResult result = ProcedureUtils.raytraceBlocks(owner, 30.0D);
        if (!(result instanceof BlockHitResult blockHit) || blockHit.getType() != HitResult.Type.BLOCK
                || !(owner.level() instanceof ServerLevel level)) {
            return null;
        }
        EarthWallEntity wall = net.narutomod.registry.ModEntityTypes.ENTITYEARTHWALL.get().create(level);
        if (wall == null) {
            return null;
        }
        wall.configure(blockHit.getLocation(), owner.getYRot(), power, true);
        if (wall.buildLayer.isEmpty()) {
            return null;
        }
        return wall;
    }

    public boolean isDone() {
        return !this.level().isClientSide && this.buildLayer.isEmpty() && this.doneTick >= 0;
    }

    public boolean hasBuildPlan() {
        return !this.buildLayer.isEmpty();
    }

    public List<BlockPos> getPlacedBlocks() {
        return List.copyOf(this.placedBlocks);
    }

    public void releasePlacedBlocksToController() {
        this.cleanedUp = true;
        this.placedBlocks.clear();
        discard();
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
        if (!this.buildLayer.isEmpty()) {
            buildNextBlocks();
            return;
        }
        if (this.doneTick < 0) {
            this.doneTick = this.tickCount;
        }
        if (this.dieOnDone && this.tickCount - this.doneTick >= REMOVE_DELAY_TICKS) {
            cleanupPlacedBlocks();
            discard();
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && !this.cleanedUp && reason.shouldDestroy()) {
            cleanupPlacedBlocks();
        }
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.wallHeight = tag.getInt("WallHeight");
        this.dieOnDone = !tag.contains("DieOnDone") || tag.getBoolean("DieOnDone");
        this.builtLayers = tag.getInt("BuiltLayers");
        this.doneTick = tag.contains("DoneTick") ? tag.getInt("DoneTick") : -1;
        this.cleanedUp = tag.getBoolean("CleanedUp");
        readPositions(tag, "BuildLayer", this.buildLayer);
        readPositions(tag, "NextLayer", this.nextLayer);
        readPositions(tag, "PlacedBlocks", this.placedBlocks);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("WallHeight", this.wallHeight);
        tag.putBoolean("DieOnDone", this.dieOnDone);
        tag.putInt("BuiltLayers", this.builtLayers);
        tag.putInt("DoneTick", this.doneTick);
        tag.putBoolean("CleanedUp", this.cleanedUp);
        writePositions(tag, "BuildLayer", this.buildLayer);
        writePositions(tag, "NextLayer", this.nextLayer);
        writePositions(tag, "PlacedBlocks", this.placedBlocks);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void collectBaseLayer(Vec3 center, float yaw, double width, double halfThickness, boolean autoRemove) {
        Vec3 line = Vec3.directionFromRotation(0.0F, yaw - 90.0F).normalize();
        Vec3 forward = new Vec3(-line.z(), 0.0D, line.x()).normalize();
        double halfWidth = width * 0.5D;
        int radius = (int)Math.ceil(halfWidth + halfThickness + 1.0D);
        int y = BlockPos.containing(center).getY();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos pos = BlockPos.containing(center.x() + dx, y, center.z() + dz);
                Vec3 rel = new Vec3(pos.getX() + 0.5D - center.x(), 0.0D, pos.getZ() + 0.5D - center.z());
                double along = rel.dot(line);
                double across = rel.dot(forward);
                if (Math.abs(along) <= halfWidth && Math.abs(across) <= halfThickness) {
                    BlockPos base = findBuildBase(pos, autoRemove);
                    if (base != null && !this.buildLayer.contains(base)) {
                        this.buildLayer.add(base);
                    }
                }
            }
        }
    }

    @Nullable
    private BlockPos findBuildBase(BlockPos pos, boolean autoRemove) {
        int minY = Math.max(this.level().getMinBuildHeight(), pos.getY() - 4);
        int maxY = Math.min(this.level().getMaxBuildHeight() - 2, pos.getY() + Math.max(this.wallHeight, 1));
        for (int y = pos.getY(); y >= minY; y--) {
            BlockPos candidate = new BlockPos(pos.getX(), y, pos.getZ());
            if (isNeighborEarthen(candidate) && canBuildInto(candidate.above(), autoRemove)) {
                return candidate;
            }
        }
        for (int y = pos.getY() + 1; y <= maxY; y++) {
            BlockPos candidate = new BlockPos(pos.getX(), y, pos.getZ());
            if (isNeighborEarthen(candidate) && canBuildInto(candidate.above(), autoRemove)) {
                return candidate;
            }
        }
        return null;
    }

    private void buildNextBlocks() {
        if (this.tickCount % 30 == 1) {
            this.level().playSound(null, getX(), getY(), getZ(),
                    ModSounds.SOUND_ROCKS.get(), SoundSource.BLOCKS, 5.0F, this.random.nextFloat() * 0.5F + 0.3F);
        }
        int placed = 0;
        while (placed < BLOCKS_PER_TICK && this.builtLayers < this.wallHeight && !this.buildLayer.isEmpty()) {
            Iterator<BlockPos> iterator = this.buildLayer.iterator();
            while (placed < BLOCKS_PER_TICK && iterator.hasNext()) {
                BlockPos pos = iterator.next().above();
                iterator.remove();
                if (pos.getY() >= this.level().getMaxBuildHeight() || !canBuildInto(pos, this.dieOnDone)) {
                    continue;
                }
                moveUpEntities(pos);
                BlockState state = neighborEarthenBlock(pos.below());
                if (state.isAir()) {
                    state = Blocks.STONE.defaultBlockState();
                }
                spawnBlockDust(pos, state);
                this.level().setBlock(pos, state, 3);
                this.nextLayer.add(pos);
                this.placedBlocks.add(pos);
                placed++;
            }
            if (this.buildLayer.isEmpty()) {
                this.buildLayer.addAll(this.nextLayer);
                this.nextLayer.clear();
                this.builtLayers++;
                if (this.builtLayers >= this.wallHeight) {
                    this.buildLayer.clear();
                }
            }
        }
    }

    private void moveUpEntities(BlockPos pos) {
        AABB box = new AABB(pos);
        for (Entity entity : this.level().getEntities(this, box, entity -> entity.isAlive() && entity != this)) {
            entity.teleportTo(entity.getX(), entity.getY() + 2.5D, entity.getZ());
        }
    }

    private boolean canBuildInto(BlockPos pos, boolean autoRemove) {
        BlockState state = this.level().getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        if (!autoRemove && state.isFaceSturdy(this.level(), pos, Direction.UP)) {
            return false;
        }
        return isBlockBreakable(pos);
    }

    private boolean isBlockBreakable(BlockPos pos) {
        BlockState state = this.level().getBlockState(pos);
        float hardness = state.getDestroySpeed(this.level(), pos);
        return !state.isFaceSturdy(this.level(), pos, Direction.UP)
                && (!state.getFluidState().isEmpty() || hardness >= 0.0F && hardness <= 5.0F);
    }

    private boolean isNeighborEarthen(BlockPos pos) {
        return !neighborEarthenBlock(pos).isAir();
    }

    private BlockState neighborEarthenBlock(BlockPos pos) {
        BlockState own = normalizeWallState(this.level().getBlockState(pos));
        if (!own.isAir() && isEarthenBlock(own)) {
            return own;
        }
        for (Direction direction : Direction.values()) {
            BlockState state = normalizeWallState(this.level().getBlockState(pos.relative(direction)));
            if (!state.isAir() && isEarthenBlock(state)) {
                return state;
            }
        }
        return Blocks.AIR.defaultBlockState();
    }

    private static BlockState normalizeWallState(BlockState state) {
        if (state.is(Blocks.BEDROCK) || isOreLike(state)) {
            return Blocks.STONE.defaultBlockState();
        }
        return state;
    }

    private static boolean isOreLike(BlockState state) {
        String path = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return path.endsWith("_ore") || path.contains("_ore_");
    }

    private static boolean isEarthenBlock(BlockState state) {
        return state.is(BlockTags.DIRT)
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.BASE_STONE_NETHER)
                || state.is(BlockTags.SAND)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.TERRACOTTA)
                || state.is(Blocks.STONE);
    }

    private void spawnBlockDust(BlockPos pos, BlockState state) {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, state),
                    pos.getX() + 0.5D,
                    pos.getY(),
                    pos.getZ() + 0.5D,
                    5,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.15D);
        }
    }

    private void cleanupPlacedBlocks() {
        this.cleanedUp = true;
        for (BlockPos pos : this.placedBlocks) {
            if (!this.level().getBlockState(pos).isAir()) {
                this.level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        this.placedBlocks.clear();
    }

    private static void writePositions(CompoundTag tag, String key, List<BlockPos> positions) {
        ListTag list = new ListTag();
        for (BlockPos pos : positions) {
            list.add(LongTag.valueOf(pos.asLong()));
        }
        tag.put(key, list);
    }

    private static void readPositions(CompoundTag tag, String key, List<BlockPos> positions) {
        positions.clear();
        ListTag list = tag.getList(key, Tag.TAG_LONG);
        for (int i = 0; i < list.size(); i++) {
            positions.add(BlockPos.of(((LongTag)list.get(i)).getAsLong()));
        }
    }
}
