package net.narutomod.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.Chakra;
import net.narutomod.item.SenjutsuItem;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class Snake8HeadsEntity extends PathfinderMob {
    public static final float MODEL_SCALE = 12.0F;
    public static final float BASE_WIDTH = 0.8F;
    public static final float BASE_HEIGHT = 2.0F;
    public static final float WIDTH = BASE_WIDTH * MODEL_SCALE;
    public static final float HEIGHT = BASE_HEIGHT * MODEL_SCALE;
    private static final int UP_TIME = 40;
    private static final int PASSENGER_HIDE_TICKS = 60;
    private static final int UNRIDDEN_GRACE_TICKS = 60;
    private static final double MOVE_ACCELERATION = 0.055D;
    private static final double HEAD_TARGET_RANGE = 64.0D;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(Snake8HeadsEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TICKS_ALIVE = SynchedEntityData.defineId(Snake8HeadsEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> CHAKRA_BURN = SynchedEntityData.defineId(Snake8HeadsEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUuid;
    private int headCooldown;

    public Snake8HeadsEntity(EntityType<? extends Snake8HeadsEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 0;
        setPersistenceRequired();
        setMaxUpStep(HEIGHT / 3.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MAX_HEALTH, 5000.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D);
    }

    @Nullable
    public static Snake8HeadsEntity spawnFrom(Player owner, double chakraBurnPerSecond) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Snake8HeadsEntity entity = ModEntityTypes.SNAKE_8_HEADS.get().create(serverLevel);
        if (entity == null) {
            return null;
        }
        entity.configure(owner, chakraBurnPerSecond);
        serverLevel.addFreshEntity(entity);
        owner.startRiding(entity, true);
        return entity;
    }

    private void configure(Player owner, double chakraBurnPerSecond) {
        setOwner(owner);
        setChakraBurn(chakraBurnPerSecond);
        setAttributeBaseValue(Attributes.ARMOR, 0.0D);
        setAttributeBaseValue(Attributes.MAX_HEALTH, 5000.0D);
        setAttributeBaseValue(Attributes.MOVEMENT_SPEED, 0.3D);
        setAttributeBaseValue(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
        setHealth(getMaxHealth());
        moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), 0.0F);
        setYBodyRot(owner.getYRot());
        setYHeadRot(owner.getYRot());
        this.level().playSound(null, getX(), getY(), getZ(),
                ModSounds.SOUND_WOODSPAWN.get(), SoundSource.NEUTRAL, 5.0F, 1.0F);
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
        this.entityData.define(TICKS_ALIVE, 0);
        this.entityData.define(CHAKRA_BURN, 0.0F);
    }

    @Override
    public void tick() {
        this.fallDistance = 0.0F;
        super.tick();
        this.entityData.set(TICKS_ALIVE, this.tickCount);
        if (this.level().isClientSide) {
            return;
        }

        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        tickSummonPresentation(owner);
        if (!burnChakra(owner)) {
            discard();
            return;
        }
        if (getControllingPassenger() instanceof Player rider && isOwnedBy(rider)) {
            tickControlled(rider);
        } else if (!isVehicle() && this.tickCount > UNRIDDEN_GRACE_TICKS) {
            discard();
        } else {
            getNavigation().stop();
            setDeltaMovement(getDeltaMovement().multiply(0.65D, 1.0D, 0.65D));
        }
        if (this.headCooldown > 0) {
            this.headCooldown--;
        }
    }

    public int getTicksAlive() {
        return this.entityData.get(TICKS_ALIVE);
    }

    public float getRenderGrowth(float partialTick) {
        return Mth.clamp(((float)getTicksAlive() + partialTick) / (float)UP_TIME, 0.05F, 1.0F);
    }

    @Nullable
    public Snake8HeadEntity shootHeadAt(@Nullable LivingEntity target) {
        LivingEntity owner = getOwner();
        LivingEntity shooter = getControllingPassenger();
        if (shooter == null) {
            shooter = owner;
        }
        if (shooter == null || this.level().isClientSide) {
            return null;
        }
        return Snake8HeadEntity.spawnFrom(shooter, target);
    }

    public boolean shootLookTarget(Player rider, double targetInflation) {
        if (this.level().isClientSide || this.headCooldown > 0 || !isOwnedBy(rider)) {
            return false;
        }
        HitResult hit = ProcedureUtils.objectEntityLookingAt(rider, HEAD_TARGET_RANGE, targetInflation, true, false,
                target -> target instanceof LivingEntity living
                        && living.isAlive()
                        && target != rider
                        && target != this
                        && !isAlliedTo(target));
        if (!(hit instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof LivingEntity target)) {
            return false;
        }
        if (shootHeadAt(target) == null) {
            return false;
        }
        this.headCooldown = 12;
        return true;
    }

    public boolean isOwnedBy(Entity entity) {
        return this.ownerUuid != null && this.ownerUuid.equals(entity.getUUID())
                || this.entityData.get(OWNER_ID) == entity.getId();
    }

    public boolean isOwnedByUuid(UUID uuid) {
        return this.ownerUuid != null && this.ownerUuid.equals(uuid);
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
        return 19.0D * 0.0625D * MODEL_SCALE;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (!hasPassenger(passenger)) {
            return;
        }
        Vec3 offset = new Vec3(3.5D * 0.0625D * MODEL_SCALE, 0.0D, 24.0D * 0.0625D * MODEL_SCALE)
                .yRot(-getYRot() * Mth.DEG_TO_RAD);
        moveFunction.accept(passenger,
                getX() + offset.x(),
                getY() + getPassengersRidingOffset() + passenger.getMyRidingOffset(),
                getZ() + offset.z());
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker instanceof Player player && isOwnedBy(player) && player.isShiftKeyDown()) {
            discard();
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        return entity == getOwner()
                || entity instanceof Snake8HeadsEntity snake
                        && snake.ownerUuid != null
                        && snake.ownerUuid.equals(this.ownerUuid)
                || entity instanceof Snake8HeadEntity head
                        && this.ownerUuid != null
                        && head.isOwnedByUuid(this.ownerUuid)
                || super.isAlliedTo(entity);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        this.entityData.set(TICKS_ALIVE, tag.getInt("life"));
        setChakraBurn(tag.getDouble("ChakraBurn"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putInt("life", getTicksAlive());
        tag.putDouble("ChakraBurn", getChakraBurn());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && (reason == RemovalReason.DISCARDED || reason == RemovalReason.KILLED)) {
            LivingEntity owner = getOwner();
            if (owner != null) {
                owner.addEffect(new MobEffectInstance(ModEffects.FEATHER_FALLING.get(), 60, 5, false, false));
            }
            this.level().playSound(null, getX(), getY(), getZ(),
                    ModSounds.SOUND_POOF.get(), SoundSource.NEUTRAL, 2.0F, 1.0F);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.POOF, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                        300, getBbWidth() * 0.5D, getBbHeight() * 0.3D, getBbWidth() * 0.5D, 0.05D);
            }
        }
        super.remove(reason);
    }

    private void tickSummonPresentation(LivingEntity owner) {
        if (this.tickCount == UP_TIME) {
            this.level().playSound(null, getX(), getY(), getZ(),
                    ModSounds.SOUND_SNAKE_HISS.get(), SoundSource.NEUTRAL, 5.0F, 0.7F);
        }
        if (this.tickCount <= UP_TIME && this.tickCount % 5 == 0) {
            this.level().playSound(null, getX(), getY(), getZ(),
                    ModSounds.SOUND_WOODGROW.get(), SoundSource.NEUTRAL, 2.0F, 1.0F);
            spawnGrowthParticles();
        }
        if (this.tickCount <= PASSENGER_HIDE_TICKS) {
            owner.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 4, 0, false, false));
        }
    }

    private void tickControlled(Player rider) {
        setYRot(rider.getYRot());
        setXRot(0.0F);
        setYBodyRot(rider.getYRot());
        setYHeadRot(rider.getYHeadRot());
        yRotO = getYRot();
        xRotO = getXRot();
        rider.fallDistance = 0.0F;
        getNavigation().stop();
        Vec3 motion = getDeltaMovement();
        Vec3 acceleration = controlAcceleration(rider);
        setDeltaMovement(motion.x() * 0.65D + acceleration.x(), motion.y(), motion.z() * 0.65D + acceleration.z());
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().multiply(0.85D, 1.0D, 0.85D));
        shootLookTargetFromSwing(rider);
    }

    private Vec3 controlAcceleration(Player rider) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, rider.getYRot());
        forward = new Vec3(forward.x(), 0.0D, forward.z());
        if (forward.lengthSqr() < 1.0E-8D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        forward = forward.normalize();
        Vec3 right = new Vec3(-forward.z(), 0.0D, forward.x());
        return forward.scale(rider.zza * MOVE_ACCELERATION)
                .add(right.scale(rider.xxa * MOVE_ACCELERATION));
    }

    private void shootLookTargetFromSwing(Player rider) {
        if (!rider.swinging || this.headCooldown > 0) {
            return;
        }
        shootLookTarget(rider, 1.0D);
    }

    private boolean burnChakra(LivingEntity owner) {
        if (owner instanceof Player player && player.isCreative()) {
            return true;
        }
        if (owner instanceof Player player && !hasActiveSageMode(player)) {
            return false;
        }
        return this.tickCount <= 0 || this.tickCount % 20 != 19
                || getChakraBurn() <= 0.0D
                || Chakra.pathway(owner).consume(getChakraBurn());
    }

    private boolean hasActiveSageMode(Player player) {
        for (ItemStack stack : ProcedureUtils.getAllItemsOfSubType(player, SenjutsuItem.class)) {
            if (SenjutsuItem.isSageModeActivated(stack)) {
                return true;
            }
        }
        return false;
    }

    private void spawnGrowthParticles() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int i = 0; i < 24; i++) {
            double x = getX() + (this.random.nextDouble() - 0.5D) * getBbWidth();
            double z = getZ() + (this.random.nextDouble() - 0.5D) * getBbWidth();
            BlockPos pos = BlockPos.containing(x, getY() - 0.25D, z);
            BlockState state = serverLevel.getBlockState(pos);
            if (!state.isAir()) {
                serverLevel.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, state),
                        pos.getX() + 0.5D,
                        pos.getY() + 1.0D,
                        pos.getZ() + 0.5D,
                        5,
                        0.25D,
                        0.45D,
                        0.25D,
                        0.08D);
            }
        }
    }

    private void setOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
    }

    private void setChakraBurn(double chakraBurn) {
        this.entityData.set(CHAKRA_BURN, (float)Math.max(chakraBurn, 0.0D));
    }

    private double getChakraBurn() {
        return this.entityData.get(CHAKRA_BURN);
    }

    private void setAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }
}
