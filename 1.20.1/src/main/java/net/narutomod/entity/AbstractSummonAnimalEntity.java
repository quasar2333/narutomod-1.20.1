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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.event.SpecialEvent;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModSounds;

public abstract class AbstractSummonAnimalEntity extends PathfinderMob {
    private static final int SPAWN_DELAY_TICKS = 20;
    private static final int DEFAULT_LIFE_SPAN = 1200;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(AbstractSummonAnimalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(AbstractSummonAnimalEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> SUMMON_AGE = SynchedEntityData.defineId(AbstractSummonAnimalEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private BlockPos commandedNavigationTarget;
    private float dimensionsScale = 1.0F;
    private int lifeSpan = DEFAULT_LIFE_SPAN;
    private int commandedNavigationTicks;

    protected AbstractSummonAnimalEntity(EntityType<? extends AbstractSummonAnimalEntity> entityType, Level level) {
        super(entityType, level);
        setPersistenceRequired();
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createSummonAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.ARMOR, 5.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D);
    }

    public final void configure(LivingEntity owner, float scale) {
        setOwner(owner);
        setSummonScale(scale);
        moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), 0.0F);
        setYBodyRot(owner.getYRot());
        setYHeadRot(owner.getYRot());
        setNoAi(true);
    }

    protected static boolean scheduleDelayedSpawn(ServerLevel level, AbstractSummonAnimalEntity entity) {
        entity.entityData.set(SUMMON_AGE, 1);
        entity.setNoAi(false);
        long start = level.getServer().overworld().getGameTime() + SPAWN_DELAY_TICKS;
        return SpecialEvent.setDelayedSpawnEvent(level, entity, 0, 0, 0, start);
    }

    public final float getSummonScale() {
        return this.entityData.get(SCALE);
    }

    public final int getSummonAge() {
        return this.entityData.get(SUMMON_AGE);
    }

    public boolean isVisibleSummon() {
        return getSummonAge() > 0;
    }

    public final boolean commandNavigationTo(BlockPos target) {
        this.commandedNavigationTarget = target.immutable();
        this.commandedNavigationTicks = 600;
        setTarget(null);
        getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 1.0D);
        return true;
    }

    public final boolean hasCommandedNavigationTarget() {
        return this.commandedNavigationTarget != null;
    }

    public abstract double baseRenderWidth();

    public abstract double baseRenderHeight();

    public abstract double baseRenderDepth();

    protected abstract void applyScaledAttributes(float scale);

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D, 0.001F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(SCALE, 1.0F);
        this.entityData.define(SUMMON_AGE, 0);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SCALE.equals(key) && this.level().isClientSide) {
            this.dimensionsScale = getSummonScale();
            refreshDimensions();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (this.tickCount < SPAWN_DELAY_TICKS) {
            return;
        }
        if (this.tickCount == SPAWN_DELAY_TICKS) {
            setNoAi(false);
            this.entityData.set(SUMMON_AGE, 1);
            if (getSummonScale() >= 4.0F && getOwner() instanceof Player owner && !owner.isPassenger()) {
                owner.startRiding(this, true);
            }
            return;
        }

        this.entityData.set(SUMMON_AGE, getSummonAge() + 1);
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive() || getSummonAge() > this.lifeSpan) {
            discardWithPoof();
            return;
        }
        if (getControllingPassenger() instanceof Player rider && isOwnedBy(rider)) {
            setTarget(null);
            clearCommandedNavigation();
            getNavigation().stop();
            return;
        }
        if (tickCommandedNavigation()) {
            return;
        }
        if (this.tickCount % 10 == 0) {
            copyOwnerTarget(owner);
            followOwner(owner);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        LivingEntity owner = getOwner();
        target.invulnerableTime = 0;
        return target.hurt(ModDamageTypes.ninjutsu(this.level(), this, owner), (float)getAttributeValue(Attributes.ATTACK_DAMAGE));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.FALL)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && isOwnedBy(player) && getSummonScale() >= 4.0F) {
            player.startRiding(this, true);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getSummonScale() >= 4.0F
                && passenger instanceof Player player
                && isOwnedBy(player)
                && getPassengers().isEmpty();
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof LivingEntity living ? living : null;
    }

    @Override
    public double getPassengersRidingOffset() {
        return getBbHeight();
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (!hasPassenger(passenger)) {
            return;
        }
        moveFunction.accept(passenger,
                getX(),
                getY() + getPassengersRidingOffset() + passenger.getMyRidingOffset(),
                getZ());
    }

    @Override
    public boolean canBeCollidedWith() {
        return isVisibleSummon() && super.canBeCollidedWith();
    }

    @Override
    public boolean isPushable() {
        return getSummonScale() < 4.0F && super.isPushable();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(this.dimensionsScale);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        if (entity == getOwner()) {
            return true;
        }
        return entity instanceof AbstractSummonAnimalEntity summon
                && summon.ownerUuid != null
                && summon.ownerUuid.equals(this.ownerUuid)
                || super.isAlliedTo(entity);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        this.lifeSpan = tag.contains("LifeSpan") ? tag.getInt("LifeSpan") : DEFAULT_LIFE_SPAN;
        setSummonScale(tag.contains("Scale") ? tag.getFloat("Scale") : 1.0F);
        this.entityData.set(SUMMON_AGE, tag.getInt("SummonAge"));
        if (tag.contains("CommandedNavigationTarget")) {
            CompoundTag targetTag = tag.getCompound("CommandedNavigationTarget");
            this.commandedNavigationTarget = new BlockPos(targetTag.getInt("X"), targetTag.getInt("Y"), targetTag.getInt("Z"));
            this.commandedNavigationTicks = tag.getInt("CommandedNavigationTicks");
        } else {
            this.commandedNavigationTarget = null;
            this.commandedNavigationTicks = 0;
        }
        if (getSummonAge() <= 0) {
            setNoAi(true);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putInt("LifeSpan", this.lifeSpan);
        tag.putFloat("Scale", getSummonScale());
        tag.putInt("SummonAge", getSummonAge());
        if (this.commandedNavigationTarget != null) {
            CompoundTag targetTag = new CompoundTag();
            targetTag.putInt("X", this.commandedNavigationTarget.getX());
            targetTag.putInt("Y", this.commandedNavigationTarget.getY());
            targetTag.putInt("Z", this.commandedNavigationTarget.getZ());
            tag.put("CommandedNavigationTarget", targetTag);
            tag.putInt("CommandedNavigationTicks", this.commandedNavigationTicks);
        }
    }

    @Nullable
    public final LivingEntity getOwner() {
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

    protected final boolean isOwnedBy(LivingEntity entity) {
        return this.ownerUuid != null && this.ownerUuid.equals(entity.getUUID()) || getOwner() == entity;
    }

    protected final boolean canMountedRiderControl(LivingEntity rider) {
        LivingEntity owner = getOwner();
        return owner != null && (rider.equals(owner) || rider.isAlliedTo(owner) || owner.isAlliedTo(rider));
    }

    protected final void setAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private void setOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
    }

    private void setSummonScale(float scale) {
        this.dimensionsScale = Mth.clamp(scale, 0.1F, 50.0F);
        this.entityData.set(SCALE, this.dimensionsScale);
        applyScaledAttributes(this.dimensionsScale);
        refreshDimensions();
        setHealth(getMaxHealth());
        setMaxUpStep(Math.max(getBbHeight() * 0.25F, 0.6F));
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

    private boolean tickCommandedNavigation() {
        if (this.commandedNavigationTarget == null) {
            return false;
        }
        if (this.commandedNavigationTicks-- <= 0) {
            clearCommandedNavigation();
            return false;
        }
        setTarget(null);
        double targetX = this.commandedNavigationTarget.getX() + 0.5D;
        double targetY = this.commandedNavigationTarget.getY();
        double targetZ = this.commandedNavigationTarget.getZ() + 0.5D;
        double reach = Math.max(1.5D, getBbWidth() * 0.5D + 0.5D);
        if (distanceToSqr(targetX, targetY, targetZ) <= reach * reach) {
            clearCommandedNavigation();
            return false;
        }
        if (this.tickCount % 10 == 0 || getNavigation().isDone()) {
            getNavigation().moveTo(targetX, targetY, targetZ, 1.0D);
        }
        return true;
    }

    private void clearCommandedNavigation() {
        this.commandedNavigationTarget = null;
        this.commandedNavigationTicks = 0;
        getNavigation().stop();
    }

    private void followOwner(LivingEntity owner) {
        if (isVehicle() || getTarget() != null) {
            return;
        }
        double distanceSqr = distanceToSqr(owner);
        if (distanceSqr > 400.0D) {
            moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), 0.0F);
            setDeltaMovement(0.0D, 0.0D, 0.0D);
            return;
        }
        if (distanceSqr > 16.0D) {
            this.getNavigation().moveTo(owner, 1.0D);
        }
    }

    private void discardWithPoof() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, getX(), getY(), getZ(), ModSounds.SOUND_POOF.get(), SoundSource.NEUTRAL, 2.0F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.POOF, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                    300, getBbWidth() * 0.5D, getBbHeight() * 0.3D, getBbWidth() * 0.5D, 0.08D);
        }
        discard();
    }
}
