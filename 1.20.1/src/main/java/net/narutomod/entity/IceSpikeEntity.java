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
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class IceSpikeEntity extends Entity {
    private static final int GROW_TIME = 10;
    private static final int MAX_IN_GROUND_TIME = 1200;
    private static final float MAX_SCALE = 3.0F;
    private static final float DAMAGE = 30.0F;
    private static final float HEIGHT = 1.82F;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(IceSpikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(IceSpikeEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUuid;

    public IceSpikeEntity(EntityType<? extends IceSpikeEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public void configure(LivingEntity owner, Vec3 position, float yaw, float pitch) {
        setOwner(owner);
        setCurrentScale(0.0F);
        moveTo(position.x(), position.y(), position.z(), yaw, pitch);
        this.yRotO = getYRot();
        this.xRotO = getXRot();
    }

    public static int spawnFrom(LivingEntity owner, float power) {
        if (!(owner.level() instanceof ServerLevel level)) {
            return 0;
        }
        BlockHitResult hit = groundTarget(owner);
        if (hit == null) {
            return 0;
        }
        level.playSound(null, owner.getX(), owner.getY(), owner.getZ(), ModSounds.SOUND_SPIKED.get(),
                SoundSource.NEUTRAL, 5.0F, owner.getRandom().nextFloat() * 0.4F + 0.8F);
        int count = Math.max((int)(power * power * 5.0F), 1);
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            IceSpikeEntity spike = ModEntityTypes.ICE_SPIKE.get().create(level);
            if (spike == null) {
                continue;
            }
            Vec3 point = hit.getLocation().add(
                    (owner.getRandom().nextDouble() - 0.5D) * power * 3.0D,
                    0.0D,
                    (owner.getRandom().nextDouble() - 0.5D) * power * 3.0D);
            Vec3 surface = findSurface(level, point);
            if (surface == null) {
                continue;
            }
            spike.configure(owner, surface.add(0.0D, 0.5D, 0.0D),
                    owner.getRandom().nextFloat() * 360.0F,
                    (owner.getRandom().nextFloat() - 0.5F) * 60.0F);
            level.addFreshEntity(spike);
            spawned++;
        }
        return spawned;
    }

    public static boolean hasUpwardGroundTarget(LivingEntity owner) {
        return groundTarget(owner) != null;
    }

    public float getCurrentScale() {
        return this.entityData.get(SCALE);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(SCALE, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount <= GROW_TIME) {
            setCurrentScale(Mth.clamp(MAX_SCALE * (float)this.tickCount / (float)GROW_TIME, 0.0F, MAX_SCALE));
            if (!this.level().isClientSide) {
                spawnIceDust();
                damageNearby();
            }
        }
        if (!this.level().isClientSide && this.tickCount > MAX_IN_GROUND_TIME) {
            discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        setCurrentScale(tag.contains("Scale") ? tag.getFloat("Scale") : MAX_SCALE);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putFloat("Scale", getCurrentScale());
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Nullable
    private static BlockHitResult groundTarget(LivingEntity owner) {
        HitResult result = ProcedureUtils.raytraceBlocks(owner, 30.0D);
        if (result instanceof BlockHitResult blockHit
                && blockHit.getType() == HitResult.Type.BLOCK
                && blockHit.getDirection() == Direction.UP) {
            return blockHit;
        }
        return null;
    }

    @Nullable
    private static Vec3 findSurface(ServerLevel level, Vec3 point) {
        BlockPos pos = BlockPos.containing(point);
        while (pos.getY() > level.getMinBuildHeight() && !isTopSolid(level, pos)) {
            pos = pos.below();
        }
        if (!isTopSolid(level, pos)) {
            return null;
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

    private void setCurrentScale(float scale) {
        this.entityData.set(SCALE, Mth.clamp(scale, 0.0F, MAX_SCALE));
    }

    private void spawnIceDust() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()),
                    getX(),
                    getY(),
                    getZ(),
                    8,
                    0.2D,
                    0.0D,
                    0.2D,
                    0.15D);
        }
    }

    private void damageNearby() {
        LivingEntity owner = getOwner();
        float scale = getCurrentScale();
        double halfWidth = 0.25D * scale;
        AABB area = new AABB(
                getX() - halfWidth,
                getY(),
                getZ() - halfWidth,
                getX() + halfWidth,
                getY() + HEIGHT * scale,
                getZ() + halfWidth).inflate(1.0D, 0.0D, 1.0D);
        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            if (target == owner || target.getRootVehicle() == rootVehicle(owner)) {
                continue;
            }
            target.invulnerableTime = 0;
            target.hurt(ModDamageTypes.ninjutsu(this.level(), this, owner), DAMAGE);
        }
    }

    @Nullable
    private static Entity rootVehicle(@Nullable Entity entity) {
        return entity == null ? null : entity.getRootVehicle();
    }
}
