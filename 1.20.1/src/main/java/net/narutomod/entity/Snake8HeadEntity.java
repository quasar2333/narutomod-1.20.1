package net.narutomod.entity;

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
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class Snake8HeadEntity extends PathfinderMob {
    public static final float WIDTH = 2.4F;
    public static final float HEIGHT = 2.4F;
    public static final float MODEL_SCALE = 4.0F;
    private static final int LAUNCH_TICKS = 20;
    private static final int MAX_LIFE_TICKS = 120;
    private static final double TRACK_SPEED = 1.15D;
    private static final float ATTACK_DAMAGE = 100.0F;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(Snake8HeadEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(Snake8HeadEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private UUID targetUuid;

    public Snake8HeadEntity(EntityType<? extends Snake8HeadEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 0;
        setNoGravity(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, TRACK_SPEED);
    }

    @Nullable
    public static Snake8HeadEntity spawnFrom(LivingEntity owner, @Nullable LivingEntity target) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Snake8HeadEntity entity = ModEntityTypes.SNAKE_8_HEAD1.get().create(serverLevel);
        if (entity == null) {
            return null;
        }
        entity.configure(owner, target);
        serverLevel.addFreshEntity(entity);
        return entity;
    }

    private void configure(LivingEntity owner, @Nullable LivingEntity target) {
        setOwner(owner);
        setTargetEntity(target);
        setAttributeBaseValue(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE);
        setHealth(getMaxHealth());

        Entity origin = owner.getVehicle() instanceof Snake8HeadsEntity snake ? snake : owner;
        Vec3 forward = Vec3.directionFromRotation(0.0F, origin.getYRot()).normalize();
        Vec3 point = origin.position().add(forward.scale(2.0D)).add(0.0D, origin instanceof Snake8HeadsEntity ? -2.0D : 0.5D, 0.0D);
        moveTo(point.x(), point.y(), point.z(), origin.getYRot(), -45.0F);
        setYBodyRot(origin.getYRot());
        setYHeadRot(origin.getYRot());
        setDeltaMovement(forward.scale(0.16D).add(0.0D, 0.72D, 0.0D));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(TARGET_ID, -1);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (this.tickCount == 1) {
            this.level().playSound(null, getX(), getY(), getZ(),
                    ModSounds.SOUND_WOODGROW.get(), SoundSource.NEUTRAL, 2.0F, 1.0F);
        }
        if (this.tickCount > MAX_LIFE_TICKS || getOwner() == null) {
            discard();
            return;
        }
        this.noPhysics = this.tickCount <= LAUNCH_TICKS;
        if (this.tickCount > LAUNCH_TICKS) {
            LivingEntity target = getTargetEntity();
            if (target == null || !target.isAlive()) {
                discard();
                return;
            }
            Vec3 toTarget = target.getEyePosition().subtract(getBoundingBox().getCenter());
            if (toTarget.lengthSqr() > 1.0E-6D) {
                setDeltaMovement(toTarget.normalize().scale(TRACK_SPEED));
                faceMotion(getDeltaMovement());
            }
        }
        move(MoverType.SELF, getDeltaMovement());
        hitFirstTarget();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker != null && isOwnedBy(attacker)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        return isOwnedBy(entity)
                || entity instanceof Snake8HeadsEntity snake && this.ownerUuid != null && snake.isOwnedByUuid(this.ownerUuid)
                || super.isAlliedTo(entity);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        if (tag.hasUUID("Target")) {
            this.targetUuid = tag.getUUID("Target");
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        if (this.targetUuid != null) {
            tag.putUUID("Target", this.targetUuid);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public boolean isOwnedByUuid(UUID uuid) {
        return this.ownerUuid != null && this.ownerUuid.equals(uuid);
    }

    public boolean isOwnedBy(Entity entity) {
        return this.ownerUuid != null && this.ownerUuid.equals(entity.getUUID())
                || this.entityData.get(OWNER_ID) == entity.getId();
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

    @Nullable
    private LivingEntity getTargetEntity() {
        int targetId = this.entityData.get(TARGET_ID);
        if (targetId >= 0 && this.level().getEntity(targetId) instanceof LivingEntity living) {
            return living;
        }
        if (this.targetUuid != null && this.level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(this.targetUuid) instanceof LivingEntity living) {
            setTargetEntity(living);
            return living;
        }
        return null;
    }

    private void hitFirstTarget() {
        LivingEntity owner = getOwner();
        AABB area = getBoundingBox().inflate(0.9D);
        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, area, target -> canHit(owner, target))) {
            target.invulnerableTime = 0;
            target.hurt(ModDamageTypes.ninjutsu(this.level(), this, owner), ATTACK_DAMAGE);
            discard();
            return;
        }
    }

    private boolean canHit(@Nullable LivingEntity owner, Entity target) {
        return target.isAlive()
                && target != this
                && target != owner
                && target.getRootVehicle() != (owner == null ? null : owner.getRootVehicle())
                && !isOwnedBy(target);
    }

    private void setOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
    }

    private void setTargetEntity(@Nullable LivingEntity target) {
        this.targetUuid = target == null ? null : target.getUUID();
        this.entityData.set(TARGET_ID, target == null ? -1 : target.getId());
    }

    private void faceMotion(Vec3 motion) {
        if (motion.lengthSqr() <= 1.0E-8D) {
            return;
        }
        double horizontal = Math.sqrt(motion.x() * motion.x() + motion.z() * motion.z());
        setYRot((float)(Mth.atan2(motion.x(), motion.z()) * Mth.RAD_TO_DEG) * -1.0F);
        setXRot((float)(Mth.atan2(motion.y(), horizontal) * Mth.RAD_TO_DEG) * -1.0F);
        setYBodyRot(getYRot());
        setYHeadRot(getYRot());
    }

    private void setAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }
}
