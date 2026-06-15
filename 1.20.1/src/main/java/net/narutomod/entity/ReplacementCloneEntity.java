package net.narutomod.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class ReplacementCloneEntity extends PathfinderMob {
    private static final int LIFE_TICKS = 40;
    private static final double REPLACEMENT_DISTANCE = 5.0D;
    private static final double REPLACEMENT_LOG_UPWARD_MOTION = 0.15D;
    private static final EntityDataAccessor<Integer> OWNER_ID =
            SynchedEntityData.defineId(ReplacementCloneEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;
    private boolean cleanedUp;

    public ReplacementCloneEntity(EntityType<? extends ReplacementCloneEntity> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.setNoAi(true);
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
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 0.0D)
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    public static boolean spawnFrom(LivingEntity user, @Nullable Entity attacker) {
        if (!(user.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        ReplacementCloneEntity clone = ModEntityTypes.REPLACEMENTCLONE.get().create(serverLevel);
        if (clone == null) {
            return false;
        }
        clone.configure(user);
        serverLevel.addFreshEntity(clone);
        moveUserToReplacementExit(user, attacker);
        return true;
    }

    private void configure(LivingEntity owner) {
        setOwner(owner);
        setCustomName(owner.getDisplayName());
        setLeftHanded(owner.getMainArm() == HumanoidArm.LEFT);
        copyOwnerEquipment(owner);
        moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
        setYBodyRot(owner.yBodyRot);
        setYHeadRot(owner.getYHeadRot());
        this.yRotO = getYRot();
        this.xRotO = getXRot();
        this.yBodyRotO = this.yBodyRot;
        this.yHeadRotO = this.yHeadRot;
        setHealth(getMaxHealth());
    }

    private void copyOwnerEquipment(LivingEntity owner) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = owner.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                setItemSlot(slot, stack.copy());
            }
        }
    }

    private static void moveUserToReplacementExit(LivingEntity user, @Nullable Entity attacker) {
        Vec3 offset = attacker != null ? user.position().subtract(attacker.position()) : Vec3.ZERO;
        if (offset.horizontalDistanceSqr() > 1.0E-4D) {
            Vec3 vec = offset.normalize().scale(REPLACEMENT_DISTANCE);
            float yaw = ProcedureUtils.getYawFromVec(vec);
            turnUser(user, yaw);
            user.teleportTo(attacker.getX() - vec.x(), attacker.getY(), attacker.getZ() - vec.z());
            return;
        }

        Vec3 vec = user.getLookAngle().scale(REPLACEMENT_DISTANCE);
        turnUser(user, user.getYRot() + 180.0F);
        user.teleportTo(user.getX() + vec.x(), user.getY(), user.getZ() + vec.z());
    }

    private static void turnUser(LivingEntity user, float yaw) {
        user.setYRot(yaw);
        user.setYHeadRot(yaw);
        user.setYBodyRot(yaw);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_ID, -1);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        this.fallDistance = 0.0F;
        this.setNoGravity(true);
        this.noPhysics = true;
        super.tick();
        if (!this.level().isClientSide && this.tickCount > LIFE_TICKS) {
            discardWithPoof();
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
        discardWithPoof();
    }

    private void discardWithPoof() {
        if (this.cleanedUp) {
            discard();
            return;
        }
        this.cleanedUp = true;
        this.level().playSound(null, getX(), getY(), getZ(),
                ModSounds.SOUND_POOF.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.0D, getZ(),
                    300, 0.75D, 0.75D, 0.75D, 0.08D);
            spawnReplacementLog(serverLevel);
        }
        discard();
    }

    private void spawnReplacementLog(ServerLevel serverLevel) {
        if (!serverLevel.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        BlockPos pos = blockPosition().above();
        if (!serverLevel.getBlockState(pos).isAir()) {
            return;
        }
        BlockState state = Blocks.OAK_LOG.defaultBlockState();
        serverLevel.setBlock(pos, state, 3);
        FallingBlockEntity fallingLog = FallingBlockEntity.fall(serverLevel, pos, state);
        fallingLog.setDeltaMovement(0.0D, REPLACEMENT_LOG_UPWARD_MOTION, 0.0D);
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
