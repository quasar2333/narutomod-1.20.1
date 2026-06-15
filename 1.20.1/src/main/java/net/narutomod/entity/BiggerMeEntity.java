package net.narutomod.entity;

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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class BiggerMeEntity extends PathfinderMob {
    private static final int GROW_TICKS = 40;
    private static final int UNRIDDEN_GRACE_TICKS = 5;
    private static final float MIN_SCALE = 1.0F;
    private static final float MAX_SCALE = 10.0F;
    private static final double BASE_ATTACK_DAMAGE = 3.0D;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(BiggerMeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> TARGET_SCALE = SynchedEntityData.defineId(BiggerMeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> CURRENT_SCALE = SynchedEntityData.defineId(BiggerMeEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUuid;
    private float dimensionsScale = MIN_SCALE;
    private int attackCooldown;

    public BiggerMeEntity(EntityType<? extends BiggerMeEntity> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.setNoAi(true);
        this.xpReward = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setDropChance(slot, 0.0F);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, BASE_ATTACK_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.08D);
    }

    public static boolean spawnFrom(Player owner, float targetScale) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        BiggerMeEntity entity = ModEntityTypes.BIGGERME.get().create(serverLevel);
        if (entity == null) {
            return false;
        }
        entity.configure(owner, targetScale);
        serverLevel.addFreshEntity(entity);
        owner.startRiding(entity, true);
        serverLevel.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                ModSounds.SOUND_JUTSU.get(), SoundSource.PLAYERS, 1.0F, 0.9F);
        return true;
    }

    public static boolean isOwnedBiggerMe(@Nullable Entity entity, Player owner) {
        return entity instanceof BiggerMeEntity biggerMe && biggerMe.isOwnedBy(owner);
    }

    private void configure(Player owner, float targetScale) {
        setOwner(owner);
        setCustomName(owner.getDisplayName());
        setLeftHanded(owner.getMainArm() == HumanoidArm.LEFT);
        setTargetScale(targetScale);
        setCurrentScale(MIN_SCALE);
        copyOwnerEquipment(owner);
        copyOwnerAttributes(owner, getTargetScale());
        addEffect(new MobEffectInstance(MobEffects.JUMP, 999999, Math.max((int)getTargetScale(), 0), false, false));
        moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
        setYBodyRot(owner.getYRot());
        setYHeadRot(owner.getYRot());
    }

    private void copyOwnerEquipment(LivingEntity owner) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = owner.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                setItemSlot(slot, stack.copy());
            }
        }
    }

    private void copyOwnerAttributes(LivingEntity owner, float targetScale) {
        setAttributeBaseValue(Attributes.ARMOR, ProcedureUtils.getArmorValue(owner));
        setAttributeBaseValue(Attributes.ATTACK_DAMAGE,
                Math.max(ProcedureUtils.getModifiedAttackDamage(owner), BASE_ATTACK_DAMAGE) + targetScale * targetScale);
        setAttributeBaseValue(Attributes.MAX_HEALTH, Math.max(owner.getMaxHealth() * Math.max(targetScale * 0.5F, 1.0F), 20.0D));
        setAttributeBaseValue(Attributes.MOVEMENT_SPEED, Math.max(ProcedureUtils.getModifiedSpeed(owner) * 0.5D, 0.08D));
        setHealth(getMaxHealth());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(TARGET_SCALE, MIN_SCALE);
        this.entityData.define(CURRENT_SCALE, MIN_SCALE);
    }

    @Override
    public void tick() {
        this.fallDistance = 0.0F;
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        LivingEntity owner = getOwner();
        if (CloneOwnerState.isUnavailable(owner)) {
            discardWithPoof();
            return;
        }
        updateGrowthScale();
        if (getControllingPassenger() instanceof Player rider && isOwnedBy(rider)) {
            tickControlled(rider);
        } else if (!isVehicle() && this.tickCount > UNRIDDEN_GRACE_TICKS) {
            discardWithPoof();
        } else {
            setDeltaMovement(getDeltaMovement().multiply(0.65D, 1.0D, 0.65D));
        }
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        LivingEntity owner = getOwner();
        target.invulnerableTime = 0;
        return target.hurt(ModDamageTypes.ninjutsu(this.level(), this, owner), (float)getAttributeValue(Attributes.ATTACK_DAMAGE));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && isOwnedBy(player)) {
            player.startRiding(this, true);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Player player && isOwnedBy(player) && getPassengers().isEmpty();
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof LivingEntity living ? living : null;
    }

    @Override
    public double getPassengersRidingOffset() {
        return Math.max(getBbHeight() - 1.85D, 0.0D);
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
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(this.dimensionsScale);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (CURRENT_SCALE.equals(key)) {
            this.dimensionsScale = getCurrentScale();
            refreshDimensions();
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker instanceof Player player && isOwnedBy(player)) {
            return false;
        }
        LivingEntity rider = getControllingPassenger();
        if (rider != null && rider.isAlive()) {
            rider.invulnerableTime = 0;
            return rider.hurt(source, amount);
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        LivingEntity owner = getOwner();
        if (CloneFamilyAlliances.hasSameOwner(entity, owner)) {
            return true;
        }
        return super.isAlliedTo(entity);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        setTargetScale(tag.getFloat("TargetScale"));
        setCurrentScale(tag.contains("CurrentScale") ? tag.getFloat("CurrentScale") : getTargetScale());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putFloat("TargetScale", getTargetScale());
        tag.putFloat("CurrentScale", getCurrentScale());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Nullable
    public LivingEntity getOwner() {
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

    public boolean isOwnedBy(Player player) {
        return this.ownerUuid != null && this.ownerUuid.equals(player.getUUID())
                || this.entityData.get(OWNER_ID) == player.getId();
    }

    public float getRenderScale(float partialTick) {
        return Mth.lerp(partialTick, this.dimensionsScale, getCurrentScale());
    }

    private void tickControlled(Player rider) {
        setYRot(rider.getYRot());
        setXRot(rider.getXRot());
        setYBodyRot(rider.getYRot());
        setYHeadRot(rider.getYHeadRot());
        yRotO = getYRot();
        xRotO = getXRot();
        rider.fallDistance = 0.0F;
        setMaxUpStep(Math.max(getBbHeight() / 3.0F, 0.6F));
        getNavigation().stop();
        Vec3 motion = getDeltaMovement();
        Vec3 acceleration = controlAcceleration(rider);
        setDeltaMovement(motion.x() * 0.65D + acceleration.x(), motion.y(), motion.z() * 0.65D + acceleration.z());
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().multiply(0.85D, 1.0D, 0.85D));
        attackLookTarget(rider);
    }

    private Vec3 controlAcceleration(Player rider) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, rider.getYRot());
        forward = new Vec3(forward.x(), 0.0D, forward.z());
        if (forward.lengthSqr() < 1.0E-8D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        forward = forward.normalize();
        Vec3 right = new Vec3(-forward.z(), 0.0D, forward.x());
        double acceleration = Math.max(getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.65D, 0.035D);
        return forward.scale(rider.zza * acceleration).add(right.scale(rider.xxa * acceleration));
    }

    private void attackLookTarget(Player rider) {
        if (!rider.swinging || this.attackCooldown > 0) {
            return;
        }
        HitResult hit = ProcedureUtils.objectEntityLookingAt(rider, getAttackReach(), 1.0D, true, false,
                target -> target instanceof LivingEntity living && living.isAlive() && target != rider && target != this && !isAlliedTo(target));
        if (hit instanceof EntityHitResult entityHit) {
            this.attackCooldown = 10;
            doHurtTarget(entityHit.getEntity());
        }
    }

    private double getAttackReach() {
        double scale = getCurrentScale();
        return Math.max(4.0D, Math.sqrt(4.0D * scale * scale + getBbHeight() * getBbHeight()));
    }

    private void updateGrowthScale() {
        float targetScale = getTargetScale();
        float progress = Mth.clamp((float)this.tickCount / (float)GROW_TICKS, 0.0F, 1.0F);
        setCurrentScale(Mth.lerp(progress, MIN_SCALE, targetScale));
    }

    private void discardWithPoof() {
        if (!this.level().isClientSide) {
            this.level().playSound(null, getX(), getY(), getZ(),
                    ModSounds.SOUND_POOF.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.POOF, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                        160, getBbWidth() * 0.25D, getBbHeight() * 0.2D, getBbWidth() * 0.25D, 0.08D);
            }
        }
        discard();
    }

    private void setOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
    }

    private void setTargetScale(float targetScale) {
        this.entityData.set(TARGET_SCALE, Mth.clamp(targetScale, MIN_SCALE, MAX_SCALE));
    }

    private float getTargetScale() {
        return this.entityData.get(TARGET_SCALE);
    }

    private void setCurrentScale(float scale) {
        this.dimensionsScale = Mth.clamp(scale, MIN_SCALE, MAX_SCALE);
        this.entityData.set(CURRENT_SCALE, this.dimensionsScale);
        refreshDimensions();
    }

    private float getCurrentScale() {
        return this.entityData.get(CURRENT_SCALE);
    }

    private void setAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }
}
