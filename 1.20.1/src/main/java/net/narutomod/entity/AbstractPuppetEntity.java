package net.narutomod.entity;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModSounds;

public abstract class AbstractPuppetEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(AbstractPuppetEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> REAL_AGE = SynchedEntityData.defineId(AbstractPuppetEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;

    protected AbstractPuppetEntity(EntityType<? extends AbstractPuppetEntity> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.setNoAi(true);
        this.xpReward = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.0F);
        }
    }

    protected static AttributeSupplier.Builder puppetAttributes(double maxHealth, double armor, double movementSpeed, double attackDamage) {
        return Mob.createMobAttributes()
                .add(Attributes.ARMOR, armor)
                .add(Attributes.ATTACK_DAMAGE, attackDamage)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.MAX_HEALTH, maxHealth)
                .add(Attributes.MOVEMENT_SPEED, movementSpeed);
    }

    public void bindTo(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
        this.setNoAi(false);
    }

    public void releaseOwner() {
        this.ownerUuid = null;
        this.entityData.set(OWNER_ID, -1);
        this.setTarget(null);
        this.getNavigation().stop();
        this.setNoAi(true);
    }

    public boolean isOwnedBy(LivingEntity entity) {
        return entity.getUUID().equals(this.ownerUuid) || getOwner() == entity;
    }

    public static int releaseOwnedNear(LivingEntity owner, double radius) {
        List<AbstractPuppetEntity> puppets = owner.level().getEntitiesOfClass(
                AbstractPuppetEntity.class,
                owner.getBoundingBox().inflate(radius),
                puppet -> puppet.isOwnedBy(owner));
        for (AbstractPuppetEntity puppet : puppets) {
            puppet.releaseOwner();
        }
        return puppets.size();
    }

    @Nullable
    public LivingEntity getOwner() {
        int ownerId = this.entityData.get(OWNER_ID);
        if (ownerId >= 0) {
            Entity entity = this.level().getEntity(ownerId);
            if (entity instanceof LivingEntity living) {
                return living;
            }
        }
        if (this.ownerUuid != null && this.level() instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(this.ownerUuid);
            if (entity instanceof LivingEntity living) {
                bindTo(living);
                return living;
            }
        }
        return null;
    }

    public int getAge() {
        return this.entityData.get(REAL_AGE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(REAL_AGE, 0);
    }

    @Override
    public void tick() {
        this.entityData.set(REAL_AGE, getAge() + 1);
        this.fallDistance = 0.0F;
        super.tick();
        if (this.level().isClientSide || this.isNoAi()) {
            return;
        }
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            releaseOwner();
            return;
        }
        if (this.tickCount % 10 == 0) {
            copyOwnerTarget(owner);
            followOwner(owner);
        }
        if (getTarget() != null && distanceToSqr(getTarget()) < 4.0D && this.tickCount % 10 == 0) {
            doHurtTarget(getTarget());
        }
        playWoodClick();
    }

    private void copyOwnerTarget(LivingEntity owner) {
        LivingEntity target = null;
        if (owner instanceof Mob mob && mob.getTarget() != null && !isAlliedTo(mob.getTarget())) {
            target = mob.getTarget();
        }
        if (target == null) {
            LivingEntity lastHurtBy = owner.getLastHurtByMob();
            if (lastHurtBy != null && lastHurtBy.isAlive() && !isAlliedTo(lastHurtBy)
                    && owner.tickCount - owner.getLastHurtByMobTimestamp() < 200) {
                target = lastHurtBy;
            }
        }
        if (target == null) {
            LivingEntity lastHurt = owner.getLastHurtMob();
            if (lastHurt != null && lastHurt.isAlive() && !isAlliedTo(lastHurt)
                    && owner.tickCount - owner.getLastHurtMobTimestamp() < 200) {
                target = lastHurt;
            }
        }
        if (target != null) {
            setTarget(target);
        } else if (getTarget() != null && !getTarget().isAlive()) {
            setTarget(null);
        }
    }

    private void followOwner(LivingEntity owner) {
        if (getTarget() != null) {
            return;
        }
        double distanceSqr = distanceToSqr(owner);
        if (distanceSqr > 225.0D) {
            moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (distanceSqr > 9.0D) {
            this.getNavigation().moveTo(owner, 1.0D);
        }
    }

    private void playWoodClick() {
        if (this.tickCount % 8 != 0 || ProcedureUtils.getVelocity(this) <= 0.01D) {
            return;
        }
        this.level().playSound(null, getX(), getY(), getZ(),
                ModSounds.SOUND_WOOD_CLICK.get(), SoundSource.NEUTRAL, 0.6F, this.random.nextFloat() * 0.6F + 0.6F);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.FALL)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        if (entity == getOwner()) {
            return true;
        }
        return entity instanceof AbstractPuppetEntity puppet
                && puppet.ownerUuid != null
                && puppet.ownerUuid.equals(this.ownerUuid)
                || super.isAlliedTo(entity);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        this.entityData.set(REAL_AGE, tag.getInt("Age"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putInt("Age", getAge());
    }
}
