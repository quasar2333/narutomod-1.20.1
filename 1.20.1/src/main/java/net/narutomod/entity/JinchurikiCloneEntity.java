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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.Chakra;
import net.narutomod.NarutomodModVariables;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEntityTypes;

public final class JinchurikiCloneEntity extends PathfinderMob implements RangedAttackMob {
    private static final int RANGED_MIN_DISTANCE = 24;
    private static final int IDLE_LIMIT_TICKS = 200;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(JinchurikiCloneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CLONE_LEVEL = SynchedEntityData.defineId(JinchurikiCloneEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;
    private int idleTime;
    private int originalGameModeId = GameType.SURVIVAL.getId();
    private int originalJinchurikiTails = -1;
    private int collectedExperience;
    private boolean cleanedUp;

    public JinchurikiCloneEntity(EntityType<? extends JinchurikiCloneEntity> entityType, Level level) {
        super(entityType, level);
        setPersistenceRequired();
        this.xpReward = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.0F);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D);
    }

    @Nullable
    public static JinchurikiCloneEntity spawnFrom(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        JinchurikiCloneEntity clone = ModEntityTypes.JINCHURIKI_CLONE.get().create(serverLevel);
        if (clone == null) {
            return null;
        }
        clone.configure(owner);
        serverLevel.addFreshEntity(clone);
        return clone;
    }

    public int getCloneLevel() {
        return this.entityData.get(CLONE_LEVEL);
    }

    public void rememberOriginalJinchurikiTails(int tails) {
        this.originalJinchurikiTails = tails;
    }

    public boolean isOwnedBy(LivingEntity entity) {
        return this.ownerUuid != null && this.ownerUuid.equals(entity.getUUID())
                || this.entityData.get(OWNER_ID) == entity.getId();
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

    private void configure(LivingEntity owner) {
        setOwner(owner);
        setCustomName(owner.getDisplayName());
        setLeftHanded(owner.getMainArm() == HumanoidArm.LEFT);
        copyOwnerEquipment(owner);
        copyOwnerAttributes(owner);
        copyOwnerChakra(owner);
        this.entityData.set(CLONE_LEVEL, resolveCloneLevel(owner));
        setTarget(owner.getLastHurtMob());
        Vec3 offset = Vec3.directionFromRotation(0.0F, owner.getYRot()).scale(1.5D);
        moveTo(owner.getX() + offset.x(), owner.getY(), owner.getZ() + offset.z(), owner.getYRot(), owner.getXRot());
        if (owner instanceof ServerPlayer player) {
            this.originalGameModeId = player.gameMode.getGameModeForPlayer().getId();
            this.originalJinchurikiTails = NarutomodModVariables.get(player).getInt(NarutomodModVariables.JINCHURIKI_TAILS);
            player.setGameMode(GameType.SPECTATOR);
            player.setCamera(this);
        }
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this,
                LivingEntity.class,
                false,
                target -> target != this && target != getOwner() && target.isAlive() && !target.isSpectator() && !isAlliedTo(target)));
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LeapAtTargetGoal(this, 1.5F));
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0D, 40, 80.0F) {
            @Override
            public boolean canUse() {
                LivingEntity target = JinchurikiCloneEntity.this.getTarget();
                return JinchurikiCloneEntity.this.getCloneLevel() == 2
                        && target != null
                        && JinchurikiCloneEntity.this.distanceToSqr(target) > RANGED_MIN_DISTANCE * RANGED_MIN_DISTANCE
                        && super.canUse();
            }
        });
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0D, true) {
            @Override
            public boolean canUse() {
                LivingEntity target = JinchurikiCloneEntity.this.getTarget();
                return target != null
                        && JinchurikiCloneEntity.this.distanceToSqr(target) <= RANGED_MIN_DISTANCE * RANGED_MIN_DISTANCE
                        && super.canUse();
            }

            @Override
            protected double getAttackReachSqr(LivingEntity target) {
                double reach = 3.0D + JinchurikiCloneEntity.this.getCloneLevel();
                return reach * reach;
            }
        });
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(CLONE_LEVEL, 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        LivingEntity owner = getOwner();
        if (CloneOwnerState.isUnavailable(owner)) {
            cleanupAndDiscard();
            return;
        }
        if (owner instanceof ServerPlayer player && player.getCamera() != this) {
            player.setCamera(this);
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            this.idleTime++;
        } else {
            this.idleTime = 0;
        }
        if (this.idleTime > IDLE_LIMIT_TICKS) {
            ServerPlayer idleOwner = owner instanceof ServerPlayer player ? player : null;
            cleanupAndDiscard();
            if (idleOwner != null && BijuManager.getCloakLevel(idleOwner) > 0) {
                BijuManager.toggleBijuCloak(idleOwner);
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        target.invulnerableTime = 0;
        return ProcedureUtils.attackEntityAsMob(this, target);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        TailBeastBallEntity.spawnFrom(this, 2.0F, 200.0F);
    }

    @Override
    public void die(DamageSource source) {
        cleanupAndDiscard();
    }

    @Override
    public void awardKillScore(Entity killed, int score, DamageSource source) {
        super.awardKillScore(killed, score, source);
        this.collectedExperience += CloneExperienceRewards.collectFromKill(this, killed);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.cleanedUp && !this.level().isClientSide) {
            restoreOwnerState();
            this.cleanedUp = true;
        }
        super.remove(reason);
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
        this.entityData.set(CLONE_LEVEL, tag.contains("CloneLevel") ? tag.getInt("CloneLevel") : 1);
        this.idleTime = tag.getInt("IdleTime");
        this.originalGameModeId = tag.contains("OriginalGameMode") ? tag.getInt("OriginalGameMode") : GameType.SURVIVAL.getId();
        this.originalJinchurikiTails = tag.contains("OriginalJinchurikiTails") ? tag.getInt("OriginalJinchurikiTails") : -1;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putInt("CloneLevel", getCloneLevel());
        tag.putInt("IdleTime", this.idleTime);
        tag.putInt("OriginalGameMode", this.originalGameModeId);
        tag.putInt("OriginalJinchurikiTails", this.originalJinchurikiTails);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void cleanupAndDiscard() {
        if (this.cleanedUp) {
            discard();
            return;
        }
        restoreOwnerState();
        this.cleanedUp = true;
        discard();
    }

    private void restoreOwnerState() {
        LivingEntity owner = getOwner();
        if (!(owner instanceof ServerPlayer player)) {
            return;
        }
        transferCollectedExperience(player);
        if (player.getCamera() == this) {
            player.setCamera(player);
        }
        GameType originalGameType = GameType.byId(this.originalGameModeId);
        if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
            player.setGameMode(originalGameType);
        }
        if (this.originalJinchurikiTails >= 0) {
            NarutomodModVariables.get(player).putInt(NarutomodModVariables.JINCHURIKI_TAILS, this.originalJinchurikiTails);
            NarutomodModVariables.sync(player);
        }
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 2, 0, false, false));
        player.setHealth(Math.min(player.getMaxHealth(), Math.max(1.0F, getHealth())));
    }

    private void transferCollectedExperience(LivingEntity owner) {
        CloneExperienceRewards.transferToOwner(owner, this.collectedExperience);
        this.collectedExperience = 0;
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
        setAttributeBaseValue(Attributes.FOLLOW_RANGE, 64.0D);
        setAttributeBaseValue(Attributes.MOVEMENT_SPEED, Math.max(ProcedureUtils.getModifiedSpeed(owner), 0.5D));
        setAttributeBaseValue(Attributes.ATTACK_DAMAGE, Math.max(ProcedureUtils.getModifiedAttackDamage(owner), 3.0D));
        setAttributeBaseValue(Attributes.ARMOR, ProcedureUtils.getArmorValue(owner));
        setAttributeBaseValue(Attributes.MAX_HEALTH, Math.max(owner.getMaxHealth(), 1.0D));
        setHealth(Math.max(1.0F, Math.min(owner.getHealth(), getMaxHealth())));
    }

    private void copyOwnerChakra(LivingEntity owner) {
        Chakra.Pathway cloneChakra = Chakra.pathway(this);
        Chakra.Pathway ownerChakra = Chakra.pathway(owner);
        cloneChakra.setMax(ownerChakra.getMax());
        cloneChakra.consume(-ownerChakra.getAmount(), true);
    }

    private int resolveCloneLevel(LivingEntity owner) {
        if (owner instanceof ServerPlayer player && BijuManager.getAssignedTail(player) > 0) {
            return 2;
        }
        if (owner instanceof Player player
                && NarutomodModVariables.get(player).getInt(NarutomodModVariables.JINCHURIKI_TAILS) > 0) {
            return 2;
        }
        return 1;
    }

    private void setOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
    }

    private void setAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }
}
