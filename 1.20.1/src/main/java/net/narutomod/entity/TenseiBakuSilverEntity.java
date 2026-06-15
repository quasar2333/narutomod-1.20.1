package net.narutomod.entity;

import java.util.HashSet;
import java.util.Set;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.NarutomodModVariables;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureSync;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class TenseiBakuSilverEntity extends Entity {
    public static final int GROW_TIME = 20;
    private static final double BLOCK_RANDOMNESS_RADIUS = 1.5D;
    private static final float BLOCK_HARDNESS_LIMIT = 1.0F;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(TenseiBakuSilverEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> POWER = SynchedEntityData.defineId(TenseiBakuSilverEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUuid;
    private boolean forceBowPoseSynced;

    public TenseiBakuSilverEntity(EntityType<? extends TenseiBakuSilverEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public static boolean spawnFrom(LivingEntity owner, float power) {
        TenseiBakuSilverEntity entity = ModEntityTypes.TENSEI_BAKU_SILVER.get().create(owner.level());
        if (entity == null) {
            return false;
        }
        entity.configure(owner, power);
        return owner.level().addFreshEntity(entity);
    }

    public void configure(LivingEntity owner, float power) {
        setOwner(owner);
        setPower(power);
        moveToIdlePosition(owner);
    }

    public float getPower() {
        return this.entityData.get(POWER);
    }

    public float getGrowth(float partialTick) {
        return Mth.clamp((this.tickCount + partialTick) / (float) GROW_TIME, 0.0F, 1.0F);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(POWER, 10.0F);
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            if (!this.level().isClientSide) {
                discardSilver();
            }
            return;
        }

        moveToIdlePosition(owner);
        if (this.level().isClientSide) {
            return;
        }

        if (this.tickCount == 1) {
            ProcedureSync.EntityNBTTag.setAndSync(owner, NarutomodModVariables.FORCE_BOW_POSE, true);
            this.forceBowPoseSynced = true;
            this.level().playSound(null, getX(), getY(), getZ(),
                    ModSounds.SOUND_TENSEIBLASTCHARGE.get(), SoundSource.NEUTRAL, 2.0F, 1.0F);
        } else if (this.tickCount >= GROW_TIME) {
            if (this.tickCount % 40 == GROW_TIME) {
                Vec3 soundAt = owner.getLookAngle().normalize().scale(getPower() * 0.5D).add(position());
                this.level().playSound(null, soundAt.x(), soundAt.y(), soundAt.z(),
                        ModSounds.SOUND_WIND.get(), SoundSource.NEUTRAL, 4.0F, 1.0F);
            }
            executeSilverBlast(owner);
        }

        if (this.tickCount > getDuration()) {
            discardSilver();
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
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 68.5D * getViewScale();
        return distance < range * range;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        setPower(tag.contains("Power") ? tag.getFloat("Power") : 10.0F);
        this.forceBowPoseSynced = tag.getBoolean("ForceBowPoseSynced");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putFloat("Power", getPower());
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

    private int getDuration() {
        return (int)getPower() * 4 + GROW_TIME;
    }

    private void moveToIdlePosition(LivingEntity owner) {
        Vec3 look = owner.getLookAngle();
        if (look.lengthSqr() <= 1.0E-8D) {
            look = Vec3.directionFromRotation(owner.getXRot(), owner.getYHeadRot());
        }
        this.setDeltaMovement(Vec3.ZERO);
        this.moveTo(owner.getX() + look.x(), owner.getY() + 0.6D, owner.getZ() + look.z(), owner.getYRot(), 0.0F);
    }

    private void executeSilverBlast(LivingEntity owner) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        double range = getPower();
        double farRadius = range * 0.1D;
        if (range <= 0.0D) {
            return;
        }
        Vec3 look = owner.getLookAngle();
        if (look.lengthSqr() <= 1.0E-8D) {
            return;
        }
        look = look.normalize();
        Vec3 start = owner.getEyePosition().subtract(0.0D, 0.4D, 0.0D);
        spawnWindParticles(serverLevel, owner, look, range, farRadius);
        pushEntities(owner, start, look, range, farRadius);
        disturbBlocks(serverLevel, owner, start, look, range, farRadius);
    }

    private void spawnWindParticles(ServerLevel level, LivingEntity owner, Vec3 look, double range, double farRadius) {
        Vec3 start = look.scale(2.0D).add(owner.getX(), owner.getY() + 1.5D, owner.getZ());
        for (int i = 1; i <= 50; i++) {
            Vec3 forward = look.scale((this.random.nextDouble() * 0.8D + 0.2D) * range * 0.125D);
            Vec3 motion = forward.add(
                    (this.random.nextDouble() - 0.5D) * farRadius * 0.15D,
                    (this.random.nextDouble() - 0.5D) * farRadius * 0.15D,
                    (this.random.nextDouble() - 0.5D) * farRadius * 0.15D);
            level.sendParticles(
                    ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED,
                            0x80C0C0C0,
                            (int)(16.0D / (this.random.nextDouble() * 0.8D + 0.2D)),
                            80 + this.random.nextInt(20),
                            0xF0,
                            -1,
                            4),
                    start.x(),
                    start.y(),
                    start.z(),
                    0,
                    motion.x(),
                    motion.y(),
                    motion.z(),
                    1.0D);
        }
    }

    private void pushEntities(LivingEntity owner, Vec3 start, Vec3 look, double range, double farRadius) {
        Vec3 ray = look.scale(range);
        AABB search = owner.getBoundingBox().expandTowards(ray).inflate(farRadius + 1.0D);
        Set<Integer> pushed = new HashSet<>();
        for (Entity target : this.level().getEntities(this, search, target -> canAffect(owner, target))) {
            double distance = owner.distanceTo(target);
            double allowedRadius = distance / range * farRadius + 1.0D;
            AABB box = target.getBoundingBox().inflate(allowedRadius);
            if ((box.contains(start) || box.clip(start, start.add(ray)).isPresent()) && pushed.add(target.getId())) {
                ProcedureUtils.pushEntity(owner, target, range, target instanceof FallingBlockEntity ? 1.0F : 2.0F);
            }
        }
    }

    private boolean canAffect(LivingEntity owner, Entity target) {
        return target.isAlive()
                && target != owner
                && !(target instanceof Player player && player.isSpectator())
                && target.getRootVehicle() != owner.getRootVehicle()
                && !(target instanceof TenseiBakuSilverEntity);
    }

    private void disturbBlocks(ServerLevel level, LivingEntity owner, Vec3 start, Vec3 look, double range, double farRadius) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        Vec3 end = start.add(look.scale(range));
        for (BlockPos pos : collectBlocksInCone(start, look, range, farRadius)) {
            if (!rayReachesInflatedBlock(start, end, pos, farRadius)) {
                continue;
            }
            maybeSpawnFallingBlock(level, owner, pos, range);
        }
    }

    private Set<BlockPos> collectBlocksInCone(Vec3 start, Vec3 look, double range, double farRadius) {
        Set<BlockPos> result = new HashSet<>();
        int steps = Mth.clamp((int)Math.ceil(range * 2.0D), 1, 160);
        for (int i = 0; i <= steps; i++) {
            double distance = range * i / steps;
            double radius = distance / range * farRadius + BLOCK_RANDOMNESS_RADIUS;
            Vec3 center = start.add(look.scale(distance));
            BlockPos min = BlockPos.containing(center.x() - radius, center.y() - radius, center.z() - radius);
            BlockPos max = BlockPos.containing(center.x() + radius, center.y() + radius, center.z() + radius);
            BlockPos.betweenClosed(min, max).forEach(pos -> {
                if (blockInsideCone(start, look, range, farRadius, pos)) {
                    result.add(pos.immutable());
                }
            });
        }
        return result;
    }

    private boolean blockInsideCone(Vec3 start, Vec3 look, double range, double farRadius, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 toBlock = center.subtract(start);
        double along = toBlock.dot(look);
        if (along < 0.0D || along > range) {
            return false;
        }
        double allowedRadius = along / range * farRadius + BLOCK_RANDOMNESS_RADIUS;
        double perpendicular = toBlock.subtract(look.scale(along)).length();
        return perpendicular <= allowedRadius;
    }

    private boolean rayReachesInflatedBlock(Vec3 start, Vec3 end, BlockPos pos, double radius) {
        AABB box = new AABB(pos).inflate(radius);
        return box.contains(start) || box.clip(start, end).isPresent();
    }

    private void maybeSpawnFallingBlock(ServerLevel level, LivingEntity owner, BlockPos pos, double range) {
        if (this.random.nextInt(10) != 0 || level.getBlockEntity(pos) != null) {
            return;
        }
        BlockState blockState = level.getBlockState(pos);
        float hardness = blockState.getDestroySpeed(level, pos);
        if (blockState.isAir() || hardness < 0.0F || hardness > BLOCK_HARDNESS_LIMIT) {
            return;
        }
        FallingBlockEntity falling = FallingBlockEntity.fall(level, pos, blockState);
        falling.dropItem = false;
        falling.setNoGravity(true);
        ProcedureUtils.pushEntity(owner, falling, range, 1.0F);
        if (ProcedureUtils.getVelocity(falling) > 0.1D) {
            falling.setNoGravity(false);
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

    private void discardSilver() {
        clearForceBowPose();
        discard();
    }
}
