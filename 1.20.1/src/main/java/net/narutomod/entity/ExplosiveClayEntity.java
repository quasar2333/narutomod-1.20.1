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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class ExplosiveClayEntity extends PathfinderMob {
    private static final int DEFAULT_LIFE_TICKS = 600;
    private static final int C2_RIDDEN_LIFE_TICKS = 10000;
    private static final int C2_UNRIDDEN_LIFE_TICKS = 400;
    private static final int C3_GROW_TIME = 30;
    private static final int C3_FUSE_TIME = 100;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(ExplosiveClayEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFE_END_TICK = SynchedEntityData.defineId(ExplosiveClayEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> C3_ARMED = SynchedEntityData.defineId(ExplosiveClayEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID ownerUuid;
    private boolean cleanedUp;
    private boolean c3WarningStarted;

    public ExplosiveClayEntity(EntityType<? extends ExplosiveClayEntity> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.xpReward = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.0F);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.6D);
    }

    public static boolean spawnFrom(LivingEntity owner, float power) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        ClayTier tier = ClayTier.fromPower(power);
        ExplosiveClayEntity clay = tier.type().create(serverLevel);
        if (clay == null) {
            return false;
        }
        clay.configure(owner);
        serverLevel.addFreshEntity(clay);
        return true;
    }

    private void configure(LivingEntity owner) {
        setOwner(owner);
        setLifeTicks(defaultLifeTicks());
        applyVariantAttributes();
        Vec3 look = owner.getLookAngle();
        moveTo(owner.getX() + look.x(), owner.getY() + 1.0D, owner.getZ() + look.z(), owner.getYRot(), 0.0F);
        setYHeadRot(owner.getYRot());
        setTarget(owner instanceof Mob mob ? mob.getTarget() : owner.getLastHurtMob());
        if (tier() == ClayTier.C3) {
            setDeltaMovement(0.0D, 0.0D, 0.0D);
        }
    }

    private void applyVariantAttributes() {
        ClayTier tier = tier();
        setAttributeBaseValue(Attributes.MAX_HEALTH, tier.maxHealth());
        setAttributeBaseValue(Attributes.MOVEMENT_SPEED, tier.speed());
        setHealth(getMaxHealth());
    }

    private void setAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(LIFE_END_TICK, DEFAULT_LIFE_TICKS);
        this.entityData.define(C3_ARMED, false);
    }

    @Override
    public void tick() {
        this.fallDistance = 0.0F;
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            discardClay(false);
            return;
        }
        if (tier() == ClayTier.C3) {
            tickC3(owner);
        } else {
            tickFlyingClay(owner);
        }
        if (this.tickCount > getLifeEndTick()) {
            discardClay(false);
        }
    }

    private void tickFlyingClay(LivingEntity owner) {
        setNoGravity(true);
        if (tier() == ClayTier.C2 && !isVehicle() && remainingLifeTicks() > C2_UNRIDDEN_LIFE_TICKS) {
            setLifeTicks(C2_UNRIDDEN_LIFE_TICKS);
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || isAlliedTo(target)) {
            target = findOwnerTarget(owner);
            setTarget(target);
        }
        if (target != null && target.isAlive() && !isAlliedTo(target)) {
            moveToward(target.getEyePosition(), tier().speed());
            if (getBoundingBox().inflate(0.4D).intersects(target.getBoundingBox())) {
                detonateAt(target, tier().explosionPower(), tier().directDamage());
            }
            return;
        }
        if (getControllingPassenger() instanceof Player rider) {
            tickControlled(rider);
        } else {
            followOwner(owner);
        }
    }

    private void tickC3(LivingEntity owner) {
        if (this.tickCount <= C3_FUSE_TIME) {
            setNoGravity(true);
            setDeltaMovement(Vec3.ZERO);
        }
        if (!this.c3WarningStarted && this.tickCount > C3_GROW_TIME) {
            this.c3WarningStarted = true;
            this.level().playSound(null, getX(), getY(), getZ(),
                    ModSounds.SOUND_C3.get(), SoundSource.NEUTRAL, 50.0F, 1.0F);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.END_ROD, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                        60, 2.5D, 2.5D, 2.5D, 0.05D);
            }
        }
        if (!isC3Armed() && this.tickCount > C3_FUSE_TIME) {
            setC3Armed(true);
            setNoGravity(false);
            this.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                    ModSounds.SOUND_KATSU.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
        if (isC3Armed() && onGround()) {
            detonateC3(owner);
        }
    }

    @Nullable
    private LivingEntity findOwnerTarget(LivingEntity owner) {
        if (owner instanceof Mob mob && mob.getTarget() != null && !isAlliedTo(mob.getTarget())) {
            return mob.getTarget();
        }
        LivingEntity lastHurt = owner.getLastHurtMob();
        if (lastHurt != null && lastHurt.isAlive() && !isAlliedTo(lastHurt)
                && owner.tickCount - owner.getLastHurtMobTimestamp() < 400) {
            return lastHurt;
        }
        LivingEntity lastHurtBy = owner.getLastHurtByMob();
        if (lastHurtBy != null && lastHurtBy.isAlive() && !isAlliedTo(lastHurtBy)
                && owner.tickCount - owner.getLastHurtByMobTimestamp() < 400) {
            return lastHurtBy;
        }
        return null;
    }

    private void followOwner(LivingEntity owner) {
        double distanceSqr = distanceToSqr(owner);
        if (distanceSqr > 144.0D) {
            moveTo(owner.getX(), owner.getY() + 1.0D, owner.getZ(), owner.getYRot(), owner.getXRot());
            setDeltaMovement(Vec3.ZERO);
        } else if (distanceSqr > 9.0D) {
            moveToward(owner.getEyePosition(), tier().speed() * 0.75D);
        } else {
            setDeltaMovement(getDeltaMovement().scale(0.5D));
        }
    }

    private void moveToward(Vec3 target, double speed) {
        Vec3 delta = target.subtract(position());
        if (delta.lengthSqr() < 0.04D) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        Vec3 motion = delta.normalize().scale(speed);
        setDeltaMovement(motion);
        move(MoverType.SELF, motion);
        this.hasImpulse = true;
        setYRot(ProcedureUtils.getYawFromVec(motion));
        setYBodyRot(getYRot());
        setYHeadRot(getYRot());
    }

    private void tickControlled(Player rider) {
        setYRot(rider.getYRot());
        setXRot(rider.getXRot());
        yRotO = getYRot();
        xRotO = getXRot();
        rider.fallDistance = 0.0F;
        setDeltaMovement(getDeltaMovement().scale(0.9D).add(controlAcceleration(rider)));
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(0.96D));
    }

    private Vec3 controlAcceleration(Player rider) {
        float forwardInput = rider.zza == 0.0F && !onGround() ? 0.4F : rider.zza;
        Vec3 look = rider.getLookAngle();
        Vec3 forward = new Vec3(look.x(), 0.0D, look.z());
        if (forward.lengthSqr() <= 1.0E-8D) {
            forward = Vec3.directionFromRotation(0.0F, rider.getYRot());
            forward = new Vec3(forward.x(), 0.0D, forward.z());
        }
        if (forward.lengthSqr() <= 1.0E-8D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        forward = forward.normalize();
        Vec3 right = new Vec3(-forward.z(), 0.0D, forward.x());
        double up = forwardInput > 0.0F ? -rider.getXRot() / 45.0D : 0.0D;
        return forward.scale(forwardInput * 0.08D)
                .add(right.scale(rider.xxa * 0.06D))
                .add(0.0D, up * 0.08D, 0.0D);
    }

    private void detonateAt(Entity target, float explosionPower, float directDamage) {
        if (!(this.level() instanceof ServerLevel serverLevel) || this.cleanedUp) {
            return;
        }
        this.cleanedUp = true;
        LivingEntity owner = getOwner();
        target.hurt(ModDamageTypes.ninjutsu(serverLevel, this, owner), directDamage);
        serverLevel.explode(owner, target.getX(), target.getY(), target.getZ(), explosionPower, Level.ExplosionInteraction.MOB);
        discard();
    }

    private void detonateC3(LivingEntity owner) {
        if (!(this.level() instanceof ServerLevel serverLevel) || this.cleanedUp) {
            return;
        }
        this.cleanedUp = true;
        double y = getY() + 5.0D;
        serverLevel.explode(owner, getX(), y, getZ(), 30.0F, Level.ExplosionInteraction.MOB);
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), y, getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        for (LivingEntity living : serverLevel.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(30.0D))) {
            if (!isAlliedTo(living)) {
                living.invulnerableTime = 0;
                living.hurt(ModDamageTypes.ninjutsu(serverLevel, this, owner), 30.0F);
            }
        }
        discard();
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (tier() == ClayTier.C1 || tier() == ClayTier.C2) {
            detonateAt(target, tier().explosionPower(), tier().directDamage());
            return true;
        }
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypes.FALL)) {
            return false;
        }
        Entity attacker = source.getEntity();
        LivingEntity owner = getOwner();
        if (owner != null && attacker == owner) {
            discardClay(true);
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        discardClay(true);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (tier() == ClayTier.C2 && !this.level().isClientSide) {
            player.startRiding(this, true);
            setLifeTicks(C2_RIDDEN_LIFE_TICKS);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return tier() == ClayTier.C2 && passenger instanceof Player && getPassengers().size() < 2;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof LivingEntity living ? living : null;
    }

    @Override
    public double getPassengersRidingOffset() {
        return getBbHeight() - 0.5D;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (!hasPassenger(passenger)) {
            return;
        }
        Vec3[] offsets = {
                new Vec3(0.4D, 0.0D, 0.0D),
                new Vec3(-0.5D, 0.0D, 0.0D)
        };
        int index = Mth.clamp(getPassengers().indexOf(passenger), 0, offsets.length - 1);
        Vec3 rotated = offsets[index].yRot(-getYRot() * Mth.DEG_TO_RAD - ((float)Math.PI / 2.0F));
        moveFunction.accept(passenger,
                getX() + rotated.x(),
                getY() + getPassengersRidingOffset() + passenger.getMyRidingOffset(),
                getZ() + rotated.z());
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        if (entity == getOwner()) {
            return true;
        }
        return entity instanceof ExplosiveClayEntity clay
                && clay.ownerUuid != null
                && clay.ownerUuid.equals(this.ownerUuid)
                || super.isAlliedTo(entity);
    }

    public float getRenderScale(float partialTick) {
        if (tier() != ClayTier.C3) {
            return tier().renderScale();
        }
        return 0.5F + 7.5F * Mth.clamp(((float)this.tickCount + partialTick) / (float)C3_GROW_TIME, 0.0F, 1.0F);
    }

    public ClayTier tier() {
        if (getType() == ModEntityTypes.C_2.get()) {
            return ClayTier.C2;
        }
        if (getType() == ModEntityTypes.C_3.get()) {
            return ClayTier.C3;
        }
        return ClayTier.C1;
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
                setOwner(living);
                return living;
            }
        }
        return null;
    }

    private void setOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
    }

    private int getLifeEndTick() {
        return this.entityData.get(LIFE_END_TICK);
    }

    private int remainingLifeTicks() {
        return getLifeEndTick() - this.tickCount;
    }

    private void setLifeTicks(int ticks) {
        this.entityData.set(LIFE_END_TICK, this.tickCount + ticks);
    }

    private int defaultLifeTicks() {
        return tier() == ClayTier.C3 ? 2400 : DEFAULT_LIFE_TICKS;
    }

    private boolean isC3Armed() {
        return this.entityData.get(C3_ARMED);
    }

    private void setC3Armed(boolean armed) {
        this.entityData.set(C3_ARMED, armed);
    }

    private void discardClay(boolean poof) {
        if (this.cleanedUp) {
            discard();
            return;
        }
        this.cleanedUp = true;
        if (poof && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                    80, getBbWidth() * 0.5D, getBbHeight() * 0.25D, getBbWidth() * 0.5D, 0.04D);
            this.level().playSound(null, getX(), getY(), getZ(),
                    ModSounds.SOUND_POOF.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
        discard();
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        this.entityData.set(LIFE_END_TICK, tag.contains("LifeEndTick") ? tag.getInt("LifeEndTick") : this.tickCount + defaultLifeTicks());
        this.entityData.set(C3_ARMED, tag.getBoolean("C3Armed"));
        this.c3WarningStarted = tag.getBoolean("C3WarningStarted");
        applyVariantAttributes();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putInt("LifeEndTick", getLifeEndTick());
        tag.putBoolean("C3Armed", isC3Armed());
        tag.putBoolean("C3WarningStarted", this.c3WarningStarted);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public enum ClayTier {
        C1(14.0D, 0.40D, 0.4F, 4.0F, 4.0F),
        C2(20.0D, 0.40D, 3.0F, 10.0F, 10.0F),
        C3(20.0D, 0.30D, 0.5F, 30.0F, 30.0F);

        private final double maxHealth;
        private final double speed;
        private final float renderScale;
        private final float explosionPower;
        private final float directDamage;

        ClayTier(double maxHealth, double speed, float renderScale, float explosionPower, float directDamage) {
            this.maxHealth = maxHealth;
            this.speed = speed;
            this.renderScale = renderScale;
            this.explosionPower = explosionPower;
            this.directDamage = directDamage;
        }

        private static ClayTier fromPower(float power) {
            int tier = Mth.clamp((int)Math.floor(power), 1, 3);
            return tier == 1 ? C1 : tier == 2 ? C2 : C3;
        }

        private EntityType<ExplosiveClayEntity> type() {
            return switch (this) {
                case C1 -> ModEntityTypes.C_1.get();
                case C2 -> ModEntityTypes.C_2.get();
                case C3 -> ModEntityTypes.C_3.get();
            };
        }

        private double maxHealth() {
            return this.maxHealth;
        }

        private double speed() {
            return this.speed;
        }

        private float renderScale() {
            return this.renderScale;
        }

        private float explosionPower() {
            return this.explosionPower;
        }

        private float directDamage() {
            return this.directDamage;
        }
    }
}
