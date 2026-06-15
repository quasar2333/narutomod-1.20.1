package net.narutomod.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.NarutomodModVariables;
import net.narutomod.registry.ModEntityTypes;

public final class ChibakuSatelliteEntity extends Entity implements IEntityAdditionalSpawnData {
    private static final String BLOCKS_TAG = "Blocks";
    private static final String SIZE_TAG = "Size";
    private static final int FALL_DELAY_TICKS = 5;
    private static final int MAX_FALL_TICKS = 220;
    private static final int MAX_BLOCKS = 8192;
    private static final int MAX_RADIUS = 10;
    private static final float EXPLOSION_STRENGTH = 12.0F;
    private static final double DAMAGE_RADIUS = 36.0D;

    private final List<BlockEntry> blocks = new ArrayList<>();
    @Nullable
    private UUID ownerUuid;
    private int size = 1;
    private boolean exploded;

    public ChibakuSatelliteEntity(EntityType<? extends ChibakuSatelliteEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    @Nullable
    public static ChibakuSatelliteEntity spawnFromStates(LivingEntity owner, Vec3 center, List<BlockState> states) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        ChibakuSatelliteEntity satellite = ModEntityTypes.CHIBAKU_SATELLITE.get().create(serverLevel);
        if (satellite == null) {
            return null;
        }
        satellite.configure(owner, center, states);
        serverLevel.addFreshEntity(satellite);
        return satellite;
    }

    public List<BlockEntry> getBlocks() {
        return List.copyOf(this.blocks);
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
        if (this.blocks.isEmpty() || this.tickCount > MAX_FALL_TICKS) {
            impact();
            return;
        }
        if (this.tickCount > FALL_DELAY_TICKS) {
            setNoGravity(false);
            Vec3 motion = getDeltaMovement().add(0.0D, -0.08D, 0.0D);
            setDeltaMovement(motion);
            move(MoverType.SELF, motion);
            setDeltaMovement(getDeltaMovement().scale(0.98D));
            updateBoundingBox();
            if (this.onGround() || this.horizontalCollision || this.verticalCollision) {
                impact();
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        this.size = Math.max(tag.getInt(SIZE_TAG), 1);
        this.exploded = tag.getBoolean("Exploded");
        readBlocks(tag.getList(BLOCKS_TAG, Tag.TAG_COMPOUND));
        updateBoundingBox();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putInt(SIZE_TAG, this.size);
        tag.putBoolean("Exploded", this.exploded);
        tag.put(BLOCKS_TAG, writeBlocks());
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.size);
        buffer.writeVarInt(this.blocks.size());
        for (BlockEntry entry : this.blocks) {
            buffer.writeBlockPos(entry.offset());
            buffer.writeVarInt(Block.getId(entry.state()));
        }
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        this.size = Math.max(additionalData.readVarInt(), 1);
        this.blocks.clear();
        int count = Math.min(additionalData.readVarInt(), MAX_BLOCKS);
        for (int i = 0; i < count; i++) {
            this.blocks.add(new BlockEntry(additionalData.readBlockPos(), Block.stateById(additionalData.readVarInt())));
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

    private void configure(LivingEntity owner, Vec3 center, List<BlockState> states) {
        this.ownerUuid = owner.getUUID();
        this.blocks.clear();
        int radius = Mth.clamp((int)Math.ceil(Math.cbrt(Math.max(states.size(), 1))) + 1, 2, MAX_RADIUS);
        this.size = radius * 2 + 1;
        BlockPos origin = BlockPos.containing(center).offset(-radius, -radius, -radius);
        List<BlockPos> offsets = sphereOffsets(radius);
        int count = Math.min(Math.min(states.size(), offsets.size()), MAX_BLOCKS);
        for (int i = 0; i < count; i++) {
            this.blocks.add(new BlockEntry(offsets.get(i), states.get(i)));
        }
        moveTo(origin.getX(), origin.getY(), origin.getZ());
        setDeltaMovement(0.0D, -0.1D, 0.0D);
        updateBoundingBox();
    }

    @Nullable
    private LivingEntity getOwner() {
        if (this.ownerUuid != null && this.level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(this.ownerUuid) instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    private List<BlockPos> sphereOffsets(int radius) {
        List<BlockPos> offsets = new ArrayList<>();
        int radiusSqr = radius * radius;
        BlockPos center = new BlockPos(radius, radius, radius);
        for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, radius * 2, radius * 2, radius * 2)) {
            if (pos.distSqr(center) <= radiusSqr) {
                offsets.add(pos.immutable());
            }
        }
        offsets.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));
        return offsets;
    }

    private void impact() {
        if (this.exploded) {
            discard();
            return;
        }
        this.exploded = true;
        if (this.level() instanceof ServerLevel serverLevel) {
            LivingEntity owner = getOwner();
            if (owner != null) {
                owner.getPersistentData().putDouble(NarutomodModVariables.INVULNERABLE_TIME, 300.0D);
            }
            Vec3 center = new Vec3(getX() + this.size * 0.5D, getY() + this.size * 0.5D, getZ() + this.size * 0.5D);
            DamageSource source = damageSources().fallingBlock(this);
            for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(DAMAGE_RADIUS),
                    entity -> entity.isAlive() && entity != owner)) {
                double distance = Math.max(target.distanceToSqr(center), 1.0D);
                target.invulnerableTime = 0;
                target.hurt(source, (float)Math.max(8.0D, DAMAGE_RADIUS * 2.0D - Math.sqrt(distance)));
            }
            serverLevel.explode(owner, center.x(), center.y(), center.z(), EXPLOSION_STRENGTH, Level.ExplosionInteraction.MOB);
        }
        discard();
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
        for (int i = 0; i < Math.min(list.size(), MAX_BLOCKS); i++) {
            CompoundTag blockTag = list.getCompound(i);
            BlockPos offset = new BlockPos(blockTag.getInt("X"), blockTag.getInt("Y"), blockTag.getInt("Z"));
            this.blocks.add(new BlockEntry(offset, Block.stateById(blockTag.getInt("State"))));
        }
    }

    private void updateBoundingBox() {
        setBoundingBox(new AABB(getX(), getY(), getZ(), getX() + this.size, getY() + this.size, getZ() + this.size));
    }

    public record BlockEntry(BlockPos offset, BlockState state) {
    }
}
