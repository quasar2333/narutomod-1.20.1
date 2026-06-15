package net.narutomod.entity;

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
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;

public final class AsuraCannonballEntity extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Float> EXPLOSIVE_POWER =
            SynchedEntityData.defineId(AsuraCannonballEntity.class, EntityDataSerializers.FLOAT);
    private static final int MAX_LIFE = 200;

    public AsuraCannonballEntity(EntityType<? extends AsuraCannonballEntity> entityType, Level level) {
        super(entityType, level);
    }

    public void configure(LivingEntity owner, float explosivePower) {
        setOwner(owner);
        setExplosionPower(explosivePower);
        setItem(new ItemStack(Items.FIREWORK_STAR));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(EXPLOSIVE_POWER, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount > MAX_LIFE) {
            explode();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            explode();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && entity != getOwner();
    }

    @Override
    protected Item getDefaultItem() {
        return Items.FIREWORK_STAR;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setExplosionPower(tag.contains("ExplosivePower") ? tag.getFloat("ExplosivePower") : 1.0F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("ExplosivePower", getExplosionPower());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public float getExplosionPower() {
        return Math.max(this.entityData.get(EXPLOSIVE_POWER), 0.0F);
    }

    private void setExplosionPower(float explosivePower) {
        this.entityData.set(EXPLOSIVE_POWER, Math.max(explosivePower, 0.0F));
    }

    private void explode() {
        if (!(this.level() instanceof ServerLevel serverLevel) || isRemoved()) {
            return;
        }
        serverLevel.explode(this, getX(), getY(), getZ(), getExplosionPower(), Level.ExplosionInteraction.MOB);
        discard();
    }
}
