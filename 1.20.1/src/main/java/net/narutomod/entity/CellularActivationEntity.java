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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.Chakra;
import net.narutomod.NarutomodMod;
import net.narutomod.PlayerTracker;
import net.narutomod.registry.ModEntityTypes;

public final class CellularActivationEntity extends Entity {
    public static final String ENTITY_ID_KEY = "IryoCellularActivationEntityIdKey";
    private static final double CHAKRA_BURN_PER_REDUCED_DAMAGE = 5.0D;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(CellularActivationEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> REDUCTION = SynchedEntityData.defineId(CellularActivationEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;

    public CellularActivationEntity(EntityType<? extends CellularActivationEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public void configure(LivingEntity owner) {
        setOwner(owner);
        moveToOwner(owner);
    }

    public static boolean toggleFor(LivingEntity owner) {
        CellularActivationEntity active = getActiveFor(owner);
        if (active != null) {
            active.discard();
            clearStoredId(owner);
            return false;
        }
        CellularActivationEntity entity = ModEntityTypes.CELLULAR_ACTIVATION.get().create(owner.level());
        if (entity == null) {
            return false;
        }
        entity.configure(owner);
        owner.level().addFreshEntity(entity);
        owner.getPersistentData().putInt(ENTITY_ID_KEY, entity.getId());
        return true;
    }

    public static boolean stopFor(LivingEntity owner) {
        CellularActivationEntity active = getActiveFor(owner);
        if (active != null) {
            active.discard();
            clearStoredId(owner);
            return true;
        }
        return false;
    }

    public static boolean hasActiveFor(LivingEntity owner) {
        return getActiveFor(owner) != null;
    }

    @Nullable
    public static CellularActivationEntity getActiveFor(LivingEntity owner) {
        int storedId = owner.getPersistentData().getInt(ENTITY_ID_KEY);
        if (storedId > 0 && owner.level().getEntity(storedId) instanceof CellularActivationEntity active
                && active.isAlive()
                && active.isOwner(owner)) {
            return active;
        }
        if (owner.level() instanceof ServerLevel serverLevel) {
            for (CellularActivationEntity active : serverLevel.getEntitiesOfClass(
                    CellularActivationEntity.class, owner.getBoundingBox().inflate(8.0D))) {
                if (active.isOwner(owner) && active.isAlive()) {
                    owner.getPersistentData().putInt(ENTITY_ID_KEY, active.getId());
                    return active;
                }
            }
        }
        if (storedId > 0) {
            clearStoredId(owner);
        }
        return null;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(REDUCTION, 0);
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
        int reductionTicks = getReductionAmount();
        if (reductionTicks > 0) {
            setReductionAmount(reductionTicks - 1);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        LivingEntity owner = getOwner();
        if (owner != null && owner.getPersistentData().getInt(ENTITY_ID_KEY) == getId()) {
            clearStoredId(owner);
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
        setReductionAmount(tag.getInt("Reduction"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putInt("Reduction", getReductionAmount());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public void reduceDamage(LivingHurtEvent event) {
        LivingEntity owner = getOwner();
        if (owner == null) {
            return;
        }
        double ninjaLevel = owner instanceof Player player ? PlayerTracker.getNinjaLevel(player) : 1.0D;
        if (ninjaLevel < 10.0D || event.getAmount() <= 0.0F) {
            return;
        }
        Chakra.Pathway pathway = Chakra.pathway(owner);
        double availableChakra = pathway.getAmount();
        if (availableChakra <= 0.0D) {
            return;
        }
        float reduction = event.getAmount() * (1.0F - 1.0F / (float)(ninjaLevel - 8.0D));
        if (reduction <= 0.0F) {
            return;
        }
        double chakraUsage = CHAKRA_BURN_PER_REDUCED_DAMAGE * reduction;
        if (chakraUsage > availableChakra) {
            float scale = (float)(availableChakra / chakraUsage);
            reduction *= scale;
            chakraUsage = availableChakra * 0.9999D;
        }
        if (reduction <= 0.0F || chakraUsage <= 0.0D) {
            return;
        }
        event.setAmount(Math.max(event.getAmount() - reduction, 0.0F));
        pathway.consume(chakraUsage);
        setReductionAmount(getReductionAmount() + (int)reduction);
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

    private boolean isOwner(LivingEntity owner) {
        return owner.getId() == this.entityData.get(OWNER_ID) || ownerUuid != null && ownerUuid.equals(owner.getUUID());
    }

    private int getReductionAmount() {
        return this.entityData.get(REDUCTION);
    }

    public int getReductionAmountForRender() {
        return getReductionAmount();
    }

    @Nullable
    public LivingEntity getOwnerForRender() {
        return getOwner();
    }

    private void setReductionAmount(int amount) {
        this.entityData.set(REDUCTION, Math.max(amount, 0));
    }

    private void moveToOwner(LivingEntity owner) {
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        this.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
    }

    private static void clearStoredId(LivingEntity owner) {
        owner.getPersistentData().remove(ENTITY_ID_KEY);
    }

    @Mod.EventBusSubscriber(modid = NarutomodMod.MODID)
    public static final class ForgeEvents {
        private ForgeEvents() {
        }

        @SubscribeEvent
        public static void onLivingHurt(LivingHurtEvent event) {
            CellularActivationEntity active = getActiveFor(event.getEntity());
            if (active != null) {
                active.reduceDamage(event);
            }
        }
    }
}
