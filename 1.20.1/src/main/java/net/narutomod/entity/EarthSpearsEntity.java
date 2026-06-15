package net.narutomod.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class EarthSpearsEntity extends Entity {
    private static final int GROW_TIME = 8;
    private static final int MAX_IN_GROUND_TIME = 1200;
    private static final float MAX_SCALE = 2.0F;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(EarthSpearsEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(EarthSpearsEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(EarthSpearsEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUuid;
    private float dimensionsScale;

    public EarthSpearsEntity(EntityType<? extends EarthSpearsEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    public void configure(LivingEntity owner, float damage) {
        setOwner(owner);
        setDamage(damage);
    }

    public static int spawnFrom(LivingEntity owner, float power) {
        HitResult result = ProcedureUtils.raytraceBlocks(owner, 30.0D);
        if (!(result instanceof BlockHitResult blockHit) || blockHit.getType() != HitResult.Type.BLOCK
                || blockHit.getDirection() != Direction.UP || !(owner.level() instanceof ServerLevel level)) {
            return 0;
        }

        level.playSound(null, blockHit.getBlockPos(), ModSounds.SOUND_HAND_PRESS.get(),
                SoundSource.BLOCKS, 5.0F, owner.getRandom().nextFloat() * 0.4F + 0.8F);
        int count = Math.max((int)(power * power * 5.0F), 1);
        for (int i = 0; i < count; i++) {
            EarthSpearsEntity spear = ModEntityTypes.EARTH_SPEARS.get().create(level);
            if (spear == null) {
                continue;
            }
            spear.configure(owner, power);
            Vec3 point = blockHit.getLocation().add(
                    (owner.getRandom().nextDouble() - 0.5D) * power * 3.0D,
                    0.0D,
                    (owner.getRandom().nextDouble() - 0.5D) * power * 3.0D);
            Vec3 ground = findSurface(level, point);
            spear.moveTo(
                    ground.x(),
                    ground.y() + 0.5D,
                    ground.z(),
                    owner.getRandom().nextFloat() * 360.0F,
                    (owner.getRandom().nextFloat() - 0.5F) * 60.0F);
            level.addFreshEntity(spear);
        }
        return count;
    }

    public static boolean hasUpwardGroundTarget(LivingEntity owner) {
        HitResult result = ProcedureUtils.raytraceBlocks(owner, 30.0D);
        return result instanceof BlockHitResult blockHit
                && blockHit.getType() == HitResult.Type.BLOCK
                && blockHit.getDirection() == Direction.UP;
    }

    public float getCurrentScale() {
        return this.entityData.get(SCALE);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(DAMAGE, 0.0F);
        this.entityData.define(SCALE, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount <= GROW_TIME) {
            setCurrentScale(Mth.clamp(MAX_SCALE * (float)this.tickCount / (float)GROW_TIME, 0.0F, MAX_SCALE));
            if (!this.level().isClientSide) {
                spawnStoneDust();
                damageNearby();
            }
        }
        if (!this.level().isClientSide && this.tickCount > MAX_IN_GROUND_TIME) {
            discard();
        }
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(this.dimensionsScale);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SCALE.equals(key)) {
            this.dimensionsScale = Mth.clamp(getCurrentScale(), 0.0F, MAX_SCALE);
            refreshDimensions();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        setDamage(tag.getFloat("Damage"));
        setCurrentScale(tag.contains("Scale") ? tag.getFloat("Scale") : MAX_SCALE);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putFloat("Damage", getDamage());
        tag.putFloat("Scale", getCurrentScale());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private static Vec3 findSurface(ServerLevel level, Vec3 point) {
        BlockPos pos = BlockPos.containing(point);
        while (pos.getY() > level.getMinBuildHeight() && !isTopSolid(level, pos)) {
            pos = pos.below();
        }
        while (pos.getY() < level.getMaxBuildHeight() - 1 && isTopSolid(level, pos.above())) {
            pos = pos.above();
        }
        return new Vec3(point.x(), pos.getY(), point.z());
    }

    private static boolean isTopSolid(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isFaceSturdy(level, pos, Direction.UP);
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

    private float getDamage() {
        return this.entityData.get(DAMAGE);
    }

    private void setDamage(float damage) {
        this.entityData.set(DAMAGE, Math.max(damage, 0.0F));
    }

    private void setCurrentScale(float scale) {
        this.dimensionsScale = Mth.clamp(scale, 0.0F, MAX_SCALE);
        this.entityData.set(SCALE, this.dimensionsScale);
        refreshDimensions();
    }

    private void spawnStoneDust() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()),
                    getX(),
                    getY(),
                    getZ(),
                    6,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.15D);
        }
    }

    private void damageNearby() {
        LivingEntity owner = getOwner();
        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(1.0D, 0.0D, 1.0D))) {
            if (target == owner || target.getRootVehicle() == (owner == null ? null : owner.getRootVehicle())) {
                continue;
            }
            target.invulnerableTime = 0;
            target.hurt(ModDamageTypes.ninjutsu(this.level(), this, owner), getDamage());
        }
    }
}
