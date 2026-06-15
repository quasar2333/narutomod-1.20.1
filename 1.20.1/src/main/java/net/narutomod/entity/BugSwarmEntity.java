package net.narutomod.entity;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
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
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.Chakra;
import net.narutomod.NarutomodMod;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class BugSwarmEntity extends Entity {
    public static final double RANGE = 30.0D;
    public static final int MAX_TARGET_TIME = 600;
    public static final double CHAKRA_DRAIN_PER_TARGET_TICK = 0.5D;
    private static final double RETURN_SPEED = 0.65D;
    private static final double ATTACK_SPEED = 0.45D;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(BugSwarmEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(BugSwarmEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> POWER = SynchedEntityData.defineId(BugSwarmEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> RETURNING = SynchedEntityData.defineId(BugSwarmEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private UUID targetUuid;
    private double storedChakra;

    public BugSwarmEntity(EntityType<? extends BugSwarmEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public static boolean spawnFrom(LivingEntity owner, LivingEntity target, float power) {
        if (!(owner.level() instanceof ServerLevel serverLevel) || !canTarget(owner, target) || power <= 0.0F) {
            return false;
        }
        BugSwarmEntity swarm = ModEntityTypes.BUGBALL.get().create(serverLevel);
        if (swarm == null) {
            return false;
        }
        swarm.configure(owner, target, power);
        serverLevel.addFreshEntity(swarm);
        return true;
    }

    @Nullable
    public static LivingEntity findTarget(LivingEntity owner) {
        HitResult hit = ProcedureUtils.objectEntityLookingAt(owner, RANGE, 0.0D, true, false,
                target -> target instanceof LivingEntity living && canTarget(owner, living));
        return hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living ? living : null;
    }

    @Nullable
    public static BugSwarmEntity findLookedAtSwarm(LivingEntity owner) {
        HitResult hit = ProcedureUtils.objectEntityLookingAt(owner, RANGE, 0.25D, true, false,
                target -> target instanceof BugSwarmEntity swarm && swarm.isOwnedBy(owner));
        return hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof BugSwarmEntity swarm ? swarm : null;
    }

    public static int returnOwnedNear(LivingEntity owner, double radius) {
        List<BugSwarmEntity> swarms = owner.level().getEntitiesOfClass(
                BugSwarmEntity.class,
                owner.getBoundingBox().inflate(radius),
                swarm -> swarm.isOwnedBy(owner));
        for (BugSwarmEntity swarm : swarms) {
            swarm.triggerReturn();
        }
        return swarms.size();
    }

    private static boolean canTarget(LivingEntity owner, LivingEntity target) {
        return target != owner && target.isAlive();
    }

    private void configure(LivingEntity owner, LivingEntity target, float power) {
        setOwner(owner);
        setTarget(target);
        this.entityData.set(POWER, Math.max(power, 0.1F));
        this.entityData.set(RETURNING, false);
        Vec3 start = ownerVector(owner);
        moveTo(start.x(), start.y(), start.z(), owner.getYRot(), owner.getXRot());
        updateSwarmBounds();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(TARGET_ID, -1);
        this.entityData.define(POWER, 1.0F);
        this.entityData.define(RETURNING, false);
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

        LivingEntity target = getTarget();
        boolean returning = isReturning() || this.tickCount >= MAX_TARGET_TIME || target == null || !canTarget(owner, target);
        if (returning) {
            this.entityData.set(RETURNING, true);
        }

        Vec3 destination = returning ? ownerVector(owner) : target.getBoundingBox().getCenter();
        moveToward(destination, returning ? RETURN_SPEED : ATTACK_SPEED);
        updateSwarmBounds();
        spawnSwarmParticles();
        playBugSound();

        if (returning) {
            if (this.position().distanceToSqr(ownerVector(owner)) < 1.0D || this.getBoundingBox().intersects(owner.getBoundingBox().inflate(0.5D))) {
                refundStoredChakra(owner);
                discard();
            }
            return;
        }

        drainTargets(owner);
        if (this.getBoundingBox().intersects(owner.getBoundingBox().inflate(0.25D))) {
            refundStoredChakra(owner);
        }
    }

    private void drainTargets(LivingEntity owner) {
        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox(), target -> canDrain(owner, target))) {
            if (Chakra.pathway(target).consume(CHAKRA_DRAIN_PER_TARGET_TICK)) {
                this.storedChakra += CHAKRA_DRAIN_PER_TARGET_TICK;
            }
        }
    }

    private boolean canDrain(LivingEntity owner, LivingEntity target) {
        return target != owner
                && target.isAlive()
                && target.getRootVehicle() != owner.getRootVehicle();
    }

    private void moveToward(Vec3 destination, double speed) {
        Vec3 delta = destination.subtract(position());
        double distance = delta.length();
        Vec3 next = distance <= speed ? destination : position().add(delta.scale(speed / distance));
        moveTo(next.x(), next.y(), next.z(), getYRot(), getXRot());
    }

    private void updateSwarmBounds() {
        double radius = getSwarmRadius();
        setBoundingBox(new AABB(
                getX() - radius,
                getY() - radius * 0.6D,
                getZ() - radius,
                getX() + radius,
                getY() + radius * 0.6D,
                getZ() + radius));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0D;
    }

    public double getSwarmRadius() {
        return Mth.clamp(0.75D + Math.sqrt(getBugCount()) * 0.045D, 0.75D, 2.2D);
    }

    public int getBugCount() {
        return Math.max(1, (int)(getPower() * 50.0F));
    }

    public float getPower() {
        return this.entityData.get(POWER);
    }

    public boolean isReturning() {
        return this.entityData.get(RETURNING);
    }

    public void triggerReturn() {
        this.entityData.set(RETURNING, true);
    }

    private void spawnSwarmParticles() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int count = Mth.clamp(getBugCount() / 3, 8, 40);
        double radius = getSwarmRadius();
        serverLevel.sendParticles(
                ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0xD0181410, 3, 0, 0, -1, 4),
                getX(),
                getY(),
                getZ(),
                count,
                radius,
                radius * 0.4D,
                radius,
                0.01D);
        if (this.tickCount % 12 == 0) {
            serverLevel.sendParticles(ParticleTypes.ASH, getX(), getY(), getZ(), count / 2,
                    radius * 0.5D, radius * 0.2D, radius * 0.5D, 0.02D);
        }
    }

    private void playBugSound() {
        if (this.tickCount % 2 != 0) {
            return;
        }
        this.level().playSound(null, getX(), getY(), getZ(),
                ModSounds.SOUND_BUGS.get(), SoundSource.BLOCKS,
                Mth.clamp(getBugCount() * 0.0015F, 0.08F, 0.8F),
                this.random.nextFloat() * 0.4F + 0.8F);
    }

    private void refundStoredChakra(LivingEntity owner) {
        if (this.storedChakra > 0.0D) {
            Chakra.pathway(owner).consume(-this.storedChakra, false);
            this.storedChakra = 0.0D;
        }
    }

    private Vec3 ownerVector(LivingEntity owner) {
        return owner.position().add(0.0D, owner.getBbHeight() * 0.6667D, 0.0D);
    }

    public boolean isOwnedBy(LivingEntity entity) {
        return entity.getUUID().equals(this.ownerUuid) || getOwner() == entity;
    }

    @Nullable
    public LivingEntity getOwner() {
        return resolveLiving(this.entityData.get(OWNER_ID), this.ownerUuid, true);
    }

    @Nullable
    public LivingEntity getTarget() {
        return resolveLiving(this.entityData.get(TARGET_ID), this.targetUuid, false);
    }

    @Nullable
    private LivingEntity resolveLiving(int entityId, @Nullable UUID uuid, boolean owner) {
        if (entityId >= 0) {
            Entity entity = this.level().getEntity(entityId);
            if (entity instanceof LivingEntity living) {
                return living;
            }
        }
        if (uuid != null && this.level() instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(uuid);
            if (entity instanceof LivingEntity living) {
                if (owner) {
                    setOwner(living);
                } else {
                    setTarget(living);
                }
                return living;
            }
        }
        return null;
    }

    private void setOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
    }

    private void setTarget(LivingEntity target) {
        this.targetUuid = target.getUUID();
        this.entityData.set(TARGET_ID, target.getId());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.FALL) || source.is(DamageTypes.FLY_INTO_WALL)) {
            return false;
        }
        if (!this.level().isClientSide && amount >= 1.0F) {
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.POOF, getX(), getY(), getZ(), 12,
                        getSwarmRadius() * 0.4D, getSwarmRadius() * 0.2D, getSwarmRadius() * 0.4D, 0.02D);
            }
            triggerReturn();
            return true;
        }
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        if (tag.hasUUID("Target")) {
            this.targetUuid = tag.getUUID("Target");
        }
        this.entityData.set(POWER, tag.contains("Power") ? tag.getFloat("Power") : 1.0F);
        this.entityData.set(RETURNING, tag.getBoolean("Returning"));
        this.storedChakra = tag.getDouble("StoredChakra");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        if (this.targetUuid != null) {
            tag.putUUID("Target", this.targetUuid);
        }
        tag.putFloat("Power", getPower());
        tag.putBoolean("Returning", isReturning());
        tag.putDouble("StoredChakra", this.storedChakra);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
