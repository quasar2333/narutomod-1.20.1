package net.narutomod.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.PlayerTracker;
import net.narutomod.registry.ModItems;

public final class FoldingFanProjectileEntity extends ThrowableItemProjectile {
    private static final int MAX_LIFE = 200;
    private static final float DAMAGE = 5.0F;
    private static final double KNOCKBACK = 2.5D;

    public FoldingFanProjectileEntity(EntityType<? extends FoldingFanProjectileEntity> entityType, Level level) {
        super(entityType, level);
    }

    public void configure(LivingEntity owner) {
        setOwner(owner);
        setSilent(true);
        setItem(new ItemStack(ModItems.FOLDING_FAN.get()));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount > MAX_LIFE) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        if (this.level().isClientSide || !target.isAlive()) {
            return;
        }
        Entity owner = getOwner();
        DamageSource source = this.damageSources().thrown(this, owner);
        if (target.hurt(source, DAMAGE) && owner instanceof Player player) {
            PlayerTracker.logBattleExp(player, 1.0D);
        }
        pushTarget(target);
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
        return super.canHitEntity(entity) && entity != getOwner();
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.FOLDING_FAN.get();
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void pushTarget(Entity target) {
        Vec3 direction = getDeltaMovement();
        if (direction.lengthSqr() <= 1.0E-8D && getOwner() != null) {
            direction = target.position().subtract(getOwner().position());
        }
        Vec3 horizontal = new Vec3(direction.x(), 0.0D, direction.z());
        if (horizontal.lengthSqr() <= 1.0E-8D) {
            horizontal = target.getLookAngle().reverse();
        }
        horizontal = horizontal.normalize().scale(KNOCKBACK);
        target.setDeltaMovement(target.getDeltaMovement().add(horizontal.x(), 0.35D, horizontal.z()));
        target.hurtMarked = true;
    }
}
