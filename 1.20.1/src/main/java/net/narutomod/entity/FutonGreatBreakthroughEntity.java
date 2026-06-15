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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class FutonGreatBreakthroughEntity extends Entity {
    private static final float PUSH_MULTIPLIER = 3.0F;
    private static final float BLOCK_HARDNESS_LIMIT = 1.0F;
    private static final double BLOCK_RANDOMNESS_RADIUS = 1.5D;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(FutonGreatBreakthroughEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> POWER = SynchedEntityData.defineId(FutonGreatBreakthroughEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUuid;

    public FutonGreatBreakthroughEntity(EntityType<? extends FutonGreatBreakthroughEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public void configure(LivingEntity owner, float power) {
        setOwner(owner);
        setPower(power);
        moveToOwner(owner);
    }

    public static boolean spawnFrom(LivingEntity owner, float power) {
        FutonGreatBreakthroughEntity entity = ModEntityTypes.FUTON_GREAT_BREAKTHROUGH.get().create(owner.level());
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
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        moveToOwner(owner);
        if (this.tickCount == 1) {
            this.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                    ModSounds.SOUND_WIND.get(), SoundSource.NEUTRAL, 1.0F, getPower() * 0.2F);
        }
        executeGreatBreakthrough(owner);
        if (this.tickCount > Math.max((int)getPower(), 1)) {
            discard();
        }
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
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putFloat("Power", getPower());
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

    private void moveToOwner(LivingEntity owner) {
        this.setDeltaMovement(Vec3.ZERO);
        this.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
    }

    private void executeGreatBreakthrough(LivingEntity owner) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        double range = getPower();
        double farRadius = range * 0.25D;
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
        breakBlocks(serverLevel, owner, start, look, range, farRadius);
    }

    private void spawnWindParticles(ServerLevel level, LivingEntity owner, Vec3 look, double range, double farRadius) {
        Vec3 start = look.scale(2.0D).add(owner.getX(), owner.getY() + 1.5D, owner.getZ());
        for (int i = 1; i <= 50; i++) {
            Vec3 forward = look.scale((this.random.nextDouble() * 0.8D + 0.2D) * range * 0.1D);
            Vec3 motion = forward.add(
                    (this.random.nextDouble() - 0.5D) * farRadius * 0.15D,
                    (this.random.nextDouble() - 0.5D) * farRadius * 0.15D,
                    (this.random.nextDouble() - 0.5D) * farRadius * 0.15D);
            level.sendParticles(
                    ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED,
                            0x80FFFFFF,
                            80 + this.random.nextInt(20),
                            (int)(16.0D / (this.random.nextDouble() * 0.8D + 0.2D)),
                            0,
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
                ProcedureUtils.pushEntity(owner, target, range * 1.6D, PUSH_MULTIPLIER);
            }
        }
    }

    private void breakBlocks(ServerLevel level, LivingEntity owner, Vec3 start, Vec3 look, double range, double farRadius) {
        for (BlockPos pos : collectBlocksInCone(start, look, range, farRadius)) {
            float breakChance = getBreakChance(owner, pos, range);
            if (breakChance > 0.0F) {
                ProcedureUtils.breakBlockAndDropWithChance(level, pos, BLOCK_HARDNESS_LIMIT, breakChance, 0.1F, true);
            }
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

    private float getBreakChance(LivingEntity owner, BlockPos pos, double range) {
        double cappedRange = Mth.clamp(range, 0.0D, 30.0D);
        if (cappedRange <= 0.0D) {
            return 0.0F;
        }
        double distance = Math.sqrt(owner.distanceToSqr(Vec3.atCenterOf(pos)));
        return Math.max((float)(1.0D - distance / cappedRange) * 0.2F, 0.0F);
    }

    private boolean canAffect(LivingEntity owner, Entity target) {
        return target.isAlive()
                && target != owner
                && target.getRootVehicle() != owner.getRootVehicle()
                && !(target instanceof FutonGreatBreakthroughEntity);
    }
}
