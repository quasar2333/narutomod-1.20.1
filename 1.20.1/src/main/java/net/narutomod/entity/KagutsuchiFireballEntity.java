package net.narutomod.entity;

import java.util.Comparator;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.registry.ModBlocks;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModParticleTypes;

public final class KagutsuchiFireballEntity extends Entity {
    private static final double ACCELERATION_SCALE = 0.1D;
    private static final double DIRECTION_SCATTER = 0.4D;
    private static final float MOTION_FACTOR = 0.95F;
    private static final float WATER_SLOWDOWN = 0.8F;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(KagutsuchiFireballEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AMATERASU_AMPLIFIER =
            SynchedEntityData.defineId(KagutsuchiFireballEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;
    private Vec3 acceleration = Vec3.ZERO;
    private int ticksInAir;

    public KagutsuchiFireballEntity(EntityType<? extends KagutsuchiFireballEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    public void configure(Entity owner, Player powerSource, Vec3 origin, Vec3 target) {
        setOwner(owner);
        this.entityData.set(AMATERASU_AMPLIFIER, powerSource.experienceLevel / 30 + 1);
        this.moveTo(origin.x(), origin.y(), origin.z(), owner.getYRot(), owner.getXRot());
        Vec3 direction = target.subtract(origin);
        shoot(direction.x(), direction.y(), direction.z());
    }

    public float getRenderScale() {
        return isBigFireball() ? 6.0F : 2.0F;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(AMATERASU_AMPLIFIER, 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        Entity owner = getOwner();
        if ((owner != null && !owner.isAlive()) || !this.level().hasChunkAt(blockPosition())) {
            discard();
            return;
        }
        travelAndImpact();
        if (!isRemoved()) {
            updateLegacyFireballMotion();
            spawnSmokeParticles();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        this.entityData.set(AMATERASU_AMPLIFIER, tag.contains("AmaterasuAmplifier") ? tag.getInt("AmaterasuAmplifier") : 1);
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
        tag.putInt("AmaterasuAmplifier", this.entityData.get(AMATERASU_AMPLIFIER));
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
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void setOwner(Entity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
    }

    @Nullable
    private Entity getOwner() {
        int ownerId = this.entityData.get(OWNER_ID);
        if (ownerId >= 0) {
            Entity owner = this.level().getEntity(ownerId);
            if (owner != null) {
                return owner;
            }
        }
        if (this.ownerUuid != null && this.level() instanceof ServerLevel serverLevel) {
            Entity owner = serverLevel.getEntity(this.ownerUuid);
            if (owner != null) {
                setOwner(owner);
                return owner;
            }
        }
        return null;
    }

    private boolean isBigFireball() {
        return getType() == ModEntityTypes.ENTITYKAGUTSUCHISWORDBIGFIREBALL.get();
    }

    private void shoot(double x, double y, double z) {
        Vec3 direction = new Vec3(x, y, z);
        if (direction.lengthSqr() <= 1.0E-8D) {
            direction = Vec3.directionFromRotation(getXRot(), getYRot());
        }
        this.acceleration = legacyFireballAcceleration(direction);
        setDeltaMovement(Vec3.ZERO);
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
        Entity owner = getOwner();
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

    private boolean canImpact(@Nullable Entity owner, Entity target) {
        return target.isAlive()
                && target.isPickable()
                && !target.noPhysics
                && target != this
                && (target != owner || this.ticksInAir >= 25);
    }

    private boolean isSameLegacyFireballType(Entity target) {
        return target instanceof KagutsuchiFireballEntity && target.getType() == getType();
    }

    private boolean impact(HitResult hit) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            discard();
            return true;
        }
        Entity owner = getOwner();
        if (hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            if (target == owner || isSameLegacyFireballType(target)) {
                return false;
            }
        }
        Vec3 point = position();
        placeAmaterasuBlocks(serverLevel, point);
        applyAmaterasuArea(serverLevel, point, owner);
        discard();
        return true;
    }

    private void placeAmaterasuBlocks(ServerLevel serverLevel, Vec3 point) {
        int radius = isBigFireball() ? 3 : 2;
        BlockPos center = BlockPos.containing(point);
        int minX = -this.random.nextInt(radius + 1);
        int maxX = this.random.nextInt(radius + 1);
        int minY = -this.random.nextInt(radius + 1);
        int maxY = this.random.nextInt(radius + 1);
        int minZ = -this.random.nextInt(radius + 1);
        int maxZ = this.random.nextInt(radius + 1);
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (serverLevel.hasChunkAt(pos) && serverLevel.getBlockState(pos).isAir()) {
                        serverLevel.setBlock(pos, ModBlocks.AMATERASUBLOCK.get().defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private void applyAmaterasuArea(ServerLevel serverLevel, Vec3 point, @Nullable Entity owner) {
        double radius = isBigFireball() ? 4.0D : 3.0D;
        int amplifier = this.entityData.get(AMATERASU_AMPLIFIER);
        DamageSource damageSource = ModDamageTypes.ninjutsu(this.level(), this, owner);
        AABB area = new AABB(point, point).inflate(radius);
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            if (target == owner) {
                continue;
            }
            target.addEffect(new MobEffectInstance(ModEffects.AMATERASUFLAME.get(), 1000, amplifier, false, false));
            if (isBigFireball()) {
                target.hurt(damageSource, 100.0F);
            }
        }
    }

    private void spawnSmokeParticles() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean big = isBigFireball();
        serverLevel.sendParticles(
                ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0xD0080505, big ? 8 : 4, 40, 0, -1, 4),
                getX(),
                getY() + getBbHeight() * 0.5D,
                getZ(),
                big ? 5 : 2,
                getBbWidth() * 0.35D,
                getBbHeight() * 0.35D,
                getBbWidth() * 0.35D,
                0.02D);
    }

    private void updateLegacyFireballMotion() {
        Vec3 motion = getDeltaMovement();
        float factor = this.isInWater() ? WATER_SLOWDOWN : MOTION_FACTOR;
        if (this.isInWater() && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.BUBBLE,
                    getX() - motion.x() * 0.25D,
                    getY() - motion.y() * 0.25D,
                    getZ() - motion.z() * 0.25D,
                    4,
                    motion.x(),
                    motion.y(),
                    motion.z(),
                    0.0D);
        }
        setDeltaMovement(motion.add(this.acceleration).scale(factor));
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

    private Vec3 legacyFireballAcceleration(Vec3 direction) {
        Vec3 scattered = new Vec3(
                direction.x() + this.random.nextGaussian() * DIRECTION_SCATTER,
                direction.y() + this.random.nextGaussian() * DIRECTION_SCATTER,
                direction.z() + this.random.nextGaussian() * DIRECTION_SCATTER);
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
