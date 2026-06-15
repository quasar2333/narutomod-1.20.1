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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.PlayerTracker;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModItems;

public final class KusanagiSwordEntity extends ThrowableItemProjectile {
    private static final double MAX_RANGE = 30.0D;
    private static final int RETURN_AFTER_TICKS = 200;
    private static final int OWNER_PICKUP_TICKS = 15;
    private static final int MAX_LIFE = 1200;
    private static final float DAMAGE = 20.0F;
    private static final EntityDataAccessor<Boolean> GROUNDED =
            SynchedEntityData.defineId(KusanagiSwordEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> TARGET_ID =
            SynchedEntityData.defineId(KusanagiSwordEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private UUID targetUuid;
    private int targetCooldown;
    private int recentHitId = -1;
    private int recentHitTicks;

    public KusanagiSwordEntity(EntityType<? extends KusanagiSwordEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public void configure(LivingEntity owner) {
        setOwner(owner);
        setKusanagiItem();
        Vec3 look = owner.getLookAngle();
        Vec3 eyes = owner.getEyePosition();
        moveTo(
                eyes.x() + look.x() * 2.0D,
                eyes.y() + look.y() * 2.0D,
                eyes.z() + look.z() * 2.0D,
                owner.getYHeadRot(),
                owner.getXRot());
        shoot(look.x(), look.y(), look.z(), 0.6F, 0.0F);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(GROUNDED, false);
        this.entityData.define(TARGET_ID, -1);
    }

    @Override
    public void tick() {
        if (this.recentHitTicks > 0 && --this.recentHitTicks <= 0) {
            this.recentHitId = -1;
        }
        if (isGrounded()) {
            tryPickup();
            if (!this.level().isClientSide && this.tickCount > MAX_LIFE) {
                spawnPickup();
                discard();
            }
            return;
        }

        super.tick();

        if (this.level().isClientSide || isRemoved() || isGrounded()) {
            return;
        }
        LivingEntity owner = getLivingOwner();
        if (owner == null || !owner.isAlive()) {
            setNoGravity(false);
            return;
        }
        tryPickup();
        updateTarget(owner);
        steer(owner);
        if (this.tickCount > MAX_LIFE) {
            setTarget(owner);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        if (this.level().isClientSide || !target.isAlive() || target == getOwner()) {
            return;
        }
        LivingEntity owner = getLivingOwner();
        if (target.hurt(ModDamageTypes.kusanagi(this.level(), this, owner), DAMAGE)
                && owner instanceof Player player) {
            PlayerTracker.logBattleExp(player, 1.0D);
        }
        if (target == getTarget()) {
            clearTarget();
            this.targetCooldown = 10;
            setDeltaMovement(Vec3.ZERO);
        }
        this.recentHitId = target.getId();
        this.recentHitTicks = 10;
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (this.level().isClientSide) {
            return;
        }
        this.entityData.set(GROUNDED, true);
        setDeltaMovement(Vec3.ZERO);
        setNoGravity(true);
        this.noPhysics = true;
        moveTo(result.getLocation().x(), result.getLocation().y(), result.getLocation().z(), getYRot(), getXRot());
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity)
                && entity != getOwner()
                && entity.getId() != this.recentHitId;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.KUSANAGI_SWORD.get();
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(GROUNDED, tag.getBoolean("Grounded"));
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        if (tag.hasUUID("Target")) {
            this.targetUuid = tag.getUUID("Target");
        }
        this.targetCooldown = tag.getInt("TargetCooldown");
        this.recentHitId = tag.getInt("RecentHitId");
        this.recentHitTicks = tag.getInt("RecentHitTicks");
        if (isGrounded()) {
            this.noPhysics = true;
            setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Grounded", isGrounded());
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        if (this.targetUuid != null) {
            tag.putUUID("Target", this.targetUuid);
        }
        tag.putInt("TargetCooldown", this.targetCooldown);
        tag.putInt("RecentHitId", this.recentHitId);
        tag.putInt("RecentHitTicks", this.recentHitTicks);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private boolean isGrounded() {
        return this.entityData.get(GROUNDED);
    }

    private void setKusanagiItem() {
        ItemStack stack = new ItemStack(ModItems.KUSANAGI_SWORD.get());
        stack.getOrCreateTag().putBoolean("inAir", true);
        setItem(stack);
    }

    private void setOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        super.setOwner(owner);
    }

    @Nullable
    private LivingEntity getLivingOwner() {
        Entity owner = getOwner();
        if (owner instanceof LivingEntity living) {
            return living;
        }
        if (this.ownerUuid != null && this.level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(this.ownerUuid) instanceof LivingEntity living) {
            super.setOwner(living);
            return living;
        }
        return null;
    }

    private void updateTarget(LivingEntity owner) {
        Entity currentTarget = getTarget();
        double ownerDistance = distanceTo(owner);
        if (ownerDistance > MAX_RANGE + 10.0D || this.tickCount > RETURN_AFTER_TICKS) {
            setTarget(owner);
            return;
        }
        if (ownerDistance <= MAX_RANGE && currentTarget == owner) {
            clearTarget();
            currentTarget = null;
        }
        if (currentTarget instanceof LivingEntity living && !living.isAlive()) {
            clearTarget();
            currentTarget = null;
        }
        if (currentTarget == null && --this.targetCooldown <= 0) {
            Entity found = owner instanceof Mob mob ? mob.getTarget() : null;
            if (found == null) {
                HitResult hit = ProcedureUtils.objectEntityLookingAt(owner, MAX_RANGE, 0.0D, false, false,
                        target -> target != owner && target != this);
                if (hit instanceof EntityHitResult entityHit) {
                    found = entityHit.getEntity();
                }
            }
            if (found != null && found.isAlive()) {
                setTarget(found);
            }
            this.targetCooldown = 10;
        }
    }

    private void steer(LivingEntity owner) {
        Entity target = getTarget();
        Vec3 direction;
        float speed;
        if (target != null && target.isAlive()) {
            if (target != owner && target.distanceTo(owner) > MAX_RANGE) {
                clearTarget();
                setDeltaMovement(Vec3.ZERO);
                return;
            }
            direction = target.getEyePosition().subtract(position());
            speed = 0.95F;
        } else {
            direction = owner.getLookAngle();
            speed = 0.6F;
        }
        if (direction.lengthSqr() <= 1.0E-8D) {
            return;
        }
        shoot(direction.x(), direction.y(), direction.z(), speed, 0.0F);
    }

    @Nullable
    private Entity getTarget() {
        int targetId = this.entityData.get(TARGET_ID);
        if (targetId >= 0) {
            Entity entity = this.level().getEntity(targetId);
            if (entity != null) {
                return entity;
            }
        }
        if (this.targetUuid != null && this.level() instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(this.targetUuid);
            if (entity != null) {
                this.entityData.set(TARGET_ID, entity.getId());
                return entity;
            }
        }
        return null;
    }

    private void setTarget(Entity target) {
        this.targetUuid = target.getUUID();
        this.entityData.set(TARGET_ID, target.getId());
    }

    private void clearTarget() {
        this.targetUuid = null;
        this.entityData.set(TARGET_ID, -1);
    }

    private void tryPickup() {
        LivingEntity owner = getLivingOwner();
        if (owner instanceof Player player && this.tickCount > OWNER_PICKUP_TICKS && distanceToSqr(player) < 4.0D) {
            giveSwordTo(player);
            return;
        }
        if (owner == null && isGrounded()) {
            for (Player player : this.level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(1.0D))) {
                giveSwordTo(player);
                return;
            }
        }
    }

    private void giveSwordTo(Player player) {
        if (this.level().isClientSide) {
            return;
        }
        ItemStack stack = new ItemStack(ModItems.KUSANAGI_SWORD.get());
        if (!player.getInventory().add(stack)) {
            ItemEntity drop = new ItemEntity(this.level(), getX(), getY(), getZ(), stack);
            drop.setPickUpDelay(0);
            this.level().addFreshEntity(drop);
        }
        discard();
    }

    private void spawnPickup() {
        if (this.level().isClientSide) {
            return;
        }
        ItemEntity drop = new ItemEntity(this.level(), getX(), getY(), getZ(), new ItemStack(ModItems.KUSANAGI_SWORD.get()));
        drop.setPickUpDelay(10);
        this.level().addFreshEntity(drop);
    }
}
