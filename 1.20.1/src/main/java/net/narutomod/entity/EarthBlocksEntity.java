package net.narutomod.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.registry.ModEntityTypes;

public final class EarthBlocksEntity extends Entity implements IEntityAdditionalSpawnData {
    private static final String BLOCKS_TAG = "Blocks";
    private static final String SIZE_X_TAG = "SizeX";
    private static final String SIZE_Y_TAG = "SizeY";
    private static final String SIZE_Z_TAG = "SizeZ";
    private static final String LIFE_TICKS_TAG = "LifeTicks";
    private static final String MOVEMENT_ENABLED_TAG = "MovementEnabled";
    private static final int MAX_SYNC_BLOCKS = 8192;

    private final List<BlockEntry> blocks = new ArrayList<>();
    private int sizeX = 1;
    private int sizeY = 1;
    private int sizeZ = 1;
    private int lifeTicks;
    private boolean movementEnabled;

    public EarthBlocksEntity(EntityType<? extends EarthBlocksEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
        this.noPhysics = true;
    }

    public static EarthBlocksEntity spawnDebug(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        EarthBlocksEntity entity = ModEntityTypes.EARTH_BLOCKS.get().create(level);
        if (entity == null) {
            return null;
        }

        Vec3 look = player.getLookAngle();
        BlockPos origin = BlockPos.containing(
                player.getX() + look.x * 4.0D,
                player.getY() + 1.0D,
                player.getZ() + look.z * 4.0D);
        entity.configureSample(origin);
        entity.lifeTicks = 600;
        level.addFreshEntity(entity);
        return entity;
    }

    public void configureFromBlocks(BlockPos origin, Collection<BlockPos> positions) {
        this.blocks.clear();
        int maxX = 0;
        int maxY = 0;
        int maxZ = 0;
        for (BlockPos pos : positions) {
            BlockState state = this.level().getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                continue;
            }
            BlockPos offset = pos.subtract(origin);
            this.blocks.add(new BlockEntry(offset, state));
            maxX = Math.max(maxX, offset.getX());
            maxY = Math.max(maxY, offset.getY());
            maxZ = Math.max(maxZ, offset.getZ());
        }
        this.sizeX = Math.max(maxX + 1, 1);
        this.sizeY = Math.max(maxY + 1, 1);
        this.sizeZ = Math.max(maxZ + 1, 1);
        moveTo(origin.getX(), origin.getY(), origin.getZ());
        updateBoundingBox();
    }

    private void configureSample(BlockPos origin) {
        this.blocks.clear();
        BlockState[] palette = {
                Blocks.STONE.defaultBlockState(),
                Blocks.DIRT.defaultBlockState(),
                Blocks.COBBLESTONE.defaultBlockState(),
                Blocks.DEEPSLATE.defaultBlockState()
        };
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 2; y++) {
                for (int z = 0; z < 3; z++) {
                    if (x == 1 && y == 1 && z == 1) {
                        continue;
                    }
                    this.blocks.add(new BlockEntry(new BlockPos(x, y, z), palette[(x + y + z) % palette.length]));
                }
            }
        }
        this.sizeX = 3;
        this.sizeY = 2;
        this.sizeZ = 3;
        moveTo(origin.getX(), origin.getY(), origin.getZ());
        updateBoundingBox();
    }

    public List<BlockEntry> getBlocks() {
        return Collections.unmodifiableList(this.blocks);
    }

    public int getBlockCount() {
        return this.blocks.size();
    }

    public List<BlockState> copyBlockStates() {
        List<BlockState> states = new ArrayList<>();
        for (BlockEntry entry : this.blocks) {
            states.add(entry.state());
        }
        return states;
    }

    public void clearBlocksAndDiscard() {
        this.blocks.clear();
        discard();
    }

    public void setMovementEnabled(boolean movementEnabled) {
        this.movementEnabled = movementEnabled;
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        this.noPhysics = true;
        if (this.movementEnabled) {
            Vec3 motion = getDeltaMovement();
            move(MoverType.SELF, motion);
            setDeltaMovement(motion.scale(0.98D));
        } else {
            setDeltaMovement(Vec3.ZERO);
        }
        updateBoundingBox();
        if (!this.level().isClientSide && (this.blocks.isEmpty() || (this.lifeTicks > 0 && this.tickCount > this.lifeTicks))) {
            discard();
        }
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.sizeX = Math.max(tag.getInt(SIZE_X_TAG), 1);
        this.sizeY = Math.max(tag.getInt(SIZE_Y_TAG), 1);
        this.sizeZ = Math.max(tag.getInt(SIZE_Z_TAG), 1);
        this.lifeTicks = tag.getInt(LIFE_TICKS_TAG);
        this.movementEnabled = tag.getBoolean(MOVEMENT_ENABLED_TAG);
        readBlocks(tag.getList(BLOCKS_TAG, Tag.TAG_COMPOUND));
        updateBoundingBox();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt(SIZE_X_TAG, this.sizeX);
        tag.putInt(SIZE_Y_TAG, this.sizeY);
        tag.putInt(SIZE_Z_TAG, this.sizeZ);
        tag.putInt(LIFE_TICKS_TAG, this.lifeTicks);
        tag.putBoolean(MOVEMENT_ENABLED_TAG, this.movementEnabled);
        tag.put(BLOCKS_TAG, writeBlocks());
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.sizeX);
        buffer.writeVarInt(this.sizeY);
        buffer.writeVarInt(this.sizeZ);
        int count = Math.min(this.blocks.size(), MAX_SYNC_BLOCKS);
        buffer.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            BlockEntry entry = this.blocks.get(i);
            buffer.writeBlockPos(entry.offset());
            buffer.writeVarInt(Block.getId(entry.state()));
        }
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        this.sizeX = Math.max(additionalData.readVarInt(), 1);
        this.sizeY = Math.max(additionalData.readVarInt(), 1);
        this.sizeZ = Math.max(additionalData.readVarInt(), 1);
        this.blocks.clear();
        int count = additionalData.readVarInt();
        for (int i = 0; i < count; i++) {
            BlockPos offset = additionalData.readBlockPos();
            BlockState state = Block.stateById(additionalData.readVarInt());
            if (i < MAX_SYNC_BLOCKS) {
                this.blocks.add(new BlockEntry(offset, state));
            }
        }
        updateBoundingBox();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private ListTag writeBlocks() {
        ListTag list = new ListTag();
        for (BlockEntry entry : this.blocks) {
            CompoundTag blockTag = new CompoundTag();
            blockTag.putInt("X", entry.offset().getX());
            blockTag.putInt("Y", entry.offset().getY());
            blockTag.putInt("Z", entry.offset().getZ());
            blockTag.putInt("State", Block.getId(entry.state()));
            list.add(blockTag);
        }
        return list;
    }

    private void readBlocks(ListTag list) {
        this.blocks.clear();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag blockTag = list.getCompound(i);
            BlockPos offset = new BlockPos(blockTag.getInt("X"), blockTag.getInt("Y"), blockTag.getInt("Z"));
            this.blocks.add(new BlockEntry(offset, Block.stateById(blockTag.getInt("State"))));
        }
    }

    private void updateBoundingBox() {
        setBoundingBox(new AABB(getX(), getY(), getZ(), getX() + this.sizeX, getY() + this.sizeY, getZ() + this.sizeZ));
    }

    public record BlockEntry(BlockPos offset, BlockState state) {
    }
}
