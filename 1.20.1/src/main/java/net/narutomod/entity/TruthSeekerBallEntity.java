package net.narutomod.entity;

import java.util.Comparator;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModItems;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class TruthSeekerBallEntity extends Entity {
    private static final float BASE_WIDTH = 0.25F;
    private static final float BASE_HEIGHT = 0.25F;
    private static final float INITIAL_SCALE = 0.8F;
    private static final float EXPANSIVE_MAX_SCALE = 25.0F;
    private static final double GROWTH_RATE = 1.03D;
    private static final double MOVE_TO_OWNER_SPEED = 0.1D;
    private static final double LAUNCH_SPEED = 0.95D;
    private static final float WATER_SLOWDOWN = 0.8F;
    private static final int EXPANSIVE_TARGET_TIME = 200;
    private static final int MAX_EXPANSIVE_LIFETIME = 400;
    private static final int DEATH_SHRINK_TICKS = 5;
    private static final int SIX_PATH_BALL_COUNT = 9;
    private static final float SHIELD_SIZE = 8.0F;
    private static final float MAX_HEALTH = 1000.0F;
    private static final double SENTRY_RANGE = 6.0D;
    private static final double SENTRY_TRIGGER_SPEED_SQR = 0.045D;
    private static final float SENTRY_TRIGGER_ARC_DEGREES = 15.0F;
    private static final int SENTRY_TARGET_TICKS = 10;
    private static final double SENTRY_SPEED = 1.5D;
    private static final Vec3[] IDLE_VECTORS = {
            new Vec3(0.0D, 2.0387D, -0.4395D),
            new Vec3(-0.4102D, 1.7629D, -0.4395D),
            new Vec3(0.4102D, 1.7629D, -0.4395D),
            new Vec3(-0.5859D, 1.3113D, -0.4395D),
            new Vec3(0.5859D, 1.3113D, -0.4395D),
            new Vec3(-0.5273D, 0.8012D, -0.4395D),
            new Vec3(0.5273D, 0.8012D, -0.4395D),
            new Vec3(-0.2344D, 0.4082D, -0.4395D),
            new Vec3(0.2344D, 0.4082D, -0.4395D)
    };
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(TruthSeekerBallEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_TIME = SynchedEntityData.defineId(TruthSeekerBallEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(TruthSeekerBallEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> MAX_SCALE = SynchedEntityData.defineId(TruthSeekerBallEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> LAUNCHED = SynchedEntityData.defineId(TruthSeekerBallEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> EXPANSIVE = SynchedEntityData.defineId(TruthSeekerBallEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHIELD_ON = SynchedEntityData.defineId(TruthSeekerBallEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID ownerUuid;
    private float dimensionsScale = INITIAL_SCALE;
    private int deathTicks;
    private int idleIndex;
    private int shieldDirection;
    private float shieldProgress;
    private float health = MAX_HEALTH;
    private Vec3 acceleration = Vec3.ZERO;
    private float motionFactor;
    @Nullable
    private UUID sentryTargetUuid;
    private int sentryTargetId = -1;
    private int sentryTargetTime = -1;

    public TruthSeekerBallEntity(EntityType<? extends TruthSeekerBallEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public static boolean spawnExpansiveFrom(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        TruthSeekerBallEntity entity = ModEntityTypes.TRUTHSEEKERBALL.get().create(serverLevel);
        if (entity == null) {
            return false;
        }
        entity.configureExpansive(owner);
        serverLevel.addFreshEntity(entity);
        return true;
    }

    public static boolean hasActiveExpansive(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        return !serverLevel.getEntitiesOfClass(TruthSeekerBallEntity.class, owner.getBoundingBox().inflate(256.0D),
                entity -> entity.isAlive() && entity.isExpansive() && entity.isOwnedBy(owner)).isEmpty();
    }

    public static void maintainSixPathBalls(Player owner, ItemStack stack) {
        if (!(owner.level() instanceof ServerLevel serverLevel) || !isOwnerHoldingStack(owner, stack)) {
            return;
        }
        int[] ids = normalizedBallIds(stack);
        boolean changed = false;
        for (int index = 0; index < SIX_PATH_BALL_COUNT; index++) {
            TruthSeekerBallEntity ball = getBallById(serverLevel, ids[index], owner);
            if (ball == null || ball.health <= 0.0F) {
                ball = ModEntityTypes.TRUTHSEEKERBALL.get().create(serverLevel);
                if (ball != null) {
                    ball.configureSixPath(owner, index);
                    serverLevel.addFreshEntity(ball);
                    ids[index] = ball.getId();
                    changed = true;
                } else {
                    ids[index] = -1;
                    changed = true;
                }
            }
        }
        if (changed) {
            stack.getOrCreateTag().putIntArray(net.narutomod.item.SixPathSenjutsuItem.SPAWNED_BALLS_TAG, ids);
        }
    }

    public static void runSixPathSentry(Player owner, ItemStack stack) {
        if (!(owner.level() instanceof ServerLevel serverLevel) || !isOwnerHoldingStack(owner, stack)) {
            return;
        }
        maintainSixPathBalls(owner, stack);
        for (Entity candidate : serverLevel.getEntities(owner, owner.getBoundingBox().inflate(SENTRY_RANGE),
                entity -> isSentryCandidate(owner, stack, entity))) {
            Vec3 motion = candidate.getDeltaMovement();
            Vec3 toOwner = owner.position().subtract(candidate.position());
            float motionYaw = ProcedureUtils.getYawFromVec(motion.x(), motion.z());
            float ownerYaw = ProcedureUtils.getYawFromVec(toOwner.x(), toOwner.z());
            if (Math.abs(Mth.wrapDegrees(ownerYaw - motionYaw)) > SENTRY_TRIGGER_ARC_DEGREES) {
                continue;
            }
            TruthSeekerBallEntity ball = nextAvailableSixPathBall(owner, stack);
            if (ball != null) {
                ball.setSentryTarget(candidate, SENTRY_TARGET_TICKS);
            }
        }
    }

    public static int countActiveSentryTargets(Player owner, ItemStack stack) {
        if (stack.isEmpty() || !(owner.level() instanceof ServerLevel serverLevel)) {
            return 0;
        }
        int count = 0;
        int[] ids = normalizedBallIds(stack);
        for (int id : ids) {
            TruthSeekerBallEntity ball = getBallById(serverLevel, id, owner);
            if (ball != null && ball.hasActiveSentryTarget()) {
                count++;
            }
        }
        return count;
    }

    @Nullable
    public static TruthSeekerBallEntity nextAvailableSixPathBall(Player owner, ItemStack stack) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        maintainSixPathBalls(owner, stack);
        int[] ids = normalizedBallIds(stack);
        int next = stack.getOrCreateTag().getInt(net.narutomod.item.SixPathSenjutsuItem.CURRENT_BALL_TAG);
        for (int attempts = 0; attempts < SIX_PATH_BALL_COUNT; attempts++) {
            next++;
            if (next >= SIX_PATH_BALL_COUNT) {
                next = 0;
            }
            TruthSeekerBallEntity ball = getBallById(serverLevel, ids[next], owner);
            if (ball != null && !ball.isLaunched() && !ball.isShieldOn()) {
                stack.getOrCreateTag().putInt(net.narutomod.item.SixPathSenjutsuItem.CURRENT_BALL_TAG, next);
                return ball;
            }
        }
        stack.getOrCreateTag().putInt(net.narutomod.item.SixPathSenjutsuItem.CURRENT_BALL_TAG, -1);
        return null;
    }

    public float getScale() {
        return this.entityData.get(SCALE);
    }

    public boolean isShieldOn() {
        return this.entityData.get(SHIELD_ON);
    }

    public float getHealthValue() {
        return this.health;
    }

    private void configureExpansive(LivingEntity owner) {
        setOwner(owner);
        setScale(INITIAL_SCALE);
        this.entityData.set(EXPANSIVE, true);
        this.entityData.set(MAX_SCALE, EXPANSIVE_MAX_SCALE);
        this.entityData.set(TARGET_TIME, EXPANSIVE_TARGET_TIME);
        Vec3 start = owner.position().add(0.0D, owner.getBbHeight() + 2.0D, 0.0D);
        moveTo(start.x(), start.y(), start.z(), owner.getYHeadRot(), owner.getXRot());
    }

    private void configureSixPath(LivingEntity owner, int idleIndex) {
        setOwner(owner);
        this.idleIndex = Math.floorMod(idleIndex, IDLE_VECTORS.length);
        this.health = MAX_HEALTH;
        this.shieldProgress = 0.0F;
        this.shieldDirection = 0;
        this.entityData.set(EXPANSIVE, false);
        this.entityData.set(LAUNCHED, false);
        this.entityData.set(SHIELD_ON, false);
        this.entityData.set(TARGET_TIME, -1);
        setSentryTarget(null, 0);
        this.entityData.set(MAX_SCALE, INITIAL_SCALE);
        setScale(INITIAL_SCALE);
        Vec3 idle = getIdlePosition(owner);
        moveTo(idle.x(), idle.y(), idle.z(), owner.getYHeadRot(), owner.getXRot());
    }

    public boolean shootFromOwner(LivingEntity owner) {
        if (isLaunched() || isShieldOn() || !isOwnedBy(owner)) {
            return false;
        }
        Vec3 look = owner.getLookAngle();
        if (look.lengthSqr() <= 1.0E-8D) {
            look = Vec3.directionFromRotation(owner.getXRot(), owner.getYHeadRot());
        }
        if (look.lengthSqr() <= 1.0E-8D) {
            look = new Vec3(0.0D, 0.0D, 1.0D);
        }
        shoot(look, LAUNCH_SPEED);
        return true;
    }

    public boolean toggleShield(LivingEntity owner) {
        if (isLaunched() || !isOwnedBy(owner)) {
            return false;
        }
        if (isShieldOn()) {
            this.shieldDirection = -1;
        } else {
            this.shieldDirection = 1;
            this.entityData.set(SHIELD_ON, true);
        }
        return true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(TARGET_TIME, -1);
        this.entityData.define(SCALE, INITIAL_SCALE);
        this.entityData.define(MAX_SCALE, INITIAL_SCALE);
        this.entityData.define(LAUNCHED, false);
        this.entityData.define(EXPANSIVE, false);
        this.entityData.define(SHIELD_ON, false);
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
        if (!isExpansive() && !isOwnerHoldingSixPath(owner)) {
            discard();
            return;
        }
        if (this.deathTicks > 0) {
            shrinkAndDiscard();
            return;
        }
        if (isExpansive() && this.tickCount > MAX_EXPANSIVE_LIFETIME) {
            startDeath();
            return;
        }
        if (isLaunched()) {
            travelAndImpact(owner);
            return;
        }
        if (isExpansive()) {
            moveGrowAndShoot(owner);
        } else {
            updateSixPathOrbit(owner);
        }
        moveByDelta();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        setScale(tag.contains("Scale") ? tag.getFloat("Scale") : INITIAL_SCALE);
        this.entityData.set(MAX_SCALE, tag.contains("MaxScale") ? tag.getFloat("MaxScale") : INITIAL_SCALE);
        this.entityData.set(TARGET_TIME, tag.contains("TargetTime") ? tag.getInt("TargetTime") : -1);
        this.entityData.set(LAUNCHED, tag.getBoolean("Launched"));
        this.entityData.set(EXPANSIVE, tag.getBoolean("Expansive"));
        this.entityData.set(SHIELD_ON, tag.getBoolean("ShieldOn"));
        this.deathTicks = tag.getInt("DeathTicks");
        this.idleIndex = tag.getInt("IdleIndex");
        this.shieldDirection = tag.getInt("ShieldDirection");
        this.shieldProgress = tag.getFloat("ShieldProgress");
        this.health = tag.contains("Health") ? tag.getFloat("Health") : MAX_HEALTH;
        this.motionFactor = tag.getFloat("MotionFactor");
        this.acceleration = tag.contains("AccelerationX")
                ? new Vec3(tag.getDouble("AccelerationX"), tag.getDouble("AccelerationY"), tag.getDouble("AccelerationZ"))
                : Vec3.ZERO;
        this.sentryTargetTime = tag.getInt("SentryTargetTime");
        if (tag.hasUUID("SentryTarget")) {
            this.sentryTargetUuid = tag.getUUID("SentryTarget");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putFloat("Scale", getScale());
        tag.putFloat("MaxScale", getMaxScale());
        tag.putInt("TargetTime", this.entityData.get(TARGET_TIME));
        tag.putBoolean("Launched", isLaunched());
        tag.putBoolean("Expansive", isExpansive());
        tag.putBoolean("ShieldOn", isShieldOn());
        tag.putInt("DeathTicks", this.deathTicks);
        tag.putInt("IdleIndex", this.idleIndex);
        tag.putInt("ShieldDirection", this.shieldDirection);
        tag.putFloat("ShieldProgress", this.shieldProgress);
        tag.putFloat("Health", this.health);
        tag.putFloat("MotionFactor", this.motionFactor);
        tag.putDouble("AccelerationX", this.acceleration.x());
        tag.putDouble("AccelerationY", this.acceleration.y());
        tag.putDouble("AccelerationZ", this.acceleration.z());
        tag.putInt("SentryTargetTime", this.sentryTargetTime);
        if (this.sentryTargetUuid != null) {
            tag.putUUID("SentryTarget", this.sentryTargetUuid);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(this.dimensionsScale);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SCALE.equals(key)) {
            this.dimensionsScale = getScale();
            refreshDimensions();
        }
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean ignoreExplosion() {
        return true;
    }

    private void updateSixPathOrbit(LivingEntity owner) {
        updateShieldProgress();
        setNextPosition(getIdlePosition(owner), isShieldOn() ? 0.4D : 0.5D);
        if (isShieldOn()) {
            shieldTouchedEntities(owner);
            return;
        }
        if (updateSentryTarget(owner)) {
            return;
        }
    }

    private void updateShieldProgress() {
        if (this.shieldDirection == 0) {
            return;
        }
        this.shieldProgress += 0.05F * this.shieldDirection;
        if (this.shieldProgress >= 1.0F) {
            this.shieldProgress = 1.0F;
            this.shieldDirection = 0;
        } else if (this.shieldProgress <= 0.0F) {
            this.shieldProgress = 0.0F;
            this.shieldDirection = 0;
            this.entityData.set(SHIELD_ON, false);
        }
        float scale = 1.0F + this.shieldProgress * SHIELD_SIZE;
        setScale(scale);
        this.entityData.set(MAX_SCALE, scale);
    }

    private void setNextPosition(Vec3 targetPosition, double speed) {
        Vec3 offset = targetPosition.subtract(position());
        if (offset.lengthSqr() > speed * speed) {
            setDeltaMovement(offset.normalize().scale(speed));
        } else {
            setDeltaMovement(offset);
        }
    }

    private Vec3 getIdlePosition(LivingEntity owner) {
        if (isShieldOn()) {
            return owner.position().add(0.0D, owner.getBbHeight() + this.shieldProgress * -1.8F, 0.0D);
        }
        Vec3 idle = IDLE_VECTORS[Math.floorMod(this.idleIndex, IDLE_VECTORS.length)];
        float yaw = -owner.yBodyRot * Mth.DEG_TO_RAD;
        return idle.yRot(yaw).add(owner.position());
    }

    private void shieldTouchedEntities(LivingEntity owner) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        DamageSource source = ModDamageTypes.senjutsu(this.level(), this, owner);
        for (Entity target : serverLevel.getEntities(this, getBoundingBox().inflate(0.1D),
                target -> target.isAlive() && target != owner && !(target instanceof TruthSeekerBallEntity))) {
            Vec3 push = target.position().subtract(position());
            if (push.horizontalDistanceSqr() > 1.0E-8D) {
                push = push.normalize().scale(0.15D);
                target.push(push.x(), 0.0D, push.z());
            }
            if (target instanceof LivingEntity living) {
                living.invulnerableTime = 0;
                living.hurt(source, 10.0F);
            } else {
                target.discard();
            }
        }
    }

    private boolean updateSentryTarget(LivingEntity owner) {
        Entity target = getSentryTarget();
        if (target != null && target.isAlive() && this.sentryTargetTime > 0) {
            Vec3 targetPosition = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            setNextPosition(targetPosition, SENTRY_SPEED);
            this.sentryTargetTime--;
            sentryTouchedEntities(owner);
            return true;
        }
        if (this.sentryTargetTime >= 0) {
            this.sentryTargetTime = 0;
            Vec3 idle = getIdlePosition(owner);
            setNextPosition(idle, 0.5D);
            if (position().distanceToSqr(idle) <= 1.0E-4D) {
                setSentryTarget(null, 0);
            }
            return true;
        }
        return false;
    }

    private void sentryTouchedEntities(LivingEntity owner) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        DamageSource source = ModDamageTypes.senjutsu(this.level(), this, owner);
        for (Entity target : serverLevel.getEntities(this, getBoundingBox().inflate(0.1D),
                target -> target.isAlive() && target != owner && target != this)) {
            if (target instanceof TruthSeekerBallEntity ball && ball.isOwnedBy(owner)) {
                continue;
            }
            Vec3 push = target.position().subtract(position());
            if (push.horizontalDistanceSqr() > 1.0E-8D) {
                push = push.normalize().scale(0.15D);
                target.push(push.x(), 0.0D, push.z());
            }
            if (target instanceof LivingEntity living) {
                living.invulnerableTime = 0;
                living.hurt(source, 10.0F);
            } else {
                target.discard();
            }
        }
    }

    private void setSentryTarget(@Nullable Entity target, int time) {
        this.sentryTargetId = target == null ? -1 : target.getId();
        this.sentryTargetUuid = target == null ? null : target.getUUID();
        this.sentryTargetTime = target == null ? -1 : time;
    }

    @Nullable
    private Entity getSentryTarget() {
        if (this.sentryTargetId >= 0) {
            Entity entity = this.level().getEntity(this.sentryTargetId);
            if (entity != null) {
                return entity;
            }
        }
        if (this.sentryTargetUuid != null && this.level() instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(this.sentryTargetUuid);
            if (entity != null) {
                this.sentryTargetId = entity.getId();
                return entity;
            }
        }
        return null;
    }

    private boolean hasActiveSentryTarget() {
        return this.sentryTargetTime >= 0 && getSentryTarget() != null;
    }

    private void moveGrowAndShoot(LivingEntity owner) {
        Vec3 launchPoint = owner.position().add(0.0D, owner.getBbHeight() + 2.0D, 0.0D);
        if (this.position().distanceTo(launchPoint) > 0.2D) {
            setDeltaMovement(launchPoint.subtract(position()).normalize().scale(MOVE_TO_OWNER_SPEED));
            return;
        }
        float scale = getScale();
        float maxScale = getMaxScale();
        setDeltaMovement(Vec3.ZERO);
        if (scale < maxScale) {
            setScale(Math.min((float)(scale * GROWTH_RATE), maxScale));
            return;
        }
        Vec3 look = owner.getLookAngle();
        if (look.lengthSqr() <= 1.0E-8D) {
            look = Vec3.directionFromRotation(owner.getXRot(), owner.getYHeadRot());
        }
        if (look.lengthSqr() <= 1.0E-8D) {
            look = new Vec3(0.0D, 0.0D, 1.0D);
        }
        shoot(look, LAUNCH_SPEED);
        this.level().playSound(null, getX(), getY(), getZ(),
                ModSounds.SOUND_KAGUYA_FINALTSB.get(), SoundSource.NEUTRAL, 5.0F, 1.0F);
    }

    private void travelAndImpact(LivingEntity owner) {
        Vec3 motion = getDeltaMovement();
        if (motion.lengthSqr() <= 1.0E-8D && this.motionFactor <= 0.0F) {
            haltMotion();
            return;
        }
        Vec3 start = centerPosition();
        Vec3 end = start.add(motion);
        HitResult hit = findImpact(owner, start, end);
        if (hit.getType() != HitResult.Type.MISS) {
            impact(owner, hit);
            return;
        }
        setPos(getX() + motion.x(), getY() + motion.y(), getZ() + motion.z());
        faceMotion(motion);
        int targetTime = this.entityData.get(TARGET_TIME);
        if (targetTime > 0) {
            this.entityData.set(TARGET_TIME, targetTime - 1);
            updateLegacyNoGravityMotion();
        } else {
            haltMotion();
        }
    }

    private HitResult findImpact(LivingEntity owner, Vec3 start, Vec3 end) {
        BlockHitResult blockHit = this.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        double maxDistance = blockHit.getType() == HitResult.Type.MISS ? start.distanceTo(end) : start.distanceTo(blockHit.getLocation());
        double inflate = Math.max(getBbWidth() * 0.5D, 0.3D);
        AABB search = getBoundingBox().expandTowards(end.subtract(start)).inflate(inflate);
        EntityHitResult entityHit = this.level().getEntities(this, search,
                        target -> target.isAlive() && target != owner && !(target instanceof TruthSeekerBallEntity))
                .stream()
                .map(entity -> entity.getBoundingBox().inflate(inflate).clip(start, end)
                        .map(location -> new EntityHitResult(entity, location))
                        .orElse(null))
                .filter(candidate -> candidate != null && start.distanceTo(candidate.getLocation()) <= maxDistance)
                .min(Comparator.comparingDouble(candidate -> start.distanceTo(candidate.getLocation())))
                .orElse(null);
        return entityHit != null ? entityHit : blockHit;
    }

    private void impact(LivingEntity owner, HitResult hit) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 point = hit.getLocation();
        float radius = getScale() * 4.0F + 15.0F;
        DamageSource source = ModDamageTypes.senjutsu(this.level(), this, owner);
        serverLevel.sendParticles(
                ModParticleTypes.options(NarutoParticleKind.EXPANDING_SPHERE,
                        Math.max((int)(radius * 10.0F), 1),
                        Math.max((int)(radius * 4.0F), 1),
                        0xC0FFFFFF),
                point.x(),
                point.y(),
                point.z(),
                1,
                0.0D,
                0.0D,
                0.0D,
                0.0D);
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, new AABB(point, point).inflate(radius),
                target -> target.isAlive() && target != owner && target.distanceToSqr(point) <= radius * radius)) {
            target.invulnerableTime = 0;
            target.hurt(source, radius * 10.0F);
        }
        if (getScale() >= EXPANSIVE_MAX_SCALE && hit instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof LivingEntity livingTarget && livingTarget != owner) {
            livingTarget.setHealth(0.0F);
        }
        if (isExpansive()) {
            startDeath();
        } else {
            haltMotion();
        }
    }

    private void shrinkAndDiscard() {
        setScale(getScale() * 0.9F);
        this.deathTicks++;
        if (this.deathTicks > DEATH_SHRINK_TICKS) {
            discard();
        }
    }

    private void startDeath() {
        if (this.deathTicks == 0) {
            this.deathTicks = 1;
            haltMotion();
        }
    }

    private void moveByDelta() {
        Vec3 movement = getDeltaMovement();
        setPos(getX() + movement.x(), getY() + movement.y(), getZ() + movement.z());
    }

    private void setOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide || isInvulnerableTo(source)) {
            return false;
        }
        LivingEntity owner = getOwner();
        if (owner != null && source.getEntity() == owner) {
            return false;
        }
        this.health -= amount;
        if (this.health <= 0.0F) {
            startDeath();
        }
        return true;
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

    private boolean isOwnedBy(LivingEntity owner) {
        return this.ownerUuid != null && this.ownerUuid.equals(owner.getUUID());
    }

    private boolean isExpansive() {
        return this.entityData.get(EXPANSIVE);
    }

    private boolean isLaunched() {
        return this.entityData.get(LAUNCHED);
    }

    private float getMaxScale() {
        return this.entityData.get(MAX_SCALE);
    }

    private boolean isOwnerHoldingSixPath(LivingEntity owner) {
        return owner.getMainHandItem().is(ModItems.SIX_PATH_SENJUTSU.get())
                || owner.getOffhandItem().is(ModItems.SIX_PATH_SENJUTSU.get());
    }

    private static boolean isOwnerHoldingStack(Player owner, ItemStack stack) {
        return ItemStack.matches(owner.getMainHandItem(), stack) || ItemStack.matches(owner.getOffhandItem(), stack);
    }

    private static boolean isSentryCandidate(Player owner, ItemStack stack, Entity entity) {
        if (entity == null || !entity.isAlive() || entity == owner || entity instanceof ItemEntity || entity instanceof ExperienceOrb) {
            return false;
        }
        if (entity instanceof TruthSeekerBallEntity ball && ball.isOwnedBy(owner)) {
            return false;
        }
        if (isAlreadyTargeted(owner, stack, entity)) {
            return false;
        }
        Vec3 motion = entity.getDeltaMovement();
        return motion.x() * motion.x() + motion.z() * motion.z() > SENTRY_TRIGGER_SPEED_SQR;
    }

    private static boolean isAlreadyTargeted(Player owner, ItemStack stack, Entity target) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        int[] ids = normalizedBallIds(stack);
        for (int id : ids) {
            TruthSeekerBallEntity ball = getBallById(serverLevel, id, owner);
            if (ball != null && ball.targetsEntity(target)) {
                return true;
            }
        }
        return false;
    }

    private boolean targetsEntity(Entity target) {
        if (this.sentryTargetId == target.getId()) {
            return true;
        }
        return this.sentryTargetUuid != null && this.sentryTargetUuid.equals(target.getUUID());
    }

    private static int[] normalizedBallIds(ItemStack stack) {
        int[] existing = stack.getOrCreateTag().getIntArray(net.narutomod.item.SixPathSenjutsuItem.SPAWNED_BALLS_TAG);
        int[] ids = new int[SIX_PATH_BALL_COUNT];
        for (int index = 0; index < ids.length; index++) {
            ids[index] = index < existing.length && existing[index] > 0 ? existing[index] : -1;
        }
        stack.getOrCreateTag().putIntArray(net.narutomod.item.SixPathSenjutsuItem.SPAWNED_BALLS_TAG, ids);
        return ids;
    }

    @Nullable
    private static TruthSeekerBallEntity getBallById(ServerLevel level, int id, LivingEntity owner) {
        if (id < 0) {
            return null;
        }
        Entity entity = level.getEntity(id);
        if (entity instanceof TruthSeekerBallEntity ball && !ball.isExpansive() && ball.isOwnedBy(owner)) {
            return ball;
        }
        return null;
    }

    private void setScale(float scale) {
        this.dimensionsScale = Math.max(scale, 0.1F);
        this.entityData.set(SCALE, this.dimensionsScale);
        refreshDimensions();
    }

    private Vec3 centerPosition() {
        return position().add(0.0D, getBbHeight() * 0.5D, 0.0D);
    }

    private void faceMotion(Vec3 motion) {
        if (motion.lengthSqr() <= 1.0E-8D) {
            return;
        }
        setYRot((float)(-Mth.atan2(motion.x(), motion.z()) * Mth.RAD_TO_DEG));
        setXRot((float)(-Mth.atan2(motion.y(), Math.sqrt(motion.x() * motion.x() + motion.z() * motion.z())) * Mth.RAD_TO_DEG));
        if (this.tickCount == 0) {
            this.yRotO = getYRot();
            this.xRotO = getXRot();
        }
    }

    private void shoot(Vec3 direction, double speed) {
        if (direction.lengthSqr() <= 1.0E-8D) {
            direction = new Vec3(0.0D, 0.0D, 1.0D);
        }
        this.acceleration = direction.normalize().scale(0.1D);
        this.motionFactor = (float)speed;
        this.entityData.set(LAUNCHED, true);
        this.entityData.set(TARGET_TIME, getMaxScale() <= SHIELD_SIZE
                ? (int)Math.sqrt(1400.0D / speed)
                : EXPANSIVE_TARGET_TIME);
        faceMotion(direction);
    }

    private void updateLegacyNoGravityMotion() {
        if (this.motionFactor <= 0.0F) {
            return;
        }
        float factor = this.isInWater() ? this.motionFactor * WATER_SLOWDOWN : this.motionFactor;
        if (this.isInWater() && this.level() instanceof ServerLevel serverLevel) {
            Vec3 motion = getDeltaMovement();
            serverLevel.sendParticles(ParticleTypes.BUBBLE,
                    getX() - motion.x() * 0.25D,
                    getY() - motion.y() * 0.25D,
                    getZ() - motion.z() * 0.25D,
                    4,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D);
        }
        setDeltaMovement(getDeltaMovement().add(this.acceleration).scale(factor));
    }

    private void haltMotion() {
        setDeltaMovement(Vec3.ZERO);
        this.acceleration = Vec3.ZERO;
        this.motionFactor = 0.0F;
        this.entityData.set(TARGET_TIME, 0);
        this.entityData.set(LAUNCHED, false);
    }
}
