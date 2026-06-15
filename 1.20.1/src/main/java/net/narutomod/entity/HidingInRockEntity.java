package net.narutomod.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.Chakra;
import net.narutomod.NarutomodModVariables;
import net.narutomod.item.DotonItem;
import net.narutomod.procedure.ProcedureSync;

public final class HidingInRockEntity extends Entity {
    private static final int WAIT_TIME = 60;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(HidingInRockEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;
    private boolean appliedNoClip;

    public HidingInRockEntity(EntityType<? extends HidingInRockEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public void configure(LivingEntity owner) {
        setOwner(owner);
        moveToOwner(owner);
    }

    public boolean isOwnedBy(LivingEntity entity) {
        return entity.getUUID().equals(this.ownerUuid) || getOwner() == entity;
    }

    public static boolean isIntangible(LivingEntity entity) {
        if (entity instanceof Player player) {
            return NarutomodModVariables.get(player).getBoolean(NarutomodModVariables.NO_CLIP_FLAG);
        }
        return entity.getPersistentData().getBoolean(NarutomodModVariables.NO_CLIP_FLAG);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            if (!this.level().isClientSide) {
                discardJutsu();
            }
            return;
        }

        moveToOwner(owner);
        setOwnerIntangible(owner, true);
        maintainOwnerMotion(owner);

        if (!this.level().isClientSide) {
            boolean paymentTick = this.tickCount % 20 == 0;
            boolean hasChakra = !(owner instanceof Player player)
                    || player.isCreative()
                    || !paymentTick
                    || Chakra.pathway(player).getAmount() >= DotonItem.HIDING_IN_ROCK.chakraUsage();
            if ((this.tickCount > WAIT_TIME && !isOwnerInEarth(owner)) || !hasChakra) {
                discardJutsu();
                return;
            }
            if (paymentTick && owner instanceof Player player && !player.isCreative()) {
                Chakra.pathway(player).consume(DotonItem.HIDING_IN_ROCK.chakraUsage());
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        LivingEntity owner = getOwner();
        if (owner != null && this.appliedNoClip) {
            setOwnerIntangible(owner, false);
        }
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        this.appliedNoClip = tag.getBoolean("AppliedNoClip");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putBoolean("AppliedNoClip", this.appliedNoClip);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void setOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
    }

    @Nullable
    private LivingEntity getOwner() {
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

    private void moveToOwner(LivingEntity owner) {
        this.setDeltaMovement(Vec3.ZERO);
        this.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
    }

    private void setOwnerIntangible(LivingEntity owner, boolean intangible) {
        boolean wasIntangible = isIntangible(owner);
        owner.noPhysics = intangible;
        this.appliedNoClip = intangible;
        if (!this.level().isClientSide && wasIntangible != intangible) {
            ProcedureSync.EntityNBTTag.setAndSync(owner, NarutomodModVariables.NO_CLIP_FLAG, intangible);
        }
        if (!this.level().isClientSide
                && owner instanceof Player player
                && (wasIntangible != intangible || intangible && this.tickCount % 20 == 1)) {
            player.displayClientMessage(Component.translatable("chattext.intangible").append(String.valueOf(intangible)), true);
        }
    }

    private void maintainOwnerMotion(LivingEntity owner) {
        if (owner.isNoGravity()) {
            return;
        }
        Vec3 motion = owner.getDeltaMovement();
        BlockPos below = BlockPos.containing(owner.getX(), owner.getY() - 0.1D, owner.getZ());
        if (this.level().getBlockState(below).isAir() || owner.isShiftKeyDown()) {
            owner.setDeltaMovement(motion.x(), motion.y() - 0.01D, motion.z());
        } else {
            owner.setDeltaMovement(motion.x(), 0.0D, motion.z());
        }
    }

    private boolean isOwnerInEarth(LivingEntity owner) {
        BlockPos pos = owner.blockPosition();
        return isEarthenBlock(this.level().getBlockState(pos)) || isEarthenBlock(this.level().getBlockState(pos.above()));
    }

    private static boolean isEarthenBlock(BlockState state) {
        return state.is(BlockTags.DIRT)
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.BASE_STONE_NETHER)
                || state.is(BlockTags.SAND)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.TERRACOTTA);
    }

    private void discardJutsu() {
        LivingEntity owner = getOwner();
        if (owner != null && this.appliedNoClip) {
            setOwnerIntangible(owner, false);
        }
        discard();
    }
}
