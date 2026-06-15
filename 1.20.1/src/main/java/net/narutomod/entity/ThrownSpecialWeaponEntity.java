package net.narutomod.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.PlayerTracker;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModItems;

public final class ThrownSpecialWeaponEntity extends ThrowableItemProjectile {
    private static final int MAX_LIFE = 200;

    public ThrownSpecialWeaponEntity(EntityType<? extends ThrownSpecialWeaponEntity> entityType, Level level) {
        super(entityType, level);
    }

    public void configure(LivingEntity owner) {
        setOwner(owner);
        setSilent(true);
        setItem(new ItemStack(defaultItem()));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            applyNearMissEffect();
            if (this.tickCount > MAX_LIFE) {
                discard();
            }
        } else if (isAshBones()) {
            for (int i = 0; i < 5; i++) {
                this.level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
            }
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
        if (target.hurt(source, directDamage()) && owner instanceof Player player) {
            PlayerTracker.logBattleExp(player, 1.0D);
        }
        if (target instanceof LivingEntity livingTarget) {
            applyDirectEffect(livingTarget);
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
        return super.canHitEntity(entity) && entity != getOwner();
    }

    @Override
    protected Item getDefaultItem() {
        return defaultItem();
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

    private void applyNearMissEffect() {
        for (LivingEntity target : this.level().getEntitiesOfClass(
                LivingEntity.class,
                getBoundingBox().inflate(0.75D),
                target -> target.isAlive() && target != getOwner())) {
            if (isAshBones()) {
                applyAshBonesEffect(target);
            } else if (isBlackReceiver()) {
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 1, false, false));
            }
        }
    }

    private void applyDirectEffect(LivingEntity target) {
        if (isAshBones()) {
            applyAshBonesEffect(target);
        } else if (isBlackReceiver()) {
            applyBlackReceiverEffect(target);
        }
    }

    private void applyBlackReceiverEffect(LivingEntity target) {
        int amplifier = 1;
        MobEffectInstance current = target.getEffect(ModEffects.HEAVINESS.get());
        if (current != null) {
            amplifier += current.getAmplifier();
        }
        target.addEffect(new MobEffectInstance(ModEffects.HEAVINESS.get(), 600, amplifier, false, false));
    }

    public static void applyAshBonesEffect(LivingEntity target) {
        ProcedureUtils.setDeathAnimations(
                target,
                1,
                200);
    }

    private boolean isAshBones() {
        return getType() == ModEntityTypes.ENTITYBULLETASHBONES.get();
    }

    private boolean isBlackReceiver() {
        return getType() == ModEntityTypes.ENTITYBULLETBLACK_RECEIVER.get();
    }

    private float directDamage() {
        return isBlackReceiver() ? 10.0F : 2.0F;
    }

    private Item defaultItem() {
        return getType() == ModEntityTypes.ENTITYBULLETASHBONES.get()
                ? ModItems.ASHBONES.get()
                : ModItems.BLACK_RECEIVER.get();
    }
}
