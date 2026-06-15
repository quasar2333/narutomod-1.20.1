package net.narutomod.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

public abstract class AbstractPuppetScrollEntity extends Entity {
    public static final int OPEN_SCROLL_TIME = 30;
    private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(AbstractPuppetScrollEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(AbstractPuppetScrollEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> PUPPET_HEALTH = SynchedEntityData.defineId(AbstractPuppetScrollEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUuid;

    protected AbstractPuppetScrollEntity(EntityType<? extends AbstractPuppetScrollEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
        this.noPhysics = true;
    }

    public void configure(LivingEntity owner, float puppetHealth) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
        setAge(0);
        setPuppetHealth(puppetHealth);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(AGE, 0);
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(PUPPET_HEALTH, 1.0F);
    }

    @Override
    public void tick() {
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();
        this.xRotO = getXRot();
        this.yRotO = getYRot();
        setDeltaMovement(0.0D, 0.0D, 0.0D);
        setAge(getAge() + 1);
        if (!this.level().isClientSide && getAge() > OPEN_SCROLL_TIME) {
            openScroll();
        }
    }

    private void openScroll() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            discard();
            return;
        }
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        AbstractPuppetEntity puppet = createPuppet(serverLevel);
        if (puppet != null) {
            puppet.moveTo(getX(), getY(), getZ(), owner.getYRot(), 0.0F);
            puppet.setHealth(Mth.clamp(getPuppetHealth(), 1.0F, puppet.getMaxHealth()));
            puppet.bindTo(owner);
            serverLevel.addFreshEntity(puppet);
            spawnPoof(serverLevel, puppet);
        }
        discard();
    }

    public static void spawnPoof(Level level, Entity entity) {
        if (level instanceof ServerLevel serverLevel) {
            spawnPoof(serverLevel, entity);
        }
    }

    private static void spawnPoof(ServerLevel level, Entity entity) {
        level.sendParticles(ParticleTypes.POOF, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(),
                24, entity.getBbWidth() * 0.35D, entity.getBbHeight() * 0.25D, entity.getBbWidth() * 0.35D, 0.02D);
    }

    @Nullable
    protected abstract AbstractPuppetEntity createPuppet(ServerLevel level);

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
                this.entityData.set(OWNER_ID, living.getId());
                return living;
            }
        }
        return null;
    }

    public int getAge() {
        return this.entityData.get(AGE);
    }

    private void setAge(int age) {
        this.entityData.set(AGE, Math.max(age, 0));
    }

    public float getPuppetHealth() {
        return this.entityData.get(PUPPET_HEALTH);
    }

    private void setPuppetHealth(float puppetHealth) {
        this.entityData.set(PUPPET_HEALTH, Math.max(puppetHealth, 1.0F));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 64.0D * getViewScale();
        return distance < range * range;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        setAge(tag.getInt("Age"));
        setPuppetHealth(tag.contains("PuppetHealth") ? tag.getFloat("PuppetHealth") : 1.0F);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putInt("Age", getAge());
        tag.putFloat("PuppetHealth", getPuppetHealth());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
