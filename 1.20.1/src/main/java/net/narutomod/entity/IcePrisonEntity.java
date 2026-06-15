package net.narutomod.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class IcePrisonEntity extends Entity {
    private static final double RANGE = 10.0D;
    private static final int ICE_LIFETIME_TICKS = 1200;
    private static final int BLOCKS_PER_TICK = 3;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(IcePrisonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(IcePrisonEntity.class, EntityDataSerializers.INT);
    private static final BlockPos[][] POSITION_MULTIPLIERS = {
        { new BlockPos(1, 0, 1), new BlockPos(-1, 0, 1), new BlockPos(1, 0, -1), new BlockPos(-1, 0, -1) },
        { new BlockPos(-1, 0, 0), new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0), new BlockPos(1, 0, 0) }
    };

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private UUID targetUuid;
    @Nullable
    private BlockPos origin;
    private int radius;
    private int targetHeight;
    private int buildIndex;
    private final List<BlockPos> buildPlan = new ArrayList<>();
    private final Map<BlockPos, Integer> placedIce = new HashMap<>();

    public IcePrisonEntity(EntityType<? extends IcePrisonEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public void configure(LivingEntity owner, LivingEntity target) {
        setOwner(owner);
        setTarget(target);
        moveTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
        this.origin = BlockPos.containing(position());
        this.radius = Math.max((int)(target.getBbWidth() * 0.5D + 1.0D), 1);
        this.targetHeight = Math.max((int)(target.getBbHeight() + 1.0D), 1);
        this.buildIndex = 0;
        rebuildBuildPlan();
    }

    public static boolean spawnFrom(LivingEntity owner) {
        LivingEntity target = findTarget(owner);
        if (target == null) {
            return false;
        }
        IcePrisonEntity prison = ModEntityTypes.ICE_PRISON.get().create(owner.level());
        if (prison == null) {
            return false;
        }
        owner.level().playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.SOUND_ICE_SHOOT.get(),
                SoundSource.NEUTRAL, 1.0F, owner.getRandom().nextFloat() * 0.4F + 0.8F);
        prison.configure(owner, target);
        owner.level().addFreshEntity(prison);
        return true;
    }

    @Nullable
    public static LivingEntity findTarget(LivingEntity owner) {
        HitResult hit = ProcedureUtils.objectEntityLookingAt(owner, RANGE, 0.0D, true, false,
                target -> target instanceof LivingEntity && target != owner && target.isAlive());
        return hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living ? living : null;
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
        expirePlacedIce();
        if (this.buildIndex < this.buildPlan.size()) {
            holdTargetAndBuild();
            return;
        }
        if (this.placedIce.isEmpty()) {
            discard();
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && reason.shouldDestroy()) {
            clearPlacedIce();
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
        if (tag.contains("Origin")) {
            this.origin = readBlockPos(tag.getCompound("Origin"));
        }
        this.radius = tag.getInt("Radius");
        this.targetHeight = tag.getInt("TargetHeight");
        this.buildIndex = tag.getInt("BuildIndex");
        rebuildBuildPlan();
        this.placedIce.clear();
        if (tag.contains("PlacedIce", 9)) {
            ListTag list = tag.getList("PlacedIce", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag iceTag = list.getCompound(i);
                this.placedIce.put(readBlockPos(iceTag), iceTag.getInt("ExpiresAt"));
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
        if (this.origin != null) {
            tag.put("Origin", writeBlockPos(this.origin));
        }
        tag.putInt("Radius", this.radius);
        tag.putInt("TargetHeight", this.targetHeight);
        tag.putInt("BuildIndex", this.buildIndex);
        ListTag list = new ListTag();
        for (Map.Entry<BlockPos, Integer> entry : this.placedIce.entrySet()) {
            CompoundTag iceTag = writeBlockPos(entry.getKey());
            iceTag.putInt("ExpiresAt", entry.getValue());
            list.add(iceTag);
        }
        tag.put("PlacedIce", list);
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

    private void holdTargetAndBuild() {
        LivingEntity owner = getOwner();
        LivingEntity target = getTarget();
        if (owner == null || target == null || !owner.isAlive() || !target.isAlive()) {
            this.buildIndex = this.buildPlan.size();
            return;
        }
        target.teleportTo(getX(), getY() + 0.5D, getZ());
        if (this.tickCount % 4 == 0) {
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 600, 1, false, false));
        }
        placeNextIceBlocks();
    }

    private void placeNextIceBlocks() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        int placedThisTick = 0;
        while (placedThisTick < BLOCKS_PER_TICK && this.buildIndex < this.buildPlan.size()) {
            BlockPos pos = this.buildPlan.get(this.buildIndex++);
            if (level.hasChunkAt(pos) && level.getBlockState(pos).isAir()) {
                level.setBlock(pos, Blocks.ICE.defaultBlockState(), 3);
                this.placedIce.put(pos, this.tickCount + ICE_LIFETIME_TICKS);
                spawnIceParticles(level, pos);
                placedThisTick++;
            }
        }
    }

    private void expirePlacedIce() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        this.placedIce.entrySet().removeIf(entry -> {
            if (entry.getValue() > this.tickCount) {
                return false;
            }
            removeIce(level, entry.getKey());
            return true;
        });
    }

    private void clearPlacedIce() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        for (BlockPos pos : this.placedIce.keySet()) {
            removeIce(level, pos);
        }
        this.placedIce.clear();
    }

    private void removeIce(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).is(Blocks.ICE)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private void rebuildBuildPlan() {
        this.buildPlan.clear();
        if (this.origin == null || this.radius <= 0 || this.targetHeight <= 0) {
            return;
        }
        Set<BlockPos> unique = new LinkedHashSet<>();
        unique.add(this.origin);
        int tx = 0;
        int ty = 0;
        int tz = 0;
        int tr = 0;
        while (ty <= this.targetHeight) {
            for (int quadrant = 0; quadrant < 4; quadrant++) {
                if (quadrant == 0) {
                    tz++;
                    if (tz > (int)Math.round(tr / 1.41421356D)) {
                        tz = 0;
                        tr++;
                    }
                    if (tr > this.radius) {
                        tr = 0;
                        ty++;
                    }
                    if (ty > this.targetHeight) {
                        break;
                    }
                    tx = (int)Math.round(Math.sqrt((double)(tr * tr - tz * tz)));
                }
                BlockPos diagonal = POSITION_MULTIPLIERS[0][quadrant];
                BlockPos side = POSITION_MULTIPLIERS[1][quadrant];
                BlockPos pos0 = this.origin.offset(diagonal.getX() * tx, ty, diagonal.getZ() * tz);
                BlockPos pos1 = this.origin.offset(diagonal.getX() * tz, ty, diagonal.getZ() * tx);
                unique.add(pos0);
                unique.add(pos1);
                unique.add(pos0.offset(side));
            }
        }
        this.buildPlan.addAll(unique);
        if (this.buildIndex > this.buildPlan.size()) {
            this.buildIndex = this.buildPlan.size();
        }
    }

    private void spawnIceParticles(ServerLevel level, BlockPos pos) {
        level.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()),
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                4,
                0.2D,
                0.2D,
                0.2D,
                0.05D);
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
}
