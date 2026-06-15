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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
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
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class ExplosiveCloneEntity extends PathfinderMob {
    public static final String ID_KEY = "ExplosiveCloneEntityIds";
    private static final int FUSE_TICKS = 30;
    private static final int MAX_LIFE_TICKS = 600;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(ExplosiveCloneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IGNITED = SynchedEntityData.defineId(ExplosiveCloneEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> IGNITION_TICK = SynchedEntityData.defineId(ExplosiveCloneEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;
    private int collectedExperience;
    private boolean cleanedUp;

    public ExplosiveCloneEntity(EntityType<? extends ExplosiveCloneEntity> entityType, Level level) {
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
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.6D);
    }

    public static boolean spawnFrom(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        ExplosiveCloneEntity clone = ModEntityTypes.EXPLOSIVE_CLONE.get().create(serverLevel);
        if (clone == null) {
            return false;
        }
        clone.configure(owner);
        serverLevel.addFreshEntity(clone);
        List<ExplosiveCloneEntity> clones = aliveClones(owner);
        clones.add(clone);
        writeCloneIds(owner, clones);
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                ModSounds.SOUND_KAGEBUNSHIN.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        return true;
    }

    public static int removeAllFor(LivingEntity owner) {
        List<ExplosiveCloneEntity> clones = aliveClones(owner);
        for (ExplosiveCloneEntity clone : clones) {
            clone.discardClone(true);
        }
        writeCloneIds(owner, new ArrayList<>());
        return clones.size();
    }

    public static List<ExplosiveCloneEntity> aliveClones(LivingEntity owner) {
        List<ExplosiveCloneEntity> clones = new ArrayList<>();
        int[] ids = owner.getPersistentData().getIntArray(ID_KEY);
        for (int id : ids) {
            Entity entity = owner.level().getEntity(id);
            if (entity instanceof ExplosiveCloneEntity clone && clone.isAlive() && clone.isOwnedBy(owner)) {
                clones.add(clone);
            }
        }
        return clones;
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
        setAttributeBaseValue(Attributes.MOVEMENT_SPEED, Math.max(ProcedureUtils.getModifiedSpeed(owner) * 3.5D, 0.6D));
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
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(IGNITED, false);
        this.entityData.define(IGNITION_TICK, 0);
    }

    @Override
    public void tick() {
        this.fallDistance = 0.0F;
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        setNoGravity(true);
        LivingEntity owner = getOwner();
        if (CloneOwnerState.isUnavailable(owner) || this.tickCount > MAX_LIFE_TICKS) {
            discardClone(true);
            return;
        }
        if (isIgnited()) {
            if (this.tickCount == this.entityData.get(IGNITION_TICK) + 1) {
                playFuseSounds(owner);
            }
            if (this.tickCount - this.entityData.get(IGNITION_TICK) > FUSE_TICKS) {
                explode();
            }
            return;
        }
        if (this.tickCount % 10 == 0) {
            defendOwner(owner);
            followOwner(owner);
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
    public boolean doHurtTarget(Entity target) {
        ignite();
        return ProcedureUtils.attackEntityAsMob(this, target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypes.FALL) || isIgnited()) {
            return false;
        }
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

    public boolean isOwnedBy(LivingEntity entity) {
        return entity.getUUID().equals(this.ownerUuid) || getOwner() == entity;
    }

    public boolean isIgnited() {
        return this.entityData.get(IGNITED);
    }

    public float getIgnitionProgress(float partialTick) {
        if (!isIgnited()) {
            return 0.0F;
        }
        return ((float)this.tickCount + partialTick - this.entityData.get(IGNITION_TICK)) / (float)(FUSE_TICKS - 2);
    }

    private void ignite() {
        if (isIgnited()) {
            return;
        }
        this.entityData.set(IGNITED, true);
        this.entityData.set(IGNITION_TICK, this.tickCount);
    }

    private void playFuseSounds(LivingEntity owner) {
        playSound(SoundEvents.CREEPER_PRIMED, 1.0F, 0.5F);
        this.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                ModSounds.SOUND_KATSU.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    private void explode() {
        if (!(this.level() instanceof ServerLevel serverLevel) || this.cleanedUp) {
            return;
        }
        this.cleanedUp = true;
        LivingEntity owner = getOwner();
        serverLevel.explode(owner, getX(), getY(), getZ(), 8.0F, Level.ExplosionInteraction.MOB);
        poof();
        transferCollectedExperience(owner);
        discard();
        if (owner != null) {
            writeCloneIds(owner, aliveClones(owner));
        }
    }

    private void discardClone(boolean poof) {
        if (this.cleanedUp) {
            discard();
            return;
        }
        this.cleanedUp = true;
        LivingEntity owner = getOwner();
        if (!this.level().isClientSide && poof) {
            poof();
        }
        if (!this.level().isClientSide) {
            transferCollectedExperience(owner);
        }
        discard();
        if (owner != null) {
            writeCloneIds(owner, aliveClones(owner));
        }
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
                    160, 0.5D, 0.75D, 0.5D, 0.06D);
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

    private static void writeCloneIds(LivingEntity owner, List<ExplosiveCloneEntity> clones) {
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
        this.entityData.set(IGNITED, tag.getBoolean("Ignited"));
        this.entityData.set(IGNITION_TICK, tag.getInt("IgnitionTick"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putBoolean("Ignited", isIgnited());
        tag.putInt("IgnitionTick", this.entityData.get(IGNITION_TICK));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
