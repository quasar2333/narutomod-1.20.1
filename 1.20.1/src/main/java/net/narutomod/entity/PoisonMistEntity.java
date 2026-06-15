package net.narutomod.entity;

import java.util.HashSet;
import java.util.Set;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class PoisonMistEntity extends Entity {
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(PoisonMistEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> POWER = SynchedEntityData.defineId(PoisonMistEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUuid;

    public PoisonMistEntity(EntityType<? extends PoisonMistEntity> entityType, Level level) {
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
        PoisonMistEntity entity = ModEntityTypes.POISON_MIST.get().create(owner.level());
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
        this.entityData.define(POWER, 5.0F);
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
        float power = getPower();
        if (this.tickCount % 5 == 1) {
            this.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                    ModSounds.SOUND_WINDECHO.get(), SoundSource.NEUTRAL, 1.0F, power * 0.2F);
        }
        executePoisonMist(owner, power);
        if (this.tickCount > Math.max((int)(power * 2.0F), 1)) {
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
        setPower(tag.contains("Power") ? tag.getFloat("Power") : 5.0F);
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

    private void executePoisonMist(LivingEntity owner, float power) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        double range = Math.max(power, 0.1D);
        double farRadius = range * 0.25D;
        Vec3 look = owner.getLookAngle();
        if (look.lengthSqr() <= 1.0E-8D) {
            return;
        }
        look = look.normalize();
        Vec3 start = owner.getEyePosition().subtract(0.0D, 0.1D, 0.0D);
        spawnPoisonParticles(serverLevel, owner, look, range, farRadius);
        poisonEntities(owner, start, look, range, farRadius);
    }

    private void spawnPoisonParticles(ServerLevel level, LivingEntity owner, Vec3 look, double range, double farRadius) {
        Vec3 start = look.scale(2.0D).add(owner.getX(), owner.getY() + 1.5D, owner.getZ());
        for (int i = 1; i <= 50; i++) {
            Vec3 forward = look.scale((this.random.nextDouble() * 0.8D + 0.2D) * range * 0.09D);
            Vec3 motion = forward.add(
                    (this.random.nextDouble() - 0.5D) * farRadius * 0.15D,
                    (this.random.nextDouble() - 0.5D) * farRadius * 0.15D,
                    (this.random.nextDouble() - 0.5D) * farRadius * 0.15D);
            level.sendParticles(
                    ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED,
                            0xFF630065,
                            80 + this.random.nextInt(20),
                            0,
                            0,
                            -1,
                            0),
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

    private void poisonEntities(LivingEntity owner, Vec3 start, Vec3 look, double range, double farRadius) {
        Vec3 ray = look.scale(range);
        AABB search = owner.getBoundingBox().expandTowards(ray).inflate(farRadius + 1.0D);
        Set<Integer> poisoned = new HashSet<>();
        for (Entity target : this.level().getEntities(this, search, target -> canAffect(owner, target))) {
            double distance = owner.distanceTo(target);
            double allowedRadius = distance / range * farRadius + 1.0D;
            AABB box = target.getBoundingBox().inflate(allowedRadius);
            if ((box.contains(start) || box.clip(start, start.add(ray)).isPresent()) && poisoned.add(target.getId())) {
                ((LivingEntity) target).addEffect(new MobEffectInstance(MobEffects.WITHER, 300, 2));
            }
        }
    }

    private boolean canAffect(LivingEntity owner, Entity target) {
        return target instanceof LivingEntity
                && target.isAlive()
                && target != owner
                && target.getRootVehicle() != owner.getRootVehicle()
                && !(target instanceof PoisonMistEntity);
    }
}
