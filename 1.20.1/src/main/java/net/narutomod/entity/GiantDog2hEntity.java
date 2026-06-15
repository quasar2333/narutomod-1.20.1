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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class GiantDog2hEntity extends PathfinderMob implements Enemy {
    public static final float ENTITY_SCALE = 8.0F;
    public static final float WIDTH = 0.6F * ENTITY_SCALE;
    public static final float HEIGHT = 0.85F * ENTITY_SCALE;
    public static final double DEFAULT_MAX_HEALTH = 500.0D;
    private static final double ARMOR = 100.0D;
    private static final double ATTACK_DAMAGE = 30.0D;
    private static final double COLLISION_DAMAGE = 5.0D;
    private static final double FOLLOW_RANGE = 64.0D;
    private static final double MOVEMENT_SPEED = 0.6D;
    private static final float MIN_SPLIT_HEALTH = 50.0F;
    private static final int SPLIT_DELAY_TICKS = 20;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(GiantDog2hEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private GiantDog2hEntity child;

    public GiantDog2hEntity(EntityType<? extends GiantDog2hEntity> entityType, Level level) {
        super(entityType, level);
        setPersistenceRequired();
        setMaxUpStep(HEIGHT / 3.0F);
        this.xpReward = 5000;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.ARMOR, ARMOR)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MAX_HEALTH, DEFAULT_MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED);
    }

    @Nullable
    public static GiantDog2hEntity spawnFor(Player owner, double maxHealth) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        GiantDog2hEntity dog = ModEntityTypes.GIANT_DOG_2H.get().create(serverLevel);
        if (dog == null) {
            return null;
        }
        dog.configureFromOwner(owner, maxHealth);
        serverLevel.addFreshEntity(dog);
        return dog;
    }

    public void configureFromOwner(Player owner, double maxHealth) {
        configure(owner, maxHealth);
        BlockHitResult hit = ProcedureUtils.raytraceBlocks(owner, 4.0D);
        double x = owner.getX() + owner.getLookAngle().x() * 4.0D;
        double y = owner.getY();
        double z = owner.getZ() + owner.getLookAngle().z() * 4.0D;
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hit.getBlockPos().above();
            x = pos.getX() + 0.5D;
            y = pos.getY();
            z = pos.getZ() + 0.5D;
        }
        moveTo(x, y, z, owner.getYRot() + 180.0F, 0.0F);
        setYBodyRot(getYRot());
        setYHeadRot(getYRot());
    }

    public void configure(@Nullable LivingEntity owner, double maxHealth) {
        if (owner != null) {
            setOwner(owner);
        }
        setAttributeBaseValue(Attributes.MAX_HEALTH, Math.max(maxHealth, 1.0D));
        setHealth(getMaxHealth());
    }

    public boolean isOwnedBy(LivingEntity entity) {
        return this.ownerUuid != null && this.ownerUuid.equals(entity.getUUID())
                || this.entityData.get(OWNER_ID) == entity.getId();
    }

    public void dismissWithPoof() {
        discardWithPoof();
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

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LeapAtTargetGoal(this, 1.0F));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.4D, true));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_ID, -1);
    }

    @Override
    public void tick() {
        this.fallDistance = 0.0F;
        super.tick();
        if (this.level().isClientSide || isDeadOrDying()) {
            return;
        }
        if (this.level().getDifficulty() == Difficulty.PEACEFUL) {
            discard();
            return;
        }
        LivingEntity owner = getOwner();
        if (this.ownerUuid != null && (owner == null || !owner.isAlive())) {
            discardWithPoof();
            return;
        }
        if (owner != null && this.tickCount % 10 == 0) {
            copyOwnerTarget(owner);
        }
        if (this.tickCount % 5 == 0) {
            hurtCollidingTargets();
        }
    }

    @Override
    protected void tickDeath() {
        this.deathTime++;
        if (this.level().isClientSide) {
            return;
        }
        if (this.deathTime < SPLIT_DELAY_TICKS) {
            return;
        }
        if (getMaxHealth() < MIN_SPLIT_HEALTH) {
            discardWithPoof();
            return;
        }
        splitAfterDeath();
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        target.invulnerableTime = 0;
        float damage = (float)(getAttributeValue(Attributes.ATTACK_DAMAGE)
                * (0.8D + this.random.nextDouble() * 0.4D));
        return target.hurt(this.damageSources().mobAttack(this), damage);
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

    @Override
    public double getPassengersRidingOffset() {
        return getBbHeight() + 0.35D;
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
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public int getMaxFallDistance() {
        return 12;
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        if (entity == getOwner()) {
            return true;
        }
        if (entity instanceof GiantDog2hEntity dog
                && this.ownerUuid != null
                && this.ownerUuid.equals(dog.ownerUuid)) {
            return true;
        }
        return super.isAlliedTo(entity);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (reason == RemovalReason.KILLED
                && this.child != null
                && this.child.isAlive()
                && !this.level().isClientSide) {
            this.child.discardWithPoof();
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        } else {
            this.ownerUuid = null;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WOLF_GROWL;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOLF_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOLF_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 2.0F;
    }

    private void splitAfterDeath() {
        float splitHealth = getMaxHealth() * 0.5F;
        this.dead = false;
        this.deathTime = 0;
        setNoAi(false);
        setPose(Pose.STANDING);
        setAttributeBaseValue(Attributes.MAX_HEALTH, splitHealth);
        setHealth(splitHealth);

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        GiantDog2hEntity newChild = ModEntityTypes.GIANT_DOG_2H.get().create(serverLevel);
        if (newChild == null) {
            return;
        }
        copySplitStateTo(newChild, splitHealth);
        serverLevel.addFreshEntity(newChild);
        this.child = newChild;
    }

    private void copySplitStateTo(GiantDog2hEntity target, double maxHealth) {
        target.ownerUuid = this.ownerUuid;
        target.entityData.set(OWNER_ID, this.entityData.get(OWNER_ID));
        target.setAttributeBaseValue(Attributes.MAX_HEALTH, maxHealth);
        target.setHealth(target.getMaxHealth());
        target.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        target.setYBodyRot(this.yBodyRot);
        target.setYHeadRot(this.yHeadRot);
        target.setTarget(getTarget());
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

    private void hurtCollidingTargets() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (Entity entity : serverLevel.getEntities(this, getBoundingBox().inflate(0.08D),
                candidate -> candidate instanceof LivingEntity living
                        && living.isAlive()
                        && !hasPassenger(candidate)
                        && !isAlliedTo(living))) {
            entity.hurt(this.damageSources().mobAttack(this), (float)COLLISION_DAMAGE);
        }
    }

    private void setOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
    }

    private void setAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private void discardWithPoof() {
        spawnPoof();
        discard();
    }

    private void spawnPoof() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, getX(), getY(), getZ(), ModSounds.SOUND_POOF.get(), SoundSource.NEUTRAL, 2.0F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.POOF,
                    getX(),
                    getY() + getBbHeight() * 0.5D,
                    getZ(),
                    300,
                    getBbWidth() * 0.5D,
                    getBbHeight() * 0.3D,
                    getBbWidth() * 0.5D,
                    0.08D);
        }
    }
}
