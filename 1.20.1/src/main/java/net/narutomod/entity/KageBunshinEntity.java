package net.narutomod.entity;

import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.Chakra;
import net.narutomod.item.JutsuItem;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class KageBunshinEntity extends PathfinderMob {
    public static final String ID_KEY = "KageBunshinEntityIds";
    private static final UUID MAX_HEALTH_MODIFIER_ID = UUID.fromString("308fe1ce-1850-4b1a-803c-ed265df4e3ce");
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(KageBunshinEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;
    private double storedChakra;
    private int collectedExperience;
    private boolean cleanedUp;

    public KageBunshinEntity(EntityType<? extends KageBunshinEntity> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.xpReward = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.0F);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D);
    }

    public static boolean spawnFrom(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (owner instanceof Player player && Chakra.pathway(player).getAmount() < 200.0D) {
            Chakra.pathway(player).warningDisplay();
            return false;
        }

        KageBunshinEntity clone = ModEntityTypes.KAGE_BUNSHIN.get().create(serverLevel);
        if (clone == null) {
            return false;
        }
        clone.configure(owner);
        serverLevel.addFreshEntity(clone);
        List<KageBunshinEntity> clones = aliveClones(owner);
        clones.add(clone);
        splitOwnerResources(owner, clones);
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                ModSounds.SOUND_KAGEBUNSHIN.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        return true;
    }

    public static int removeAllFor(LivingEntity owner) {
        List<KageBunshinEntity> clones = aliveClones(owner);
        for (KageBunshinEntity clone : clones) {
            clone.discardClone(true);
        }
        refreshOwnerCloneState(owner);
        return clones.size();
    }

    public boolean isOwnedBy(LivingEntity entity) {
        return entity.getUUID().equals(this.ownerUuid) || getOwner() == entity;
    }

    private void configure(LivingEntity owner) {
        setOwner(owner);
        setCustomName(owner.getDisplayName());
        setLeftHanded(owner.getMainArm() == HumanoidArm.LEFT);
        copyOwnerEquipment(owner);
        copyOwnerAttributes(owner);
        Vec3 offset = Vec3.directionFromRotation(0.0F, owner.getYRot() + this.random.nextFloat() * 120.0F - 60.0F)
                .scale(1.2D);
        moveTo(owner.getX() + offset.x(), owner.getY(), owner.getZ() + offset.z(), owner.getYRot(), owner.getXRot());
        setTarget(owner instanceof Mob mob ? mob.getTarget() : null);
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
        setAttributeBaseValue(Attributes.MOVEMENT_SPEED, Math.max(ProcedureUtils.getModifiedSpeed(owner) * 3.5D, 0.5D));
        setAttributeBaseValue(Attributes.ARMOR, ProcedureUtils.getArmorValue(owner));
        setAttributeBaseValue(Attributes.ATTACK_DAMAGE, Math.max(ProcedureUtils.getModifiedAttackDamage(owner), 3.0D));
        setAttributeBaseValue(Attributes.MAX_HEALTH, Math.max(owner.getHealth(), 1.0F));
        setHealth(getMaxHealth());
    }

    private void setAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 0.9D, true));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_ID, -1);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        LivingEntity owner = getOwner();
        if (CloneOwnerState.isUnavailable(owner)) {
            discardClone(false);
            return;
        }
        if (this.tickCount == 1) {
            activateHeldJutsuOnSpawn();
        }
        if (this.tickCount % 10 == 0) {
            defendOwner(owner);
            followOwner(owner);
        }
        if (this.tickCount % 40 == 0) {
            refreshOwnerCloneState(owner);
        }
    }

    private void activateHeldJutsuOnSpawn() {
        ItemStack stack = getMainHandItem();
        if (stack.getItem() instanceof JutsuItem jutsuItem) {
            jutsuItem.activateCloneHeldJutsus(stack, this);
        }
    }

    private void defendOwner(LivingEntity owner) {
        LivingEntity nextTarget = null;
        if (owner instanceof Mob mob && mob.getTarget() != null && !isAlliedTo(mob.getTarget())) {
            nextTarget = mob.getTarget();
        }
        if (nextTarget == null) {
            LivingEntity lastHurtBy = owner.getLastHurtByMob();
            if (lastHurtBy != null && lastHurtBy.isAlive() && !isAlliedTo(lastHurtBy)
                    && owner.tickCount - owner.getLastHurtByMobTimestamp() < 200) {
                nextTarget = lastHurtBy;
            }
        }
        if (nextTarget == null) {
            LivingEntity lastHurt = owner.getLastHurtMob();
            if (lastHurt != null && lastHurt.isAlive() && !isAlliedTo(lastHurt)
                    && owner.tickCount - owner.getLastHurtMobTimestamp() < 200) {
                nextTarget = lastHurt;
            }
        }
        if (nextTarget != null) {
            setTarget(nextTarget);
        } else if (getTarget() != null && !getTarget().isAlive()) {
            setTarget(null);
        }
    }

    private void followOwner(LivingEntity owner) {
        if (getTarget() != null) {
            return;
        }
        CloneOwnerMovement.followWithSpacing(this, owner, 0.6D, 3.0D, 12.0D);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        LivingEntity owner = getOwner();
        if (owner != null && attacker == owner) {
            discardClone(true);
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        discardClone(true);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return ProcedureUtils.attackEntityAsMob(this, target);
    }

    @Override
    public void awardKillScore(Entity killed, int score, DamageSource source) {
        super.awardKillScore(killed, score, source);
        this.collectedExperience += CloneExperienceRewards.collectFromKill(this, killed);
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        LivingEntity owner = getOwner();
        if (CloneFamilyAlliances.hasSameOwner(entity, owner)) {
            return true;
        }
        return super.isAlliedTo(entity);
    }

    private void discardClone(boolean refundChakra) {
        if (this.cleanedUp) {
            discard();
            return;
        }
        this.cleanedUp = true;
        LivingEntity owner = getOwner();
        if (!this.level().isClientSide) {
            poof();
            transferCollectedExperience(owner);
            if (refundChakra && owner instanceof Player player && this.storedChakra > 0.0D) {
                Chakra.pathway(player).consume(-this.storedChakra * 0.8D, false);
            }
            if (owner != null) {
                refreshOwnerCloneState(owner);
            }
        }
        discard();
    }

    private void transferCollectedExperience(@Nullable LivingEntity owner) {
        CloneExperienceRewards.transferToOwner(owner, this.collectedExperience);
        this.collectedExperience = 0;
    }

    private void poof() {
        this.level().playSound(null, getX(), getY(), getZ(),
                ModSounds.SOUND_POOF.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.0D, getZ(),
                    120, 0.5D, 0.75D, 0.5D, 0.05D);
        }
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

    private void setOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
    }

    private static List<KageBunshinEntity> aliveClones(LivingEntity owner) {
        List<KageBunshinEntity> clones = new ArrayList<>();
        int[] ids = owner.getPersistentData().getIntArray(ID_KEY);
        for (int id : ids) {
            Entity entity = owner.level().getEntity(id);
            if (entity instanceof KageBunshinEntity clone && clone.isAlive() && clone.isOwnedBy(owner)) {
                clones.add(clone);
            }
        }
        return clones;
    }

    private static void splitOwnerResources(LivingEntity owner, List<KageBunshinEntity> clones) {
        Chakra.Pathway chakra = owner instanceof Player player ? Chakra.pathway(player) : null;
        if (chakra != null && !clones.isEmpty()) {
            chakra.consume(chakra.getAmount() / (clones.size() + 1.0D));
        }
        applyOwnerMaxHealthModifier(owner, clones.size());
        if (owner.getHealth() > owner.getMaxHealth()) {
            owner.setHealth(owner.getMaxHealth());
        }
        for (KageBunshinEntity clone : clones) {
            clone.copyOwnerAttributes(owner);
            clone.storedChakra = chakra != null ? chakra.getAmount() : 0.0D;
        }
        writeCloneIds(owner, clones);
    }

    private static void refreshOwnerCloneState(LivingEntity owner) {
        List<KageBunshinEntity> clones = aliveClones(owner);
        applyOwnerMaxHealthModifier(owner, clones.size());
        writeCloneIds(owner, clones);
    }

    private static void applyOwnerMaxHealthModifier(LivingEntity owner, int cloneCount) {
        AttributeInstance maxHealth = owner.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        if (maxHealth.getModifier(MAX_HEALTH_MODIFIER_ID) != null) {
            maxHealth.removeModifier(MAX_HEALTH_MODIFIER_ID);
        }
        if (cloneCount > 0) {
            maxHealth.addTransientModifier(new AttributeModifier(
                    MAX_HEALTH_MODIFIER_ID,
                    "kagebunshin.maxhealth",
                    1.0D / (cloneCount + 1.0D) - 1.0D,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
            owner.setHealth(Math.min(owner.getHealth(), owner.getMaxHealth()));
        }
    }

    private static void writeCloneIds(LivingEntity owner, List<KageBunshinEntity> clones) {
        int[] ids = new int[clones.size()];
        for (int i = 0; i < clones.size(); i++) {
            ids[i] = clones.get(i).getId();
        }
        owner.getPersistentData().putIntArray(ID_KEY, ids);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        this.storedChakra = tag.getDouble("StoredChakra");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putDouble("StoredChakra", this.storedChakra);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
