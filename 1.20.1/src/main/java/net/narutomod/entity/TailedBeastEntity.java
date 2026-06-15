package net.narutomod.entity;

import java.util.Locale;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
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
import net.minecraftforge.registries.RegistryObject;
import net.narutomod.NarutomodMod;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEntityTypes;

public final class TailedBeastEntity extends PathfinderMob implements RangedAttackMob {
    private static final double TARGET_RANGE = 108.0D;
    private static final int BIJUDAMA_COOLDOWN = 200;
    private static final int ATTACK_COOLDOWN_TICKS = 10;
    private static final double CONTROL_MOTION_LIMIT = 0.05D;
    private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(TailedBeastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(TailedBeastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SHOOTING = SynchedEntityData.defineId(TailedBeastEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CAN_STEER = SynchedEntityData.defineId(TailedBeastEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> KCM = SynchedEntityData.defineId(TailedBeastEntity.class, EntityDataSerializers.BOOLEAN);

    private final Variant variant;
    private final ServerBossEvent bossEvent = new ServerBossEvent(
            getDisplayName(),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.PROGRESS);

    @Nullable
    private UUID ownerUuid;
    private int tailBeastBallCooldown;
    private int attackCooldown;
    private int angerLevel;
    private int lifeSpan = Integer.MAX_VALUE - 1;

    public TailedBeastEntity(EntityType<? extends TailedBeastEntity> entityType, Level level, Variant variant) {
        super(entityType, level);
        this.variant = variant;
        this.xpReward = 12000;
        this.tailBeastBallCooldown = BIJUDAMA_COOLDOWN;
        this.setPersistenceRequired();
        this.setMaxUpStep(variant.height() / 3.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.ARMOR, 100.0D)
                .add(Attributes.ATTACK_DAMAGE, 200.0D)
                .add(Attributes.FOLLOW_RANGE, TARGET_RANGE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MAX_HEALTH, 10000.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.8D);
    }

    @Nullable
    public static TailedBeastEntity spawnFrom(LivingEntity owner, Variant variant, boolean mountOwner) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Vec3 pos = mountOwner
                ? owner.position()
                : owner.getEyePosition().add(owner.getLookAngle().scale(20.0D)).add(0.0D, 8.0D, 0.0D);
        TailedBeastEntity beast = spawnAt(serverLevel, variant, pos, owner.getYRot(), owner);
        if (beast != null && mountOwner && owner instanceof Player) {
            owner.startRiding(beast, true);
        }
        return beast;
    }

    @Nullable
    public static TailedBeastEntity spawnAt(ServerLevel level, Variant variant, Vec3 pos, float yaw, @Nullable LivingEntity owner) {
        TailedBeastEntity beast = variant.entityType().get().create(level);
        if (beast == null) {
            return null;
        }
        if (owner != null) {
            beast.setOwner(owner);
        }
        beast.moveTo(pos.x(), pos.y(), pos.z(), yaw, 0.0F);
        beast.setYBodyRot(yaw);
        beast.setYHeadRot(yaw);
        beast.setHealth(beast.getMaxHealth());
        if (!level.addFreshEntity(beast)) {
            return null;
        }
        BijuManager.saveSpawnedTailedBeast(beast);
        return beast;
    }

    public Variant getVariant() {
        return this.variant;
    }

    public int getTailCount() {
        return this.variant.tailCount();
    }

    public float getModelScale() {
        return this.variant.modelScale();
    }

    public boolean isShooting() {
        return this.entityData.get(SHOOTING);
    }

    public boolean isKcm() {
        return this.entityData.get(KCM);
    }

    public void setKcm(boolean kcm) {
        this.entityData.set(KCM, kcm && this.variant == Variant.NINE);
    }

    public void setAngerLevel(int angerLevel) {
        this.angerLevel = Mth.clamp(angerLevel, 0, 2);
    }

    public void setLifeSpan(int lifeSpan) {
        this.lifeSpan = lifeSpan;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(AGE, 0);
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(SHOOTING, false);
        this.entityData.define(CAN_STEER, false);
        this.entityData.define(KCM, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, true) {
            @Override
            public boolean canUse() {
                LivingEntity target = TailedBeastEntity.this.getTarget();
                return target != null
                        && TailedBeastEntity.this.distanceToSqr(target) <= TARGET_RANGE * TARGET_RANGE
                        && super.canUse();
            }
        });
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.5D, BIJUDAMA_COOLDOWN, (float)TARGET_RANGE) {
            @Override
            public boolean canUse() {
                LivingEntity target = TailedBeastEntity.this.getTarget();
                return target != null
                        && TailedBeastEntity.this.canShootBijudama()
                        && TailedBeastEntity.this.distanceToSqr(target) > 32.0D * 32.0D
                        && !TailedBeastEntity.this.isInWater()
                        && super.canUse();
            }

            @Override
            public void stop() {
                super.stop();
                TailedBeastEntity.this.setShooting(false);
            }
        });
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true, false) {
            @Override
            public boolean canUse() {
                return TailedBeastEntity.this.angerLevel > 0 && super.canUse();
            }

            @Override
            protected double getFollowDistance() {
                return TARGET_RANGE;
            }
        });
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, true, false) {
            @Override
            public boolean canUse() {
                return TailedBeastEntity.this.angerLevel > 1 && super.canUse();
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
        if (getTarget() != null && !getTarget().isAlive()) {
            setTarget(null);
        }
        LivingEntity rider = getControllingPassenger();
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
        if (this.tickCount % 100 == 0 && getHealth() > 0.0F && getHealth() < getMaxHealth()) {
            heal(100.0F);
        }
        if (this.tickCount % 100 == 0) {
            BijuManager.saveSpawnedTailedBeast(this);
        }
        this.bossEvent.setProgress(Mth.clamp(getHealth() / getMaxHealth(), 0.0F, 1.0F));
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && reason.shouldDestroy()) {
            BijuManager.clearSpawnedTailedBeast(this);
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
            this.tailBeastBallCooldown = BIJUDAMA_COOLDOWN;
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && canRideTailedBeast(player)) {
            player.startRiding(this, true);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && canRideTailedBeast(passenger);
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
            this.entityData.set(CAN_STEER, canRideTailedBeast(passenger));
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
        return this.variant.riderOffset();
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

    @Override
    public boolean isNoGravity() {
        return false;
    }

    public boolean isNonBoss() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 192.0D * getViewScale();
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

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 50.0F;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(AGE, tag.getInt("age"));
        this.angerLevel = tag.contains("AngerLevel") ? tag.getInt("AngerLevel") : this.angerLevel;
        this.tailBeastBallCooldown = tag.contains("BijudamaCooldown") ? tag.getInt("BijudamaCooldown") : this.tailBeastBallCooldown;
        this.lifeSpan = tag.contains("LifeSpan") ? tag.getInt("LifeSpan") : this.lifeSpan;
        this.entityData.set(KCM, tag.getBoolean("KCM") && this.variant == Variant.NINE);
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        setMaxUpStep(this.variant.height() / 3.0F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("age", this.entityData.get(AGE));
        tag.putInt("AngerLevel", this.angerLevel);
        tag.putInt("BijudamaCooldown", this.tailBeastBallCooldown);
        tag.putInt("LifeSpan", this.lifeSpan);
        tag.putBoolean("KCM", isKcm());
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
    }

    private boolean canShootBijudama() {
        return getHealth() >= getMaxHealth() * 0.4F;
    }

    private boolean canRideTailedBeast(Entity entity) {
        return entity instanceof Player player && (player.isCreative() || entity == getOwner()
                || this.ownerUuid != null && this.ownerUuid.equals(entity.getUUID())
                || BijuManager.isJinchurikiOf(player, getTailCount()));
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

    public enum Variant {
        ONE(1, "one_tail", 20.0F, 8.0F, 22.0F, "onetail", 22.35D),
        TWO(2, "two_tails", 10.0F, 10.0F, 16.0F, "twotails", 16.35D),
        THREE(3, "three_tails", 20.0F, 10.0F, 14.0F, "threetails", 14.35D),
        FOUR(4, "four_tails", 20.0F, 8.0F, 20.0F, "fourtails", 20.35D),
        FIVE(5, "five_tails", 20.0F, 10.0F, 14.0F, "fivetails", 14.35D),
        SIX(6, "six_tails", 22.0F, 6.6F, 19.8F, "sixtails", 20.15D),
        SEVEN(7, "seven_tails", 14.0F, 4.2F, 22.4F, "seventails", 22.75D),
        EIGHT(8, "eight_tails", 10.0F, 6.0F, 20.0F, "eighttails", 12.5D),
        NINE(9, "nine_tails", 10.0F, 6.0F, 21.0F, "ninetails", 15.625D);

        private final int tailCount;
        private final String registryName;
        private final float modelScale;
        private final float width;
        private final float height;
        private final ResourceLocation texture;
        private final double riderOffset;

        Variant(int tailCount, String registryName, float modelScale, float width, float height, String textureName, double riderOffset) {
            this.tailCount = tailCount;
            this.registryName = registryName;
            this.modelScale = modelScale;
            this.width = width;
            this.height = height;
            this.texture = NarutomodMod.location("textures/" + textureName + ".png");
            this.riderOffset = riderOffset;
        }

        public int tailCount() {
            return this.tailCount;
        }

        public String registryName() {
            return this.registryName;
        }

        public float modelScale() {
            return this.modelScale;
        }

        public float width() {
            return this.width;
        }

        public float height() {
            return this.height;
        }

        public ResourceLocation texture() {
            return this.texture;
        }

        public ResourceLocation kcmTexture() {
            return NarutomodMod.location("textures/ninetailskcm.png");
        }

        public double riderOffset() {
            return this.riderOffset;
        }

        public RegistryObject<EntityType<TailedBeastEntity>> entityType() {
            return switch (this) {
                case ONE -> ModEntityTypes.ONE_TAIL;
                case TWO -> ModEntityTypes.TWO_TAILS;
                case THREE -> ModEntityTypes.THREE_TAILS;
                case FOUR -> ModEntityTypes.FOUR_TAILS;
                case FIVE -> ModEntityTypes.FIVE_TAILS;
                case SIX -> ModEntityTypes.SIX_TAILS;
                case SEVEN -> ModEntityTypes.SEVEN_TAILS;
                case EIGHT -> ModEntityTypes.EIGHT_TAILS;
                case NINE -> ModEntityTypes.NINE_TAILS;
            };
        }

        public static Variant byTailCount(int tailCount) {
            for (Variant variant : values()) {
                if (variant.tailCount == tailCount) {
                    return variant;
                }
            }
            throw new IllegalArgumentException("Unknown tailed beast count: " + tailCount);
        }

        public static Variant byName(String name) {
            String normalized = name.toLowerCase(Locale.ROOT);
            for (Variant variant : values()) {
                if (variant.registryName.equals(normalized) || variant.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                    return variant;
                }
            }
            throw new IllegalArgumentException("Unknown tailed beast variant: " + name);
        }
    }
}
