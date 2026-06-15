package net.narutomod.entity;

import java.util.Comparator;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModParticleTypes;

public final class AsakujakuFireballEntity extends Entity implements ItemSupplier {
    private static final int BURST_COUNT = 10;
    private static final int MAX_LIFE = 12;
    private static final double ACCELERATION_SCALE = 0.1D;
    private static final float MOTION_FACTOR = 1.1F;
    private static final float DAMAGE_MULTIPLIER = 0.5F;
    private static final float EXPLOSION_STRENGTH = 2.0F;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(AsakujakuFireballEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(AsakujakuFireballEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUuid;
    private Vec3 acceleration = Vec3.ZERO;
    private int ticksInAir;

    public AsakujakuFireballEntity(EntityType<? extends AsakujakuFireballEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    public static int spawnBurst(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel level)) {
            return 0;
        }
        Vec3 look = owner.getLookAngle();
        if (look.lengthSqr() <= 1.0E-8D) {
            look = Vec3.directionFromRotation(owner.getXRot(), owner.getYHeadRot());
        }
        int spawned = 0;
        for (int i = 0; i < BURST_COUNT; i++) {
            AsakujakuFireballEntity entity = ModEntityTypes.ASAKUJAKU_FIREBALL.get().create(level);
            if (entity == null) {
                continue;
            }
            entity.configure(owner, look);
            level.addFreshEntity(entity);
            spawned++;
        }
        return spawned;
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.FIRE_CHARGE);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(DAMAGE, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        LivingEntity owner = getOwner();
        if ((owner != null && !owner.isAlive()) || !this.level().hasChunkAt(blockPosition())) {
            discard();
            return;
        }
        travelAndImpact();
        if (isRemoved()) {
            return;
        }
        spawnFlameParticles();
        applySmallFireballMotion();
        if (this.tickCount > MAX_LIFE) {
            discard();
        }
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        this.entityData.set(DAMAGE, tag.contains("Damage") ? tag.getFloat("Damage") : 1.0F);
        this.acceleration = tag.contains("AccelerationX")
                ? new Vec3(tag.getDouble("AccelerationX"), tag.getDouble("AccelerationY"), tag.getDouble("AccelerationZ"))
                : Vec3.ZERO;
        this.ticksInAir = tag.contains("TicksInAir") ? tag.getInt("TicksInAir") : tag.getInt("FlightTicks");
        if (tag.contains("MotionX")) {
            setDeltaMovement(tag.getDouble("MotionX"), tag.getDouble("MotionY"), tag.getDouble("MotionZ"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putFloat("Damage", this.entityData.get(DAMAGE));
        tag.putDouble("AccelerationX", this.acceleration.x());
        tag.putDouble("AccelerationY", this.acceleration.y());
        tag.putDouble("AccelerationZ", this.acceleration.z());
        tag.putInt("FlightTicks", this.ticksInAir);
        tag.putInt("TicksInAir", this.ticksInAir);
        Vec3 motion = getDeltaMovement();
        tag.putDouble("MotionX", motion.x());
        tag.putDouble("MotionY", motion.y());
        tag.putDouble("MotionZ", motion.z());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void configure(LivingEntity owner, Vec3 look) {
        setOwner(owner);
        this.entityData.set(DAMAGE, (float) ProcedureUtils.getModifiedAttackDamage(owner) * DAMAGE_MULTIPLIER);
        Vec3 direction = look;
        if (direction.lengthSqr() <= 1.0E-8D) {
            direction = Vec3.directionFromRotation(owner.getXRot(), owner.getYHeadRot());
        }
        if (direction.lengthSqr() <= 1.0E-8D) {
            direction = new Vec3(0.0D, 0.0D, 1.0D);
        }
        this.acceleration = legacySmallFireballAcceleration(direction);
        Vec3 eye = owner.getEyePosition();
        moveTo(eye.x(), eye.y(), eye.z(), owner.getYRot(), owner.getXRot());
        setDeltaMovement(Vec3.ZERO);
    }

    private void setOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
    }

    private void applySmallFireballMotion() {
        setDeltaMovement(getDeltaMovement().add(this.acceleration).scale(MOTION_FACTOR));
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

    private void travelAndImpact() {
        Vec3 start = position();
        Vec3 motion = getDeltaMovement();
        this.ticksInAir++;
        Vec3 end = start.add(motion);
        HitResult hit = findImpact(start, end);
        if (hit.getType() != HitResult.Type.MISS && impact(hit)) {
            return;
        }
        setPos(getX() + motion.x(), getY() + motion.y(), getZ() + motion.z());
        faceMotion(motion);
    }

    private HitResult findImpact(Vec3 start, Vec3 end) {
        BlockHitResult blockHit = this.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        double maxDistance = blockHit.getType() == HitResult.Type.MISS ? start.distanceTo(end) : start.distanceTo(blockHit.getLocation());
        LivingEntity owner = getOwner();
        EntityHitResult entityHit = this.level().getEntities(this, getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D),
                        target -> canImpact(owner, target))
                .stream()
                .map(entity -> entity.getBoundingBox().inflate(0.3D).clip(start, end)
                        .map(location -> new EntityHitResult(entity, location))
                        .orElse(null))
                .filter(candidate -> candidate != null && start.distanceTo(candidate.getLocation()) <= maxDistance)
                .min(Comparator.comparingDouble(candidate -> start.distanceTo(candidate.getLocation())))
                .orElse(null);
        return entityHit != null ? entityHit : blockHit;
    }

    private boolean canImpact(@Nullable LivingEntity owner, Entity target) {
        return target.isAlive()
                && target.isPickable()
                && !target.noPhysics
                && target != this
                && (target != owner || this.ticksInAir >= 25);
    }

    private boolean impact(HitResult hit) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            discard();
            return true;
        }
        LivingEntity owner = getOwner();
        if (hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            if (target == owner || target instanceof AsakujakuFireballEntity) {
                return false;
            }
            target.hurt(fireballDamageSource(serverLevel, owner), this.entityData.get(DAMAGE));
            target.setSecondsOnFire(10);
        }
        boolean damagesTerrain = ForgeEventFactory.getMobGriefingEvent(serverLevel, owner);
        serverLevel.explode(owner, getX(), getY(), getZ(), EXPLOSION_STRENGTH, false,
                damagesTerrain ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE);
        discard();
        return true;
    }

    private DamageSource fireballDamageSource(ServerLevel level, @Nullable LivingEntity owner) {
        return ModDamageTypes.fireball(level, this, owner);
    }

    private void spawnFlameParticles() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int color = 0xFFFF0000 | ((0x40 + this.random.nextInt(0x80)) << 8);
        serverLevel.sendParticles(
                ModParticleTypes.options(NarutoParticleKind.FLAME_COLORED, color, 12),
                getX(),
                getY() + getBbHeight() * 0.5D,
                getZ(),
                2,
                0.1D,
                0.1D,
                0.1D,
                0.0D);
    }

    private void faceMotion(Vec3 motion) {
        if (motion.lengthSqr() <= 1.0E-8D) {
            return;
        }
        double horizontal = Math.sqrt(motion.x() * motion.x() + motion.z() * motion.z());
        float targetYaw = (float)(Mth.atan2(motion.z(), motion.x()) * Mth.RAD_TO_DEG + 90.0F);
        float targetPitch = (float)(Mth.atan2(horizontal, motion.y()) * Mth.RAD_TO_DEG - 90.0F);
        setYRot(approachRotation(getYRot(), targetYaw, 0.2F));
        setXRot(approachRotation(getXRot(), targetPitch, 0.2F));
    }

    private Vec3 legacySmallFireballAcceleration(Vec3 direction) {
        Vec3 scattered = new Vec3(
                direction.x() + this.random.nextGaussian() * 0.4D,
                direction.y() + this.random.nextGaussian() * 0.4D,
                direction.z() + this.random.nextGaussian() * 0.4D);
        if (scattered.lengthSqr() <= 1.0E-8D) {
            scattered = new Vec3(0.0D, 0.0D, 1.0D);
        }
        return scattered.normalize().scale(ACCELERATION_SCALE);
    }

    private static float approachRotation(float current, float target, float factor) {
        while (target - current < -180.0F) {
            current -= 360.0F;
        }
        while (target - current >= 180.0F) {
            current += 360.0F;
        }
        return current + (target - current) * factor;
    }
}
