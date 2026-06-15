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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.item.JutsuItem;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModItems;
import net.narutomod.registry.ModSounds;

public final class LaserRingEntity extends Entity {
    private static final int LASER_CIRCUS_INDEX = 1;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(LaserRingEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;

    public LaserRingEntity(EntityType<? extends LaserRingEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public void configure(LivingEntity owner) {
        setOwner(owner);
        moveToIdlePosition(owner);
    }

    @Nullable
    public static LaserRingEntity spawnOrGet(LivingEntity owner) {
        if (owner.level() instanceof ServerLevel level) {
            LaserRingEntity active = findActive(level, owner);
            if (active != null) {
                return active;
            }
        }
        LaserRingEntity entity = ModEntityTypes.LASER_RING.get().create(owner.level());
        if (entity == null) {
            return null;
        }
        entity.configure(owner);
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                ModSounds.SOUND_LASERCIRCUS.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        owner.level().addFreshEntity(entity);
        return entity;
    }

    @Nullable
    public static LaserRingEntity findActive(ServerLevel level, LivingEntity owner) {
        UUID ownerId = owner.getUUID();
        return level.getEntitiesOfClass(LaserRingEntity.class, owner.getBoundingBox().inflate(128.0D),
                        entity -> entity.ownerUuid != null && entity.ownerUuid.equals(ownerId))
                .stream()
                .findFirst()
                .orElse(null);
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
            discard();
            return;
        }
        moveToIdlePosition(owner);
        if (!this.level().isClientSide && this.tickCount > 20 && !shouldStay(owner)) {
            discard();
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 96.0D * getViewScale();
        return distance < range * range;
    }

    @Override
    public boolean isPickable() {
        return false;
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
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private boolean shouldStay(LivingEntity owner) {
        if (this.level() instanceof ServerLevel level && LaserCircusEntity.findActive(level, owner) != null) {
            return true;
        }
        if (owner instanceof Player player && player.isUsingItem()) {
            ItemStack stack = player.getUseItem();
            return stack.is(ModItems.RANTON.get())
                    && stack.getTag() != null
                    && stack.getTag().getInt(JutsuItem.JUTSU_INDEX_TAG) == LASER_CIRCUS_INDEX;
        }
        return false;
    }

    private void moveToIdlePosition(LivingEntity owner) {
        Vec3 look = owner.getLookAngle();
        if (look.lengthSqr() <= 1.0E-8D) {
            look = new Vec3(0.0D, 0.0D, 1.0D);
        }
        Vec3 pos = owner.position().add(look.x(), 0.6D, look.z());
        this.setDeltaMovement(Vec3.ZERO);
        this.moveTo(pos.x(), pos.y(), pos.z(), owner.getYRot(), 0.0F);
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
}
