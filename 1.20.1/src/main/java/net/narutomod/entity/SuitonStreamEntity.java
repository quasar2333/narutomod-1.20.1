package net.narutomod.entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;

public final class SuitonStreamEntity extends Entity {
    private static final int MAX_LIFE = 100;
    private static final double STREAM_RADIUS = 0.5D;
    private static final float DAMAGE_MODIFIER = 0.5F;
    private static final int EXECUTE_INTERVAL_TICKS = 5;
    private static final int TEMPORARY_WATER_LIFE_TICKS = 10;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(SuitonStreamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> POWER = SynchedEntityData.defineId(SuitonStreamEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUuid;
    private final Map<BlockPos, Integer> temporaryWater = new HashMap<>();

    public SuitonStreamEntity(EntityType<? extends SuitonStreamEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public void configure(LivingEntity owner, float power) {
        setOwner(owner);
        setPower(power);
        moveToBeamOrigin(owner);
    }

    public static boolean spawnFrom(LivingEntity owner, float power) {
        SuitonStreamEntity entity = ModEntityTypes.SUITONSTREAM.get().create(owner.level());
        if (entity == null) {
            return false;
        }
        entity.configure(owner, power);
        owner.level().addFreshEntity(entity);
        return true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(POWER, 5.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        expireTemporaryWater();
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive() || this.tickCount > MAX_LIFE) {
            discardStream();
            return;
        }

        moveToBeamOrigin(owner);
        if (this.tickCount == 1) {
            this.level().playSound(null, getX(), getY(), getZ(),
                    ModSounds.SOUND_WATERBLAST.get(), SoundSource.PLAYERS, 0.5F, getPower() / 30.0F);
        }
        spawnWaterParticles(owner);
        if (this.tickCount % EXECUTE_INTERVAL_TICKS == 1) {
            executeWaterStream(owner);
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
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 68.5D * getViewScale();
        return distance < range * range;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        setPower(tag.contains("Power") ? tag.getFloat("Power") : 5.0F);
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
        tag.putFloat("Power", getPower());
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

    private float getPower() {
        return this.entityData.get(POWER);
    }

    private void setPower(float power) {
        this.entityData.set(POWER, Math.max(power, 0.1F));
    }

    public float getPowerForRender() {
        return getPower();
    }

    public int getMaxLifeForRender() {
        return MAX_LIFE;
    }

    private void moveToBeamOrigin(LivingEntity owner) {
        Vec3 look = owner.getLookAngle();
        this.moveTo(
                owner.getX() + look.x(),
                owner.getY() + owner.getEyeHeight() + look.y() - 0.2D,
                owner.getZ() + look.z(),
                owner.getYRot(),
                owner.getXRot());
        this.setDeltaMovement(Vec3.ZERO);
    }

    private void executeWaterStream(LivingEntity owner) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        double range = getPower();
        if (range <= 0.0D) {
            return;
        }
        Vec3 start = owner.getEyePosition().subtract(0.0D, 0.2D, 0.0D);
        Vec3 look = owner.getLookAngle();
        if (look.lengthSqr() <= 1.0E-8D) {
            return;
        }
        look = look.normalize();
        damageEntities(owner, start, look, range);
        affectBlocks(serverLevel, owner, start, look, range);
    }

    private void damageEntities(LivingEntity owner, Vec3 start, Vec3 look, double range) {
        Vec3 ray = look.scale(range);
        AABB search = owner.getBoundingBox().expandTowards(ray).inflate(STREAM_RADIUS + 1.0D);
        DamageSource source = ModDamageTypes.ninjutsu(this.level(), this, owner);
        Set<Integer> damaged = new HashSet<>();
        for (Entity target : this.level().getEntities(this, search, target -> canAffect(owner, target))) {
            AABB box = target.getBoundingBox().inflate(STREAM_RADIUS + 1.0D);
            if ((box.contains(start) || box.clip(start, start.add(ray)).isPresent()) && damaged.add(target.getId())) {
                target.hurt(source, getPower() * DAMAGE_MODIFIER);
            }
        }
    }

    private boolean canAffect(LivingEntity owner, Entity target) {
        return target.isAlive()
                && target != owner
                && target.getRootVehicle() != owner.getRootVehicle()
                && !(target instanceof SuitonStreamEntity);
    }

    private void affectBlocks(ServerLevel level, LivingEntity owner, Vec3 start, Vec3 look, double range) {
        Vec3 end = start.add(look.scale(range));
        for (BlockPos pos : collectBlocksAlongStream(start, look, range)) {
            if (!rayReachesInflatedBlock(start, end, pos, STREAM_RADIUS)) {
                continue;
            }
            float breakChance = getBreakChance(owner, pos, range);
            if (breakChance > 0.0F
                    && ProcedureUtils.breakBlockAndDropWithChance(level, pos, 5.0F, breakChance, 0.4F, true)) {
                placeTemporaryWater(level, pos.above());
            }
        }
    }

    private Set<BlockPos> collectBlocksAlongStream(Vec3 start, Vec3 look, double range) {
        Set<BlockPos> result = new HashSet<>();
        int steps = Mth.clamp((int)Math.ceil(range * 2.0D), 1, 160);
        for (int i = 0; i <= steps; i++) {
            double distance = range * i / steps;
            Vec3 center = start.add(look.scale(distance));
            BlockPos min = BlockPos.containing(center.x() - STREAM_RADIUS, center.y() - STREAM_RADIUS, center.z() - STREAM_RADIUS);
            BlockPos max = BlockPos.containing(center.x() + STREAM_RADIUS, center.y() + STREAM_RADIUS, center.z() + STREAM_RADIUS);
            BlockPos.betweenClosed(min, max).forEach(pos -> result.add(pos.immutable()));
        }
        return result;
    }

    private boolean rayReachesInflatedBlock(Vec3 start, Vec3 end, BlockPos pos, double radius) {
        AABB box = new AABB(pos).inflate(radius);
        return box.contains(start) || box.clip(start, end).isPresent();
    }

    private float getBreakChance(LivingEntity owner, BlockPos pos, double range) {
        if (range <= 0.0D) {
            return 0.0F;
        }
        double distance = Math.sqrt(owner.distanceToSqr(Vec3.atCenterOf(pos)));
        return Mth.clamp(1.0F - (float)(distance / range), 0.0F, 1.0F);
    }

    private void placeTemporaryWater(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir()) {
            return;
        }
        level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
        this.temporaryWater.put(pos.immutable(), this.tickCount + TEMPORARY_WATER_LIFE_TICKS);
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

    private void spawnWaterParticles(LivingEntity owner) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 start = owner.getEyePosition().subtract(0.0D, 0.2D, 0.0D);
        Vec3 look = owner.getLookAngle();
        if (look.lengthSqr() <= 1.0E-8D) {
            return;
        }
        look = look.normalize();
        double range = getPower();
        int count = Mth.clamp((int)(range * 1.5D), 6, 80);
        for (int i = 0; i < count; i++) {
            double distance = this.random.nextDouble() * range;
            Vec3 point = start.add(look.scale(distance));
            level.sendParticles(
                    ParticleTypes.SPLASH,
                    point.x(),
                    point.y(),
                    point.z(),
                    1,
                    STREAM_RADIUS * 0.35D,
                    STREAM_RADIUS * 0.35D,
                    STREAM_RADIUS * 0.35D,
                    0.05D);
        }
    }

    private void discardStream() {
        clearTemporaryWater();
        discard();
    }
}
