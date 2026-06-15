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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.Chakra;
import net.narutomod.item.SenjutsuItem;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class Buddha1000Entity extends Entity {
    private static final float MODEL_SCALE = 20.0F;
    private static final int GROW_TICKS = 40;
    private static final int ARM_VOLLEY_COUNT = 100;
    private static final double SITTING_MOVE_ACCELERATION = 0.04D;
    private static final double STANDING_MOVE_ACCELERATION = 0.09D;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(Buddha1000Entity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SITTING = SynchedEntityData.defineId(Buddha1000Entity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> TICKS_ALIVE = SynchedEntityData.defineId(Buddha1000Entity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> CHAKRA_BURN = SynchedEntityData.defineId(Buddha1000Entity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUuid;
    private int attackCooldown;

    public Buddha1000Entity(EntityType<? extends Buddha1000Entity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
        this.blocksBuilding = true;
    }

    public static boolean spawnFrom(Player owner, double chakraBurnPerSecond) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        Buddha1000Entity entity = ModEntityTypes.BUDDHA_1000.get().create(serverLevel);
        if (entity == null) {
            return false;
        }
        entity.configure(owner, chakraBurnPerSecond);
        boolean added = serverLevel.addFreshEntity(entity);
        if (added) {
            owner.startRiding(entity, true);
            serverLevel.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                    ModSounds.SOUND_SHINSUSENJU.get(), SoundSource.PLAYERS, 5.0F, 1.0F);
        }
        return added;
    }

    private void configure(Player owner, double chakraBurnPerSecond) {
        setOwner(owner);
        setSitting(true);
        setChakraBurn(chakraBurnPerSecond);
        moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), 0.0F);
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(SITTING, true);
        this.entityData.define(TICKS_ALIVE, 0);
        this.entityData.define(CHAKRA_BURN, 0.0F);
    }

    @Override
    public void tick() {
        this.fallDistance = 0.0F;
        super.tick();
        setNoGravity(true);
        this.entityData.set(TICKS_ALIVE, this.tickCount);
        if (this.level().isClientSide) {
            return;
        }

        Player owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        if (!(getControllingPassenger() instanceof Player rider) || !isOwnedBy(rider)) {
            discard();
            return;
        }
        tickSummonPresentation();
        if (!burnChakra(owner)) {
            discard();
            return;
        }
        tickControlled(rider);
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
    }

    public boolean isSitting() {
        return this.entityData.get(SITTING);
    }

    public void setSitting(boolean sitting) {
        this.entityData.set(SITTING, sitting);
    }

    public int getTicksAlive() {
        return this.entityData.get(TICKS_ALIVE);
    }

    public double getChakraBurn() {
        return this.entityData.get(CHAKRA_BURN);
    }

    public float getRenderGrowth(float partialTick) {
        return Mth.clamp(((float)getTicksAlive() + partialTick) / (float)GROW_TICKS, 0.05F, 1.0F);
    }

    public boolean isOwnedBy(Entity entity) {
        return this.ownerUuid != null && this.ownerUuid.equals(entity.getUUID())
                || this.entityData.get(OWNER_ID) == entity.getId();
    }

    public int shootArms() {
        if (this.level().isClientSide || this.attackCooldown > 0) {
            return 0;
        }
        Player owner = getOwner();
        LivingEntity shooter = getControllingPassenger();
        if (owner == null || shooter == null) {
            return 0;
        }
        int spawned = isSitting() ? shootSittingArmVolley(owner, shooter) : shootStandingArm(owner, shooter);
        if (spawned > 0) {
            this.attackCooldown = 20;
            this.level().playSound(null, getX(), getY(), getZ(),
                    ModSounds.SOUND_WOODSPAWN.get(), SoundSource.NEUTRAL, 5.0F, this.random.nextFloat() * 0.6F + 0.6F);
        }
        return spawned;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker instanceof Player player && isOwnedBy(player) && player.isShiftKeyDown()) {
            discard();
        }
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && isOwnedBy(player)) {
            player.startRiding(this, true);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && passenger instanceof Player player && isOwnedBy(player);
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof LivingEntity living ? living : null;
    }

    @Override
    public double getPassengersRidingOffset() {
        double seatedDrop = isSitting() ? 0.5D * MODEL_SCALE : 0.0D;
        return (getBbHeight() - seatedDrop) * getRenderGrowth(0.0F) + 0.35D;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (!hasPassenger(passenger)) {
            return;
        }
        Vec3 offset = new Vec3(0.0D, 1.0D, 0.25D * MODEL_SCALE).yRot(-getYRot() * Mth.DEG_TO_RAD);
        moveFunction.accept(passenger,
                getX() + offset.x(),
                getY() + getPassengersRidingOffset() + passenger.getMyRidingOffset() + offset.y(),
                getZ() + offset.z());
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 256.0D * getViewScale();
        return distance < range * range;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        this.entityData.set(SITTING, !tag.contains("Sitting") || tag.getBoolean("Sitting"));
        this.entityData.set(TICKS_ALIVE, tag.getInt("TicksAlive"));
        setChakraBurn(tag.getDouble("ChakraBurn"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putBoolean("Sitting", isSitting());
        tag.putInt("TicksAlive", getTicksAlive());
        tag.putDouble("ChakraBurn", getChakraBurn());
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && (reason == RemovalReason.DISCARDED || reason == RemovalReason.KILLED)) {
            Player owner = getOwner();
            if (owner != null) {
                owner.addEffect(new MobEffectInstance(ModEffects.FEATHER_FALLING.get(), 60, 5, false, false));
            }
            this.level().playSound(null, getX(), getY(), getZ(), ModSounds.SOUND_POOF.get(), SoundSource.NEUTRAL, 2.0F, 1.0F);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.POOF, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                        300, getBbWidth() * 0.5D, getBbHeight() * 0.3D, getBbWidth() * 0.5D, 0.05D);
            }
        }
        super.remove(reason);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private int shootSittingArmVolley(Player owner, LivingEntity shooter) {
        Vec3 direction = shooter.getLookAngle();
        if (direction.lengthSqr() <= 1.0E-8D) {
            direction = Vec3.directionFromRotation(shooter.getXRot(), shooter.getYRot());
        }
        int spawned = 0;
        for (int i = 0; i < ARM_VOLLEY_COUNT; i++) {
            Vec3 local = ProcedureUtils.rotateRoll(new Vec3(0.0D, 1.0D, 0.1D),
                    (this.random.nextFloat() - 0.5F) * Mth.PI * 1.2F)
                    .yRot(-getYRot() * Mth.DEG_TO_RAD)
                    .scale((this.random.nextDouble() * 3.0D + 1.5D) * getBbWidth());
            Vec3 origin = new Vec3(getX(), getY() + 0.625D * getBbHeight(), getZ()).add(local);
            if (BuddhaArmEntity.spawnFrom(owner, origin, direction, 100.0F, true, 1.15F, false)) {
                spawned++;
            }
        }
        return spawned;
    }

    private int shootStandingArm(Player owner, LivingEntity shooter) {
        Vec3 look = Vec3.directionFromRotation(0.0F, getYRot());
        Vec3 origin = new Vec3(getX(), getY() + 0.625D * getBbHeight(), getZ()).add(look.scale(getBbWidth()));
        Vec3 target = ProcedureUtils.objectEntityLookingAt(shooter, 64.0D).getLocation();
        Vec3 direction = target.subtract(origin);
        return BuddhaArmEntity.spawnFrom(owner, origin, direction, 500.0F, false, 1.2F, false) ? 1 : 0;
    }

    private void tickControlled(Player rider) {
        float yaw = isSitting()
                ? Mth.approachDegrees(getYRot(), rider.getYRot(), 2.0F)
                : rider.getYRot();
        setYRot(yaw);
        setXRot(0.0F);
        yRotO = getYRot();
        xRotO = getXRot();
        rider.fallDistance = 0.0F;
        Vec3 motion = getDeltaMovement();
        Vec3 acceleration = controlAcceleration(rider);
        setDeltaMovement(motion.x() * 0.65D + acceleration.x(), 0.0D, motion.z() * 0.65D + acceleration.z());
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().multiply(0.85D, 0.0D, 0.85D));
    }

    private Vec3 controlAcceleration(Player rider) {
        double acceleration = isSitting() ? SITTING_MOVE_ACCELERATION : STANDING_MOVE_ACCELERATION;
        Vec3 forward = Vec3.directionFromRotation(0.0F, rider.getYRot());
        forward = new Vec3(forward.x(), 0.0D, forward.z());
        if (forward.lengthSqr() < 1.0E-8D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        forward = forward.normalize();
        Vec3 right = new Vec3(-forward.z(), 0.0D, forward.x());
        return forward.scale(rider.zza * acceleration)
                .add(right.scale(rider.xxa * acceleration));
    }

    private void tickSummonPresentation() {
        if (this.tickCount <= GROW_TICKS) {
            spawnGrowthParticles();
            if (this.tickCount % 5 == 0) {
                this.level().playSound(null,
                        getX() + (this.random.nextDouble() - 0.5D) * getBbWidth() * 5.0D,
                        getY(),
                        getZ() + (this.random.nextDouble() - 0.5D) * getBbWidth() * 5.0D,
                        ModSounds.SOUND_WOODSPAWN.get(), SoundSource.BLOCKS, 2.0F,
                        this.random.nextFloat() * 0.6F + 0.6F);
            }
        }
    }

    private boolean burnChakra(Player owner) {
        if (owner.isCreative()) {
            return true;
        }
        if (!hasActiveSageMode(owner)) {
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
        for (int i = 0; i < 18; i++) {
            double x = getX() + (this.random.nextDouble() - 0.5D) * getBbWidth();
            double z = getZ() + (this.random.nextDouble() - 0.5D) * getBbWidth();
            BlockPos pos = BlockPos.containing(x, getY() - 0.25D, z);
            BlockState state = serverLevel.getBlockState(pos);
            if (!state.isAir() && state.isSolid()) {
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

    private void setOwner(Player owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
    }

    @Nullable
    private Player getOwner() {
        int ownerId = this.entityData.get(OWNER_ID);
        if (ownerId >= 0 && this.level().getEntity(ownerId) instanceof Player player) {
            return player;
        }
        if (this.ownerUuid != null && this.level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(this.ownerUuid) instanceof Player player) {
            setOwner(player);
            return player;
        }
        return null;
    }

    private void setChakraBurn(double chakraBurn) {
        this.entityData.set(CHAKRA_BURN, (float)Math.max(chakraBurn, 0.0D));
    }
}
