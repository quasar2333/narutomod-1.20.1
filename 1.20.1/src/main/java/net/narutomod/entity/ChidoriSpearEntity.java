package net.narutomod.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.Chakra;
import net.narutomod.NarutomodModVariables;
import net.narutomod.PlayerTracker;
import net.narutomod.procedure.ProcedureSync;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class ChidoriSpearEntity extends Entity {
    public static final int DURATION = 81;
    private static final int CLIENT_HAND_POSITION_TTL_TICKS = 5;
    private static final double MAX_CLIENT_HAND_DISTANCE_SQR = 8.0D * 8.0D;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(ChidoriSpearEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RYU = SynchedEntityData.defineId(ChidoriSpearEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID ownerUuid;
    private boolean forceBowPoseSynced;
    @Nullable
    private Vec3 clientHandPosition;
    private long clientHandPositionGameTime = Long.MIN_VALUE;

    public ChidoriSpearEntity(EntityType<? extends ChidoriSpearEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public void configure(LivingEntity owner, boolean ryu) {
        setOwner(owner);
        setRyu(ryu);
        moveToOwner(owner);
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
        this.entityData.define(RYU, false);
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity owner = getOwner();
        if (!this.level().isClientSide) {
            if (owner == null || !owner.isAlive() || this.tickCount > DURATION || !canContinue(owner)) {
                discardSpear();
                return;
            }
            syncForceBowPose(owner);
            moveToOwner(owner);
            burnChakra(owner);
            playElectricity(owner);
            if (isRyu()) {
                spawnRyuArcs(owner);
            } else {
                spawnSpearArc(owner);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        setRyu(tag.getBoolean("Ryu"));
        this.forceBowPoseSynced = tag.getBoolean("ForceBowPoseSynced");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putBoolean("Ryu", isRyu());
        tag.putBoolean("ForceBowPoseSynced", this.forceBowPoseSynced);
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

    private boolean isRyu() {
        return this.entityData.get(RYU);
    }

    private void setRyu(boolean ryu) {
        this.entityData.set(RYU, ryu);
    }

    private boolean canContinue(LivingEntity owner) {
        if (owner.getMainHandItem().getItem() instanceof net.narutomod.item.RaitonItem) {
            return true;
        }
        return owner.getMainHandItem().isEmpty() || ProcedureUtils.isWeapon(owner.getMainHandItem());
    }

    private void syncForceBowPose(LivingEntity owner) {
        if (!this.forceBowPoseSynced) {
            ProcedureSync.EntityNBTTag.setAndSync(owner, NarutomodModVariables.FORCE_BOW_POSE, true);
            this.forceBowPoseSynced = true;
        }
    }

    private void moveToOwner(LivingEntity owner) {
        this.setDeltaMovement(Vec3.ZERO);
        this.moveTo(owner.getX(), owner.getY() + 1.0D, owner.getZ(), owner.getYRot(), owner.getXRot());
    }

    private void burnChakra(LivingEntity owner) {
        if (this.tickCount > 0 && this.tickCount % 20 == 1 && !Chakra.pathway(owner).consume(ChidoriEntity.CHAKRA_BURN_PER_SECOND)) {
            discardSpear();
        }
    }

    private void playElectricity(LivingEntity owner) {
        if (this.random.nextFloat() <= 0.3F) {
            this.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                    ModSounds.SOUND_ELECTRICITY.get(), SoundSource.PLAYERS, 1.0F, this.random.nextFloat() * 2.0F + 1.5F);
        }
    }

    private void spawnSpearArc(LivingEntity owner) {
        Vec3 from = resolveSpearStart(owner);
        Vec3 to = owner.getEyePosition().add(owner.getLookAngle().scale(6.0D));
        spawnArc(from, to, 0x800000FF, 1, 0.0F, 0.04F, 0, 10.0F * damageMultiplier(owner));
        if (this.random.nextInt(3) == 0) {
            spawnArc(from, to, 0xC00000FF, 1, 0.0F, 0.0F, 4, 0.0F);
        }
    }

    private Vec3 resolveSpearStart(LivingEntity owner) {
        if (this.clientHandPosition != null
                && this.level().getGameTime() - this.clientHandPositionGameTime <= CLIENT_HAND_POSITION_TTL_TICKS) {
            return this.clientHandPosition;
        }
        return owner.getEyePosition().add(0.0D, -0.5D, 0.0D);
    }

    private void spawnRyuArcs(LivingEntity owner) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 from = owner.position().add(0.0D, 1.0D, 0.0D);
        for (Entity target : level.getEntities(owner, owner.getBoundingBox().inflate(4.0D), entity -> entity != this && !(entity instanceof LightningArcEntity))) {
            if (this.random.nextInt(3) == 0) {
                Vec3 to = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
                spawnArc(from, to, 0xC00000FF, 1, 0.0F, 0.0F, 4, 10.0F * damageMultiplier(owner));
            }
        }
        LightningArcEntity randomArc = ModEntityTypes.LIGHTNING_ARC.get().create(level);
        if (randomArc != null) {
            randomArc.configureRandom(from, this.random.nextDouble() * 3.0D + 1.0D, Vec3.ZERO, 0xC00000FF, 0, 0.0F, 0.1F);
            randomArc.setDamage(10.0F * damageMultiplier(owner), owner);
            level.addFreshEntity(randomArc);
        }
    }

    private void spawnArc(Vec3 from, Vec3 to, int color, int duration, float inaccuracy, float thickness, int sections, float damage) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        LightningArcEntity arc = ModEntityTypes.LIGHTNING_ARC.get().create(level);
        if (arc == null) {
            return;
        }
        arc.configureBetween(from, to, color, duration, inaccuracy, thickness, sections);
        LivingEntity owner = getOwner();
        if (damage > 0.0F) {
            arc.setDamage(damage, owner);
        }
        level.addFreshEntity(arc);
    }

    private float damageMultiplier(LivingEntity owner) {
        if (owner instanceof Player player) {
            return (float) (PlayerTracker.getNinjaLevel(player) / 25.0D);
        }
        return 1.0F;
    }

    private static boolean isValidClientHandPosition(ServerPlayer sender, Vec3 position) {
        return Double.isFinite(position.x())
                && Double.isFinite(position.y())
                && Double.isFinite(position.z())
                && sender.distanceToSqr(position) <= MAX_CLIENT_HAND_DISTANCE_SQR;
    }

    private void discardSpear() {
        LivingEntity owner = getOwner();
        if (this.forceBowPoseSynced && owner != null) {
            ProcedureSync.EntityNBTTag.removeAndSync(owner, NarutomodModVariables.FORCE_BOW_POSE);
            this.forceBowPoseSynced = false;
        }
        discard();
    }
}
