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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.Chakra;
import net.narutomod.NarutomodMod;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class MindTransferEntity extends Entity {
    public static final String ACTIVE_ID_TAG = "MindTransferEntityIdKey";
    private static final int TRAVEL_TICKS = 60;
    private static final int MAX_ACTIVE_TICKS = 20 * 30;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(MindTransferEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(MindTransferEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BODY_ID = SynchedEntityData.defineId(MindTransferEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CONTROL_ACTIVE = SynchedEntityData.defineId(MindTransferEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private UUID targetUuid;
    @Nullable
    private UUID bodyUuid;
    private Vec3 targetPoint = Vec3.ZERO;
    private Vec3 travelStep = Vec3.ZERO;
    private double chakraBurnPerTick = 1.5D;

    public MindTransferEntity(EntityType<? extends MindTransferEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
        this.noPhysics = true;
    }

    public static boolean spawnFrom(LivingEntity owner, LivingEntity target, double chakraBurnPerTick) {
        if (!(owner.level() instanceof ServerLevel serverLevel) || !canTarget(owner, target)) {
            return false;
        }
        stopFor(owner);
        MindTransferEntity entity = ModEntityTypes.MIND_TRANSFER.get().create(serverLevel);
        if (entity == null) {
            return false;
        }
        entity.configure(owner, target, chakraBurnPerTick);
        serverLevel.addFreshEntity(entity);
        owner.getPersistentData().putInt(ACTIVE_ID_TAG, entity.getId());
        return true;
    }

    public static boolean stopFor(LivingEntity owner) {
        Entity entity = owner.level().getEntity(owner.getPersistentData().getInt(ACTIVE_ID_TAG));
        if (entity instanceof MindTransferEntity mindTransfer) {
            mindTransfer.discard();
            return true;
        }
        owner.getPersistentData().remove(ACTIVE_ID_TAG);
        return false;
    }

    public static boolean hasActiveFor(LivingEntity owner) {
        return owner.level().getEntity(owner.getPersistentData().getInt(ACTIVE_ID_TAG)) instanceof MindTransferEntity;
    }

    private static boolean canTarget(LivingEntity owner, LivingEntity target) {
        return target != owner && target.isAlive();
    }

    private void configure(LivingEntity owner, LivingEntity target, double chakraBurnPerTick) {
        setOwner(owner);
        setTarget(target);
        this.chakraBurnPerTick = Math.max(chakraBurnPerTick, 0.0D);
        this.targetPoint = target.position();
        this.travelStep = targetPoint.subtract(owner.position()).scale(1.0D / TRAVEL_TICKS);
        moveTo(owner.getX(), owner.getY() + owner.getBbHeight() * 0.5D, owner.getZ(), owner.getYRot(), owner.getXRot());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(TARGET_ID, -1);
        this.entityData.define(BODY_ID, -1);
        this.entityData.define(CONTROL_ACTIVE, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        LivingEntity owner = getOwnerEntity();
        LivingEntity target = getTargetEntity();
        if (owner == null || !owner.isAlive() || target == null || !canTarget(owner, target)) {
            discard();
            return;
        }
        if (!Chakra.pathway(owner).consume(this.chakraBurnPerTick)) {
            discard();
            return;
        }
        owner.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 3, 0, false, false));
        MindTransferSelfEntity body = getBodyEntity();
        if (body == null) {
            if (this.tickCount <= 2) {
                body = spawnBody(owner);
            }
            if (body == null) {
                discard();
                return;
            }
        }
        if (!body.isAlive() || body.getHealth() < body.getMaxHealth() * 0.2F) {
            discard();
            return;
        }
        if (this.tickCount == 1) {
            this.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                    ModSounds.SOUND_MINDTRANSFER.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        if (this.tickCount <= TRAVEL_TICKS) {
            setPos(position().add(this.travelStep));
            return;
        }
        if (!this.entityData.get(CONTROL_ACTIVE)) {
            this.entityData.set(CONTROL_ACTIVE, true);
            setPos(target.position());
        }
        moveTo(target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), target.getYRot(), target.getXRot());
        target.addEffect(new MobEffectInstance(ModEffects.PARALYSIS.get(), 22, 1, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, true));
        if (target instanceof Player targetPlayer) {
            targetPlayer.getAbilities().flying = false;
            targetPlayer.onUpdateAbilities();
        }
        if (this.tickCount > TRAVEL_TICKS + MAX_ACTIVE_TICKS) {
            discard();
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        LivingEntity owner = getOwnerEntity();
        LivingEntity target = getTargetEntity();
        super.remove(reason);
        if (!this.level().isClientSide) {
            if (owner != null) {
                MindTransferSelfEntity body = getBodyEntity();
                if (body != null) {
                    body.syncBackToOwner();
                }
                owner.getPersistentData().remove(ACTIVE_ID_TAG);
                owner.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 32, false, false));
                owner.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 600, 1, false, true));
                owner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600, 4, false, false));
            }
            if (target != null) {
                target.removeEffect(ModEffects.PARALYSIS.get());
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        if (tag.hasUUID("Target")) {
            this.targetUuid = tag.getUUID("Target");
        }
        if (tag.hasUUID("Body")) {
            this.bodyUuid = tag.getUUID("Body");
        }
        this.targetPoint = new Vec3(tag.getDouble("TargetX"), tag.getDouble("TargetY"), tag.getDouble("TargetZ"));
        this.travelStep = new Vec3(tag.getDouble("StepX"), tag.getDouble("StepY"), tag.getDouble("StepZ"));
        this.chakraBurnPerTick = tag.contains("ChakraBurnPerTick") ? tag.getDouble("ChakraBurnPerTick") : 1.5D;
        this.entityData.set(CONTROL_ACTIVE, tag.getBoolean("ControlActive"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        if (this.targetUuid != null) {
            tag.putUUID("Target", this.targetUuid);
        }
        if (this.bodyUuid != null) {
            tag.putUUID("Body", this.bodyUuid);
        }
        tag.putDouble("TargetX", this.targetPoint.x());
        tag.putDouble("TargetY", this.targetPoint.y());
        tag.putDouble("TargetZ", this.targetPoint.z());
        tag.putDouble("StepX", this.travelStep.x());
        tag.putDouble("StepY", this.travelStep.y());
        tag.putDouble("StepZ", this.travelStep.z());
        tag.putDouble("ChakraBurnPerTick", this.chakraBurnPerTick);
        tag.putBoolean("ControlActive", this.entityData.get(CONTROL_ACTIVE));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Nullable
    private LivingEntity getOwnerEntity() {
        return resolveLiving(this.entityData.get(OWNER_ID), this.ownerUuid, true);
    }

    @Nullable
    private LivingEntity getTargetEntity() {
        return resolveLiving(this.entityData.get(TARGET_ID), this.targetUuid, false);
    }

    @Nullable
    private MindTransferSelfEntity getBodyEntity() {
        int bodyId = this.entityData.get(BODY_ID);
        if (bodyId >= 0 && this.level().getEntity(bodyId) instanceof MindTransferSelfEntity body) {
            return body;
        }
        if (this.bodyUuid != null && this.level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(this.bodyUuid) instanceof MindTransferSelfEntity body) {
            setBody(body);
            return body;
        }
        return null;
    }

    @Nullable
    private MindTransferSelfEntity spawnBody(LivingEntity owner) {
        MindTransferSelfEntity body = MindTransferSelfEntity.spawnFor(owner);
        if (body != null) {
            setBody(body);
        }
        return body;
    }

    @Nullable
    private LivingEntity resolveLiving(int entityId, @Nullable UUID uuid, boolean owner) {
        if (entityId >= 0 && this.level().getEntity(entityId) instanceof LivingEntity living) {
            return living;
        }
        if (uuid != null && this.level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(uuid) instanceof LivingEntity living) {
            if (owner) {
                setOwner(living);
            } else {
                setTarget(living);
            }
            return living;
        }
        return null;
    }

    private void setOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
    }

    private void setTarget(LivingEntity target) {
        this.targetUuid = target.getUUID();
        this.entityData.set(TARGET_ID, target.getId());
    }

    private void setBody(MindTransferSelfEntity body) {
        this.bodyUuid = body.getUUID();
        this.entityData.set(BODY_ID, body.getId());
    }

    @Mod.EventBusSubscriber(modid = NarutomodMod.MODID)
    public static final class ForgeEvents {
        private ForgeEvents() {
        }

        @SubscribeEvent
        public static void onEntityTravelToDimension(EntityTravelToDimensionEvent event) {
            if (event.getEntity() instanceof LivingEntity living && hasActiveFor(living)) {
                stopFor(living);
                event.setCanceled(true);
            }
        }
    }
}
