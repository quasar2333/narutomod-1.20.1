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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class LaserCircusEntity extends Entity {
    private static final int LIGHTNING_COLOR = 0xC00000FF;
    private static final double TARGET_RANGE = 25.0D;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(LaserCircusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(LaserCircusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DAMAGE_XP = SynchedEntityData.defineId(LaserCircusEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;

    public LaserCircusEntity(EntityType<? extends LaserCircusEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public void configure(LivingEntity owner, float power, int damageXp) {
        setOwner(owner);
        setDuration(Math.max((int)(power * 20.0F), 1));
        setDamageXp(damageXp);
        moveToIdlePosition(owner);
    }

    public static boolean spawnFrom(LivingEntity owner, float power, int damageXp) {
        LaserCircusEntity entity = ModEntityTypes.LASER_CIRCUS.get().create(owner.level());
        if (entity == null) {
            return false;
        }
        entity.configure(owner, power, damageXp);
        LaserRingEntity.spawnOrGet(owner);
        return owner.level().addFreshEntity(entity);
    }

    @Nullable
    public static LaserCircusEntity findActive(ServerLevel level, LivingEntity owner) {
        UUID ownerId = owner.getUUID();
        return level.getEntitiesOfClass(LaserCircusEntity.class, owner.getBoundingBox().inflate(128.0D),
                        entity -> entity.ownerUuid != null && entity.ownerUuid.equals(ownerId))
                .stream()
                .findFirst()
                .orElse(null);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(DURATION, 1);
        this.entityData.define(DAMAGE_XP, 0);
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive() || this.tickCount > getDuration()) {
            discardWithRing(owner);
            return;
        }
        moveToIdlePosition(owner);
        if (this.level().isClientSide) {
            return;
        }
        if (this.tickCount % 10 == 0) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.SOUND_ELECTRICITY.get(), SoundSource.PLAYERS, 1.0F, this.random.nextFloat() * 0.6F + 0.6F);
        }
        HitResult result = ProcedureUtils.objectEntityLookingAt(owner, TARGET_RANGE);
        if (result != null) {
            spawnLightningAt((ServerLevel)this.level(), owner, result.getLocation());
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
        setDuration(tag.contains("Duration") ? tag.getInt("Duration") : 1);
        setDamageXp(tag.getInt("DamageXp"));
        this.tickCount = tag.getInt("Life");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putInt("Duration", getDuration());
        tag.putInt("DamageXp", getDamageXp());
        tag.putInt("Life", this.tickCount);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void spawnLightningAt(ServerLevel level, LivingEntity owner, Vec3 target) {
        LightningArcEntity arc = ModEntityTypes.LIGHTNING_ARC.get().create(level);
        if (arc == null) {
            return;
        }
        arc.configureBetween(this.position(), target, LIGHTNING_COLOR, 10, 0.1F, 0.0F, 4);
        float damage = this.random.nextFloat() * 0.05F * getDamageXp();
        if (damage > 0.0F) {
            arc.setDamage(damage, owner);
        }
        level.addFreshEntity(arc);
    }

    private void moveToIdlePosition(LivingEntity owner) {
        Vec3 look = owner.getLookAngle();
        if (look.lengthSqr() <= 1.0E-8D) {
            look = new Vec3(0.0D, 0.0D, 1.0D);
        }
        Vec3 pos = owner.position().add(look.x(), 1.2D, look.z());
        this.setDeltaMovement(Vec3.ZERO);
        this.moveTo(pos.x(), pos.y(), pos.z(), owner.getYRot(), 0.0F);
    }

    private void discardWithRing(@Nullable LivingEntity owner) {
        if (!this.level().isClientSide && owner != null && this.level() instanceof ServerLevel level) {
            LaserRingEntity ring = LaserRingEntity.findActive(level, owner);
            if (ring != null) {
                ring.discard();
            }
        }
        discard();
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

    private int getDuration() {
        return this.entityData.get(DURATION);
    }

    private void setDuration(int duration) {
        this.entityData.set(DURATION, Math.max(duration, 1));
    }

    private int getDamageXp() {
        return this.entityData.get(DAMAGE_XP);
    }

    private void setDamageXp(int damageXp) {
        this.entityData.set(DAMAGE_XP, Math.max(damageXp, 0));
    }
}
