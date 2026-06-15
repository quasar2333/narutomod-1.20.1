package net.narutomod.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class FutonVacuumEntity extends Entity {
    private static final float DAMAGE_MODIFIER = 0.5F;
    private static final double STREAM_RADIUS = 0.5D;
    private static final int EXECUTE_INTERVAL_TICKS = 5;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(FutonVacuumEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> POWER = SynchedEntityData.defineId(FutonVacuumEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUuid;

    public FutonVacuumEntity(EntityType<? extends FutonVacuumEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public void configure(LivingEntity owner, float power) {
        setOwner(owner);
        setPower(power);
        moveToOwner(owner);
    }

    public static boolean spawnFrom(LivingEntity owner, float power) {
        if (power < 1.0F) {
            return false;
        }
        FutonVacuumEntity entity = ModEntityTypes.FUTON_VACUUM.get().create(owner.level());
        if (entity == null) {
            return false;
        }
        entity.configure(owner, power);
        owner.level().addFreshEntity(entity);
        return true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(POWER, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        moveToOwner(owner);
        if (this.tickCount % EXECUTE_INTERVAL_TICKS == 1) {
            playStreamSound(owner);
            executeAirStream(owner);
        }
        if (this.tickCount > Math.max((int)(getPower() * 4.0F), 1)) {
            discard();
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 68.5D * getViewScale();
        return distance < range * range;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        setPower(tag.contains("Power") ? tag.getFloat("Power") : 1.0F);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putFloat("Power", getPower());
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

    private float getPower() {
        return this.entityData.get(POWER);
    }

    private void setPower(float power) {
        this.entityData.set(POWER, Math.max(power, 0.1F));
    }

    private void moveToOwner(LivingEntity owner) {
        this.setDeltaMovement(Vec3.ZERO);
        this.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
    }

    private void playStreamSound(LivingEntity owner) {
        this.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 1.0F, this.random.nextFloat() * 0.5F + 0.8F);
    }

    private void executeAirStream(LivingEntity owner) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        double range = getPower();
        if (range <= 0.0D) {
            return;
        }
        Vec3 start = owner.getEyePosition().subtract(0.0D, 0.4D, 0.0D);
        Vec3 look = owner.getLookAngle();
        if (look.lengthSqr() <= 1.0E-8D) {
            return;
        }
        look = look.normalize();
        spawnVacuumParticles(serverLevel, owner, look);
        affectBlocks(serverLevel, owner, start, look, range);
        damageEntities(owner, start, look, range);
    }

    private void spawnVacuumParticles(ServerLevel level, LivingEntity owner, Vec3 look) {
        Vec3 start = look.scale(2.0D).add(owner.getX(), owner.getY() + 1.6D, owner.getZ());
        for (int i = 1; i < 400; i++) {
            Vec3 motion = look.scale(this.random.nextDouble() * getPower() * 0.25D);
            level.sendParticles(
                    ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0x20FFFFFF, 10, 0, 0, -1, 4),
                    start.x(),
                    start.y(),
                    start.z(),
                    0,
                    motion.x(),
                    motion.y(),
                    motion.z(),
                    1.0D);
        }
    }

    private void affectBlocks(ServerLevel level, LivingEntity owner, Vec3 start, Vec3 look, double range) {
        Vec3 end = start.add(look.scale(range));
        Set<BlockPos> affected = collectBlocksAlongStream(start, look, range);
        for (BlockPos pos : affected) {
            if (rayReachesInflatedBlock(start, end, pos, STREAM_RADIUS)
                    && ProcedureUtils.breakBlockAndDropWithChance(level, pos, 2.0F, 0.2F, 0.1F, true)) {
                playImpactSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
            }
        }
    }

    private Set<BlockPos> collectBlocksAlongStream(Vec3 start, Vec3 look, double range) {
        Set<BlockPos> result = new HashSet<>();
        int steps = Mth.clamp((int)Math.ceil(range * 2.0D), 1, 160);
        for (int i = 0; i <= steps; i++) {
            double distance = range * i / steps;
            Vec3 center = start.add(look.scale(distance));
            BlockPos min = BlockPos.containing(center.x() - STREAM_RADIUS, center.y() - STREAM_RADIUS, center.z() - STREAM_RADIUS);
            BlockPos max = BlockPos.containing(center.x() + STREAM_RADIUS, center.y() + STREAM_RADIUS, center.z() + STREAM_RADIUS);
            BlockPos.betweenClosed(min, max).forEach(pos -> result.add(pos.immutable()));
        }
        return result;
    }

    private boolean rayReachesInflatedBlock(Vec3 start, Vec3 end, BlockPos pos, double radius) {
        AABB box = new AABB(pos).inflate(radius);
        return box.contains(start) || box.clip(start, end).isPresent();
    }

    private void damageEntities(LivingEntity owner, Vec3 start, Vec3 look, double range) {
        Vec3 ray = look.scale(range);
        AABB search = owner.getBoundingBox().expandTowards(ray).inflate(STREAM_RADIUS + 1.0D);
        DamageSource source = ModDamageTypes.ninjutsu(this.level(), this, owner);
        Set<Integer> hit = new HashSet<>();
        for (Entity target : this.level().getEntities(this, search, target -> canAffect(owner, target))) {
            AABB box = target.getBoundingBox().inflate(STREAM_RADIUS + 1.0D);
            if ((box.contains(start) || box.clip(start, start.add(ray)).isPresent()) && hit.add(target.getId())) {
                playImpactSound(target.getX(), target.getY(), target.getZ());
                target.hurt(source, getPower() * DAMAGE_MODIFIER);
            }
        }
    }

    private boolean canAffect(LivingEntity owner, Entity target) {
        return target.isAlive()
                && target != owner
                && target.getRootVehicle() != owner.getRootVehicle()
                && !(target instanceof FutonVacuumEntity);
    }

    private void playImpactSound(double x, double y, double z) {
        this.level().playSound(null, x, y, z, ModSounds.SOUND_BULLET_IMPACT.get(), SoundSource.NEUTRAL,
                1.0F, 0.4F + this.random.nextFloat() * 0.6F);
    }
}
