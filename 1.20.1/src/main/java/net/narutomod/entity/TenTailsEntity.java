package net.narutomod.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.item.SixPathSenjutsuItem;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class TenTailsEntity extends PathfinderMob implements RangedAttackMob {
    public static final float MODEL_SCALE = 36.0F;
    public static final float WIDTH = MODEL_SCALE * 0.2F;
    public static final float HEIGHT = MODEL_SCALE * 0.72F;
    public static final double TARGET_RANGE = 108.0D;
    private static final int BIJUDAMA_COOLDOWN = 200;
    private static final int ATTACK_COOLDOWN_TICKS = 10;
    private static final double CONTROL_MOTION_LIMIT = 0.05D;
    private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(TenTailsEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(TenTailsEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SHOOTING = SynchedEntityData.defineId(TenTailsEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CAN_STEER = SynchedEntityData.defineId(TenTailsEntity.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            getDisplayName(),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.PROGRESS);

    @Nullable
    private UUID ownerUuid;
    private int tailBeastBallCooldown = BIJUDAMA_COOLDOWN;
    private int attackCooldown;
    private int angerLevel = 2;
    private int lifeSpan = Integer.MAX_VALUE - 1;
    private int shootingTicks;

    public TenTailsEntity(EntityType<? extends TenTailsEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 16000;
        setPersistenceRequired();
        setMaxUpStep(HEIGHT / 3.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.ARMOR, 100.0D)
                .add(Attributes.ATTACK_DAMAGE, 1000.0D)
                .add(Attributes.FOLLOW_RANGE, TARGET_RANGE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MAX_HEALTH, 200000.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.8D);
    }

    @Nullable
    public static TenTailsEntity spawnFrom(LivingEntity owner, boolean mountOwner) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Vec3 pos = mountOwner
                ? owner.position()
                : owner.getEyePosition().add(owner.getLookAngle().scale(20.0D)).add(0.0D, 10.0D, 0.0D);
        TenTailsEntity entity = spawnAt(serverLevel, pos, owner.getYRot(), owner);
        if (entity != null && mountOwner && owner instanceof Player) {
            owner.startRiding(entity, true);
        }
        return entity;
    }

    @Nullable
    public static TenTailsEntity spawnAt(ServerLevel level, Vec3 pos, float yaw, @Nullable LivingEntity owner) {
        TenTailsEntity entity = ModEntityTypes.TEN_TAILS.get().create(level);
        if (entity == null) {
            return null;
        }
        if (owner != null) {
            entity.setOwner(owner);
        }
        entity.moveTo(pos.x(), pos.y(), pos.z(), yaw, 0.0F);
        entity.setYBodyRot(yaw);
        entity.setYHeadRot(yaw);
        entity.setHealth(entity.getMaxHealth());
        if (!level.addFreshEntity(entity)) {
            return null;
        }
        BijuManager.saveSpawnedTenTails(entity);
        return entity;
    }

    public boolean isShooting() {
        return this.entityData.get(SHOOTING);
    }

    public void setAngerLevel(int angerLevel) {
        this.angerLevel = Mth.clamp(angerLevel, 0, 2);
    }

    public void setLifeSpan(int lifeSpan) {
        this.lifeSpan = lifeSpan;
    }

    public void readyTailBeastBall() {
        this.tailBeastBallCooldown = 0;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(AGE, 0);
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(SHOOTING, false);
        this.entityData.define(CAN_STEER, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, true) {
            @Override
            public boolean canUse() {
                LivingEntity target = TenTailsEntity.this.getTarget();
                return target != null
                        && TenTailsEntity.this.distanceToSqr(target) <= TARGET_RANGE * TARGET_RANGE
                        && super.canUse();
            }
        });
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.5D, BIJUDAMA_COOLDOWN, (float)TARGET_RANGE) {
            @Override
            public boolean canUse() {
                LivingEntity target = TenTailsEntity.this.getTarget();
                return target != null
                        && TenTailsEntity.this.canShootBijudama()
                        && TenTailsEntity.this.distanceToSqr(target) > 32.0D * 32.0D
                        && !TenTailsEntity.this.isInWater()
                        && super.canUse();
            }

            @Override
            public void stop() {
                super.stop();
                TenTailsEntity.this.setShooting(false);
            }
        });
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true, false) {
            @Override
            public boolean canUse() {
                return TenTailsEntity.this.angerLevel > 0 && super.canUse();
            }

            @Override
            protected double getFollowDistance() {
                return TARGET_RANGE;
            }
        });
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, true, false) {
            @Override
            public boolean canUse() {
                return TenTailsEntity.this.angerLevel > 1 && super.canUse();
            }

            @Override
            protected double getFollowDistance() {
                return TARGET_RANGE * 0.5D;
            }
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        this.entityData.set(AGE, this.entityData.get(AGE) + 1);
        if (this.entityData.get(AGE) > this.lifeSpan) {
            discard();
            return;
        }
        LivingEntity rider = getControllingPassenger();
        if (rider instanceof Player player && !player.isCreative() && !SixPathSenjutsuItem.hasRinneSharingan(player)) {
            discard();
            return;
        }
        if (getTarget() != null && !getTarget().isAlive()) {
            setTarget(null);
        }
        if (rider != null && this.entityData.get(CAN_STEER)) {
            tickControlled(rider);
            clampMotion(CONTROL_MOTION_LIMIT);
        }
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
        if (this.tailBeastBallCooldown > 0) {
            this.tailBeastBallCooldown--;
        }
        if (this.shootingTicks > 0 && --this.shootingTicks <= 0) {
            setShooting(false);
        }
        if (this.tickCount % 100 == 0 && getHealth() > 0.0F && getHealth() < getMaxHealth()) {
            heal(100.0F);
        }
        if (this.tickCount % 100 == 0) {
            BijuManager.saveSpawnedTenTails(this);
        }
        this.bossEvent.setProgress(Mth.clamp(getHealth() / getMaxHealth(), 0.0F, 1.0F));
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && reason.shouldDestroy()) {
            BijuManager.clearSpawnedTenTails(this);
        }
        super.remove(reason);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        target.invulnerableTime = 0;
        boolean hurt = target.hurt(ModDamageTypes.ninjutsu(this.level(), this, getOwner()), (float)getAttributeValue(Attributes.ATTACK_DAMAGE));
        if (hurt && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.explode(this, target.getX(), target.getY(), target.getZ(), 10.0F, Level.ExplosionInteraction.MOB);
            for (LivingEntity living : serverLevel.getEntitiesOfClass(LivingEntity.class,
                    target.getBoundingBox().inflate(5.0D), living -> living != this && living != target && !isAlliedTo(living))) {
                living.invulnerableTime = 0;
                living.hurt(ModDamageTypes.ninjutsu(serverLevel, this, getOwner()),
                        (float)getAttributeValue(Attributes.ATTACK_DAMAGE) * (getRandom().nextFloat() * 0.5F + 0.4F));
            }
        }
        return hurt;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        if (this.tailBeastBallCooldown <= 0 && canShootBijudama() && TailBeastBallEntity.spawnFrom(this, 14.0F, 1000.0F)) {
            setShooting(true);
            this.shootingTicks = 20;
            this.tailBeastBallCooldown = BIJUDAMA_COOLDOWN;
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && canRideTenTails(player)) {
            player.startRiding(this, true);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && canRideTenTails(passenger);
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof LivingEntity living ? living : null;
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (!this.level().isClientSide) {
            this.entityData.set(CAN_STEER, canRideTenTails(passenger));
            if (passenger instanceof LivingEntity) {
                setNoAi(true);
            }
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!this.level().isClientSide && getPassengers().isEmpty()) {
            this.entityData.set(CAN_STEER, false);
            setNoAi(false);
        }
    }

    @Override
    public double getPassengersRidingOffset() {
        return getBbHeight() + 0.35D - (getControllingPassenger() == getOwner() ? 3.0D : 0.0D);
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
    protected void doPush(Entity entity) {
        if (!this.level().isClientSide && entity instanceof LivingEntity living
                && living.isAlive()
                && entity.getRootVehicle() != getRootVehicle()
                && !isAlliedTo(entity)) {
            living.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 5, false, false), this);
        }
        super.doPush(entity);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        if (attacker != null && attacker == getControllingPassenger()) {
            return false;
        }
        if (direct != null && direct == getControllingPassenger()) {
            return false;
        }
        if (source.is(DamageTypes.CACTUS)
                || source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.FALL)
                || source.is(DamageTypes.LIGHTNING_BOLT)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        return entity == getOwner()
                || this.ownerUuid != null && this.ownerUuid.equals(entity.getUUID())
                || super.isAlliedTo(entity);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    public boolean isNonBoss() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 256.0D * getViewScale();
        return distance < range * range;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SOUND_MONSTERGROWL.get();
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected float getSoundVolume() {
        return 50.0F;
    }

    @Override
    public float getVoicePitch() {
        return 0.5F + this.random.nextFloat() * 0.4F;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(AGE, tag.getInt("age"));
        this.angerLevel = tag.contains("AngerLevel") ? tag.getInt("AngerLevel") : this.angerLevel;
        this.tailBeastBallCooldown = tag.contains("BijudamaCooldown") ? tag.getInt("BijudamaCooldown") : this.tailBeastBallCooldown;
        this.lifeSpan = tag.contains("LifeSpan") ? tag.getInt("LifeSpan") : this.lifeSpan;
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        setMaxUpStep(HEIGHT / 3.0F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("age", this.entityData.get(AGE));
        tag.putInt("AngerLevel", this.angerLevel);
        tag.putInt("BijudamaCooldown", this.tailBeastBallCooldown);
        tag.putInt("LifeSpan", this.lifeSpan);
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
    }

    private boolean canShootBijudama() {
        return getHealth() >= getMaxHealth() * 0.4F;
    }

    private boolean canRideTenTails(Entity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        boolean owner = entity == getOwner() || this.ownerUuid != null && this.ownerUuid.equals(entity.getUUID());
        return player.isCreative() || owner && SixPathSenjutsuItem.hasRinneSharingan(player);
    }

    private void tickControlled(LivingEntity rider) {
        setYRot(rider.getYRot());
        setXRot(rider.getXRot());
        setYBodyRot(rider.getYRot());
        setYHeadRot(rider.getYHeadRot());
        yRotO = getYRot();
        xRotO = getXRot();
        rider.fallDistance = 0.0F;
        rider.setShiftKeyDown(false);
        setMaxUpStep(Math.max(getBbHeight() / 3.0F, 0.6F));
        getNavigation().stop();
        Vec3 acceleration = controlAcceleration(rider);
        Vec3 motion = getDeltaMovement();
        setDeltaMovement(motion.x() * 0.65D + acceleration.x(), motion.y(), motion.z() * 0.65D + acceleration.z());
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().multiply(0.85D, 1.0D, 0.85D));
        attackLookTarget(rider);
    }

    private Vec3 controlAcceleration(LivingEntity rider) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, rider.getYRot());
        forward = new Vec3(forward.x(), 0.0D, forward.z());
        if (forward.lengthSqr() < 1.0E-8D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        forward = forward.normalize();
        Vec3 right = new Vec3(-forward.z(), 0.0D, forward.x());
        double acceleration = Math.max(getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.55D, 0.04D);
        return forward.scale(rider.zza * acceleration).add(right.scale(rider.xxa * acceleration));
    }

    private void attackLookTarget(LivingEntity rider) {
        if (!rider.swinging || this.attackCooldown > 0) {
            return;
        }
        HitResult hit = ProcedureUtils.objectEntityLookingAt(rider, Math.max(30.0D, getBbWidth() * 2.0D), 1.0D, true, false,
                target -> target instanceof LivingEntity living && living.isAlive() && target != rider && target != this && !isAlliedTo(target));
        if (hit instanceof EntityHitResult entityHit) {
            this.attackCooldown = ATTACK_COOLDOWN_TICKS;
            doHurtTarget(entityHit.getEntity());
        }
    }

    private void clampMotion(double limit) {
        Vec3 motion = getDeltaMovement();
        setDeltaMovement(
                Mth.clamp(motion.x(), -limit, limit),
                Mth.clamp(motion.y(), -limit, limit),
                Mth.clamp(motion.z(), -limit, limit));
    }

    private void setShooting(boolean shooting) {
        this.entityData.set(SHOOTING, shooting);
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

    protected void setAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }
}
