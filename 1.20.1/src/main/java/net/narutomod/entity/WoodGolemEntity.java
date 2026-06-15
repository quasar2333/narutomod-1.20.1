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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.Chakra;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class WoodGolemEntity extends PathfinderMob {
    public static final float WIDTH = 4.8F;
    public static final float HEIGHT = 16.0F;
    private static final int GROW_TICKS = 30;
    private static final int UNRIDDEN_GRACE_TICKS = 120;
    private static final double ATTACK_REACH = 18.0D;
    private static final double ATTACK_DAMAGE = 200.0D;
    private static final double MOVE_ACCELERATION = 0.09D;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(WoodGolemEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> CHAKRA_BURN = SynchedEntityData.defineId(WoodGolemEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUuid;
    private int attackCooldown;

    public WoodGolemEntity(EntityType<? extends WoodGolemEntity> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.setMaxUpStep(HEIGHT / 3.0F);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.ARMOR, 20.0D)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D);
    }

    public static boolean spawnFrom(Player owner, double chakraBurnPerSecond) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        WoodGolemEntity golem = ModEntityTypes.WOOD_GOLEM.get().create(serverLevel);
        if (golem == null) {
            return false;
        }
        golem.configure(owner, chakraBurnPerSecond);
        serverLevel.addFreshEntity(golem);
        owner.startRiding(golem, true);
        return true;
    }

    public static boolean isOwnedGolem(@Nullable Entity entity, Player owner) {
        return entity instanceof WoodGolemEntity golem && golem.isOwnedBy(owner);
    }

    private void configure(Player owner, double chakraBurnPerSecond) {
        setOwner(owner);
        setChakraBurn(chakraBurnPerSecond);
        double maxHealth = Math.max(Chakra.getLevel(owner) * 50.0D, 50.0D);
        setAttributeBaseValue(Attributes.MAX_HEALTH, maxHealth);
        setAttributeBaseValue(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE);
        setAttributeBaseValue(Attributes.MOVEMENT_SPEED, 0.5D);
        setHealth(getMaxHealth());
        moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), 0.0F);
        setYBodyRot(owner.getYRot());
        setYHeadRot(owner.getYRot());
        this.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                ModSounds.SOUND_MOKUJIN_NO_JUTSU.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
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
        this.entityData.define(CHAKRA_BURN, 0.0F);
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
            discard();
            return;
        }
        if (this.tickCount <= GROW_TICKS) {
            spawnGrowthParticles();
        }
        if (!burnChakra(owner)) {
            discard();
            return;
        }
        if (getControllingPassenger() instanceof Player rider && isOwnedBy(rider)) {
            tickControlled(rider);
        } else if (!isVehicle() && this.tickCount > UNRIDDEN_GRACE_TICKS) {
            discard();
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
        return getBbHeight() * getRenderGrowth(0.0F) + 0.35D;
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
        if (entity == getOwner()) {
            return true;
        }
        return entity instanceof WoodGolemEntity golem
                && golem.ownerUuid != null
                && golem.ownerUuid.equals(this.ownerUuid)
                || super.isAlliedTo(entity);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        setChakraBurn(tag.getDouble("ChakraBurn"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putDouble("ChakraBurn", getChakraBurn());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public float getRenderGrowth(float partialTick) {
        return Mth.clamp(((float)this.tickCount + partialTick) / (float)GROW_TICKS, 0.05F, 1.0F);
    }

    private void tickControlled(Player rider) {
        setYRot(rider.getYRot());
        setXRot(0.0F);
        yRotO = getYRot();
        xRotO = getXRot();
        rider.fallDistance = 0.0F;
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
        return forward.scale(rider.zza * MOVE_ACCELERATION)
                .add(right.scale(rider.xxa * MOVE_ACCELERATION));
    }

    private void attackLookTarget(Player rider) {
        if (!rider.swinging || this.attackCooldown > 0) {
            return;
        }
        HitResult hit = ProcedureUtils.objectEntityLookingAt(rider, ATTACK_REACH, 1.0D, true, false,
                target -> target instanceof LivingEntity living && living.isAlive() && target != rider && target != this && !isAlliedTo(target));
        if (hit instanceof EntityHitResult entityHit) {
            this.attackCooldown = 12;
            doHurtTarget(entityHit.getEntity());
        }
    }

    private boolean burnChakra(LivingEntity owner) {
        if (owner instanceof Player player && player.isCreative()) {
            return true;
        }
        return this.tickCount <= 0 || this.tickCount % 20 != 19
                || getChakraBurn() <= 0.0D
                || Chakra.pathway(owner).consume(getChakraBurn());
    }

    private void spawnGrowthParticles() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int i = 0; i < 18; i++) {
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
                        0.2D,
                        0.35D,
                        0.2D,
                        0.08D);
            }
        }
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

    private boolean isOwnedBy(Player player) {
        return this.ownerUuid != null && this.ownerUuid.equals(player.getUUID())
                || this.entityData.get(OWNER_ID) == player.getId();
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
