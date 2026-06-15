package net.narutomod.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.Chakra;
import net.narutomod.NarutomodModVariables;
import net.narutomod.PlayerTracker;
import net.narutomod.block.LightSourceBlock;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureSync;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class ChidoriEntity extends Entity {
    public static final int GROW_TIME = 40;
    public static final double CHAKRA_USAGE = 150.0D;
    public static final double CHAKRA_BURN_PER_SECOND = 40.0D;
    private static final int DEFAULT_DURATION = 120;
    private static final int CLIENT_HAND_POSITION_TTL_TICKS = 5;
    private static final double MAX_CLIENT_HAND_DISTANCE_SQR = 6.0D * 6.0D;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(ChidoriEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;
    private int duration = DEFAULT_DURATION;
    private double chakraBurn = CHAKRA_BURN_PER_SECOND;
    private boolean forceBowPoseSynced;
    private int ticksSinceLastSwing = 100;
    @Nullable
    private Vec3 clientHandPosition;
    private long clientHandPositionGameTime = Long.MIN_VALUE;

    public ChidoriEntity(EntityType<? extends ChidoriEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    public void configure(LivingEntity owner, double chakraBurnPerSecond, int durationTicks) {
        setOwner(owner);
        this.chakraBurn = chakraBurnPerSecond;
        this.duration = Math.max(durationTicks, GROW_TIME + 1);
        moveToOwner(owner);
    }

    public boolean isOwnedBy(LivingEntity entity) {
        return entity.getUUID().equals(this.ownerUuid) || getOwner() == entity;
    }

    public void stopChidori() {
        discardChidori();
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

    public float getGrowth() {
        return Mth.clamp(this.tickCount / (float) GROW_TIME, 0.0F, 1.0F);
    }

    public void acceptClientHandPosition(ServerPlayer sender, Vec3 position) {
        LivingEntity owner = getOwner();
        if (owner != sender || !isValidClientHandPosition(sender, position)) {
            return;
        }
        this.clientHandPosition = position;
        this.clientHandPositionGameTime = this.level().getGameTime();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
    }

    @Override
    public void tick() {
        super.tick();
        this.clearFire();
        LivingEntity owner = getOwner();
        if (!this.level().isClientSide) {
            if (owner == null || !owner.isAlive() || this.tickCount > this.duration || !canContinue(owner)) {
                discardChidori();
                return;
            }
            syncForceBowPose(owner);
            owner.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 2, 6, false, false));
            moveToOwner(owner);
            burnChakra(owner);
            playElectricity();
            refreshLegacyLightSource();
            spawnFeedback(owner);
            handleMatureChidori(owner);
        }
        this.ticksSinceLastSwing++;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        this.duration = tag.contains("Duration") ? tag.getInt("Duration") : DEFAULT_DURATION;
        this.chakraBurn = tag.contains("ChakraBurn") ? tag.getDouble("ChakraBurn") : CHAKRA_BURN_PER_SECOND;
        this.forceBowPoseSynced = tag.getBoolean("ForceBowPoseSynced");
        this.ticksSinceLastSwing = tag.getInt("TicksSinceLastSwing");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putInt("Duration", this.duration);
        tag.putDouble("ChakraBurn", this.chakraBurn);
        tag.putBoolean("ForceBowPoseSynced", this.forceBowPoseSynced);
        tag.putInt("TicksSinceLastSwing", this.ticksSinceLastSwing);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void setOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
    }

    private boolean canContinue(LivingEntity owner) {
        if (owner.getMainHandItem().getItem() instanceof net.narutomod.item.RaitonItem) {
            return true;
        }
        return owner.getMainHandItem().isEmpty() || ProcedureUtils.isWeapon(owner.getMainHandItem());
    }

    private boolean holdingWeapon(LivingEntity owner) {
        return ProcedureUtils.isWeapon(owner.getMainHandItem());
    }

    private void syncForceBowPose(LivingEntity owner) {
        boolean shouldForce = !holdingWeapon(owner);
        if (shouldForce && !this.forceBowPoseSynced) {
            ProcedureSync.EntityNBTTag.setAndSync(owner, NarutomodModVariables.FORCE_BOW_POSE, true);
            this.forceBowPoseSynced = true;
        } else if (!shouldForce && this.forceBowPoseSynced) {
            ProcedureSync.EntityNBTTag.removeAndSync(owner, NarutomodModVariables.FORCE_BOW_POSE);
            this.forceBowPoseSynced = false;
        }
    }

    private void moveToOwner(LivingEntity owner) {
        this.setDeltaMovement(Vec3.ZERO);
        this.moveTo(owner.getX(), owner.getY() + 1.4D, owner.getZ(), owner.getYRot(), owner.getXRot());
    }

    private void burnChakra(LivingEntity owner) {
        if (this.tickCount > 0 && this.tickCount % 20 == 0 && !Chakra.pathway(owner).consume(this.chakraBurn)) {
            discardChidori();
        }
    }

    private void playElectricity() {
        if (this.random.nextFloat() <= getGrowth() * 0.3F) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.SOUND_ELECTRICITY.get(), SoundSource.PLAYERS, getGrowth() * 0.5F, this.random.nextFloat() * 2.0F + 1.0F);
        }
    }

    private void refreshLegacyLightSource() {
        if (this.tickCount > GROW_TIME / 2 && this.level() instanceof ServerLevel serverLevel) {
            LightSourceBlock.setOrRefresh(serverLevel, BlockPos.containing(this.getX(), this.getY(), this.getZ()));
        }
    }

    private void spawnFeedback(LivingEntity owner) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (holdingWeapon(owner)) {
            return;
        }
        Vec3 point = resolveFeedbackPoint(owner);
        serverLevel.sendParticles(
                ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0x20FFFFFF, 5 + this.random.nextInt(55), 5, 0xF0, -1, 0),
                point.x(),
                point.y(),
                point.z(),
                Math.max(1, (int)(getGrowth() * 4.0F)),
                0.08D,
                0.04D,
                0.08D,
                0.03D);
        if (this.tickCount % 4 == 0) {
            LightningArcEntity arc = ModEntityTypes.LIGHTNING_ARC.get().create(serverLevel);
            if (arc != null) {
                arc.configureRandom(point, Math.max(getGrowth(), 0.1F), Vec3.ZERO, 0xC00000FF, 1, 0.0F, 0.1F);
                serverLevel.addFreshEntity(arc);
            }
        }
    }

    private Vec3 resolveFeedbackPoint(LivingEntity owner) {
        if (this.clientHandPosition != null
                && this.level().getGameTime() - this.clientHandPositionGameTime <= CLIENT_HAND_POSITION_TTL_TICKS) {
            return this.clientHandPosition;
        }
        return owner.getEyePosition()
                .add(owner.getLookAngle().scale(0.55D))
                .add(0.0D, -0.45D, 0.0D);
    }

    private static boolean isValidClientHandPosition(ServerPlayer sender, Vec3 position) {
        return Double.isFinite(position.x())
                && Double.isFinite(position.y())
                && Double.isFinite(position.z())
                && sender.distanceToSqr(position) <= MAX_CLIENT_HAND_DISTANCE_SQR;
    }

    private void handleMatureChidori(LivingEntity owner) {
        if (this.tickCount <= GROW_TIME) {
            return;
        }
        LivingEntity target = lookTarget(owner);
        if (!holdingWeapon(owner) && target != null && this.tickCount % 6 == 0) {
            launchAtTarget(owner, target);
        }
        if (!owner.swinging || this.ticksSinceLastSwing < 6) {
            return;
        }
        if (target != null && owner.distanceTo(target) <= 5.0D) {
            float damage = holdingWeapon(owner)
                    ? (float) ProcedureUtils.getModifiedAttackDamage(owner) * damageMultiplier(owner) * 1.3F
                    : 25.0F * damageMultiplier(owner);
            LightningArcEntity.onStruck(target, ModDamageTypes.ninjutsu(this.level(), this, owner), damage * cooledAttackStrength(owner), false);
        }
        this.ticksSinceLastSwing = 0;
    }

    @Nullable
    private LivingEntity lookTarget(LivingEntity owner) {
        if (owner instanceof Mob mob && mob.getTarget() != null) {
            return mob.getTarget();
        }
        HitResult hit = ProcedureUtils.objectEntityLookingAt(owner, 20.0D, 0.0D, false, false,
                target -> target != this && target != owner && target instanceof LivingEntity);
        return hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target ? target : null;
    }

    private void launchAtTarget(LivingEntity owner, LivingEntity target) {
        Vec3 delta = target.position().subtract(owner.position());
        ProcedureUtils.setVelocity(owner, delta.x() * 0.4D, delta.y() * 0.4D, delta.z() * 0.4D);
    }

    private float damageMultiplier(LivingEntity owner) {
        if (owner instanceof Player player) {
            return (float) (PlayerTracker.getNinjaLevel(player) / 25.0D);
        }
        return 1.0F;
    }

    private float cooledAttackStrength(LivingEntity owner) {
        float attackInterval = (float)(1.0D / Math.max(ProcedureUtils.getAttackSpeed(owner), 0.01D) * 20.0D);
        return Mth.clamp(this.ticksSinceLastSwing / attackInterval, 0.0F, 1.0F);
    }

    private void discardChidori() {
        LivingEntity owner = getOwner();
        if (this.forceBowPoseSynced && owner != null) {
            ProcedureSync.EntityNBTTag.removeAndSync(owner, NarutomodModVariables.FORCE_BOW_POSE);
            this.forceBowPoseSynced = false;
        }
        discard();
    }
}
