package net.narutomod.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModItems;

public final class HiramekareiEffectEntity extends Entity {
    public static final String ACTIVE_TAG = "EffectEntityActive";
    public static final String ACTIVE_ENTITY_ID_TAG = "Id";
    public static final String ACTIVE_STRENGTH_TAG = "strength";
    private static final int DURATION_TICKS = 200;
    private static final int REACH_AMPLIFIER = 3;
    private static final EntityDataAccessor<Integer> OWNER_ID =
            SynchedEntityData.defineId(HiramekareiEffectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> STRENGTH =
            SynchedEntityData.defineId(HiramekareiEffectEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUuid;

    public HiramekareiEffectEntity(EntityType<? extends HiramekareiEffectEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
        this.noPhysics = true;
    }

    public static boolean spawnFor(Player owner, ItemStack stack, double strength) {
        if (owner.level().isClientSide) {
            return false;
        }
        discardBoundEntity(owner.level(), stack);
        HiramekareiEffectEntity entity = ModEntityTypes.ENTITYBULLETHIRAMEKAREI_SWORD.get().create(owner.level());
        if (entity == null) {
            return false;
        }
        entity.configure(owner, strength);
        owner.level().addFreshEntity(entity);
        writeActiveTag(stack, entity.getId(), strength);
        return true;
    }

    public static boolean hasActiveTag(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(ACTIVE_TAG, Tag.TAG_COMPOUND);
    }

    public static boolean isActive(Level level, ItemStack stack) {
        if (!hasActiveTag(stack)) {
            return false;
        }
        Entity entity = level.getEntity(getActiveEntityId(stack));
        return entity instanceof HiramekareiEffectEntity && !entity.isRemoved();
    }

    public static void clearIfInactive(Level level, ItemStack stack) {
        if (!hasActiveTag(stack)) {
            return;
        }
        if (!isActive(level, stack)) {
            clearActiveTag(stack);
        }
    }

    public static double getActiveStrength(ItemStack stack) {
        if (!hasActiveTag(stack)) {
            return 0.0D;
        }
        CompoundTag active = stack.getOrCreateTag().getCompound(ACTIVE_TAG);
        return active.contains(ACTIVE_STRENGTH_TAG, Tag.TAG_DOUBLE) ? active.getDouble(ACTIVE_STRENGTH_TAG) : 0.0D;
    }

    public void configure(LivingEntity owner, double strength) {
        setOwner(owner);
        setStrength(strength);
        moveToOwner(owner);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(STRENGTH, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive() || this.tickCount > DURATION_TICKS) {
            discard();
            return;
        }
        ItemStack mainHand = owner.getMainHandItem();
        if (!isStackBoundToThis(mainHand)) {
            discard();
            return;
        }
        moveToOwner(owner);
        owner.addEffect(new MobEffectInstance(ModEffects.REACH.get(), 2, REACH_AMPLIFIER, false, false));
    }

    @Override
    public void remove(RemovalReason reason) {
        LivingEntity owner = getOwner();
        if (owner != null) {
            clearBoundStacks(owner);
        }
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        setStrength(tag.getDouble("Strength"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putDouble("Strength", getStrength());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private static void writeActiveTag(ItemStack stack, int entityId, double strength) {
        CompoundTag active = new CompoundTag();
        active.putInt(ACTIVE_ENTITY_ID_TAG, entityId);
        active.putDouble(ACTIVE_STRENGTH_TAG, strength);
        stack.getOrCreateTag().put(ACTIVE_TAG, active);
    }

    public static void clearActiveTag(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(ACTIVE_TAG);
            if (tag.isEmpty()) {
                stack.setTag(null);
            }
        }
    }

    private static int getActiveEntityId(ItemStack stack) {
        if (!hasActiveTag(stack)) {
            return -1;
        }
        return stack.getOrCreateTag().getCompound(ACTIVE_TAG).getInt(ACTIVE_ENTITY_ID_TAG);
    }

    private static void discardBoundEntity(Level level, ItemStack stack) {
        if (!hasActiveTag(stack)) {
            return;
        }
        Entity existing = level.getEntity(getActiveEntityId(stack));
        if (existing instanceof HiramekareiEffectEntity) {
            existing.discard();
        }
        clearActiveTag(stack);
    }

    private void setOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
    }

    @Nullable
    public LivingEntity getOwnerForRender() {
        return getOwner();
    }

    public boolean isOwnerHoldingHiramekareiForRender() {
        LivingEntity owner = getOwner();
        return owner != null && owner.getMainHandItem().is(ModItems.HIRAMEKAREI_SWORD.get());
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

    private double getStrength() {
        return this.entityData.get(STRENGTH);
    }

    private void setStrength(double strength) {
        this.entityData.set(STRENGTH, (float)Math.max(strength, 0.0D));
    }

    private boolean isStackBoundToThis(ItemStack stack) {
        return stack.is(ModItems.HIRAMEKAREI_SWORD.get()) && getActiveEntityId(stack) == getId();
    }

    private void clearBoundStacks(LivingEntity owner) {
        clearIfBound(owner.getMainHandItem());
        clearIfBound(owner.getOffhandItem());
        if (owner instanceof Player player) {
            for (ItemStack stack : player.getInventory().items) {
                clearIfBound(stack);
            }
            for (ItemStack stack : player.getInventory().offhand) {
                clearIfBound(stack);
            }
        }
    }

    private void clearIfBound(ItemStack stack) {
        if (isStackBoundToThis(stack)) {
            clearActiveTag(stack);
        }
    }

    private void moveToOwner(LivingEntity owner) {
        setDeltaMovement(Vec3.ZERO);
        moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
    }
}
