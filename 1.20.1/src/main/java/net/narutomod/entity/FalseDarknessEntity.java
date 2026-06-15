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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class FalseDarknessEntity extends Entity {
    public static final float BASE_DAMAGE = 30.0F;
    private static final int BLACK_LIGHTNING_COLOR = 0x000000FF;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(FalseDarknessEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(FalseDarknessEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> POWER = SynchedEntityData.defineId(FalseDarknessEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private UUID targetUuid;

    public FalseDarknessEntity(EntityType<? extends FalseDarknessEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public void configure(LivingEntity owner, LivingEntity target, float power) {
        setOwner(owner);
        setTarget(target);
        setPower(power);
        moveToOwner(owner);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(TARGET_ID, -1);
        this.entityData.define(POWER, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        LivingEntity owner = getOwner();
        if (owner == null) {
            discard();
            return;
        }
        moveToOwner(owner);
        int buildTime = Math.max(1, (int)(getPower() * 20.0F));
        if (this.tickCount <= buildTime) {
            charge(buildTime);
            return;
        }
        LivingEntity target = getTarget();
        if (target != null) {
            fire(owner, target);
        }
        discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        if (tag.hasUUID("Target")) {
            this.targetUuid = tag.getUUID("Target");
        }
        setPower(tag.contains("Power") ? tag.getFloat("Power") : 1.0F);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        if (this.targetUuid != null) {
            tag.putUUID("Target", this.targetUuid);
        }
        tag.putFloat("Power", getPower());
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

    private void setTarget(LivingEntity target) {
        this.targetUuid = target.getUUID();
        this.entityData.set(TARGET_ID, target.getId());
    }

    private void setPower(float power) {
        this.entityData.set(POWER, Math.max(power, 0.1F));
    }

    private float getPower() {
        return this.entityData.get(POWER);
    }

    @Nullable
    private LivingEntity getOwner() {
        int ownerId = this.entityData.get(OWNER_ID);
        if (ownerId >= 0) {
            Entity entity = this.level().getEntity(ownerId);
            if (entity instanceof LivingEntity living) {
                return living;
            }
        }
        if (this.ownerUuid != null && this.level() instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(this.ownerUuid);
            if (entity instanceof LivingEntity living) {
                setOwner(living);
                return living;
            }
        }
        return null;
    }

    @Nullable
    private LivingEntity getTarget() {
        int targetId = this.entityData.get(TARGET_ID);
        if (targetId >= 0) {
            Entity entity = this.level().getEntity(targetId);
            if (entity instanceof LivingEntity living) {
                return living;
            }
        }
        if (this.targetUuid != null && this.level() instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(this.targetUuid);
            if (entity instanceof LivingEntity living) {
                setTarget(living);
                return living;
            }
        }
        return null;
    }

    private void moveToOwner(LivingEntity owner) {
        this.setDeltaMovement(Vec3.ZERO);
        this.moveTo(owner.getX(), owner.getEyeY() - 0.2D, owner.getZ(), owner.getYRot(), owner.getXRot());
    }

    private void charge(int buildTime) {
        float progress = Math.min((float)this.tickCount / (float)buildTime, 1.0F);
        if (this.random.nextFloat() <= progress * 0.2F) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.SOUND_ELECTRICITY.get(), SoundSource.PLAYERS, 0.4F, this.random.nextFloat() * 0.5F + 1.5F);
        }
        int arcCount = (int)(progress * 8.0F);
        for (int i = 0; i < arcCount; i++) {
            spawnChargeArc();
        }
    }

    private void spawnChargeArc() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LightningArcEntity arc = ModEntityTypes.LIGHTNING_ARC.get().create(serverLevel);
        if (arc == null) {
            return;
        }
        Vec3 center = new Vec3(
                this.getX() + (this.random.nextDouble() - 0.5D) * 0.6D,
                this.getY() + (this.random.nextDouble() - 0.5D) * 0.6D,
                this.getZ() + (this.random.nextDouble() - 0.5D) * 0.6D);
        arc.configureRandom(center, 0.15D, Vec3.ZERO, BLACK_LIGHTNING_COLOR, 0, 0.0F, 0.1F);
        serverLevel.addFreshEntity(arc);
    }

    private void fire(LivingEntity owner, LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSounds.SOUND_ELECTRICITY.get(), SoundSource.PLAYERS, 10.0F, this.random.nextFloat() * 0.6F + 0.3F);
        this.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 2.0F, 0.5F + this.random.nextFloat() * 0.2F);
        LightningArcEntity arc = ModEntityTypes.LIGHTNING_ARC.get().create(serverLevel);
        if (arc == null) {
            return;
        }
        arc.configureBetween(this.position(), target.getEyePosition(), BLACK_LIGHTNING_COLOR, 40, 0.0F, 0.0F, 4);
        arc.setDamage(BASE_DAMAGE * getPower(), owner);
        serverLevel.addFreshEntity(arc);
    }
}
