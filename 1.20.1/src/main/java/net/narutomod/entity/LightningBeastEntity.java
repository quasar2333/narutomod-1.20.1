package net.narutomod.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class LightningBeastEntity extends PathfinderMob {
    private static final float MODEL_SCALE = 2.0F;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(LightningBeastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> POWER = SynchedEntityData.defineId(LightningBeastEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private BlockPos destination;
    private boolean summonedByPlayer;

    public LightningBeastEntity(EntityType<? extends LightningBeastEntity> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.setMaxUpStep(8.0F);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.ARMOR, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.6D)
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    public void configure(Player owner, float power) {
        setOwner(owner);
        setPower(power);
        this.summonedByPlayer = true;
        this.setHealth(this.getMaxHealth());
        BlockHitResult hit = ProcedureUtils.raytraceBlocks(owner, 4.0D);
        double x = hit.getBlockPos().getX() + 0.5D;
        double z = hit.getBlockPos().getZ() + 0.5D;
        this.moveTo(x, owner.getY(), z, owner.getYRot() - 180.0F, 0.0F);
    }

    public boolean isOwnedBy(LivingEntity entity) {
        return entity.getUUID().equals(this.ownerUuid) || getOwner() == entity;
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

    public float getPower() {
        return this.entityData.get(POWER);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(POWER, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive() || this.tickCount > Math.max(1, (int)(getPower() * 20.0F))) {
            discard();
            return;
        }
        BlockPos nextDestination = findDestination(owner);
        if (nextDestination != null) {
            this.destination = nextDestination;
        }
        if (this.tickCount % 10 == 0) {
            moveTowardDestination();
        }
        if (this.tickCount % 4 == 0) {
            spawnOwnerArc(owner);
        }
        playElectricity();
        spawnLocalArc();
        hurtCollidingTargets(owner);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return !this.summonedByPlayer;
    }

    @Override
    protected float getWaterSlowDown() {
        return 1.0F;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        setPower(tag.contains("Power") ? tag.getFloat("Power") : 1.0F);
        this.summonedByPlayer = tag.contains("PlayerSummoned") ? tag.getBoolean("PlayerSummoned") : this.ownerUuid != null;
        if (tag.contains("Destination")) {
            this.destination = BlockPos.of(tag.getLong("Destination"));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putFloat("Power", getPower());
        tag.putBoolean("PlayerSummoned", this.summonedByPlayer);
        if (this.destination != null) {
            tag.putLong("Destination", this.destination.asLong());
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void setOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
    }

    private void setPower(float power) {
        this.entityData.set(POWER, Math.max(power, 0.1F));
    }

    @Nullable
    private BlockPos findDestination(LivingEntity owner) {
        Vec3 start = owner.getEyePosition();
        Vec3 end = start.add(owner.getLookAngle().scale(50.0D));
        BlockHitResult hit = this.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, owner));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = this.level().getBlockState(pos);
        if (state.isFaceSturdy(this.level(), pos, Direction.UP) || !state.getFluidState().isEmpty()) {
            return pos.above();
        }
        return null;
    }

    private void moveTowardDestination() {
        if (this.destination == null) {
            return;
        }
        if (this.destination.getY() > this.getY() + 1.0D) {
            Vec3 jump = Vec3.atCenterOf(this.destination).subtract(this.position()).normalize().scale(1.6D);
            this.setDeltaMovement(jump.x(), jump.y() + 0.4D, jump.z());
            this.hasImpulse = true;
        }
        this.getNavigation().moveTo(
                this.destination.getX() + 0.5D,
                this.destination.getY(),
                this.destination.getZ() + 0.5D,
                Math.max(ProcedureUtils.getModifiedSpeed(this), 1.6D));
    }

    private void spawnOwnerArc(LivingEntity owner) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LightningArcEntity arc = ModEntityTypes.LIGHTNING_ARC.get().create(serverLevel);
        if (arc != null) {
            arc.configureBetween(owner.getEyePosition(), this.getEyePosition(), 0xC00000FF, 1, 0.0F, 0.0F, 4);
            serverLevel.addFreshEntity(arc);
        }
    }

    private void spawnLocalArc() {
        if (!(this.level() instanceof ServerLevel serverLevel) || this.random.nextFloat() > 0.4F) {
            return;
        }
        LightningArcEntity arc = ModEntityTypes.LIGHTNING_ARC.get().create(serverLevel);
        if (arc != null) {
            Vec3 center = new Vec3(
                    this.getX() + this.random.nextGaussian() * this.getBbWidth() * 0.5D,
                    this.getY() + this.random.nextDouble() * this.getBbHeight(),
                    this.getZ() + this.random.nextGaussian() * this.getBbWidth() * 0.5D);
            arc.configureRandom(center, 0.3D, new Vec3(0.0D, 0.15D, 0.0D), 0xC00000FF, 1, 0.0F, 0.1F);
            serverLevel.addFreshEntity(arc);
        }
    }

    private void playElectricity() {
        if (this.random.nextInt(8) == 0) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.SOUND_ELECTRICITY.get(), SoundSource.HOSTILE, 1.0F, this.random.nextFloat() * 0.6F + 0.9F);
        }
    }

    private void hurtCollidingTargets(LivingEntity owner) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        DamageSource source = ModDamageTypes.ninjutsu(this.level(), this, owner);
        for (Entity entity : serverLevel.getEntities(this, this.getBoundingBox().inflate(0.15D),
                target -> target != owner && target.isAlive() && !(target instanceof LightningArcEntity))) {
            if (entity instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(ModEffects.PARALYSIS.get(), 200, 2, false, false));
            }
            entity.hurt(source, getPower());
        }
    }
}
