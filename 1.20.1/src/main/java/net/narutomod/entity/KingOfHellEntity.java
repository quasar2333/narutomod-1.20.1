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
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.Chakra;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class KingOfHellEntity extends PathfinderMob {
    public static final float WIDTH = 5.0F;
    public static final float HEIGHT = 4.8F;
    public static final double DEFAULT_CHAKRA_USAGE = 100.0D;
    private static final int MASK_ANIMATION_TICKS = 10;
    private static final int POPOUT_TICKS = 60;
    private static final int RETREAT_TICKS = 60;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(KingOfHellEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(KingOfHellEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MASK_OPEN_TICKS = SynchedEntityData.defineId(KingOfHellEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RETREAT_AGE = SynchedEntityData.defineId(KingOfHellEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private UUID healingUuid;
    private double chakraUsage = DEFAULT_CHAKRA_USAGE;

    public KingOfHellEntity(EntityType<? extends KingOfHellEntity> entityType, Level level) {
        super(entityType, level);
        setNoAi(true);
        setPersistenceRequired();
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Nullable
    public static KingOfHellEntity spawnFrom(Player owner) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        KingOfHellEntity entity = ModEntityTypes.KINGOFHELLENTITY.get().create(serverLevel);
        if (entity == null) {
            return null;
        }
        entity.configure(owner, DEFAULT_CHAKRA_USAGE);
        serverLevel.addFreshEntity(entity);
        return entity;
    }

    public void configure(Player owner, double chakraUsage) {
        setOwner(owner);
        this.chakraUsage = chakraUsage;
        BlockHitResult hit = ProcedureUtils.raytraceBlocks(owner, 4.0D);
        double x = owner.getX() + owner.getLookAngle().x() * 4.0D;
        double y = owner.getY();
        double z = owner.getZ() + owner.getLookAngle().z() * 4.0D;
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hit.getBlockPos();
            x = pos.getX() + 0.5D;
            z = pos.getZ() + 0.5D;
        }
        moveTo(x, y, z, owner.getYRot() - 180.0F, 0.0F);
        setYBodyRot(getYRot());
        setYHeadRot(getYRot());
    }

    public int getAge() {
        return this.entityData.get(AGE);
    }

    public float getMaskOpenAmount(float partialTick) {
        return Mth.clamp((this.entityData.get(MASK_OPEN_TICKS) + partialTick) / (float)MASK_ANIMATION_TICKS, 0.0F, 1.0F);
    }

    public float getPopoutFactor(float partialTick) {
        int retreatAge = this.entityData.get(RETREAT_AGE);
        if (retreatAge > 0) {
            return Mth.clamp(1.0F - (retreatAge + partialTick) / (float)RETREAT_TICKS, 0.0F, 1.0F);
        }
        return Mth.clamp((getAge() + partialTick) / (float)POPOUT_TICKS, 0.0F, 1.0F);
    }

    public boolean isRetreating() {
        return this.entityData.get(RETREAT_AGE) > 0;
    }

    public void dismiss() {
        beginRetreat();
    }

    public boolean isOwnedByOrAlly(Player player) {
        LivingEntity owner = getOwner();
        return owner != null && (owner == player || owner.isAlliedTo(player) || player.isAlliedTo(owner));
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
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(AGE, 0);
        this.entityData.define(MASK_OPEN_TICKS, 0);
        this.entityData.define(RETREAT_AGE, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        tickAgeAndMask();
        spawnAmbientParticles();
        if (getAge() == 1) {
            playKoHSound();
        }
        if (isRetreating()) {
            tickRetreat();
            return;
        }
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            beginRetreat();
            return;
        }
        if (owner.getHealth() < 4.0F && this.healingUuid == null) {
            beginHealing(owner);
        }
        if (getHealingEntity() != null) {
            keepMaskOpen();
            rejuvenateHealingEntity();
        } else if (this.entityData.get(MASK_OPEN_TICKS) >= MASK_ANIMATION_TICKS) {
            closeMask();
        }
        if (this.tickCount % 20 == 0 && this.chakraUsage > 0.0D && !Chakra.pathway(owner).consume(this.chakraUsage)) {
            beginRetreat();
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!isRetreating() && this.healingUuid == null && isOwnedByOrAlly(player)) {
            beginHealing(player);
            playKoHSound();
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
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
    protected void doPush(Entity entity) {
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public void knockback(double strength, double x, double z) {
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty()
                && passenger instanceof LivingEntity living
                && this.healingUuid != null
                && this.healingUuid.equals(living.getUUID());
    }

    @Override
    public double getPassengersRidingOffset() {
        return 1.0D;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (!hasPassenger(passenger)) {
            return;
        }
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot()).scale(0.25D);
        moveFunction.accept(passenger, getX() + forward.x(), getY() + getPassengersRidingOffset(), getZ() + forward.z());
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
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
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        if (tag.hasUUID("Healing")) {
            this.healingUuid = tag.getUUID("Healing");
        }
        this.chakraUsage = tag.contains("ChakraUsage") ? tag.getDouble("ChakraUsage") : DEFAULT_CHAKRA_USAGE;
        this.entityData.set(AGE, tag.getInt("Age"));
        this.entityData.set(MASK_OPEN_TICKS, tag.getInt("MaskOpenTicks"));
        this.entityData.set(RETREAT_AGE, tag.getInt("RetreatAge"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        if (this.healingUuid != null) {
            tag.putUUID("Healing", this.healingUuid);
        }
        tag.putDouble("ChakraUsage", this.chakraUsage);
        tag.putInt("Age", getAge());
        tag.putInt("MaskOpenTicks", this.entityData.get(MASK_OPEN_TICKS));
        tag.putInt("RetreatAge", this.entityData.get(RETREAT_AGE));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void beginHealing(LivingEntity target) {
        this.healingUuid = target.getUUID();
        keepMaskOpen();
    }

    @Nullable
    private LivingEntity getHealingEntity() {
        if (this.healingUuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(this.healingUuid);
        if (entity instanceof LivingEntity living && living.isAlive()) {
            return living;
        }
        this.healingUuid = null;
        return null;
    }

    private void rejuvenateHealingEntity() {
        LivingEntity healing = getHealingEntity();
        if (healing == null) {
            return;
        }
        if (!healing.isPassengerOfSameVehicle(this) && healing.startRiding(this, true)) {
            healing.removeAllEffects();
            healing.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 160, 4));
        }
        healing.heal(0.1F);
        if (healing instanceof Player player) {
            player.setShiftKeyDown(false);
        }
        if (healing.getHealth() >= healing.getMaxHealth()) {
            healing.stopRiding();
            this.healingUuid = null;
        }
    }

    private void beginRetreat() {
        if (isRetreating()) {
            return;
        }
        ejectPassengers();
        this.healingUuid = null;
        this.entityData.set(RETREAT_AGE, 1);
        closeMask();
    }

    private void tickRetreat() {
        int retreatAge = this.entityData.get(RETREAT_AGE) + 1;
        this.entityData.set(RETREAT_AGE, retreatAge);
        if (retreatAge > RETREAT_TICKS) {
            discard();
        }
    }

    private void tickAgeAndMask() {
        this.entityData.set(AGE, getAge() + 1);
        int openTicks = this.entityData.get(MASK_OPEN_TICKS);
        if (this.healingUuid != null && openTicks < MASK_ANIMATION_TICKS) {
            this.entityData.set(MASK_OPEN_TICKS, openTicks + 1);
        } else if (this.healingUuid == null && openTicks > 0) {
            this.entityData.set(MASK_OPEN_TICKS, openTicks - 1);
        }
    }

    private void keepMaskOpen() {
        this.entityData.set(MASK_OPEN_TICKS, MASK_ANIMATION_TICKS);
    }

    private void closeMask() {
        this.entityData.set(MASK_OPEN_TICKS, 0);
    }

    private void spawnAmbientParticles() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    getX(),
                    getY() + 0.3D,
                    getZ(),
                    24,
                    getBbWidth() * 0.25D,
                    0.2D,
                    getBbWidth() * 0.25D,
                    0.01D);
        }
    }

    private void playKoHSound() {
        this.level().playSound(null, getX(), getY(), getZ(), ModSounds.SOUND_KOH_SPAWN.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    private void setOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
    }
}
