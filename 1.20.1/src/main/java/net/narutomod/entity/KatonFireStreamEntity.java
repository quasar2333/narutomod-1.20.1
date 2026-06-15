package net.narutomod.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class KatonFireStreamEntity extends Entity {
    public static final int ANNIHILATION_WAIT_TICKS = 50;
    public static final int DEFAULT_MAX_LIFE = 110;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(KatonFireStreamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> WIDTH = SynchedEntityData.defineId(KatonFireStreamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> RANGE = SynchedEntityData.defineId(KatonFireStreamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> WAIT_TICKS = SynchedEntityData.defineId(KatonFireStreamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MAX_LIFE = SynchedEntityData.defineId(KatonFireStreamEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;

    public KatonFireStreamEntity(EntityType<? extends KatonFireStreamEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public void configure(LivingEntity owner, float width, float range, int waitTicks, int maxLife) {
        setOwner(owner);
        setWidth(width);
        setRange(range);
        setWaitTicks(waitTicks);
        setMaxLife(maxLife);
        setIdlePosition(owner);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(WIDTH, 1.0F);
        this.entityData.define(RANGE, 1.0F);
        this.entityData.define(WAIT_TICKS, ANNIHILATION_WAIT_TICKS);
        this.entityData.define(MAX_LIFE, DEFAULT_MAX_LIFE);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive() || this.tickCount > getMaxLife() || isInWater()) {
            discard();
            return;
        }

        setIdlePosition(owner);
        if (this.tickCount > getWaitTicks()) {
            double factor = currentFalloff();
            double activeRange = getRange() * factor;
            double activeWidth = getWidth() * factor;
            executeFireStream(owner, activeRange, activeWidth);
            if (this.tickCount % 10 == 1) {
                this.level().playSound(null, getX(), getY(), getZ(),
                        ModSounds.SOUND_FLAMETHROW.get(), SoundSource.NEUTRAL, 1.0F, this.random.nextFloat() * 0.5F + 0.6F);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        setWidth(tag.contains("Width") ? tag.getFloat("Width") : 1.0F);
        setRange(tag.contains("Range") ? tag.getFloat("Range") : 1.0F);
        setWaitTicks(tag.contains("WaitTicks") ? tag.getInt("WaitTicks") : ANNIHILATION_WAIT_TICKS);
        setMaxLife(tag.contains("MaxLife") ? tag.getInt("MaxLife") : DEFAULT_MAX_LIFE);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putFloat("Width", getWidth());
        tag.putFloat("Range", getRange());
        tag.putInt("WaitTicks", getWaitTicks());
        tag.putInt("MaxLife", getMaxLife());
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

    private float getWidth() {
        return this.entityData.get(WIDTH);
    }

    private void setWidth(float width) {
        this.entityData.set(WIDTH, Math.max(width, 0.01F));
    }

    private float getRange() {
        return this.entityData.get(RANGE);
    }

    private void setRange(float range) {
        this.entityData.set(RANGE, Math.max(range, 0.1F));
    }

    private int getWaitTicks() {
        return this.entityData.get(WAIT_TICKS);
    }

    private void setWaitTicks(int waitTicks) {
        this.entityData.set(WAIT_TICKS, Math.max(waitTicks, 0));
    }

    private int getMaxLife() {
        return this.entityData.get(MAX_LIFE);
    }

    private void setMaxLife(int maxLife) {
        this.entityData.set(MAX_LIFE, Math.max(maxLife, 1));
    }

    private void setIdlePosition(LivingEntity owner) {
        Vec3 look = owner.getLookAngle();
        this.moveTo(
                owner.getX() + look.x(),
                owner.getY() + owner.getEyeHeight() + look.y() - 0.2D,
                owner.getZ() + look.z(),
                owner.getYRot(),
                owner.getXRot());
        this.setDeltaMovement(Vec3.ZERO);
    }

    private double currentFalloff() {
        double life = Math.max(getMaxLife(), 1);
        double progress = (double)this.tickCount / life;
        return Math.max(1.0D - progress * progress * 0.8D, 0.0D);
    }

    private void executeFireStream(LivingEntity owner, double range, double farRadius) {
        if (!(this.level() instanceof ServerLevel serverLevel) || range <= 0.0D || farRadius <= 0.0D) {
            return;
        }
        Vec3 start = owner.getEyePosition().subtract(0.0D, 0.4D, 0.0D);
        Vec3 look = owner.getLookAngle().normalize();
        spawnFlameParticles(serverLevel, owner, start, look, range, farRadius);
        damageEntities(owner, start, look, range, farRadius);
        igniteBlocks(serverLevel, start, look, range, farRadius);
    }

    private void damageEntities(LivingEntity owner, Vec3 start, Vec3 look, double range, double farRadius) {
        Vec3 ray = look.scale(range);
        AABB search = owner.getBoundingBox().expandTowards(ray).inflate(Math.max(farRadius + 1.0D, 1.0D));
        Set<Integer> damaged = new HashSet<>();
        DamageSource source = ModDamageTypes.katonFireStream(this.level(), this, owner);
        for (Entity target : this.level().getEntities(this, search, target -> canAffect(owner, target))) {
            double distance = owner.distanceTo(target);
            double allowedRadius = distance / range * farRadius + 1.0D;
            AABB box = target.getBoundingBox().inflate(allowedRadius);
            if ((box.contains(start) || box.clip(start, start.add(ray)).isPresent()) && damaged.add(target.getId())) {
                float damage = (float)(range * (this.random.nextDouble() * 0.5D + 0.5D));
                target.hurt(source, damage);
                target.setSecondsOnFire(10);
            }
        }
    }

    private boolean canAffect(LivingEntity owner, Entity target) {
        return target.isAlive()
                && target != owner
                && target.getRootVehicle() != owner.getRootVehicle()
                && !(target instanceof KatonFireStreamEntity);
    }

    private void spawnFlameParticles(ServerLevel level, LivingEntity owner, Vec3 start, Vec3 look, double range, double farRadius) {
        int count = (int)(range * farRadius * 0.8D);
        double angle = Math.atan(farRadius / range);
        for (int i = 0; i < count; i++) {
            Vec3 direction = randomDirectionInCone(owner, look, angle * 3.0D);
            Vec3 motion = direction.scale(range * 0.1D);
            int scale = Math.max((int)(motion.length() * 50.0D) + this.random.nextInt(20), 1);
            level.sendParticles(
                    ModParticleTypes.options(NarutoParticleKind.FLAME_COLORED, 0xFFFFCF00, scale),
                    start.x() + look.x(),
                    start.y() + look.y(),
                    start.z() + look.z(),
                    0,
                    motion.x(),
                    motion.y(),
                    motion.z(),
                    1.0D);
        }
    }

    private Vec3 randomDirectionInCone(LivingEntity owner, Vec3 fallback, double maxRadians) {
        float pitch = owner.getXRot() + (float)((this.random.nextDouble() - 0.5D) * maxRadians * Mth.RAD_TO_DEG);
        float yaw = owner.getYRot() + (float)((this.random.nextDouble() - 0.5D) * maxRadians * Mth.RAD_TO_DEG);
        Vec3 direction = Vec3.directionFromRotation(pitch, yaw);
        return direction.lengthSqr() <= 1.0E-8D ? fallback : direction.normalize();
    }

    private void igniteBlocks(ServerLevel level, Vec3 start, Vec3 look, double range, double farRadius) {
        if (range <= 4.0D) {
            return;
        }
        int samples = Mth.clamp((int)(range * farRadius * 0.25D), 1, 96);
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() <= 1.0E-8D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        }
        right = right.normalize();
        Vec3 up = right.cross(look).normalize();
        for (int i = 0; i < samples; i++) {
            double distance = 4.0D + this.random.nextDouble() * (range - 4.0D);
            if (this.random.nextFloat() >= 0.1F) {
                continue;
            }
            double radius = distance / range * farRadius;
            Vec3 sample = start.add(look.scale(distance))
                    .add(right.scale((this.random.nextDouble() - 0.5D) * radius * 2.0D))
                    .add(up.scale((this.random.nextDouble() - 0.5D) * radius * 2.0D));
            igniteAround(level, BlockPos.containing(sample));
        }
    }

    private void igniteAround(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).isAir()) {
            tryPlaceFire(level, pos);
            return;
        }
        for (Direction direction : Direction.values()) {
            tryPlaceFire(level, pos.relative(direction));
        }
    }

    private void tryPlaceFire(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir() || level.getBlockState(pos.below()).is(Blocks.FIRE)) {
            return;
        }
        var fireState = BaseFireBlock.getState(level, pos);
        if (fireState.canSurvive(level, pos)) {
            level.setBlock(pos, fireState, 3);
        }
    }
}
