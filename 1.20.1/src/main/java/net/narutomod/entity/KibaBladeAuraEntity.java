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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModItems;
import net.narutomod.registry.ModSounds;

public final class KibaBladeAuraEntity extends Entity {
    public static final String STACK_ENTITY_ID_TAG = "KibaBladeAuraEntityId";
    private static final EntityDataAccessor<Integer> OWNER_ID =
            SynchedEntityData.defineId(KibaBladeAuraEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;

    public KibaBladeAuraEntity(EntityType<? extends KibaBladeAuraEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public static void ensureFor(LivingEntity owner, ItemStack stack) {
        if (owner.level().isClientSide) {
            return;
        }
        Entity existing = owner.level().getEntity(stack.getOrCreateTag().getInt(STACK_ENTITY_ID_TAG));
        if (existing instanceof KibaBladeAuraEntity aura && aura.getOwner() == owner) {
            return;
        }
        KibaBladeAuraEntity aura = ModEntityTypes.ENTITYBULLETKIBA_BLADES.get().create(owner.level());
        if (aura == null) {
            return;
        }
        aura.configure(owner);
        owner.level().addFreshEntity(aura);
        stack.getOrCreateTag().putInt(STACK_ENTITY_ID_TAG, aura.getId());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
    }

    public void configure(LivingEntity owner) {
        setOwner(owner);
        moveToOwner(owner);
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive() || !owner.getMainHandItem().is(ModItems.KIBA_BLADES.get())) {
            if (!this.level().isClientSide) {
                discard();
            }
            return;
        }
        moveToOwner(owner);
        if (!this.level().isClientSide && this.random.nextFloat() < 0.01F) {
            this.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                    ModSounds.SOUND_ELECTRICITY.get(), SoundSource.PLAYERS, 0.1F,
                    this.random.nextFloat() * 0.5F + 0.4F);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
    }

    @Override
    public boolean fireImmune() {
        return true;
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

    public boolean isOwnerHoldingBladeForRender(InteractionHand hand) {
        LivingEntity owner = getOwner();
        return owner != null && owner.getItemInHand(hand).is(ModItems.KIBA_BLADES.get());
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
        moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
        setDeltaMovement(Vec3.ZERO);
    }
}
