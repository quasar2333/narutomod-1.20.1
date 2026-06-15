package net.narutomod.entity;

import java.util.List;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.Chakra;
import net.narutomod.NarutomodMod;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class ShadowImitationEntity extends Entity {
    public static final String ACTIVE_IDS_TAG = "ShadowImitationEntityIdKey";
    private static final double MAX_DISTANCE = 30.0D;
    private static final float LEGACY_MOVE_RELATIVE_STRENGTH = 0.2F;
    private static final double LEGACY_JUMP_VELOCITY = 0.42D;
    private static final double LEGACY_GRAVITY = -0.08D;
    private static final double LEGACY_HORIZONTAL_DAMPING = 0.1D;
    private static final double LEGACY_VERTICAL_DAMPING = 0.98D;
    private static final double LEGACY_ATTACK_REACH = 3.0D;
    private static final int MIRRORED_ATTACK_COOLDOWN_TICKS = 6;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(ShadowImitationEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(ShadowImitationEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private UUID targetUuid;
    private double chakraBurnPerSecond = 50.0D;
    private int mirroredAttackCooldown;

    public ShadowImitationEntity(EntityType<? extends ShadowImitationEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
        this.noPhysics = true;
    }

    public static boolean spawnFrom(LivingEntity owner, LivingEntity target, double baseChakraBurnPerSecond) {
        if (!(owner.level() instanceof ServerLevel serverLevel) || !canTarget(owner, target) || hasActiveForTarget(owner, target)) {
            return false;
        }
        ShadowImitationEntity entity = ModEntityTypes.SHADOW_IMITATION.get().create(serverLevel);
        if (entity == null) {
            return false;
        }
        entity.configure(owner, target, baseChakraBurnPerSecond + ProcedureUtils.getPunchDamage(target));
        serverLevel.addFreshEntity(entity);
        addActiveId(owner, entity.getId());
        return true;
    }

    public static int stopOwnedNear(LivingEntity owner, double radius) {
        List<ShadowImitationEntity> shadows = owner.level().getEntitiesOfClass(
                ShadowImitationEntity.class,
                owner.getBoundingBox().inflate(radius),
                shadow -> shadow.isOwnedBy(owner));
        for (ShadowImitationEntity shadow : shadows) {
            shadow.discard();
        }
        return shadows.size();
    }

    public static boolean hasAnyActiveFor(LivingEntity owner) {
        int[] ids = owner.getPersistentData().getIntArray(ACTIVE_IDS_TAG);
        for (int id : ids) {
            if (owner.level().getEntity(id) instanceof ShadowImitationEntity) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasActiveForTarget(LivingEntity owner, LivingEntity target) {
        int[] ids = owner.getPersistentData().getIntArray(ACTIVE_IDS_TAG);
        for (int id : ids) {
            Entity entity = owner.level().getEntity(id);
            if (entity instanceof ShadowImitationEntity shadow && shadow.getTargetEntity() == target) {
                return true;
            }
        }
        return false;
    }

    private static void addActiveId(LivingEntity owner, int id) {
        int[] oldIds = owner.getPersistentData().getIntArray(ACTIVE_IDS_TAG);
        int[] newIds = java.util.Arrays.copyOf(oldIds, oldIds.length + 1);
        newIds[oldIds.length] = id;
        owner.getPersistentData().putIntArray(ACTIVE_IDS_TAG, newIds);
    }

    private static void removeActiveId(LivingEntity owner, int id) {
        int[] oldIds = owner.getPersistentData().getIntArray(ACTIVE_IDS_TAG);
        if (oldIds.length <= 1) {
            owner.getPersistentData().remove(ACTIVE_IDS_TAG);
            return;
        }
        int[] newIds = java.util.Arrays.stream(oldIds).filter(candidate -> candidate != id).toArray();
        owner.getPersistentData().putIntArray(ACTIVE_IDS_TAG, newIds);
    }

    private static boolean canTarget(LivingEntity owner, LivingEntity target) {
        return target != owner && canMaintainTarget(target) && owner.distanceTo(target) <= MAX_DISTANCE;
    }

    private static boolean canMaintainTarget(LivingEntity target) {
        return target.isAlive() && !target.getPersistentData().getBoolean("kamui_intangible");
    }

    private void configure(LivingEntity owner, LivingEntity target, double chakraBurnPerSecond) {
        setOwner(owner);
        setTarget(target);
        this.chakraBurnPerSecond = Math.max(chakraBurnPerSecond, 0.0D);
        moveToOwner(owner);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(TARGET_ID, -1);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        LivingEntity owner = getOwnerEntity();
        LivingEntity target = getTargetEntity();
        if (owner == null || !owner.isAlive() || target == null || !canMaintainTarget(target) || !canTargetBeSeen(owner, target)) {
            discard();
            return;
        }
        moveToOwner(owner);
        if (this.tickCount == 1) {
            this.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                    ModSounds.SOUND_SHADOW_SFX.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            haltTargetInput(target, true);
        }
        if (this.tickCount % 20 == 1 && !Chakra.pathway(owner).consume(this.chakraBurnPerSecond)) {
            discard();
            return;
        }
        if (this.mirroredAttackCooldown > 0) {
            this.mirroredAttackCooldown--;
        }
        if (target instanceof ServerPlayer) {
            holdPlayerTargetFallback(owner, target);
        } else {
            mirrorOwnerInputToTarget(owner, target);
        }
        spawnShadowParticles(owner, target);
    }

    @Override
    public void remove(RemovalReason reason) {
        LivingEntity owner = getOwnerEntity();
        LivingEntity target = getTargetEntity();
        super.remove(reason);
        if (!this.level().isClientSide) {
            if (owner != null) {
                removeActiveId(owner, getId());
            }
            if (target != null) {
                haltTargetInput(target, false);
                if (target instanceof ServerPlayer) {
                    target.removeEffect(ModEffects.PARALYSIS.get());
                }
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
        this.chakraBurnPerSecond = tag.contains("ChakraBurnPerSecond") ? tag.getDouble("ChakraBurnPerSecond") : 50.0D;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        if (this.targetUuid != null) {
            tag.putUUID("Target", this.targetUuid);
        }
        tag.putDouble("ChakraBurnPerSecond", this.chakraBurnPerSecond);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public boolean isOwnedBy(LivingEntity entity) {
        return entity.getUUID().equals(this.ownerUuid) || getOwnerEntity() == entity;
    }

    @Nullable
    public LivingEntity getOwnerForRender() {
        return getOwnerEntity();
    }

    @Nullable
    public LivingEntity getTargetForRender() {
        return getTargetEntity();
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

    private void moveToOwner(LivingEntity owner) {
        moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
    }

    private boolean canTargetBeSeen(LivingEntity owner, LivingEntity target) {
        Vec3 start = owner.getEyePosition();
        Vec3 end = target.getEyePosition();
        HitResult eyeHit = this.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (eyeHit.getType() == HitResult.Type.MISS) {
            return true;
        }
        Vec3 lowEnd = target.position().add(0.0D, 0.2D, 0.0D);
        HitResult lowHit = this.level().clip(new ClipContext(start, lowEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return lowHit.getType() == HitResult.Type.MISS;
    }

    private void copyOwnerFacing(LivingEntity owner, LivingEntity target) {
        target.setYRot(owner.getYRot());
        target.setYHeadRot(owner.getYHeadRot());
        target.setYBodyRot(owner.yBodyRot);
        target.setXRot(owner.getXRot());
    }

    private void holdPlayerTargetFallback(LivingEntity owner, LivingEntity target) {
        copyOwnerFacing(owner, target);
        target.addEffect(new MobEffectInstance(ModEffects.PARALYSIS.get(), 22, 1, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 22, 4, false, false));
        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
    }

    private static void haltTargetInput(LivingEntity target, boolean halt) {
        if (target instanceof Mob mob) {
            mob.setNoAi(halt);
            if (halt) {
                mob.getNavigation().stop();
            }
        }
    }

    private void mirrorOwnerInputToTarget(LivingEntity owner, LivingEntity target) {
        copyOwnerFacing(owner, target);
        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        if (shouldMirrorJump(owner, target)) {
            Vec3 motion = target.getDeltaMovement();
            target.setDeltaMovement(motion.x(), LEGACY_JUMP_VELOCITY, motion.z());
        }
        target.moveRelative(LEGACY_MOVE_RELATIVE_STRENGTH, new Vec3(owner.xxa, 0.0D, owner.zza));
        target.move(MoverType.SELF, target.getDeltaMovement());
        Vec3 moved = target.getDeltaMovement();
        if (!target.isNoGravity()) {
            moved = moved.add(0.0D, LEGACY_GRAVITY, 0.0D);
        }
        target.setDeltaMovement(
                moved.x() * LEGACY_HORIZONTAL_DAMPING,
                moved.y() * LEGACY_VERTICAL_DAMPING,
                moved.z() * LEGACY_HORIZONTAL_DAMPING);
        target.setShiftKeyDown(owner.isShiftKeyDown());
        target.hurtMarked = true;
        mirrorOwnerAttack(owner, target);
    }

    private static boolean shouldMirrorJump(LivingEntity owner, LivingEntity target) {
        return target.onGround() && owner.getDeltaMovement().y() > 0.08D && owner.getY() > owner.yOld;
    }

    private void mirrorOwnerAttack(LivingEntity owner, LivingEntity target) {
        if (!owner.swinging || this.mirroredAttackCooldown > 0) {
            return;
        }
        target.swing(InteractionHand.MAIN_HAND);
        this.mirroredAttackCooldown = MIRRORED_ATTACK_COOLDOWN_TICKS;
        if (!(target instanceof Mob mob)) {
            return;
        }
        HitResult hit = ProcedureUtils.objectEntityLookingAt(target, LEGACY_ATTACK_REACH, 0.0D, true, false,
                candidate -> candidate instanceof LivingEntity living && living.isAlive());
        if (hit instanceof EntityHitResult entityHit) {
            mob.doHurtTarget(entityHit.getEntity());
        }
    }

    private void spawnShadowParticles(LivingEntity owner, LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel) || this.tickCount % 3 != 0) {
            return;
        }
        Vec3 start = owner.position().add(0.0D, 0.04D, 0.0D);
        Vec3 delta = target.position().subtract(start);
        int steps = Math.max((int)(delta.length() * 2.0D), 1);
        for (int i = 0; i <= steps; i++) {
            Vec3 point = start.add(delta.scale(i / (double)steps));
            serverLevel.sendParticles(
                    ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0xB0000000, 3, 10, 0, -1, 1),
                    point.x(),
                    point.y(),
                    point.z(),
                    2,
                    0.08D,
                    0.01D,
                    0.08D,
                    0.0D);
        }
    }

    @Mod.EventBusSubscriber(modid = NarutomodMod.MODID)
    public static final class ForgeEvents {
        private ForgeEvents() {
        }

        @SubscribeEvent
        public static void onEntityTravelToDimension(EntityTravelToDimensionEvent event) {
            if (event.getEntity() instanceof LivingEntity living && hasAnyActiveFor(living)) {
                stopOwnedNear(living, 128.0D);
            }
        }
    }
}
