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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class JintonBeamEntity extends Entity {
    private static final int WAIT_TICKS = 60;
    private static final int ACTIVE_TICKS = 60;
    private static final double ACTIVE_RANGE = 30.0D;
    private static final float BLOCK_HARDNESS_LIMIT = 100.0F;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(JintonBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> BEAM_LENGTH = SynchedEntityData.defineId(JintonBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(JintonBeamEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUuid;

    public JintonBeamEntity(EntityType<? extends JintonBeamEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public void configure(LivingEntity owner, float chargedPower) {
        setOwner(owner);
        setScale(Math.min(chargedPower / 2.0F + 0.5F, 10.0F));
        updateFromOwner(owner, 1.0D);
    }

    public static boolean spawnFrom(LivingEntity owner, float chargedPower) {
        JintonBeamEntity beam = ModEntityTypes.JINTONBEAM.get().create(owner.level());
        if (beam == null) {
            return false;
        }
        beam.configure(owner, chargedPower);
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), ModSounds.SOUND_GENKAIHAKURINOJUTSU.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        owner.level().addFreshEntity(beam);
        return true;
    }

    public float getBeamLength() {
        return this.entityData.get(BEAM_LENGTH);
    }

    public float getBeamScale() {
        return this.entityData.get(SCALE);
    }

    public int getWaitTicks() {
        return WAIT_TICKS;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(BEAM_LENGTH, 1.0F);
        this.entityData.define(SCALE, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        if (this.tickCount < WAIT_TICKS) {
            updateFromOwner(owner, 1.0D);
            return;
        }
        updateFromOwner(owner, ACTIVE_RANGE);
        if (!this.level().isClientSide && this.tickCount >= WAIT_TICKS + 2) {
            applyBeamEffects((ServerLevel) this.level(), owner);
        }
        if (this.tickCount > WAIT_TICKS + ACTIVE_TICKS) {
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
        return new AABB(position(), end).inflate(Math.max(getBeamScale(), 1.0F));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 128.0D;
        return distance < range * range;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        this.tickCount = tag.getInt("Life");
        setBeamLength(tag.contains("BeamLength") ? tag.getFloat("BeamLength") : 1.0F);
        setScale(tag.contains("Scale") ? tag.getFloat("Scale") : 1.0F);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putInt("Life", this.tickCount);
        tag.putFloat("BeamLength", getBeamLength());
        tag.putFloat("Scale", getBeamScale());
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

    private void setBeamLength(float length) {
        this.entityData.set(BEAM_LENGTH, Math.max(length, 1.0F));
    }

    private void setScale(float scale) {
        this.entityData.set(SCALE, Mth.clamp(scale, 0.1F, 10.0F));
    }

    private void updateFromOwner(LivingEntity owner, double range) {
        Vec3 start = owner.position().add(0.0D, owner.getEyeHeight() - 0.2D, 0.0D);
        Vec3 look = owner.getLookAngle();
        if (look.lengthSqr() <= 1.0E-8D) {
            look = new Vec3(0.0D, 0.0D, 1.0D);
        }
        look = look.normalize();
        setPos(start.x(), start.y(), start.z());
        aimAlong(look, range);
    }

    private void aimAlong(Vec3 direction, double range) {
        setBeamLength((float) range + 0.1F);
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

    private void applyBeamEffects(ServerLevel level, LivingEntity owner) {
        Vec3 start = position();
        Vec3 direction = beamDirection();
        double range = getBeamLength();
        double farRadius = getBeamScale() * 0.5D;
        damageEntities(level, owner, start, direction, range, farRadius);
        destroyBlocks(level, owner, start, direction, range, farRadius);
    }

    private void damageEntities(ServerLevel level, LivingEntity owner, Vec3 start, Vec3 direction, double range, double farRadius) {
        Vec3 end = start.add(direction.scale(range));
        AABB search = new AABB(start, end).inflate(farRadius + 1.0D);
        DamageSource source = ModDamageTypes.jinton(level, this, owner);
        for (Entity target : level.getEntities(this, search, entity -> entity.isAlive()
                && entity != owner
                && entity.getRootVehicle() != owner.getRootVehicle()
                && !(entity instanceof JintonBeamEntity))) {
            if (!intersectsBeam(target.getBoundingBox(), start, direction, range, farRadius + 1.0D)) {
                continue;
            }
            if (target instanceof LivingEntity living) {
                living.invulnerableTime = 0;
                double averageEdge = averageEdgeLength(living.getBoundingBox());
                float damage = living.getMaxHealth() * (float)(farRadius / Math.max(averageEdge, 0.1D) * 0.25D);
                living.hurt(source, Math.max(damage, 1.0F));
            } else {
                target.discard();
            }
        }
    }

    private void destroyBlocks(ServerLevel level, LivingEntity owner, Vec3 start, Vec3 direction, double range, double farRadius) {
        if (!ForgeEventFactory.getMobGriefingEvent(level, owner)) {
            return;
        }
        Vec3 end = start.add(direction.scale(range));
        AABB search = new AABB(start, end).inflate(farRadius + 1.0D);
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
                    if (!level.hasChunkAt(cursor) || !isBlockInBeam(cursor, start, direction, range, farRadius)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(cursor);
                    float hardness = state.getDestroySpeed(level, cursor);
                    if (state.isAir() || hardness < 0.0F || hardness > BLOCK_HARDNESS_LIMIT) {
                        continue;
                    }
                    level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 3);
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                            x + 0.5D, y + 0.5D, z + 0.5D, 2, 0.2D, 0.2D, 0.2D, 0.0D);
                }
            }
        }
    }

    private boolean isBlockInBeam(BlockPos pos, Vec3 start, Vec3 direction, double range, double farRadius) {
        Vec3 center = Vec3.atCenterOf(pos);
        double along = center.subtract(start).dot(direction);
        if (along < 0.0D || along > range) {
            return false;
        }
        double radius = farRadius * along / range + 0.9D;
        return center.distanceToSqr(start.add(direction.scale(along))) <= radius * radius;
    }

    private boolean intersectsBeam(AABB box, Vec3 start, Vec3 direction, double range, double radiusPadding) {
        Vec3 center = box.getCenter();
        double along = Mth.clamp(center.subtract(start).dot(direction), 0.0D, range);
        double radius = radiusPadding * along / range + Math.max(box.getXsize(), Math.max(box.getYsize(), box.getZsize())) * 0.5D;
        return center.distanceToSqr(start.add(direction.scale(along))) <= radius * radius;
    }

    private static double averageEdgeLength(AABB box) {
        return (box.getXsize() + box.getYsize() + box.getZsize()) / 3.0D;
    }
}
