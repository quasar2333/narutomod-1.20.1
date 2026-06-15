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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.Chakra;
import net.narutomod.item.FutonItem;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModEntityTypes;

public final class FutonChakraFlowEntity extends Entity {
    public static final String ENTITY_ID_KEY = "FutonChakraFlowEntityIdKey";
    private static final double CHAKRA_BURN_EVERY_10_TICKS = FutonItem.CHAKRA_FLOW.chakraUsage() * 0.1D;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(FutonChakraFlowEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STRENGTH_MODIFIER = SynchedEntityData.defineId(FutonChakraFlowEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;
    private ItemStack sourceStack = ItemStack.EMPTY;
    private int originalStrengthBonus;

    public FutonChakraFlowEntity(EntityType<? extends FutonChakraFlowEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public void configure(LivingEntity owner, ItemStack stack, int strengthModifier) {
        setOwner(owner);
        setStrengthModifier(strengthModifier);
        this.sourceStack = stack.copyWithCount(1);
        moveToOwner(owner);
    }

    public static boolean spawnOrToggle(LivingEntity owner, ItemStack stack, int strengthModifier) {
        if (stopActive(owner)) {
            return false;
        }
        FutonChakraFlowEntity entity = ModEntityTypes.FUTONCHAKRAFLOW.get().create(owner.level());
        if (entity == null) {
            return false;
        }
        entity.configure(owner, stack, strengthModifier);
        owner.level().addFreshEntity(entity);
        owner.getPersistentData().putInt(ENTITY_ID_KEY, entity.getId());
        return true;
    }

    public static boolean stopActive(LivingEntity owner) {
        Entity existing = owner.level().getEntity(owner.getPersistentData().getInt(ENTITY_ID_KEY));
        if (existing instanceof FutonChakraFlowEntity chakraFlow) {
            chakraFlow.discard();
            owner.getPersistentData().remove(ENTITY_ID_KEY);
            return true;
        }
        return false;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(STRENGTH_MODIFIER, 3);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        moveToOwner(owner);
        if (ProcedureUtils.isWeapon(owner.getMainHandItem())) {
            applyEffects(owner);
            if (this.tickCount % 10 == 1 && !Chakra.pathway(owner).consume(CHAKRA_BURN_EVERY_10_TICKS)) {
                discard();
            }
        } else {
            this.originalStrengthBonus = 0;
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        LivingEntity owner = getOwner();
        if (owner != null) {
            owner.getPersistentData().remove(ENTITY_ID_KEY);
        }
        super.remove(reason);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 68.5D * getViewScale();
        return distance < range * range;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        if (tag.contains("SourceStack")) {
            this.sourceStack = ItemStack.of(tag.getCompound("SourceStack"));
        }
        setStrengthModifier(tag.contains("StrengthModifier") ? tag.getInt("StrengthModifier") : 3);
        this.originalStrengthBonus = tag.getInt("OriginalStrengthBonus");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        if (!this.sourceStack.isEmpty()) {
            tag.put("SourceStack", this.sourceStack.save(new CompoundTag()));
        }
        tag.putInt("StrengthModifier", getStrengthModifier());
        tag.putInt("OriginalStrengthBonus", this.originalStrengthBonus);
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
    public LivingEntity getOwnerForRender() {
        return getOwner();
    }

    public boolean isOwnerHoldingWeaponForRender() {
        LivingEntity owner = getOwner();
        return owner != null && ProcedureUtils.isWeapon(owner.getMainHandItem());
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

    private int getStrengthModifier() {
        return this.entityData.get(STRENGTH_MODIFIER);
    }

    private void setStrengthModifier(int strengthModifier) {
        this.entityData.set(STRENGTH_MODIFIER, Math.max(strengthModifier, 0));
    }

    private void moveToOwner(LivingEntity owner) {
        this.setDeltaMovement(Vec3.ZERO);
        this.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
    }

    private void applyEffects(LivingEntity owner) {
        if (this.originalStrengthBonus == 0) {
            this.originalStrengthBonus = 1;
            if (owner.hasEffect(MobEffects.DAMAGE_BOOST)) {
                this.originalStrengthBonus += owner.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier();
            }
        }
        owner.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_BOOST,
                2,
                getStrengthModifier() + this.originalStrengthBonus,
                false,
                false));
        owner.addEffect(new MobEffectInstance(ModEffects.REACH.get(), 2, 0, false, false));
    }

    public static int strengthModifierFor(Player player, ItemStack stack) {
        if (!(stack.getItem() instanceof FutonItem futonItem)) {
            return 3;
        }
        float modifier = futonItem.getJutsuXp(stack, FutonItem.CHAKRA_FLOW) > 0
                ? (float)futonItem.getJutsuXp(stack, FutonItem.CHAKRA_FLOW)
                / (float)Math.max(futonItem.getRequiredXp(stack, FutonItem.CHAKRA_FLOW), 1)
                : 0.0F;
        if (modifier <= 0.0F) {
            return 3;
        }
        return (int)(modifier * net.narutomod.PlayerTracker.getNinjaLevel(player) / 20.0D);
    }
}
