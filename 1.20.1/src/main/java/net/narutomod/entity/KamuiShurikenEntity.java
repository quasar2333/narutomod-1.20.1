package net.narutomod.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.PlayerTracker;
import net.narutomod.registry.ModItems;
import net.narutomod.registry.ModSounds;

public final class KamuiShurikenEntity extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Float> SCALE =
            SynchedEntityData.defineId(KamuiShurikenEntity.class, EntityDataSerializers.FLOAT);
    private static final int MAX_LIFE = 200;
    private static final float BASE_SIZE = 0.4F;

    public KamuiShurikenEntity(EntityType<? extends KamuiShurikenEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public void configure(LivingEntity owner, float scale) {
        setOwner(owner);
        setScale(scale);
        setItem(new ItemStack(ModItems.KAMUISHURIKEN.get()));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SCALE, 1.0F);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SCALE.equals(key)) {
            refreshDimensions();
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float size = BASE_SIZE * getScale();
        return EntityDimensions.fixed(size, size);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount % 40 == 2) {
            this.level().playSound(null, getX(), getY(), getZ(), ModSounds.SOUND_KAMUISFX.get(),
                    SoundSource.NEUTRAL, 1.0F, 1.0F / (this.random.nextFloat() * 0.5F + 1.0F) + 0.25F);
        }
        if (!this.level().isClientSide && this.tickCount > MAX_LIFE) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        if (this.level().isClientSide || !target.isAlive() || isOwnerChain(target)) {
            return;
        }
        Entity owner = getOwner();
        if (owner instanceof Player player) {
            double divisor = Math.max((target.getBbWidth() + target.getBbWidth() + target.getBbHeight()) / 3.0D, 0.1D);
            double factor = 0.0001D * PlayerTracker.getBattleXp(player) / divisor;
            if (target instanceof LivingEntity living) {
                living.hurt(this.damageSources().fellOutOfWorld(), living.getMaxHealth() * (float)factor);
            } else {
                target.hurt(this.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
                if (target.isAlive()) {
                    target.discard();
                }
            }
        }
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!this.level().isClientSide) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && !isOwnerChain(entity);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.KAMUISHURIKEN.get();
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setScale(tag.contains("Scale") ? tag.getFloat("Scale") : 1.0F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Scale", getScale());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public float getScale() {
        return Math.max(this.entityData.get(SCALE), 0.1F);
    }

    private void setScale(float scale) {
        this.entityData.set(SCALE, Math.max(scale, 0.1F));
        refreshDimensions();
    }

    private boolean isOwnerChain(Entity entity) {
        for (Entity current = getOwner(); current != null; current = current.getVehicle()) {
            if (entity == current) {
                return true;
            }
        }
        return false;
    }
}
