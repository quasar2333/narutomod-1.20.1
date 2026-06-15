package net.narutomod.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.narutomod.Chakra;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class TransformationJutsuEntity extends PathfinderMob {
    public static final String ID_KEY = "TransformationEntityIdKey";
    public static final double CHAKRA_BURN_PER_SECOND = 5.0D;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(TransformationJutsuEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(TransformationJutsuEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private UUID targetUuid;
    private boolean cleanedUp;

    public TransformationJutsuEntity(EntityType<? extends TransformationJutsuEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoAi(true);
        this.setPersistenceRequired();
        this.xpReward = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.0F);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    public static boolean spawnFrom(LivingEntity owner, LivingEntity target) {
        if (!(owner.level() instanceof ServerLevel serverLevel) || !canTransformTo(owner, target)) {
            return false;
        }
        stopFor(owner);
        TransformationJutsuEntity entity = ModEntityTypes.TRANSFORMATION_JUTSU.get().create(serverLevel);
        if (entity == null) {
            return false;
        }
        entity.configure(owner, target);
        serverLevel.addFreshEntity(entity);
        owner.getPersistentData().putInt(ID_KEY, entity.getId());
        return true;
    }

    @Nullable
    public static LivingEntity findTarget(LivingEntity owner) {
        net.minecraft.world.phys.HitResult hit = net.narutomod.procedure.ProcedureUtils.objectEntityLookingAt(owner, 30.0D, 0.0D, true, false,
                target -> target instanceof LivingEntity living && canTransformTo(owner, living));
        return hit instanceof net.minecraft.world.phys.EntityHitResult entityHit
                && entityHit.getEntity() instanceof LivingEntity living ? living : null;
    }

    @Nullable
    public static TransformationJutsuEntity getActiveFor(LivingEntity owner) {
        int id = owner.getPersistentData().getInt(ID_KEY);
        Entity entity = owner.level().getEntity(id);
        return entity instanceof TransformationJutsuEntity transformation && transformation.isOwnedBy(owner) ? transformation : null;
    }

    public static int stopFor(LivingEntity owner) {
        TransformationJutsuEntity entity = getActiveFor(owner);
        if (entity != null) {
            entity.finishTransformation(true);
            return 1;
        }
        owner.getPersistentData().remove(ID_KEY);
        return 0;
    }

    public static boolean interceptAttack(LivingEntity owner, DamageSource source, float amount) {
        TransformationJutsuEntity entity = getActiveFor(owner);
        if (entity == null || !entity.isAlive()) {
            return false;
        }
        entity.hurt(source, amount);
        return true;
    }

    private static boolean canTransformTo(LivingEntity owner, LivingEntity target) {
        return target != owner && target.isAlive() && !(target instanceof TransformationJutsuEntity);
    }

    private void configure(LivingEntity owner, LivingEntity target) {
        setOwner(owner);
        setDisguiseTarget(target);
        copyDisguiseFrom(target);
        setAttributeBaseValue(Attributes.MAX_HEALTH, Math.max(owner.getMaxHealth(), 1.0D));
        setHealth(Math.min(owner.getHealth(), getMaxHealth()));
        mirrorOwnerPosition(owner);
        setCustomName(target.getName());
        setCustomNameVisible(true);
        poof(owner);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(TARGET_ID, -1);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            finishTransformation(false);
            return;
        }
        if (this.tickCount % 20 == 0 && !Chakra.pathway(owner).consume(CHAKRA_BURN_PER_SECOND)) {
            finishTransformation(true);
            return;
        }
        owner.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 22, 0, false, false));
        owner.setSprinting(false);
        mirrorOwnerPosition(owner);
        if (getHealth() < getMaxHealth() * 0.2F) {
            finishTransformation(true);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        LivingEntity owner = getOwner();
        if (owner != null && source.getEntity() == owner) {
            finishTransformation(true);
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        finishTransformation(true);
    }

    private void finishTransformation(boolean syncOwnerHealth) {
        if (this.cleanedUp) {
            discard();
            return;
        }
        this.cleanedUp = true;
        LivingEntity owner = getOwner();
        if (!this.level().isClientSide) {
            if (owner != null) {
                owner.getPersistentData().remove(ID_KEY);
                if (syncOwnerHealth) {
                    owner.setHealth(Math.max(Math.min(getHealth(), owner.getMaxHealth()), 1.0F));
                }
                poof(owner);
            }
            poof(this);
        }
        discard();
    }

    private void mirrorOwnerPosition(LivingEntity owner) {
        moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
        setYBodyRot(owner.getYRot());
        setYHeadRot(owner.getYHeadRot());
        setDeltaMovement(owner.getDeltaMovement());
        hurtMarked = true;
    }

    private void copyDisguiseFrom(LivingEntity target) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = target.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                setItemSlot(slot, stack.copy());
            }
        }
    }

    private void poof(Entity entity) {
        this.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                ModSounds.SOUND_POOF.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(),
                    80, 0.5D, 0.75D, 0.5D, 0.05D);
        }
    }

    public boolean isOwnedBy(LivingEntity entity) {
        return entity.getUUID().equals(this.ownerUuid) || getOwner() == entity;
    }

    @Nullable
    public LivingEntity getOwner() {
        return resolveLiving(this.entityData.get(OWNER_ID), this.ownerUuid, true);
    }

    @Nullable
    public LivingEntity getDisguiseTarget() {
        return resolveLiving(this.entityData.get(TARGET_ID), this.targetUuid, false);
    }

    @Nullable
    private LivingEntity resolveLiving(int entityId, @Nullable UUID uuid, boolean owner) {
        if (entityId >= 0) {
            Entity entity = this.level().getEntity(entityId);
            if (entity instanceof LivingEntity living) {
                return living;
            }
        }
        if (uuid != null && this.level() instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(uuid);
            if (entity instanceof LivingEntity living) {
                if (owner) {
                    setOwner(living);
                } else {
                    setDisguiseTarget(living);
                }
                return living;
            }
        }
        return null;
    }

    private void setOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
    }

    private void setDisguiseTarget(LivingEntity target) {
        this.targetUuid = target.getUUID();
        this.entityData.set(TARGET_ID, target.getId());
    }

    private void setAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        if (tag.hasUUID("Target")) {
            this.targetUuid = tag.getUUID("Target");
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        if (this.targetUuid != null) {
            tag.putUUID("Target", this.targetUuid);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
