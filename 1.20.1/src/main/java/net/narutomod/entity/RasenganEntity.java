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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.NarutomodModVariables;
import net.narutomod.item.JutsuItem;
import net.narutomod.item.NinjutsuItem;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureSync;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class RasenganEntity extends Entity {
    public static final int GROW_TIME = 30;
    private static final int MAX_LIFETIME = 200;
    private static final float BASE_SIZE = 0.35F;
    private static final int CLIENT_HAND_POSITION_TTL_TICKS = 5;
    private static final double MAX_CLIENT_HAND_DISTANCE_SQR = 6.0D * 6.0D;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(RasenganEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> MODEL_SCALE = SynchedEntityData.defineId(RasenganEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FULL_SCALE = SynchedEntityData.defineId(RasenganEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> SENJUTSU_DAMAGE = SynchedEntityData.defineId(RasenganEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID ownerUuid;
    private float dimensionsScale = 0.1F;
    private boolean hasHit;
    private boolean forceBowPoseSynced;
    @Nullable
    private Vec3 clientHandPosition;
    private long clientHandPositionGameTime = Long.MIN_VALUE;

    public RasenganEntity(EntityType<? extends RasenganEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    public void configureAttached(LivingEntity owner, float fullScale) {
        configureAttached(owner, fullScale, false);
    }

    public void configureAttached(LivingEntity owner, float fullScale, boolean senjutsuDamage) {
        setOwner(owner);
        setFullScale(fullScale);
        setSenjutsuDamage(senjutsuDamage);
        setRasenganScale(0.1F);
        snapToOwnerHand(owner);
    }

    public void configureStationary(float fullScale) {
        clearOwner();
        setFullScale(fullScale);
        setSenjutsuDamage(false);
        setRasenganScale(fullScale);
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

    public float getFullScale() {
        return this.entityData.get(FULL_SCALE);
    }

    public void setFullScale(float fullScale) {
        this.entityData.set(FULL_SCALE, Mth.clamp(fullScale, 0.1F, 32.0F));
    }

    public float getRasenganScale() {
        return this.entityData.get(MODEL_SCALE);
    }

    public void acceptClientHandPosition(ServerPlayer sender, Vec3 position) {
        LivingEntity owner = getOwner();
        if (owner != sender || !isValidClientHandPosition(sender, position)) {
            return;
        }
        this.clientHandPosition = position;
        this.clientHandPositionGameTime = this.level().getGameTime();
        moveToHandPosition(position, sender);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(MODEL_SCALE, 0.1F);
        this.entityData.define(FULL_SCALE, 1.0F);
        this.entityData.define(SENJUTSU_DAMAGE, false);
    }

    @Override
    public void tick() {
        super.tick();
        this.clearFire();
        if (this.tickCount > MAX_LIFETIME) {
            discardRasengan();
            return;
        }

        LivingEntity owner = getOwner();
        if (owner != null) {
            if (!owner.isAlive()) {
                discardRasengan();
                return;
            }
            if (shouldDiscardForHeldItem(owner)) {
                discardRasengan();
                return;
            }
            snapToOwnerHand(owner);
        }

        if (!this.level().isClientSide) {
            syncForceBowPose(owner);
            growTowardFullScale();
            playLoopSound();
            spawnHandSmoke(owner);
            breakTouchedBlocks();
            if (this.tickCount > GROW_TIME) {
                hitNearbyTarget(owner);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        setFullScale(tag.contains("FullScale") ? tag.getFloat("FullScale") : 1.0F);
        setRasenganScale(tag.contains("Scale") ? tag.getFloat("Scale") : 0.1F);
        setSenjutsuDamage(tag.getBoolean("SenjutsuDamage"));
        this.tickCount = tag.getInt("Life");
        this.hasHit = tag.getBoolean("HasHit");
        this.forceBowPoseSynced = tag.getBoolean("ForceBowPoseSynced");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putFloat("FullScale", getFullScale());
        tag.putFloat("Scale", getRasenganScale());
        tag.putBoolean("SenjutsuDamage", isSenjutsuDamage());
        tag.putInt("Life", this.tickCount);
        tag.putBoolean("HasHit", this.hasHit);
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

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(this.dimensionsScale);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (MODEL_SCALE.equals(key)) {
            this.dimensionsScale = Mth.clamp(getRasenganScale(), 0.03F, 32.0F);
            refreshDimensions();
        }
    }

    private void setOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
    }

    private void clearOwner() {
        this.ownerUuid = null;
        this.entityData.set(OWNER_ID, -1);
    }

    private void setRasenganScale(float scale) {
        this.dimensionsScale = Mth.clamp(scale, 0.03F, 32.0F);
        this.entityData.set(MODEL_SCALE, this.dimensionsScale);
        refreshDimensions();
    }

    private boolean isSenjutsuDamage() {
        return this.entityData.get(SENJUTSU_DAMAGE);
    }

    private void setSenjutsuDamage(boolean senjutsuDamage) {
        this.entityData.set(SENJUTSU_DAMAGE, senjutsuDamage);
    }

    private void growTowardFullScale() {
        float fullScale = getFullScale();
        float growth = Mth.clamp(fullScale * this.tickCount / (float)GROW_TIME, 0.1F, fullScale);
        setRasenganScale(this.tickCount <= GROW_TIME ? growth : fullScale);
    }

    private void snapToOwnerHand(LivingEntity owner) {
        if (owner.swinging) {
            float swing = owner.getAttackAnim(1.0F);
            Vec3 hand = owner.getEyePosition()
                    .add(owner.getLookAngle().scale(2.0D + 3.0D * Math.sin(swing * Math.PI)));
            moveToHandPosition(hand, owner);
            return;
        }
        if (!this.level().isClientSide && this.clientHandPosition != null
                && this.level().getGameTime() - this.clientHandPositionGameTime <= CLIENT_HAND_POSITION_TTL_TICKS) {
            moveToHandPosition(this.clientHandPosition, owner);
            return;
        }

        Vec3 look = owner.getLookAngle();
        double yaw = Math.toRadians(owner.getYRot());
        Vec3 right = new Vec3(Math.cos(yaw), 0.0D, Math.sin(yaw));
        float scale = getRasenganScale();
        Vec3 hand = owner.position()
                .add(look.scale(0.75D + scale * 0.12D))
                .add(right.scale(-0.35D))
                .add(0.0D, owner.getBbHeight() * 0.68D - BASE_SIZE * scale * 0.5D, 0.0D);
        moveToHandPosition(hand, owner);
    }

    private static boolean isValidClientHandPosition(ServerPlayer sender, Vec3 position) {
        return Double.isFinite(position.x())
                && Double.isFinite(position.y())
                && Double.isFinite(position.z())
                && sender.distanceToSqr(position) <= MAX_CLIENT_HAND_DISTANCE_SQR;
    }

    private void moveToHandPosition(Vec3 hand, LivingEntity owner) {
        this.setDeltaMovement(Vec3.ZERO);
        this.moveTo(hand.x(), hand.y(), hand.z(), owner.getYRot(), owner.getXRot());
    }

    private boolean shouldDiscardForHeldItem(LivingEntity owner) {
        return !owner.getMainHandItem().isEmpty() && !(owner.getMainHandItem().getItem() instanceof JutsuItem);
    }

    private void playLoopSound() {
        if (this.tickCount % 15 == 0) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.SOUND_RASENGAN_DURING.get(), SoundSource.NEUTRAL, 0.2F, 1.0F);
        }
    }

    private void syncForceBowPose(@Nullable LivingEntity owner) {
        if (!this.forceBowPoseSynced && owner != null) {
            ProcedureSync.EntityNBTTag.setAndSync(owner, NarutomodModVariables.FORCE_BOW_POSE, true);
            this.forceBowPoseSynced = true;
        }
    }

    private void spawnHandSmoke(@Nullable LivingEntity owner) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 source = owner == null ? this.position().add(0.0D, BASE_SIZE * getRasenganScale(), 0.0D) : this.position().add(0.0D, BASE_SIZE * getRasenganScale() * 0.5D, 0.0D);
        serverLevel.sendParticles(
                ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0x10FFFFFF, 5, 0, 0, -1, 4),
                source.x(),
                source.y(),
                source.z(),
                3,
                0.06D,
                0.02D,
                0.06D,
                0.02D
        );
    }

    private void hitNearbyTarget(@Nullable LivingEntity owner) {
        if (this.hasHit || owner == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        float scale = getRasenganScale();
        AABB hitBox = this.getBoundingBox().inflate(0.05D);
        for (Entity target : serverLevel.getEntities(this, hitBox, entity -> entity instanceof LivingEntity && entity != owner && entity.isAlive())) {
            float damage = 15.0F + getFullScale() * getFullScale() * 10.0F;
            if (target.hurt(isSenjutsuDamage()
                    ? ModDamageTypes.senjutsu(this.level(), this, owner)
                    : ModDamageTypes.ninjutsu(this.level(), this, owner), damage)) {
                this.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL, 1.0F, this.random.nextFloat() * 0.5F + 0.5F);
                Vec3 pushed = ProcedureUtils.pushEntity(owner, target, 20.0D, 2.0F);
                spawnWhirlpoolTrail(serverLevel, owner, pushed);
            }
            this.hasHit = true;
            discardRasengan();
            return;
        }
    }

    private void breakTouchedBlocks() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        AABB bounds = getScaledBounds();
        int minX = Mth.floor(bounds.minX);
        int minY = Mth.floor(bounds.minY);
        int minZ = Mth.floor(bounds.minZ);
        int maxX = Mth.floor(bounds.maxX);
        int maxY = Mth.floor(bounds.maxY);
        int maxZ = Mth.floor(bounds.maxZ);
        for (BlockPos mutablePos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            BlockPos pos = mutablePos.immutable();
            VoxelShape shape = serverLevel.getBlockState(pos).getCollisionShape(serverLevel, pos);
            if (!shape.isEmpty() && shape.bounds().move(pos).intersects(bounds)) {
                ProcedureUtils.breakBlockAndDropWithChance(serverLevel, pos, 5.0F, 1.0F, 0.3F);
            }
        }
    }

    private AABB getScaledBounds() {
        return this.getBoundingBox();
    }

    private void spawnWhirlpoolTrail(ServerLevel level, LivingEntity owner, Vec3 pushed) {
        if (pushed.lengthSqr() <= 1.0E-6D) {
            return;
        }
        Vec3 start = owner.getEyePosition().add(owner.getLookAngle());
        Vec3 direction = pushed.normalize();
        double length = pushed.length();
        for (int i = 1; i <= 100; i++) {
            double distance = i * length * 0.05D;
            Vec3 velocity = direction.scale(distance);
            level.sendParticles(
                    ModParticleTypes.options(NarutoParticleKind.WHIRLPOOL, 0x80B9FFFD, (int)(distance * 20.0D), (int)distance, 0xF0),
                    start.x(),
                    start.y(),
                    start.z(),
                    0,
                    velocity.x(),
                    velocity.y(),
                    velocity.z(),
                    1.0D
            );
        }
    }

    private void discardRasengan() {
        LivingEntity owner = getOwner();
        if (owner instanceof net.minecraft.world.entity.player.Player player) {
            NinjutsuItem.clearRasenganSize(player);
        }
        if (this.forceBowPoseSynced && owner != null) {
            ProcedureSync.EntityNBTTag.removeAndSync(owner, NarutomodModVariables.FORCE_BOW_POSE);
            this.forceBowPoseSynced = false;
        }
        discard();
    }
}
