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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEffects;

public final class LightningArcEntity extends Entity {
    private static final EntityDataAccessor<Float> END_X = SynchedEntityData.defineId(LightningArcEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> END_Y = SynchedEntityData.defineId(LightningArcEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> END_Z = SynchedEntityData.defineId(LightningArcEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(LightningArcEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> THICKNESS = SynchedEntityData.defineId(LightningArcEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> MAX_RECURSIVE_DEPTH = SynchedEntityData.defineId(LightningArcEntity.class, EntityDataSerializers.INT);

    @Nullable
    private Vec3 originalEnd;
    private float inaccuracy;
    private int livingTime;
    private float damageAmount;
    private boolean resetHurtTime;
    private boolean senjutsuDamage;
    @Nullable
    private UUID ownerUuid;

    public LightningArcEntity(EntityType<? extends LightningArcEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.livingTime = this.random.nextInt(3) + 1;
    }

    public void configureBetween(Vec3 from, Vec3 to, int color, int duration, float inaccuracy, float thickness, int sections) {
        this.moveTo(from.x(), from.y(), from.z(), 0.0F, 0.0F);
        this.originalEnd = to;
        this.inaccuracy = inaccuracy;
        setEndVec(jitter(to, inaccuracy));
        setColor(color);
        setThickness(thickness);
        setMaxRecursiveDepth(sections);
        if (duration > 0) {
            this.livingTime = duration;
        }
    }

    public void configureRandom(Vec3 center, double length, Vec3 motion, int color, int duration, float thickness, float inaccuracy) {
        Vec3 end = center.add(
                (this.random.nextDouble() - 0.5D) * length * 2.0D,
                (this.random.nextDouble() - 0.5D) * length * 2.0D,
                (this.random.nextDouble() - 0.5D) * length * 2.0D);
        configureBetween(center, end, color, duration, inaccuracy, thickness, 4);
        this.setDeltaMovement(motion);
    }

    public void setDamage(float amount, @Nullable LivingEntity owner) {
        setDamage(amount, false, owner);
    }

    public void setDamage(float amount, boolean resetHurtTime, @Nullable LivingEntity owner) {
        this.damageAmount = amount;
        this.resetHurtTime = resetHurtTime;
        this.ownerUuid = owner == null ? null : owner.getUUID();
    }

    public void setSenjutsuDamage(float amount, boolean resetHurtTime, @Nullable LivingEntity owner) {
        setDamage(amount, resetHurtTime, owner);
        this.senjutsuDamage = true;
    }

    public Vec3 getEndVec() {
        return new Vec3(this.entityData.get(END_X), this.entityData.get(END_Y), this.entityData.get(END_Z));
    }

    public int getColor() {
        return this.entityData.get(COLOR);
    }

    public float getThickness() {
        return this.entityData.get(THICKNESS);
    }

    public int getMaxRecursiveDepth() {
        return this.entityData.get(MAX_RECURSIVE_DEPTH);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(END_X, 0.0F);
        this.entityData.define(END_Y, 0.0F);
        this.entityData.define(END_Z, 0.0F);
        this.entityData.define(COLOR, 0xC00000FF);
        this.entityData.define(THICKNESS, 0.0F);
        this.entityData.define(MAX_RECURSIVE_DEPTH, 4);
    }

    @Override
    public void tick() {
        if (this.inaccuracy > 0.0F && this.originalEnd != null) {
            setEndVec(jitter(this.originalEnd, this.inaccuracy));
        }
        Vec3 motion = getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-8D) {
            this.setPos(this.getX() + motion.x(), this.getY() + motion.y(), this.getZ() + motion.z());
        }
        if (!this.level().isClientSide && this.damageAmount > 0.0F) {
            damageIntersectingEntities();
        }
        if (!this.level().isClientSide && --this.livingTime <= 0) {
            discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setEndVec(new Vec3(tag.getDouble("EndX"), tag.getDouble("EndY"), tag.getDouble("EndZ")));
        this.originalEnd = tag.contains("OriginalEndX")
                ? new Vec3(tag.getDouble("OriginalEndX"), tag.getDouble("OriginalEndY"), tag.getDouble("OriginalEndZ"))
                : getEndVec();
        setColor(tag.contains("Color") ? tag.getInt("Color") : 0xC00000FF);
        setThickness(tag.getFloat("Thickness"));
        setMaxRecursiveDepth(tag.contains("Depth") ? tag.getInt("Depth") : 4);
        this.inaccuracy = tag.getFloat("Inaccuracy");
        this.livingTime = tag.contains("Life") ? tag.getInt("Life") : 1;
        this.damageAmount = tag.getFloat("Damage");
        this.resetHurtTime = tag.getBoolean("ResetHurtTime");
        this.senjutsuDamage = tag.getBoolean("SenjutsuDamage");
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        Vec3 end = getEndVec();
        tag.putDouble("EndX", end.x());
        tag.putDouble("EndY", end.y());
        tag.putDouble("EndZ", end.z());
        if (this.originalEnd != null) {
            tag.putDouble("OriginalEndX", this.originalEnd.x());
            tag.putDouble("OriginalEndY", this.originalEnd.y());
            tag.putDouble("OriginalEndZ", this.originalEnd.z());
        }
        tag.putInt("Color", getColor());
        tag.putFloat("Thickness", getThickness());
        tag.putInt("Depth", getMaxRecursiveDepth());
        tag.putFloat("Inaccuracy", this.inaccuracy);
        tag.putInt("Life", this.livingTime);
        tag.putFloat("Damage", this.damageAmount);
        tag.putBoolean("ResetHurtTime", this.resetHurtTime);
        tag.putBoolean("SenjutsuDamage", this.senjutsuDamage);
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void setEndVec(Vec3 vec) {
        this.entityData.set(END_X, (float) vec.x());
        this.entityData.set(END_Y, (float) vec.y());
        this.entityData.set(END_Z, (float) vec.z());
    }

    private void setColor(int color) {
        this.entityData.set(COLOR, color);
    }

    private void setThickness(float thickness) {
        this.entityData.set(THICKNESS, thickness);
    }

    private void setMaxRecursiveDepth(int depth) {
        this.entityData.set(MAX_RECURSIVE_DEPTH, Math.max(depth, 0));
    }

    private Vec3 jitter(Vec3 vec, float amount) {
        if (amount <= 0.0F) {
            return vec;
        }
        return vec.add(
                (this.random.nextFloat() - 0.5D) * amount * 2.0D,
                this.random.nextFloat() * amount * 2.0D,
                this.random.nextFloat() * amount * 2.0D);
    }

    @Nullable
    private LivingEntity getOwner() {
        if (this.ownerUuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(this.ownerUuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    private void damageIntersectingEntities() {
        Vec3 start = position();
        Vec3 end = this.originalEnd == null ? getEndVec() : this.originalEnd;
        AABB search = getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        LivingEntity owner = getOwner();
        DamageSource source = this.senjutsuDamage
                ? ModDamageTypes.senjutsu(this.level(), this, owner)
                : ModDamageTypes.ninjutsu(this.level(), this, owner);
        for (Entity entity : this.level().getEntities(this, search, target -> target != owner && target.isAlive())) {
            if (entity.getBoundingBox().inflate(0.25D).clip(start, end).isPresent()) {
                onStruck(entity, source, this.damageAmount, this.resetHurtTime);
            }
        }
    }

    public static void onStruck(Entity entity, DamageSource source, float damage, boolean resetHurtTime) {
        if (resetHurtTime) {
            entity.invulnerableTime = 0;
        }
        entity.hurt(source, damage);
        boolean wasBurning = entity.isOnFire();
        if (entity.level() instanceof ServerLevel serverLevel) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(serverLevel);
            if (bolt != null) {
                bolt.moveTo(entity.getX(), entity.getY(), entity.getZ());
                bolt.setVisualOnly(true);
                entity.thunderHit(serverLevel, bolt);
            }
        }
        if (!wasBurning) {
            entity.clearFire();
        }
        if (entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(ModEffects.PARALYSIS.get(), 100, 2 + (int)(damage * 0.1F), false, false));
        }
    }
}
