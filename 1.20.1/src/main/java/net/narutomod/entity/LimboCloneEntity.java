package net.narutomod.entity;

import java.util.ArrayList;
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
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.PlayerTracker;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModItems;
import net.narutomod.registry.ModSounds;

public final class LimboCloneEntity extends PathfinderMob {
    public static final String ID_KEY = "LimboCloneEntityIds";
    public static final double CHAKRA_USAGE = 500.0D;
    public static final int COOLDOWN_TICKS = 1800;
    private static final int LIFE_TICKS = 600;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(LimboCloneEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;
    private int collectedExperience;

    public LimboCloneEntity(EntityType<? extends LimboCloneEntity> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.setNoGravity(true);
        this.noPhysics = true;
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
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.0D);
    }

    public static boolean spawnPairFrom(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel serverLevel) || !getLimboClones(owner).isEmpty()) {
            return false;
        }
        List<LimboCloneEntity> clones = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            LimboCloneEntity clone = ModEntityTypes.LIMBO_CLONE.get().create(serverLevel);
            if (clone == null) {
                continue;
            }
            clone.configure(owner, i);
            serverLevel.addFreshEntity(clone);
            clones.add(clone);
        }
        if (clones.isEmpty()) {
            return false;
        }
        writeCloneIds(owner, clones);
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                ModSounds.SOUND_RINBO_HENGOKU.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        return true;
    }

    public static List<LimboCloneEntity> getLimboClones(LivingEntity owner) {
        List<LimboCloneEntity> clones = new ArrayList<>();
        int[] ids = owner.getPersistentData().getIntArray(ID_KEY);
        for (int id : ids) {
            Entity entity = owner.level().getEntity(id);
            if (entity instanceof LimboCloneEntity clone && clone.isAlive() && clone.isOwnedBy(owner)) {
                clones.add(clone);
            }
        }
        return clones;
    }

    public static int removeAllFor(LivingEntity owner) {
        List<LimboCloneEntity> clones = getLimboClones(owner);
        for (LimboCloneEntity clone : clones) {
            clone.discard();
        }
        owner.getPersistentData().putIntArray(ID_KEY, new int[0]);
        return clones.size();
    }

    public static boolean interceptAttack(LivingEntity owner, DamageSource source, float amount) {
        for (LimboCloneEntity clone : getLimboClones(owner)) {
            if (clone.distanceToSqr(owner) <= 64.0D) {
                clone.moveToDamageSource(owner, source);
                clone.hurt(source, amount);
                return true;
            }
        }
        return false;
    }

    private static void writeCloneIds(LivingEntity owner, List<LimboCloneEntity> clones) {
        int[] ids = new int[clones.size()];
        for (int i = 0; i < clones.size(); i++) {
            ids[i] = clones.get(i).getId();
        }
        owner.getPersistentData().putIntArray(ID_KEY, ids);
    }

    private void configure(LivingEntity owner, int index) {
        setOwner(owner);
        setCustomName(owner.getDisplayName());
        setLeftHanded(owner.getMainArm() == HumanoidArm.LEFT);
        copyOwnerEquipment(owner);
        copyOwnerAttributes(owner);
        double angle = Math.toRadians(owner.getYRot() + 90.0F + index * 180.0F);
        moveTo(owner.getX() + Math.cos(angle) * 1.5D, owner.getY() + 0.25D, owner.getZ() + Math.sin(angle) * 1.5D,
                owner.getYRot(), owner.getXRot());
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
        double attackDamage = owner instanceof Player player
                ? Math.max(PlayerTracker.getNinjaLevel(player), 3.0D)
                : Math.max(ProcedureUtils.getModifiedAttackDamage(owner), 3.0D);
        setAttributeBaseValue(Attributes.ATTACK_DAMAGE, attackDamage);
        setAttributeBaseValue(Attributes.ARMOR, ProcedureUtils.getArmorValue(owner));
        setAttributeBaseValue(Attributes.MAX_HEALTH, 1000.0D);
        setAttributeBaseValue(Attributes.MOVEMENT_SPEED, 1.0D);
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
            discard();
            return;
        }
        if (this.tickCount > LIFE_TICKS) {
            moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
            if (this.tickCount > LIFE_TICKS + 2) {
                discard();
            }
            return;
        }
        if (this.tickCount % 10 == 0) {
            defendOwner(owner);
        }
        LivingEntity target = getTarget();
        if (target != null && target.isAlive() && !isAlliedTo(target)) {
            moveToward(target.getEyePosition(), 0.7D);
            if (distanceToSqr(target) < 4.0D && this.tickCount % 10 == 0) {
                ProcedureUtils.attackEntityAsMob(this, target);
            }
        } else {
            setTarget(null);
            followOwner(owner);
        }
    }

    private void defendOwner(LivingEntity owner) {
        LivingEntity target = null;
        if (owner instanceof Mob mob && mob.getTarget() != null && !isAlliedTo(mob.getTarget())) {
            target = mob.getTarget();
        }
        if (target == null) {
            LivingEntity lastHurtBy = owner.getLastHurtByMob();
            if (lastHurtBy != null && lastHurtBy.isAlive() && !isAlliedTo(lastHurtBy)
                    && owner.tickCount - owner.getLastHurtByMobTimestamp() < 200) {
                target = lastHurtBy;
            }
        }
        if (target == null) {
            LivingEntity lastHurt = owner.getLastHurtMob();
            if (lastHurt != null && lastHurt.isAlive() && !isAlliedTo(lastHurt)
                    && owner.tickCount - owner.getLastHurtMobTimestamp() < 200) {
                target = lastHurt;
            }
        }
        if (target != null) {
            setTarget(target);
        }
    }

    private void followOwner(LivingEntity owner) {
        double distanceSqr = distanceToSqr(owner);
        if (distanceSqr > 100.0D) {
            moveTo(owner.getX(), owner.getY() + 0.25D, owner.getZ(), owner.getYRot(), owner.getXRot());
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (distanceSqr > 9.0D) {
            moveToward(owner.getEyePosition(), 0.55D);
        } else if (distanceSqr <= 2.25D) {
            moveAwayFrom(owner, 0.35D);
        } else {
            setDeltaMovement(getDeltaMovement().scale(0.5D));
        }
    }

    private void moveAwayFrom(LivingEntity owner, double speed) {
        Vec3 delta = position().subtract(owner.position());
        if (delta.lengthSqr() < 1.0E-6D) {
            delta = getLookAngle().scale(-1.0D);
        }
        Vec3 motion = delta.normalize().scale(speed);
        setDeltaMovement(motion);
        this.hasImpulse = true;
        setYRot(ProcedureUtils.getYawFromVec(motion));
        setYBodyRot(getYRot());
        setYHeadRot(getYRot());
    }

    private void moveToward(Vec3 target, double speed) {
        Vec3 delta = target.subtract(position());
        if (delta.lengthSqr() < 0.04D) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        Vec3 motion = delta.normalize().scale(speed);
        setDeltaMovement(motion);
        this.hasImpulse = true;
        setYRot(ProcedureUtils.getYawFromVec(motion));
        setYBodyRot(getYRot());
        setYHeadRot(getYRot());
    }

    private void moveToDamageSource(LivingEntity owner, DamageSource source) {
        Vec3 sourcePosition = source.getSourcePosition();
        if (sourcePosition == null) {
            return;
        }
        Vec3 vec = sourcePosition.subtract(owner.position()).scale(source.getDirectEntity() instanceof LivingEntity ? 0.5D : 0.8D);
        setYRot(ProcedureUtils.getYawFromVec(vec));
        setXRot(ProcedureUtils.getPitchFromVec(vec.x(), vec.y(), vec.z()));
        moveTo(owner.getX() + vec.x(), owner.getY() + vec.y(), owner.getZ() + vec.z(), getYRot(), getXRot());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity living && canBeDetectedBy(living)) {
            return super.hurt(source, amount);
        }
        if (attacker instanceof LivingEntity living && !isAlliedTo(living)) {
            setTarget(living);
        }
        return false;
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
    public boolean isPickable() {
        return false;
    }

    @Override
    public void awardKillScore(Entity killed, int score, DamageSource source) {
        super.awardKillScore(killed, score, source);
        this.collectedExperience += CloneExperienceRewards.collectFromKill(this, killed);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide) {
            transferCollectedExperience(getOwner());
        }
        super.remove(reason);
    }

    private void transferCollectedExperience(@Nullable LivingEntity owner) {
        CloneExperienceRewards.transferToOwner(owner, this.collectedExperience);
        this.collectedExperience = 0;
    }

    public boolean isOwnedBy(LivingEntity entity) {
        return entity.getUUID().equals(this.ownerUuid) || getOwner() == entity;
    }

    public boolean canBeDetectedBy(Entity entity) {
        if (entity == getOwner()) {
            return true;
        }
        if (entity instanceof LivingEntity living && living.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.RINNEGANHELMET.get())) {
            return true;
        }
        return entity instanceof Player player
                && ProcedureUtils.hasItemInInventory(player, ModItems.SIX_PATH_SENJUTSU.get());
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
