package net.narutomod.entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class SandBindEntity extends Entity {
    public static final int DEFAULT_COLOR = SandBulletEntity.DEFAULT_COLOR;
    public static final double FUNERAL_CHAKRA_COST = 50.0D;
    private static final int MAX_LIFE = 600;
    private static final int FUNERAL_DURATION = 20;
    private static final int SWARM_TOTAL = 220;
    private static final int SWARM_SPAWN_BATCH = 24;
    private static final int PARTICLE_SAMPLE_STEP = 6;
    private static final float FUNERAL_DAMAGE = 4.0F;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(SandBindEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(SandBindEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(SandBindEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FUNERAL_TIME = SynchedEntityData.defineId(SandBindEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private UUID targetUuid;
    @Nullable
    private Vec3 capturedPosition;
    private SandSwarm swarm;

    public SandBindEntity(EntityType<? extends SandBindEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.blocksBuilding = false;
    }

    public static boolean spawnFrom(LivingEntity owner, LivingEntity target, int color) {
        SandBindEntity entity = ModEntityTypes.SAND_BIND.get().create(owner.level());
        if (entity == null) {
            return false;
        }
        entity.configure(owner, target, color);
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 0.7F, owner.getRandom().nextFloat() * 0.4F + 0.8F);
        return owner.level().addFreshEntity(entity);
    }

    @Nullable
    public static LivingEntity findLookedAtTarget(LivingEntity owner, double range) {
        HitResult hit = ProcedureUtils.objectEntityLookingAt(owner, range, 0.1D, true, false,
                target -> target != owner
                        && target.isAlive()
                        && !(target instanceof SandBindEntity));
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    @Nullable
    public static SandBindEntity findLookedAtOwnedBind(LivingEntity owner, double range) {
        HitResult hit = ProcedureUtils.objectEntityLookingAt(owner, range, 0.2D, true, false,
                target -> target instanceof SandBindEntity bind && bind.isOwnedBy(owner));
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof SandBindEntity bind) {
            return bind;
        }
        return null;
    }

    public boolean isOwnedBy(Entity owner) {
        if (owner.getUUID().equals(this.ownerUuid)) {
            return true;
        }
        int ownerId = this.entityData.get(OWNER_ID);
        return ownerId >= 0 && ownerId == owner.getId();
    }

    public boolean triggerSandFuneral(LivingEntity owner) {
        if (!isOwnedBy(owner) || getFuneralTime() != -1) {
            return false;
        }
        setOwner(owner);
        setFuneralTime(FUNERAL_DURATION);
        this.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                ModSounds.SOUND_SABAKUSOSO.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    public int getFuneralTime() {
        return this.entityData.get(FUNERAL_TIME);
    }

    public void configure(LivingEntity owner, LivingEntity target, int color) {
        setOwner(owner);
        setTarget(target);
        setColor(color);
        setFuneralTime(-1);
        Vec3 start = gourdMouthPosition(owner);
        moveTo(start.x(), start.y(), start.z(), owner.getYRot(), owner.getXRot());
        initializeSwarm(start, target.getBoundingBox(), false);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(TARGET_ID, -1);
        this.entityData.define(COLOR, DEFAULT_COLOR);
        this.entityData.define(FUNERAL_TIME, -1);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        LivingEntity owner = getOwner();
        LivingEntity target = getTarget();
        if (owner == null || !owner.isAlive() || target == null || this.tickCount > MAX_LIFE) {
            startReturnOrDiscard(owner);
            return;
        }
        if (this.swarm == null || this.swarm.isEmpty()) {
            initializeSwarm(position(), target.getBoundingBox(), false);
        }
        updateBind(owner, target);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 96.0D * getViewScale();
        return distance < range * range;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        if (tag.hasUUID("Target")) {
            this.targetUuid = tag.getUUID("Target");
        }
        setColor(tag.contains("Color") ? tag.getInt("Color") : DEFAULT_COLOR);
        setFuneralTime(tag.contains("FuneralTime") ? tag.getInt("FuneralTime") : -1);
        if (tag.contains("CapturedX") && tag.contains("CapturedY") && tag.contains("CapturedZ")) {
            this.capturedPosition = new Vec3(tag.getDouble("CapturedX"), tag.getDouble("CapturedY"), tag.getDouble("CapturedZ"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        if (this.targetUuid != null) {
            tag.putUUID("Target", this.targetUuid);
        }
        tag.putInt("Color", getColor());
        tag.putInt("FuneralTime", getFuneralTime());
        if (this.capturedPosition != null) {
            tag.putDouble("CapturedX", this.capturedPosition.x());
            tag.putDouble("CapturedY", this.capturedPosition.y());
            tag.putDouble("CapturedZ", this.capturedPosition.z());
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void updateBind(LivingEntity owner, LivingEntity target) {
        if (!target.isAlive() || getFuneralTime() == 0) {
            startReturnOrDiscard(owner);
            return;
        }
        boolean captured = isTargetCaptured(target);
        if (captured) {
            if (getFuneralTime() < 0 && this.swarm.allReachedTarget()) {
                this.swarm.setTarget(capturedTargetBox(), 0.0D, 0.0D, false);
            } else if (getFuneralTime() > 0) {
                this.swarm.setTarget(target.getBoundingBox(), 0.95D, 0.03D, false);
                attackTarget(owner, target);
                setFuneralTime(getFuneralTime() - 1);
            }
            holdTarget(target);
        } else {
            this.swarm.setTarget(target.getBoundingBox(), 0.95D, 0.03D, false);
        }
        updateSwarm();
    }

    private void attackTarget(LivingEntity owner, LivingEntity target) {
        target.invulnerableTime = 0;
        DamageSource source = ModDamageTypes.ninjutsu(this.level(), this, owner);
        target.hurt(source, FUNERAL_DAMAGE);
    }

    private void holdTarget(LivingEntity target) {
        Vec3 position = this.capturedPosition;
        if (position == null) {
            position = target.position();
            this.capturedPosition = position;
        }
        target.addEffect(new MobEffectInstance(ModEffects.PARALYSIS.get(), 2, 0, false, false));
        target.teleportTo(position.x(), position.y(), position.z());
        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
    }

    private boolean isTargetCaptured(LivingEntity target) {
        AABB bindBox = getBoundingBox();
        AABB targetBox = target.getBoundingBox();
        boolean captured = bindBox.intersects(targetBox)
                && bindBox.inflate(0.25D).contains(targetBox.getCenter());
        if (captured && this.capturedPosition == null) {
            this.capturedPosition = target.position();
        } else if (!captured) {
            this.capturedPosition = null;
        }
        return captured;
    }

    private AABB capturedTargetBox() {
        Vec3 center = this.capturedPosition != null ? this.capturedPosition : position();
        return new AABB(center.x() - 0.05D, center.y() - 0.05D, center.z() - 0.05D,
                center.x() + 0.05D, center.y() + 0.05D, center.z() + 0.05D);
    }

    private void startReturnOrDiscard(@Nullable LivingEntity owner) {
        if (owner == null || this.swarm == null || this.swarm.isEmpty()) {
            discard();
            return;
        }
        this.swarm.setTarget(gourdMouthPosition(owner), 0.8D, 0.02D, true);
        updateSwarm();
        if (this.swarm.isEmpty()) {
            discard();
        }
    }

    private void updateSwarm() {
        this.swarm.tick();
        AABB bounds = this.swarm.bounds();
        Vec3 center = bounds.getCenter();
        moveTo(center.x(), center.y(), center.z(), getYRot(), getXRot());
        setBoundingBox(bounds);
        spawnSwarmParticles();
    }

    private void initializeSwarm(Vec3 start, AABB target, boolean returnOnReach) {
        this.swarm = new SandSwarm(start, target, new Vec3(0.1D, 0.4D, 0.1D), 0.95D, 0.03D, returnOnReach);
        AABB bounds = this.swarm.bounds();
        moveTo(bounds.getCenter().x(), bounds.getCenter().y(), bounds.getCenter().z(), getYRot(), getXRot());
        setBoundingBox(bounds);
    }

    private void spawnSwarmParticles() {
        if (!(this.level() instanceof ServerLevel serverLevel) || this.swarm == null) {
            return;
        }
        int index = 0;
        for (Vec3 point : this.swarm.positions()) {
            if (index++ % PARTICLE_SAMPLE_STEP != 0) {
                continue;
            }
            serverLevel.sendParticles(
                    ModParticleTypes.options(NarutoParticleKind.SUSPENDED_COLORED, getColor(), 12, 3),
                    point.x(),
                    point.y(),
                    point.z(),
                    2,
                    0.035D,
                    0.035D,
                    0.035D,
                    0.0D);
        }
        Vec3 center = this.swarm.bounds().getCenter();
        this.level().playSound(null, center.x(), center.y(), center.z(),
                SoundEvents.SAND_PLACE, SoundSource.BLOCKS,
                Math.min(0.6F, this.swarm.positions().size() * 0.0025F),
                this.random.nextFloat() * 0.4F + 0.8F);
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

    private void setTarget(LivingEntity target) {
        this.targetUuid = target.getUUID();
        this.entityData.set(TARGET_ID, target.getId());
    }

    @Nullable
    private LivingEntity getTarget() {
        int targetId = this.entityData.get(TARGET_ID);
        if (targetId >= 0 && this.level().getEntity(targetId) instanceof LivingEntity living) {
            return living;
        }
        if (this.targetUuid != null && this.level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(this.targetUuid) instanceof LivingEntity living) {
            setTarget(living);
            return living;
        }
        return null;
    }

    private int getColor() {
        return this.entityData.get(COLOR);
    }

    private void setColor(int color) {
        this.entityData.set(COLOR, color);
    }

    private void setFuneralTime(int ticks) {
        this.entityData.set(FUNERAL_TIME, ticks);
    }

    private Vec3 gourdMouthPosition(LivingEntity owner) {
        Vec3 look = owner.getLookAngle();
        Vec3 right = new Vec3(-look.z(), 0.0D, look.x());
        if (right.lengthSqr() <= 1.0E-8D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        return owner.position()
                .add(right.scale(-0.35D))
                .add(0.0D, owner.getBbHeight() * 0.75D, 0.0D)
                .subtract(look.normalize().scale(0.25D));
    }

    private final class SandSwarm {
        private final List<SwarmParticle> particles = new ArrayList<>();
        private final Vec3 start;
        private final Vec3 spawnMotion;
        private AABB targetBox;
        private double speed;
        private double inaccuracy;
        private boolean dieOnReached;
        private int spawned;

        private SandSwarm(Vec3 start, AABB targetBox, Vec3 spawnMotion, double speed, double inaccuracy, boolean dieOnReached) {
            this.start = start;
            this.spawnMotion = spawnMotion;
            setTarget(targetBox, speed, inaccuracy, dieOnReached);
            spawnNewParticles();
        }

        private void setTarget(Vec3 target, double speed, double inaccuracy, boolean dieOnReached) {
            double half = Math.max(0.025D, inaccuracy * 0.5D);
            setTarget(new AABB(target.x() - half, target.y() - half, target.z() - half,
                    target.x() + half, target.y() + half, target.z() + half), speed, inaccuracy, dieOnReached);
        }

        private void setTarget(AABB targetBox, double speed, double inaccuracy, boolean dieOnReached) {
            this.targetBox = targetBox;
            this.speed = speed;
            this.inaccuracy = inaccuracy;
            this.dieOnReached = dieOnReached;
        }

        private void setTarget(AABB targetBox, boolean dieOnReached) {
            this.targetBox = targetBox;
            this.dieOnReached = dieOnReached;
        }

        private void tick() {
            if (this.particles.isEmpty() && this.spawned >= SWARM_TOTAL) {
                return;
            }
            Iterator<SwarmParticle> iterator = this.particles.iterator();
            while (iterator.hasNext()) {
                SwarmParticle particle = iterator.next();
                Vec3 delta = randomTargetPoint().subtract(particle.position);
                if (this.dieOnReached && delta.length() < 0.1D + this.inaccuracy) {
                    iterator.remove();
                    continue;
                }
                if (delta.lengthSqr() > 1.0E-8D) {
                    Vec3 pull = delta.normalize().scale(this.speed * 0.1D);
                    Vec3 noise = new Vec3(
                            SandBindEntity.this.random.nextGaussian() * this.inaccuracy,
                            SandBindEntity.this.random.nextGaussian() * this.inaccuracy,
                            SandBindEntity.this.random.nextGaussian() * this.inaccuracy);
                    particle.motion = particle.motion.add(pull).add(noise).scale(0.96D);
                    particle.position = particle.position.add(particle.motion);
                }
            }
            spawnNewParticles();
        }

        private void spawnNewParticles() {
            int limit = Math.min(SWARM_TOTAL, this.spawned + SWARM_SPAWN_BATCH);
            while (this.spawned < limit) {
                double mx = (SandBindEntity.this.random.nextDouble() - 0.5D) * 2.0D * this.spawnMotion.x();
                double my = this.spawnMotion.y();
                double mz = (SandBindEntity.this.random.nextDouble() - 0.5D) * 2.0D * this.spawnMotion.z();
                this.particles.add(new SwarmParticle(this.start, new Vec3(mx, my, mz)));
                this.spawned++;
            }
        }

        private boolean allReachedTarget() {
            if (this.particles.isEmpty()) {
                return false;
            }
            return this.targetBox.contains(bounds().getCenter());
        }

        private boolean isEmpty() {
            return this.particles.isEmpty();
        }

        private List<Vec3> positions() {
            return this.particles.stream()
                    .map(particle -> particle.position)
                    .toList();
        }

        private AABB bounds() {
            if (this.particles.isEmpty()) {
                return new AABB(getX() - 0.1D, getY() - 0.1D, getZ() - 0.1D, getX() + 0.1D, getY() + 0.1D, getZ() + 0.1D);
            }
            SwarmParticle first = this.particles.get(0);
            double minX = first.position.x();
            double minY = first.position.y();
            double minZ = first.position.z();
            double maxX = minX;
            double maxY = minY;
            double maxZ = minZ;
            for (SwarmParticle particle : this.particles) {
                minX = Math.min(minX, particle.position.x());
                minY = Math.min(minY, particle.position.y());
                minZ = Math.min(minZ, particle.position.z());
                maxX = Math.max(maxX, particle.position.x());
                maxY = Math.max(maxY, particle.position.y());
                maxZ = Math.max(maxZ, particle.position.z());
            }
            return new AABB(minX, minY, minZ, maxX, maxY, maxZ).inflate(0.15D);
        }

        private Vec3 randomTargetPoint() {
            double x = this.targetBox.minX + SandBindEntity.this.random.nextDouble() * this.targetBox.getXsize();
            double y = this.targetBox.minY + SandBindEntity.this.random.nextDouble() * this.targetBox.getYsize();
            double z = this.targetBox.minZ + SandBindEntity.this.random.nextDouble() * this.targetBox.getZsize();
            return new Vec3(x, y, z);
        }
    }

    private static final class SwarmParticle {
        private Vec3 position;
        private Vec3 motion;

        private SwarmParticle(Vec3 position, Vec3 motion) {
            this.position = position;
            this.motion = motion;
        }
    }
}
