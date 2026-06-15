package net.narutomod.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class SekizoEntity extends Entity {
    private static final double RANGE = 30.0D;
    private static final double FAR_RADIUS = 5.0D;
    private static final int IMPACT_TICK = 10;
    private static final int MAX_LIFE = 60;
    private static final float BLOCK_HARDNESS_LIMIT = 2.0F;
    private static final float BLOCK_DROP_CHANCE = 0.2F;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(SekizoEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> BEAM_LENGTH = SynchedEntityData.defineId(SekizoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(SekizoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> PUNCH_INDEX = SynchedEntityData.defineId(SekizoEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;
    private boolean impactApplied;

    public SekizoEntity(EntityType<? extends SekizoEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public static boolean spawnFrom(LivingEntity owner, int punchIndex) {
        SekizoEntity entity = ModEntityTypes.ENTITYSEKIZO.get().create(owner.level());
        if (entity == null) {
            return false;
        }
        entity.configure(owner, punchIndex);
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                ModSounds.SOUND_SEKIZO.get(), SoundSource.NEUTRAL, 2.0F, 1.0F);
        return owner.level().addFreshEntity(entity);
    }

    public float getBeamLength() {
        return this.entityData.get(BEAM_LENGTH);
    }

    public int getPunchIndex() {
        return this.entityData.get(PUNCH_INDEX);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(BEAM_LENGTH, 1.0F);
        this.entityData.define(DAMAGE, 1.0F);
        this.entityData.define(PUNCH_INDEX, 0);
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        if (this.tickCount <= IMPACT_TICK) {
            updateFromOwner(owner);
        }
        if (!this.level().isClientSide && this.tickCount == IMPACT_TICK && !this.impactApplied) {
            this.impactApplied = true;
            applyAirPunch((ServerLevel) this.level(), owner);
        }
        if (!this.level().isClientSide && this.tickCount > MAX_LIFE) {
            discard();
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        Vec3 end = position().add(beamDirection().scale(getBeamLength()));
        return new AABB(position(), end).inflate(FAR_RADIUS + 1.0D);
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
        setBeamLength(tag.contains("BeamLength") ? tag.getFloat("BeamLength") : 1.0F);
        this.entityData.set(DAMAGE, tag.contains("Damage") ? tag.getFloat("Damage") : 1.0F);
        this.entityData.set(PUNCH_INDEX, tag.getInt("PunchIndex"));
        this.impactApplied = tag.getBoolean("ImpactApplied");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putInt("Life", this.tickCount);
        tag.putFloat("BeamLength", getBeamLength());
        tag.putFloat("Damage", this.entityData.get(DAMAGE));
        tag.putInt("PunchIndex", getPunchIndex());
        tag.putBoolean("ImpactApplied", this.impactApplied);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void configure(LivingEntity owner, int punchIndex) {
        setOwner(owner);
        this.entityData.set(PUNCH_INDEX, Mth.clamp(punchIndex, 0, 4));
        this.entityData.set(DAMAGE, (float) ProcedureUtils.getModifiedAttackDamage(owner) * (float) Math.pow(2.0D, punchIndex));
        updateFromOwner(owner);
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

    private void setBeamLength(float length) {
        this.entityData.set(BEAM_LENGTH, Math.max(length, 1.0F));
    }

    private void updateFromOwner(LivingEntity owner) {
        Vec3 start = owner.getEyePosition().subtract(0.0D, 0.1D, 0.0D);
        Vec3 look = owner.getLookAngle();
        if (look.lengthSqr() <= 1.0E-8D) {
            look = Vec3.directionFromRotation(owner.getXRot(), owner.getYHeadRot());
        }
        if (look.lengthSqr() <= 1.0E-8D) {
            look = new Vec3(0.0D, 0.0D, 1.0D);
        }
        look = look.normalize();
        setPos(start.x(), start.y(), start.z());
        HitResult hit = ProcedureUtils.objectEntityLookingAt(owner, RANGE, true, false, target -> target != this);
        double length = hit.getType() == HitResult.Type.MISS ? RANGE : Mth.clamp(start.distanceTo(hit.getLocation()), 1.0D, RANGE);
        aimAlong(look, length + 0.1D);
    }

    private void aimAlong(Vec3 direction, double range) {
        setBeamLength((float) range);
        double horizontal = Math.sqrt(direction.x() * direction.x() + direction.z() * direction.z());
        setYRot((float)(Mth.atan2(direction.x(), direction.z()) * Mth.RAD_TO_DEG));
        setXRot((float)(Mth.atan2(direction.y(), horizontal) * Mth.RAD_TO_DEG));
        this.yRotO = getYRot();
        this.xRotO = getXRot();
    }

    private Vec3 beamDirection() {
        double yaw = getYRot() * Mth.DEG_TO_RAD;
        double pitch = getXRot() * Mth.DEG_TO_RAD;
        double horizontal = Math.cos(pitch);
        return new Vec3(Math.sin(yaw) * horizontal, Math.sin(pitch), Math.cos(yaw) * horizontal).normalize();
    }

    private void applyAirPunch(ServerLevel level, LivingEntity owner) {
        Vec3 start = position();
        Vec3 direction = beamDirection();
        spawnPreParticles(level, start, direction);
        damageAndPushEntities(level, owner, start, direction);
        breakBlocks(level, owner, start, direction);
    }

    private void spawnPreParticles(ServerLevel level, Vec3 start, Vec3 direction) {
        for (int i = 1; i <= RANGE; i++) {
            Vec3 point = start.add(direction.scale(i));
            double radius = FAR_RADIUS * i / RANGE;
            level.sendParticles(ParticleTypes.EXPLOSION, point.x(), point.y(), point.z(), i, radius, radius, radius, 0.1D);
        }
    }

    private void damageAndPushEntities(ServerLevel level, LivingEntity owner, Vec3 start, Vec3 direction) {
        Vec3 end = start.add(direction.scale(RANGE));
        AABB search = new AABB(start, end).inflate(FAR_RADIUS + 1.0D);
        DamageSource source = owner.damageSources().mobAttack(owner);
        for (Entity target : level.getEntities(this, search, entity -> entity.isAlive()
                && entity != owner
                && entity.getRootVehicle() != owner.getRootVehicle()
                && !(entity instanceof SekizoEntity))) {
            if (!intersectsCone(target.getBoundingBox(), start, direction, RANGE, FAR_RADIUS + 1.0D)) {
                continue;
            }
            if (target.isPushable()) {
                ProcedureUtils.pushEntity(owner, target, RANGE, 2.0F);
            }
            target.hurt(source, this.entityData.get(DAMAGE));
        }
    }

    private void breakBlocks(ServerLevel level, LivingEntity owner, Vec3 start, Vec3 direction) {
        Vec3 end = start.add(direction.scale(RANGE));
        AABB search = new AABB(start, end).inflate(FAR_RADIUS + 1.0D);
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
                    if (!level.hasChunkAt(cursor) || !isBlockInCone(cursor, start, direction, RANGE, FAR_RADIUS)) {
                        continue;
                    }
                    BlockPos pos = cursor.immutable();
                    if (ProcedureUtils.breakBlockAndDropWithChance(level, pos, BLOCK_HARDNESS_LIMIT, 1.0F, BLOCK_DROP_CHANCE, false)) {
                        level.sendParticles(ParticleTypes.EXPLOSION, x + 0.5D, y + 0.5D, z + 0.5D, 2, 0.2D, 0.2D, 0.2D, 0.0D);
                    }
                }
            }
        }
    }

    private boolean isBlockInCone(BlockPos pos, Vec3 start, Vec3 direction, double range, double farRadius) {
        Vec3 center = Vec3.atCenterOf(pos);
        double along = center.subtract(start).dot(direction);
        if (along < 0.0D || along > range) {
            return false;
        }
        double radius = farRadius * along / range + 0.9D;
        return center.distanceToSqr(start.add(direction.scale(along))) <= radius * radius;
    }

    private boolean intersectsCone(AABB box, Vec3 start, Vec3 direction, double range, double radiusPadding) {
        Vec3 center = box.getCenter();
        double along = Mth.clamp(center.subtract(start).dot(direction), 0.0D, range);
        double radius = radiusPadding * along / range + Math.max(box.getXsize(), Math.max(box.getYsize(), box.getZsize())) * 0.5D;
        return center.distanceToSqr(start.add(direction.scale(along))) <= radius * radius;
    }
}
