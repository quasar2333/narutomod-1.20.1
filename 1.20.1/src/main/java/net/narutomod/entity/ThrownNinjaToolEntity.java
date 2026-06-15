package net.narutomod.entity;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.PlayerTracker;
import net.narutomod.item.NinjaToolItem;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModItems;
import net.narutomod.registry.ModParticleTypes;

public final class ThrownNinjaToolEntity extends ThrowableItemProjectile {
    private static final int MAX_LIFE = 200;
    private static final int SMOKE_TICKS = 60;
    private static final EntityDataAccessor<Boolean> DROPS_ON_BLOCK =
            SynchedEntityData.defineId(ThrownNinjaToolEntity.class, EntityDataSerializers.BOOLEAN);

    private int smokeTicksRemaining;
    private int knockbackStrength;
    private float configuredDamage = -1.0F;

    public ThrownNinjaToolEntity(EntityType<? extends ThrownNinjaToolEntity> entityType, Level level) {
        super(entityType, level);
    }

    public void configure(LivingEntity owner, NinjaToolItem item, boolean dropsOnBlock) {
        setOwner(owner);
        setItem(new ItemStack(item));
        this.entityData.set(DROPS_ON_BLOCK, dropsOnBlock);
        this.knockbackStrength = 0;
        this.configuredDamage = -1.0F;
    }

    public void setKnockbackStrength(int knockbackStrength) {
        this.knockbackStrength = Math.max(knockbackStrength, 0);
    }

    public void setBaseDamage(float damage) {
        this.configuredDamage = Math.max(damage, 0.0F);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DROPS_ON_BLOCK, true);
    }

    @Override
    public void tick() {
        if (this.smokeTicksRemaining > 0) {
            if (!this.level().isClientSide) {
                spawnSmokeBurst();
                this.smokeTicksRemaining--;
                if (this.smokeTicksRemaining <= 0) {
                    discard();
                }
            }
            return;
        }
        super.tick();
        if (!this.level().isClientSide && this.tickCount > MAX_LIFE) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        if (isSmokeBomb()) {
            startSmoke(result.getLocation());
            return;
        }
        if (isExplosiveKunai()) {
            hurtTarget(target);
            explode(result.getLocation());
            return;
        }
        hurtTarget(target);
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (isSmokeBomb()) {
            startSmoke(result.getLocation());
            return;
        }
        if (isExplosiveKunai()) {
            explode(result.getLocation());
            return;
        }
        if (this.entityData.get(DROPS_ON_BLOCK)) {
            spawnPickup(result.getLocation());
        }
        discard();
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
        this.entityData.set(DROPS_ON_BLOCK, tag.getBoolean("DropsOnBlock"));
        this.smokeTicksRemaining = tag.getInt("SmokeTicksRemaining");
        this.knockbackStrength = Math.max(tag.getInt("KnockbackStrength"), 0);
        this.configuredDamage = tag.contains("ConfiguredDamage") ? Math.max(tag.getFloat("ConfiguredDamage"), 0.0F) : -1.0F;
        if (this.smokeTicksRemaining > 0) {
            this.noPhysics = true;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("DropsOnBlock", this.entityData.get(DROPS_ON_BLOCK));
        tag.putInt("SmokeTicksRemaining", this.smokeTicksRemaining);
        tag.putInt("KnockbackStrength", this.knockbackStrength);
        if (this.configuredDamage >= 0.0F) {
            tag.putFloat("ConfiguredDamage", this.configuredDamage);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void hurtTarget(Entity target) {
        if (this.level().isClientSide || !target.isAlive()) {
            return;
        }
        Entity owner = getOwner();
        DamageSource source = this.damageSources().thrown(this, owner);
        if (target.hurt(source, damage())) {
            applyKnockback(target);
            if (owner instanceof Player player) {
                PlayerTracker.logBattleExp(player, 1.0D);
            }
        }
    }

    private void applyKnockback(Entity target) {
        if (this.knockbackStrength <= 0 || !(target instanceof LivingEntity livingTarget)) {
            return;
        }
        Vec3 vector = getDeltaMovement().multiply(1.0D, 0.0D, 1.0D).normalize().scale(this.knockbackStrength * 0.6D);
        if (vector.lengthSqr() > 0.0D) {
            livingTarget.push(vector.x(), 0.1D, vector.z());
        }
    }

    private void spawnPickup(Vec3 point) {
        if (this.level().isClientSide) {
            return;
        }
        ItemEntity pickup = new ItemEntity(this.level(), point.x(), point.y(), point.z(), new ItemStack(defaultItem()));
        pickup.setPickUpDelay(10);
        this.level().addFreshEntity(pickup);
    }

    private void explode(Vec3 point) {
        if (this.level() instanceof ServerLevel serverLevel) {
            Entity owner = getOwner();
            boolean mobGriefing = ForgeEventFactory.getMobGriefingEvent(serverLevel, owner);
            serverLevel.explode(null, point.x(), point.y(), point.z(), 4.0F, mobGriefing, Level.ExplosionInteraction.MOB);
        }
        discard();
    }

    private void startSmoke(Vec3 point) {
        this.smokeTicksRemaining = SMOKE_TICKS;
        this.noPhysics = true;
        setDeltaMovement(Vec3.ZERO);
        moveTo(point.x(), point.y(), point.z(), getYRot(), getXRot());
    }

    private void spawnSmokeBurst() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int i = 0; i < 10; i++) {
            serverLevel.sendParticles(
                    ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED,
                            0xFF101010, 8, 40 + this.random.nextInt(21), 0, -1, 1),
                    getX(),
                    getY(),
                    getZ(),
                    20,
                    2.0D,
                    1.0D,
                    2.0D,
                    0.02D);
        }
    }

    private boolean isExplosiveKunai() {
        return getType() == ModEntityTypes.ENTITYBULLETKUNAI_EXPLOSIVE.get();
    }

    private boolean isSmokeBomb() {
        return getType() == ModEntityTypes.ENTITYBULLETSMOKE_BOMB.get();
    }

    private float damage() {
        if (this.configuredDamage >= 0.0F) {
            return this.configuredDamage;
        }
        if (getType() == ModEntityTypes.ENTITYBULLETSHURIKEN.get()) {
            return 4.0F;
        }
        if (getType() == ModEntityTypes.ENTITYBULLETSMOKE_BOMB.get()) {
            return 0.0F;
        }
        return 5.0F;
    }

    private Item defaultItem() {
        if (getType() == ModEntityTypes.ENTITYBULLETSHURIKEN.get()) {
            return ModItems.SHURIKEN.get();
        }
        if (getType() == ModEntityTypes.ENTITYBULLETKUNAI_EXPLOSIVE.get()) {
            return ModItems.KUNAI_EXPLOSIVE.get();
        }
        if (getType() == ModEntityTypes.ENTITYBULLETSMOKE_BOMB.get()) {
            return ModItems.SMOKE_BOMB.get();
        }
        return ModItems.KUNAI.get();
    }
}
