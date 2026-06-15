package net.narutomod.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.NarutomodModVariables;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureSync;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class TenseiBakuGoldEntity extends Entity {
    public static final int GROW_TIME = 20;
    private static final int MAX_ACTIVE_TICKS = 100;
    private static final double BEAM_RADIUS = 3.0D;
    private static final float BLOCK_HARDNESS_LIMIT = 100.0F;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(TenseiBakuGoldEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> POWER = SynchedEntityData.defineId(TenseiBakuGoldEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BEAM_LENGTH = SynchedEntityData.defineId(TenseiBakuGoldEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUuid;
    private boolean forceBowPoseSynced;

    public TenseiBakuGoldEntity(EntityType<? extends TenseiBakuGoldEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public static boolean spawnFrom(LivingEntity owner, float power) {
        TenseiBakuGoldEntity entity = ModEntityTypes.TENSEI_BAKU_GOLD.get().create(owner.level());
        if (entity == null) {
            return false;
        }
        entity.configure(owner, power);
        owner.level().playSound(null, owner.getX(), owner.getY() + 2.0D, owner.getZ(),
                ModSounds.SOUND_LASER.get(), SoundSource.PLAYERS, 4.0F, 1.0F);
        return owner.level().addFreshEntity(entity);
    }

    public void configure(LivingEntity owner, float power) {
        setOwner(owner);
        setPower(power);
        updateFromOwner(owner);
    }

    public float getPower() {
        return this.entityData.get(POWER);
    }

    public float getBeamLength() {
        return this.entityData.get(BEAM_LENGTH);
    }

    public float getGrowth(float partialTick) {
        float growth = Mth.clamp((this.tickCount + partialTick) / (float) GROW_TIME, 0.0F, 1.0F);
        return growth * growth;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(POWER, 10.0F);
        this.entityData.define(BEAM_LENGTH, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            if (!this.level().isClientSide) {
                discardGold();
            }
            return;
        }

        updateFromOwner(owner);
        if (this.level().isClientSide) {
            return;
        }

        if (this.tickCount == 1) {
            ProcedureSync.EntityNBTTag.setAndSync(owner, NarutomodModVariables.FORCE_BOW_POSE, true);
            this.forceBowPoseSynced = true;
        }
        if (this.tickCount > GROW_TIME) {
            applyBeamEffects((ServerLevel) this.level(), owner);
        }
        if (this.tickCount > GROW_TIME + MAX_ACTIVE_TICKS) {
            discardGold();
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        clearForceBowPose();
        super.remove(reason);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        Vec3 end = position().add(beamDirection().scale(getBeamLength()));
        return new AABB(position(), end).inflate(BEAM_RADIUS + 2.0D);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 128.0D * getViewScale();
        return distance < range * range;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        this.tickCount = tag.getInt("Life");
        setPower(tag.contains("Power") ? tag.getFloat("Power") : 10.0F);
        setBeamLength(tag.contains("BeamLength") ? tag.getFloat("BeamLength") : 1.0F);
        this.forceBowPoseSynced = tag.getBoolean("ForceBowPoseSynced");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putInt("Life", this.tickCount);
        tag.putFloat("Power", getPower());
        tag.putFloat("BeamLength", getBeamLength());
        tag.putBoolean("ForceBowPoseSynced", this.forceBowPoseSynced);
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

    private void setPower(float power) {
        this.entityData.set(POWER, Math.max(power, 0.1F));
    }

    private void setBeamLength(float length) {
        this.entityData.set(BEAM_LENGTH, Math.max(length, 1.0F));
    }

    private void updateFromOwner(LivingEntity owner) {
        Vec3 look = owner.getLookAngle();
        if (look.lengthSqr() <= 1.0E-8D) {
            look = Vec3.directionFromRotation(owner.getXRot(), owner.getYHeadRot());
        }
        Vec3 start = owner.position().add(look).add(0.0D, 1.2D, 0.0D);
        setPos(start.x(), start.y(), start.z());

        HitResult hit = ProcedureUtils.objectEntityLookingAt(owner, getPower());
        Vec3 target = hit.getLocation();
        Vec3 direction = target.subtract(position());
        if (direction.lengthSqr() <= 1.0E-8D) {
            direction = look;
        }
        aimAlong(direction);
    }

    private void aimAlong(Vec3 direction) {
        double length = direction.length();
        if (length <= 1.0E-8D) {
            direction = new Vec3(0.0D, 0.0D, 1.0D);
            length = 1.0D;
        }
        Vec3 normal = direction.normalize();
        setBeamLength((float) length + 0.1F);
        double horizontal = Math.sqrt(normal.x() * normal.x() + normal.z() * normal.z());
        setYRot((float)(Mth.atan2(normal.x(), normal.z()) * Mth.RAD_TO_DEG));
        setXRot((float)(Mth.atan2(normal.y(), horizontal) * Mth.RAD_TO_DEG));
        this.yRotO = getYRot();
        this.xRotO = getXRot();
    }

    private Vec3 beamDirection() {
        double yaw = getYRot() * Mth.DEG_TO_RAD;
        double pitch = getXRot() * Mth.DEG_TO_RAD;
        double horizontal = Math.cos(pitch);
        return new Vec3(Math.sin(yaw) * horizontal, Math.sin(pitch), Math.cos(yaw) * horizontal).normalize();
    }

    private void applyBeamEffects(ServerLevel level, LivingEntity owner) {
        Vec3 start = position();
        Vec3 direction = beamDirection();
        double range = getBeamLength();
        damageEntities(level, owner, start, direction, range);
        destroyBlocks(level, owner, start, direction, range);
    }

    private void damageEntities(ServerLevel level, LivingEntity owner, Vec3 start, Vec3 direction, double range) {
        Vec3 end = start.add(direction.scale(range));
        AABB search = new AABB(start, end).inflate(BEAM_RADIUS + 1.0D);
        DamageSource source = this.damageSources().indirectMagic(this, owner);
        for (Entity target : level.getEntities(this, search, entity -> canAffect(owner, entity))) {
            if (!intersectsBeam(target.getBoundingBox(), start, direction, range, BEAM_RADIUS + 1.0D)) {
                continue;
            }
            if (target instanceof LivingEntity living) {
                living.invulnerableTime = 0;
            }
            target.hurt(source, getPower() * 0.6F);
        }
    }

    private boolean canAffect(LivingEntity owner, Entity target) {
        return target.isAlive()
                && target != owner
                && target.getRootVehicle() != owner.getRootVehicle()
                && !(target instanceof TenseiBakuGoldEntity);
    }

    private void destroyBlocks(ServerLevel level, LivingEntity owner, Vec3 start, Vec3 direction, double range) {
        if (!ForgeEventFactory.getMobGriefingEvent(level, owner)) {
            return;
        }
        Vec3 end = start.add(direction.scale(range));
        AABB search = new AABB(start, end).inflate(BEAM_RADIUS + 1.0D);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int minX = Mth.floor(search.minX);
        int maxX = Mth.ceil(search.maxX);
        int minY = Mth.floor(search.minY);
        int maxY = Mth.ceil(search.maxY);
        int minZ = Mth.floor(search.minZ);
        int maxZ = Mth.ceil(search.maxZ);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cursor.set(x, y, z);
                    if (!level.hasChunkAt(cursor) || !isBlockInBeam(cursor, start, direction, range)) {
                        continue;
                    }
                    if (ProcedureUtils.breakBlockAndDropWithChance(level, cursor, BLOCK_HARDNESS_LIMIT, 1.0F, -1.0F, true)) {
                        spawnBlockBreakFeedback(level, cursor);
                    }
                }
            }
        }
    }

    private boolean isBlockInBeam(BlockPos pos, Vec3 start, Vec3 direction, double range) {
        Vec3 center = Vec3.atCenterOf(pos);
        double along = center.subtract(start).dot(direction);
        if (along < 0.0D || along > range) {
            return false;
        }
        return center.distanceToSqr(start.add(direction.scale(along))) <= BEAM_RADIUS * BEAM_RADIUS;
    }

    private boolean intersectsBeam(AABB box, Vec3 start, Vec3 direction, double range, double radiusPadding) {
        Vec3 center = box.getCenter();
        double along = Mth.clamp(center.subtract(start).dot(direction), 0.0D, range);
        double radius = radiusPadding + Math.max(box.getXsize(), Math.max(box.getYsize(), box.getZsize())) * 0.5D;
        return center.distanceToSqr(start.add(direction.scale(along))) <= radius * radius;
    }

    private void spawnBlockBreakFeedback(ServerLevel level, BlockPos pos) {
        level.sendParticles(
                ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0x80000000, 16, 60, 0, -1, 4),
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                1,
                0.0D,
                0.0D,
                0.0D,
                0.0D);
        if (this.random.nextFloat() < 0.005F) {
            level.playSound(null, pos, ModSounds.SOUND_EXPLOSION.get(), SoundSource.BLOCKS,
                    4.0F, this.random.nextFloat() * 0.5F + 0.75F);
        }
    }

    private void clearForceBowPose() {
        if (!this.level().isClientSide && this.forceBowPoseSynced) {
            LivingEntity owner = getOwner();
            if (owner != null) {
                ProcedureSync.EntityNBTTag.removeAndSync(owner, NarutomodModVariables.FORCE_BOW_POSE);
            }
            this.forceBowPoseSynced = false;
        }
    }

    private void discardGold() {
        clearForceBowPose();
        discard();
    }
}
