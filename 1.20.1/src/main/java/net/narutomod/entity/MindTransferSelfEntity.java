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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class MindTransferSelfEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(MindTransferSelfEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;
    private boolean cleanedUp;

    public MindTransferSelfEntity(EntityType<? extends MindTransferSelfEntity> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.setNoAi(true);
        this.xpReward = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.0F);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 0.0D)
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Nullable
    public static MindTransferSelfEntity spawnFor(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        MindTransferSelfEntity body = ModEntityTypes.MIND_TRANSFER_SELF.get().create(serverLevel);
        if (body == null) {
            return null;
        }
        body.configure(owner);
        serverLevel.addFreshEntity(body);
        return body;
    }

    public boolean isOwnedBy(LivingEntity entity) {
        return entity.getUUID().equals(this.ownerUuid) || getOwner() == entity;
    }

    public void syncBackToOwner() {
        LivingEntity owner = getOwner();
        if (owner != null && owner.isAlive()) {
            owner.setHealth(Math.max(0.0F, Math.min(getHealth(), owner.getMaxHealth())));
        }
        discardBody(false);
    }

    private void configure(LivingEntity owner) {
        setOwner(owner);
        copyOwnerEquipment(owner);
        copyOwnerAttributes(owner);
        setCustomName(owner.getDisplayName());
        setLeftHanded(owner.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT);
        moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
        setYBodyRot(owner.yBodyRot);
        setYHeadRot(owner.getYHeadRot());
    }

    private void copyOwnerEquipment(LivingEntity owner) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = owner.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                setItemSlot(slot, stack.copy());
            }
        }
    }

    private void copyOwnerAttributes(LivingEntity owner) {
        setAttributeBaseValue(Attributes.MOVEMENT_SPEED, 0.0D);
        setAttributeBaseValue(Attributes.ARMOR, ProcedureUtils.getArmorValue(owner));
        setAttributeBaseValue(Attributes.ATTACK_DAMAGE, Math.max(ProcedureUtils.getModifiedAttackDamage(owner), 1.0D));
        setAttributeBaseValue(Attributes.MAX_HEALTH, Math.max(owner.getMaxHealth(), 1.0D));
        setHealth(Math.max(1.0F, Math.min(owner.getHealth(), getMaxHealth())));
    }

    private void setAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_ID, -1);
    }

    @Override
    public void tick() {
        this.fallDistance = 0.0F;
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
        LivingEntity owner = getOwner();
        if (CloneOwnerState.isUnavailable(owner)) {
            discardBody(false);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        LivingEntity owner = getOwner();
        if (owner != null && attacker == owner) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
    }

    private void discardBody(boolean poof) {
        if (this.cleanedUp) {
            discard();
            return;
        }
        this.cleanedUp = true;
        if (!this.level().isClientSide && poof) {
            this.level().playSound(null, getX(), getY(), getZ(),
                    ModSounds.SOUND_POOF.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.0D, getZ(),
                        120, 0.5D, 0.75D, 0.5D, 0.05D);
            }
        }
        discard();
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

    private void setOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        LivingEntity owner = getOwner();
        if (CloneFamilyAlliances.hasSameOwner(entity, owner)) {
            return true;
        }
        return super.isAlliedTo(entity);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
