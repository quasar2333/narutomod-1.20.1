package net.narutomod.entity;

import java.util.UUID;
import javax.annotation.Nullable;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class WoodArmEntity extends Entity {
    private static final double RANGE = 30.0D;
    private static final int LIFETIME_TICKS = 200;
    private static final int GROW_TICKS = 20;
    private static final int HOLD_TICKS = LIFETIME_TICKS * 4 / 5;
    private static final float DAMAGE = 4.0F;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(WoodArmEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(WoodArmEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> REACHED_TARGET = SynchedEntityData.defineId(WoodArmEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private UUID targetUuid;
    private boolean damagedTarget;
    private double holdDistance;
    private double holdYOffset;
    private float holdYawOffset;

    public WoodArmEntity(EntityType<? extends WoodArmEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public static boolean spawnFrom(LivingEntity owner) {
        LivingEntity target = findTarget(owner);
        if (target == null) {
            return false;
        }
        WoodArmEntity woodArm = ModEntityTypes.WOOD_ARM.get().create(owner.level());
        if (woodArm == null) {
            return false;
        }
        woodArm.configure(owner, target);
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                ModSounds.SOUND_WOODGROW.get(), SoundSource.PLAYERS, 1.0F, owner.getRandom().nextFloat() * 0.4F + 0.6F);
        owner.level().addFreshEntity(woodArm);
        return true;
    }

    @Nullable
    public static LivingEntity findTarget(LivingEntity owner) {
        HitResult hit = ProcedureUtils.objectEntityLookingAt(owner, RANGE, 1.0D, true, false,
                target -> target instanceof LivingEntity living && living != owner && living.isAlive()
                        && !(target instanceof WoodArmEntity));
        return hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living ? living : null;
    }

    public void configure(LivingEntity owner, LivingEntity target) {
        setOwner(owner);
        setTarget(target);
        Vec3 base = armBase(owner);
        moveTo(base.x(), base.y(), base.z(), owner.getYRot(), owner.getXRot());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(TARGET_ID, -1);
        this.entityData.define(REACHED_TARGET, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        LivingEntity owner = getOwner();
        LivingEntity target = getTarget();
        if (owner == null || target == null || !owner.isAlive() || !target.isAlive() || this.tickCount >= LIFETIME_TICKS) {
            discard();
            return;
        }
        spawnWoodTrail(owner, target);
        if (!hasReachedTarget()) {
            updateGrowingArm(owner, target);
        } else if (this.tickCount < HOLD_TICKS) {
            holdTarget(owner, target);
        } else {
            discard();
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        if (tag.hasUUID("Target")) {
            this.targetUuid = tag.getUUID("Target");
        }
        this.damagedTarget = tag.getBoolean("DamagedTarget");
        this.holdDistance = tag.getDouble("HoldDistance");
        this.holdYOffset = tag.getDouble("HoldYOffset");
        this.holdYawOffset = tag.getFloat("HoldYawOffset");
        this.entityData.set(REACHED_TARGET, tag.getBoolean("ReachedTarget"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        if (this.targetUuid != null) {
            tag.putUUID("Target", this.targetUuid);
        }
        tag.putBoolean("DamagedTarget", this.damagedTarget);
        tag.putBoolean("ReachedTarget", hasReachedTarget());
        tag.putDouble("HoldDistance", this.holdDistance);
        tag.putDouble("HoldYOffset", this.holdYOffset);
        tag.putFloat("HoldYawOffset", this.holdYawOffset);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void updateGrowingArm(LivingEntity owner, LivingEntity target) {
        Vec3 base = armBase(owner);
        Vec3 targetCenter = target.getBoundingBox().getCenter();
        double progress = Math.min(this.tickCount / (double) GROW_TICKS, 1.0D);
        Vec3 tip = base.lerp(targetCenter, progress);
        moveTo(tip.x(), tip.y(), tip.z(), owner.getYRot(), owner.getXRot());
        if (progress >= 1.0D || this.getBoundingBox().inflate(0.75D).intersects(target.getBoundingBox())) {
            captureTarget(owner, target);
        }
    }

    private void captureTarget(LivingEntity owner, LivingEntity target) {
        Vec3 delta = target.position().subtract(owner.position());
        this.holdDistance = Math.max(Math.sqrt(delta.x() * delta.x() + delta.z() * delta.z()), 0.25D);
        this.holdYOffset = delta.y();
        this.holdYawOffset = (float) (yawFromHorizontal(delta.x(), delta.z()) - owner.getYRot());
        this.entityData.set(REACHED_TARGET, true);
        if (!this.damagedTarget) {
            target.invulnerableTime = 0;
            target.hurt(ModDamageTypes.ninjutsu(this.level(), this, owner), DAMAGE);
            this.damagedTarget = true;
        }
        holdTarget(owner, target);
    }

    private void holdTarget(LivingEntity owner, LivingEntity target) {
        double yaw = Math.toRadians(owner.getYRot() + this.holdYawOffset);
        double x = owner.getX() - Math.sin(yaw) * this.holdDistance;
        double z = owner.getZ() + Math.cos(yaw) * this.holdDistance;
        double y = owner.getY() + this.holdYOffset;
        target.addEffect(new MobEffectInstance(ModEffects.PARALYSIS.get(), 2, 0, false, false));
        target.teleportTo(x, y, z);
        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
        moveTo(x, y + target.getBbHeight() * 0.5D, z, owner.getYRot(), owner.getXRot());
    }

    private void spawnWoodTrail(LivingEntity owner, LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 base = armBase(owner);
        Vec3 end = hasReachedTarget() ? target.getBoundingBox().getCenter() : position();
        Vec3 delta = end.subtract(base);
        int steps = Math.max(2, Math.min(9, (int) (delta.length() * 1.25D)));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = base.add(delta.scale(i / (double) steps));
            serverLevel.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LOG.defaultBlockState()),
                    point.x(),
                    point.y(),
                    point.z(),
                    2,
                    0.08D,
                    0.08D,
                    0.08D,
                    0.02D);
        }
    }

    private boolean hasReachedTarget() {
        return this.entityData.get(REACHED_TARGET);
    }

    public boolean hasReachedTargetForRender() {
        return hasReachedTarget();
    }

    @Nullable
    public LivingEntity getOwnerForRender() {
        return getOwner();
    }

    @Nullable
    public LivingEntity getTargetForRender() {
        return getTarget();
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

    private void setTarget(LivingEntity target) {
        this.targetUuid = target.getUUID();
        this.entityData.set(TARGET_ID, target.getId());
    }

    @Nullable
    private LivingEntity getTarget() {
        int targetId = this.entityData.get(TARGET_ID);
        if (targetId >= 0 && this.level().getEntity(targetId) instanceof LivingEntity living) {
            return living;
        }
        if (this.targetUuid != null && this.level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(this.targetUuid) instanceof LivingEntity living) {
            setTarget(living);
            return living;
        }
        return null;
    }

    private static Vec3 armBase(LivingEntity owner) {
        Vec3 look = owner.getLookAngle();
        Vec3 horizontal = new Vec3(look.x(), 0.0D, look.z());
        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = Vec3.directionFromRotation(0.0F, owner.getYRot());
        }
        horizontal = horizontal.normalize();
        Vec3 side = horizontal.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        return owner.position()
                .add(0.0D, Math.min(owner.getBbHeight() * 0.75D, 1.2D), 0.0D)
                .add(side.scale(0.4D))
                .add(horizontal.scale(0.35D));
    }

    public static Vec3 armBaseForRender(LivingEntity owner, float partialTick) {
        float yaw = Mth.rotLerp(partialTick, owner.yRotO, owner.getYRot());
        float pitch = Mth.lerp(partialTick, owner.xRotO, owner.getXRot());
        Vec3 look = Vec3.directionFromRotation(pitch, yaw);
        Vec3 horizontal = new Vec3(look.x(), 0.0D, look.z());
        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = Vec3.directionFromRotation(0.0F, yaw);
        }
        horizontal = horizontal.normalize();
        Vec3 side = horizontal.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        Vec3 ownerPos = new Vec3(
            Mth.lerp(partialTick, owner.xOld, owner.getX()),
            Mth.lerp(partialTick, owner.yOld, owner.getY()),
            Mth.lerp(partialTick, owner.zOld, owner.getZ())
        );
        return ownerPos
                .add(0.0D, Math.min(owner.getBbHeight() * 0.75D, 1.2D), 0.0D)
                .add(side.scale(0.4D))
                .add(horizontal.scale(0.35D));
    }

    private static double yawFromHorizontal(double x, double z) {
        return -Math.atan2(x, z) * (180.0D / Math.PI);
    }
}
