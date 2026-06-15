package net.narutomod.entity;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.Chakra;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class SealingChainsEntity extends Entity {
    private static final double RANGE = 50.0D;
    private static final double BASE_TARGET_CHAKRA_DRAIN_PER_SECOND = 10.0D;
    private static final int RETRACT_TICKS = 20;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(SealingChainsEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(SealingChainsEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> TARGET_OFFSET_X = SynchedEntityData.defineId(SealingChainsEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TARGET_OFFSET_Y = SynchedEntityData.defineId(SealingChainsEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TARGET_OFFSET_Z = SynchedEntityData.defineId(SealingChainsEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> RETRACT_TIME = SynchedEntityData.defineId(SealingChainsEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private UUID targetUuid;
    private double initialDistance = 1.0D;
    private int slowAmplifier = 1;

    public SealingChainsEntity(EntityType<? extends SealingChainsEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.fireImmune();
    }

    public static boolean spawnFrom(LivingEntity owner, LivingEntity target) {
        if (!(owner.level() instanceof ServerLevel serverLevel) || !canBind(owner, target)) {
            return false;
        }
        SealingChainsEntity entity = ModEntityTypes.SEALING_CHAINS.get().create(serverLevel);
        if (entity == null) {
            return false;
        }
        entity.configure(owner, target);
        serverLevel.addFreshEntity(entity);
        return true;
    }

    @Nullable
    public static LivingEntity findTarget(LivingEntity owner) {
        HitResult hit = ProcedureUtils.objectEntityLookingAt(owner, RANGE, 0.0D, true, false,
                target -> target instanceof LivingEntity living && canBind(owner, living));
        return hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living ? living : null;
    }

    public static int retractOwnedNear(LivingEntity owner, double radius) {
        List<SealingChainsEntity> chains = owner.level().getEntitiesOfClass(
                SealingChainsEntity.class,
                owner.getBoundingBox().inflate(radius),
                chain -> chain.isOwnedBy(owner));
        for (SealingChainsEntity chain : chains) {
            chain.retract();
        }
        return chains.size();
    }

    private static boolean canBind(LivingEntity owner, LivingEntity target) {
        return target != owner && target.isAlive();
    }

    private void configure(LivingEntity owner, LivingEntity target) {
        setOwner(owner);
        setTarget(target);
        setTargetAttachVec(0.0D, Math.max(target.getEyeHeight() - 0.1D * target.getBbHeight(), 0.1D), 0.0D);
        this.initialDistance = Math.max(owner.distanceTo(target) - 1.0D, 1.0D);
        this.slowAmplifier = target.hasEffect(ModEffects.HEAVINESS.get())
                ? target.getEffect(ModEffects.HEAVINESS.get()).getAmplifier() + 1
                : 1;
        moveToOwner(owner);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(TARGET_ID, -1);
        this.entityData.define(TARGET_OFFSET_X, 0.0F);
        this.entityData.define(TARGET_OFFSET_Y, 0.0F);
        this.entityData.define(TARGET_OFFSET_Z, 0.0F);
        this.entityData.define(RETRACT_TIME, -1);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        LivingEntity owner = getOwner();
        LivingEntity target = getTarget();
        if (owner == null || !owner.isAlive() || target == null || !isTargetable(target)) {
            discard();
            return;
        }

        moveToOwner(owner);
        int retractTime = this.entityData.get(RETRACT_TIME);
        if (retractTime >= 0) {
            this.entityData.set(RETRACT_TIME, retractTime - 1);
            if (retractTime <= 0) {
                discard();
            }
            return;
        }

        if (this.tickCount % 10 == 0) {
            this.level().playSound(null, getX(), getY(), getZ(),
                    ModSounds.SOUND_CHAINS.get(), SoundSource.NEUTRAL, 1.0F, this.random.nextFloat() * 0.6F + 0.2F);
        }
        if (this.tickCount % 20 == 19) {
            target.addEffect(new MobEffectInstance(ModEffects.HEAVINESS.get(), 22, this.slowAmplifier, false, false));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 22, Math.min(this.slowAmplifier + 1, 4), false, false));
            Chakra.pathway(target).consume(BASE_TARGET_CHAKRA_DRAIN_PER_SECOND * Math.max(Chakra.getLevel(owner) * 0.02D, 0.1D));
        }

        double distance = owner.distanceTo(target);
        if (distance > this.initialDistance) {
            Vec3 pull = owner.position().subtract(target.position()).normalize().scale(0.2D * distance / this.initialDistance);
            target.setDeltaMovement(target.getDeltaMovement().add(pull));
            target.hurtMarked = true;
        }
    }

    private boolean isTargetable(@Nullable LivingEntity target) {
        return target != null
                && target.isAlive()
                && target.distanceTo(this) < this.initialDistance * 2.0D
                && getOwner() != target;
    }

    public void retract() {
        this.entityData.set(RETRACT_TIME, RETRACT_TICKS);
    }

    private void moveToOwner(LivingEntity owner) {
        moveTo(owner.getX(), owner.getY() + owner.getBbHeight() * 0.5D, owner.getZ(), owner.getYRot(), owner.getXRot());
    }

    public Vec3 getTargetAttachVec() {
        LivingEntity target = getTarget();
        if (target == null) {
            return position();
        }
        return target.position().add(
                this.entityData.get(TARGET_OFFSET_X),
                this.entityData.get(TARGET_OFFSET_Y),
                this.entityData.get(TARGET_OFFSET_Z));
    }

    public int getRetractTicks() {
        return this.entityData.get(RETRACT_TIME);
    }

    private void setTargetAttachVec(double x, double y, double z) {
        this.entityData.set(TARGET_OFFSET_X, (float)x);
        this.entityData.set(TARGET_OFFSET_Y, (float)y);
        this.entityData.set(TARGET_OFFSET_Z, (float)z);
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
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        if (tag.hasUUID("Target")) {
            this.targetUuid = tag.getUUID("Target");
        }
        this.initialDistance = tag.contains("InitialDistance") ? tag.getDouble("InitialDistance") : 1.0D;
        this.slowAmplifier = tag.contains("SlowAmplifier") ? tag.getInt("SlowAmplifier") : 1;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        if (this.targetUuid != null) {
            tag.putUUID("Target", this.targetUuid);
        }
        tag.putDouble("InitialDistance", this.initialDistance);
        tag.putInt("SlowAmplifier", this.slowAmplifier);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
