package net.narutomod.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.block.WaterStillBlock;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModBlocks;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class WaterShockwaveEntity extends Entity {
    public static final String ACTIVE_ID_TAG = "WaterShockwaveEntityIdKey";
    private static final UUID SWIM_SPEED_MODIFIER_ID = UUID.fromString("5a2b7a42-5a54-46bf-aee0-d74c07dd37b8");
    private static final AttributeModifier SWIM_SPEED_MODIFIER = new AttributeModifier(
            SWIM_SPEED_MODIFIER_ID,
            "watershockwave.swimspeed",
            1.2D,
            AttributeModifier.Operation.ADDITION);
    private static final int STABLE_BLOCKS_PER_TICK = 512;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(WaterShockwaveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RADIUS = SynchedEntityData.defineId(WaterShockwaveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> BUILDING = SynchedEntityData.defineId(WaterShockwaveEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DYING = SynchedEntityData.defineId(WaterShockwaveEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DEATH_TICKS = SynchedEntityData.defineId(WaterShockwaveEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;
    private final Set<BlockPos> domeBlocks = new HashSet<>();

    public WaterShockwaveEntity(EntityType<? extends WaterShockwaveEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public void configure(LivingEntity owner, float power) {
        setOwner(owner);
        int radius = Mth.clamp((int)power, 1, 32);
        this.entityData.set(RADIUS, radius);
        this.entityData.set(BUILDING, true);
        this.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
    }

    public static ToggleResult toggleFrom(LivingEntity owner, float power) {
        WaterShockwaveEntity existing = getActive(owner);
        if (existing != null) {
            existing.setShouldDie();
            return ToggleResult.TOGGLED_OFF;
        }
        WaterShockwaveEntity entity = ModEntityTypes.WATER_SHOCKWAVE.get().create(owner.level());
        if (entity == null) {
            return ToggleResult.FAILED;
        }
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                ModSounds.SOUND_DAIBAKUSUISHOHA.get(), SoundSource.PLAYERS, 2.0F, 1.0F);
        entity.configure(owner, power);
        owner.level().addFreshEntity(entity);
        owner.getPersistentData().putInt(ACTIVE_ID_TAG, entity.getId());
        return ToggleResult.CREATED;
    }

    @Nullable
    public static WaterShockwaveEntity getActive(LivingEntity owner) {
        int id = owner.getPersistentData().getInt(ACTIVE_ID_TAG);
        if (id <= 0 || !(owner.level().getEntity(id) instanceof WaterShockwaveEntity entity)) {
            owner.getPersistentData().remove(ACTIVE_ID_TAG);
            return null;
        }
        LivingEntity entityOwner = entity.getOwner();
        if (entityOwner != owner) {
            owner.getPersistentData().remove(ACTIVE_ID_TAG);
            return null;
        }
        return entity;
    }

    public int getRadius() {
        return this.entityData.get(RADIUS);
    }

    public boolean isBuilding() {
        return this.entityData.get(BUILDING);
    }

    public boolean isDying() {
        return this.entityData.get(DYING);
    }

    public void setShouldDie() {
        this.entityData.set(DYING, true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(RADIUS, 1);
        this.entityData.define(BUILDING, true);
        this.entityData.define(DYING, false);
        this.entityData.define(DEATH_TICKS, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (isDying()) {
            deathTick();
            return;
        }
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            setShouldDie();
            return;
        }
        if (isBuilding()) {
            owner.teleportTo(getX(), getY(), getZ());
            List<BlockPos> airBlocks = getAirBlocksInRadius();
            if (airBlocks.isEmpty()) {
                this.entityData.set(BUILDING, false);
            } else {
                placeWater(airBlocks, Math.min(getRadius() * getRadius(), airBlocks.size()));
            }
        } else {
            this.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
            List<BlockPos> airBlocks = getAirBlocksInRadius();
            if (airBlocks.size() > this.domeBlocks.size() / 2) {
                setShouldDie();
                return;
            }
            placeWater(airBlocks, Math.min(STABLE_BLOCKS_PER_TICK, airBlocks.size()));
            removeOutOfRangeDomeBlocks(STABLE_BLOCKS_PER_TICK);
            applyOwnerWaterEffects(owner);
        }
        slowEntitiesInsideWater(owner);
        spawnWaterParticles();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && reason.shouldDestroy()) {
            clearDomeBlocks();
            clearOwnerState();
        }
        super.remove(reason);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = Math.max(96.0D, getRadius() + 64.0D) * getViewScale();
        return distance < range * range;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        this.entityData.set(RADIUS, tag.contains("Radius") ? tag.getInt("Radius") : 1);
        this.entityData.set(BUILDING, tag.getBoolean("Building"));
        this.entityData.set(DYING, tag.getBoolean("Dying"));
        this.entityData.set(DEATH_TICKS, tag.getInt("DeathTicks"));
        this.domeBlocks.clear();
        if (tag.contains("DomeBlocks", 9)) {
            ListTag list = tag.getList("DomeBlocks", 10);
            for (int i = 0; i < list.size(); i++) {
                this.domeBlocks.add(readBlockPos(list.getCompound(i)));
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putInt("Radius", getRadius());
        tag.putBoolean("Building", isBuilding());
        tag.putBoolean("Dying", isDying());
        tag.putInt("DeathTicks", this.entityData.get(DEATH_TICKS));
        ListTag list = new ListTag();
        for (BlockPos pos : this.domeBlocks) {
            list.add(writeBlockPos(pos));
        }
        tag.put("DomeBlocks", list);
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

    private List<BlockPos> getAirBlocksInRadius() {
        List<BlockPos> result = new ArrayList<>();
        int radius = getRadius();
        BlockPos min = BlockPos.containing(getX() - radius, getY() - radius, getZ() - radius);
        BlockPos max = BlockPos.containing(getX() + radius, getY() + radius, getZ() + radius);
        double radiusSqr = (radius + 0.5D) * (radius + 0.5D);
        BlockPos center = blockPosition();
        for (BlockPos mutablePos : BlockPos.betweenClosed(min, max)) {
            BlockPos pos = mutablePos.immutable();
            if (pos.distSqr(center) <= radiusSqr && this.level().getBlockState(pos).isAir()) {
                result.add(pos);
            }
        }
        result.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));
        return result;
    }

    private void placeWater(List<BlockPos> blocks, int limit) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        for (int i = 0; i < limit; i++) {
            BlockPos pos = blocks.get(i);
            if (level.getBlockState(pos).isAir()) {
                level.setBlock(pos, ModBlocks.WATER_STILL.get().defaultBlockState(), 3);
                this.domeBlocks.add(pos);
            }
        }
    }

    private void removeOutOfRangeDomeBlocks(int limit) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        double radiusSqr = (getRadius() + 0.5D) * (getRadius() + 0.5D);
        BlockPos center = blockPosition();
        List<BlockPos> toRemove = new ArrayList<>();
        for (BlockPos pos : this.domeBlocks) {
            if (toRemove.size() >= limit) {
                break;
            }
            if (pos.distSqr(center) > radiusSqr) {
                toRemove.add(pos);
            }
        }
        for (BlockPos pos : toRemove) {
            if (isOwnedWaterBlock(level, pos)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
            this.domeBlocks.remove(pos);
        }
    }

    private void slowEntitiesInsideWater(LivingEntity owner) {
        AABB box = getBoundingBox().inflate(getRadius());
        for (Entity entity : this.level().getEntities(owner, box, entity -> !(entity instanceof WaterSharkEntity))) {
            if (entity.isInWaterOrBubble() || WaterStillBlock.isInsideBlock(entity, false)) {
                ProcedureUtils.multiplyVelocity(entity, 0.8D);
            }
        }
    }

    private void applyOwnerWaterEffects(LivingEntity owner) {
        owner.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 2, 0, false, false));
        owner.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 210, 0, false, false));
        AttributeInstance swimSpeed = owner.getAttribute(ForgeMod.SWIM_SPEED.get());
        if (swimSpeed != null && swimSpeed.getModifier(SWIM_SPEED_MODIFIER_ID) == null) {
            swimSpeed.addTransientModifier(SWIM_SPEED_MODIFIER);
        }
    }

    private void deathTick() {
        removeOwnerSwimModifier();
        int deathTicks = this.entityData.get(DEATH_TICKS);
        if (deathTicks == 0) {
            scatterDomeWater();
            clearOwnerState();
        } else if (deathTicks % 5 == 0) {
            clearDomeBlocks();
        } else if (deathTicks > 30) {
            this.domeBlocks.clear();
            discard();
            return;
        }
        this.entityData.set(DEATH_TICKS, deathTicks + 1);
    }

    private void scatterDomeWater() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        for (BlockPos pos : this.domeBlocks) {
            if (this.random.nextInt(3) == 0) {
                level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
            } else {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private void clearDomeBlocks() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        for (BlockPos pos : this.domeBlocks) {
            if (isOwnedWaterBlock(level, pos)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private static boolean isOwnedWaterBlock(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(ModBlocks.WATER_STILL.get()) || level.getBlockState(pos).is(Blocks.WATER);
    }

    private void clearOwnerState() {
        LivingEntity owner = getOwner();
        if (owner != null) {
            owner.getPersistentData().remove(ACTIVE_ID_TAG);
        }
        removeOwnerSwimModifier();
    }

    private void removeOwnerSwimModifier() {
        LivingEntity owner = getOwner();
        if (owner == null) {
            return;
        }
        AttributeInstance swimSpeed = owner.getAttribute(ForgeMod.SWIM_SPEED.get());
        if (swimSpeed != null) {
            swimSpeed.removeModifier(SWIM_SPEED_MODIFIER_ID);
        }
    }

    private void spawnWaterParticles() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        int count = Mth.clamp(getRadius() * 2, 8, 120);
        level.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 1.0D, getZ(), count,
                Math.max(0.5D, getRadius() * 0.35D),
                Math.max(0.5D, getRadius() * 0.35D),
                Math.max(0.5D, getRadius() * 0.35D),
                0.02D);
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

    public enum ToggleResult {
        CREATED,
        TOGGLED_OFF,
        FAILED
    }
}
