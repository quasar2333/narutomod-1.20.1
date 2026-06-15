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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.registry.ModParticleTypes;

public final class HidingInAshEntity extends Entity {
    private static final int MAX_LIFE = 110;
    private static final EntityDataAccessor<Integer> USER_ID = SynchedEntityData.defineId(HidingInAshEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RANGE = SynchedEntityData.defineId(HidingInAshEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID userUuid;

    public HidingInAshEntity(EntityType<? extends HidingInAshEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    public void configure(LivingEntity user, float range) {
        setUser(user);
        setRange(range);
        setIdlePosition();
        user.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, MAX_LIFE, 0, false, false));
    }

    public float getRange() {
        return this.entityData.get(RANGE);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(USER_ID, -1);
        this.entityData.define(RANGE, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        setIdlePosition();
        if (this.level().isClientSide) {
            spawnBurningAsh();
            return;
        }
        if (this.tickCount > MAX_LIFE) {
            discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("User")) {
            this.userUuid = tag.getUUID("User");
        }
        setRange(tag.contains("Range") ? tag.getFloat("Range") : 1.0F);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.userUuid != null) {
            tag.putUUID("User", this.userUuid);
        }
        tag.putFloat("Range", getRange());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void setUser(LivingEntity user) {
        this.userUuid = user.getUUID();
        this.entityData.set(USER_ID, user.getId());
    }

    @Nullable
    private LivingEntity getUser() {
        int userId = this.entityData.get(USER_ID);
        if (userId >= 0 && this.level().getEntity(userId) instanceof LivingEntity living) {
            return living;
        }
        if (this.userUuid != null && this.level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(this.userUuid) instanceof LivingEntity living) {
            setUser(living);
            return living;
        }
        return null;
    }

    private void setRange(float range) {
        this.entityData.set(RANGE, Math.max(range, 1.0F));
    }

    private void setIdlePosition() {
        LivingEntity user = getUser();
        if (user == null) {
            return;
        }
        Vec3 look = user.getLookAngle();
        this.moveTo(
                user.getX() + look.x(),
                user.getY() + user.getEyeHeight() + look.y() - 0.2D,
                user.getZ() + look.z(),
                user.getYRot(),
                user.getXRot());
    }

    private void spawnBurningAsh() {
        float range = getRange();
        for (int i = 0; i < (int)(range * 20.0F); i++) {
            this.level().addParticle(
                    ModParticleTypes.options(NarutoParticleKind.BURNING_ASH, this.entityData.get(USER_ID)),
                    getX(),
                    getY(),
                    getZ(),
                    range * (this.random.nextDouble() - 0.5D) * 0.1D,
                    range * (this.random.nextDouble() - 0.5D) * 0.1D,
                    range * (this.random.nextDouble() - 0.5D) * 0.1D);
        }
    }
}
