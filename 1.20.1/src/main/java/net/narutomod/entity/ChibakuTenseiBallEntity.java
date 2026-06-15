package net.narutomod.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.NarutomodModVariables;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class ChibakuTenseiBallEntity extends Entity {
    public static final float BASE_SIZE = 0.25F;
    public static final float MAX_SCALE = 80.0F;
    private static final int LAUNCH_TICKS = 100;
    private static final int DROP_DELAY_TICKS = 30;
    private static final int BLOCK_CAPTURE_RADIUS = 100;
    private static final int EARTH_BLOCK_GROUP_MIN = 6;
    private static final int EARTH_BLOCK_GROUP_VARIANCE = 4;
    private static final int MAX_SATELLITE_BLOCKS = 8192;
    private static final double PULL_RADIUS = 128.0D;
    private static final double PULL_SPEED = 0.1D;
    private static final int COLLISION_SLOW_TICKS = 2;
    private static final String ACTIVE_TAG = "chibakutensei_active";
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(ChibakuTenseiBallEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(ChibakuTenseiBallEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUuid;
    private final List<BlockPos> blockList = new ArrayList<>();
    private final List<BlockPos> airBlocks = new ArrayList<>();
    private final List<Integer> affectedEarthBlockIds = new ArrayList<>();
    private float dimensionsScale = 1.0F;
    private boolean blockCaptureStarted;
    private boolean maxSizeReached;
    private int dropTime;

    public ChibakuTenseiBallEntity(EntityType<? extends ChibakuTenseiBallEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
        this.noPhysics = true;
    }

    @Nullable
    public static ChibakuTenseiBallEntity spawnFrom(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        ChibakuTenseiBallEntity ball = ModEntityTypes.CHIBAKU_TENSEI_BALL.get().create(serverLevel);
        if (ball == null) {
            return null;
        }
        ball.configure(owner);
        serverLevel.addFreshEntity(ball);
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                ModSounds.SOUND_CHIBAKUTENSEI.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return ball;
    }

    public float getBallScale() {
        return this.entityData.get(SCALE);
    }

    public float activeRadius() {
        return Math.max(getBallScale() * BASE_SIZE * 0.5F, 0.05F);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(SCALE, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            cleanupAndDiscard();
            return;
        }
        if (this.tickCount < LAUNCH_TICKS) {
            tickLaunch();
        } else {
            tickBlackhole(owner);
        }
        spawnParticles();
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(this.dimensionsScale);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SCALE.equals(key)) {
            this.dimensionsScale = Math.max(getBallScale(), 0.1F);
            updateBoundingBox();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        setScale(tag.contains("Scale") ? tag.getFloat("Scale") : 1.0F);
        this.blockCaptureStarted = tag.getBoolean("BlockCaptureStarted");
        this.maxSizeReached = tag.getBoolean("MaxSizeReached");
        this.dropTime = tag.getInt("DropTime");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putFloat("Scale", getBallScale());
        tag.putBoolean("BlockCaptureStarted", this.blockCaptureStarted);
        tag.putBoolean("MaxSizeReached", this.maxSizeReached);
        tag.putInt("DropTime", this.dropTime);
    }

    @Override
    public void remove(RemovalReason reason) {
        LivingEntity owner = getOwner();
        if (owner != null && !this.level().isClientSide) {
            owner.getPersistentData().putBoolean(ACTIVE_TAG, false);
        }
        super.remove(reason);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void configure(LivingEntity owner) {
        setOwner(owner);
        setScale(1.0F);
        moveTo(owner.getX(), owner.getY() + owner.getBbHeight() + 0.5D, owner.getZ(), owner.getYRot(), owner.getXRot());
        owner.getPersistentData().putBoolean(ACTIVE_TAG, true);
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

    private void setScale(float scale) {
        this.dimensionsScale = Mth.clamp(scale, 0.1F, MAX_SCALE);
        this.entityData.set(SCALE, this.dimensionsScale);
        updateBoundingBox();
    }

    private void tickLaunch() {
        Vec3 motion = getDeltaMovement();
        move(MoverType.SELF, motion);
        setScale(Math.max(MAX_SCALE * 0.2F * this.tickCount / (float)LAUNCH_TICKS, 1.0F));
        setDeltaMovement(motion.add(0.0D, 0.03D, 0.0D).scale(0.98D));
        updateBoundingBox();
    }

    private void tickBlackhole(LivingEntity owner) {
        setDeltaMovement(Vec3.ZERO);
        setScale(MAX_SCALE);
        if (!this.blockCaptureStarted) {
            createBlockList();
            this.blockCaptureStarted = true;
        } else {
            doBlackholeThings();
            collideWithNearbyEntities(owner);
        }
        pullNearbyEntities(owner);
        if (this.maxSizeReached && --this.dropTime <= 0) {
            dropSatellite(owner);
        }
    }

    private void pullNearbyEntities(LivingEntity owner) {
        Vec3 center = centerPosition();
        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(PULL_RADIUS),
                entity -> canAffect(owner, entity))) {
            Vec3 delta = center.subtract(target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D));
            if (delta.lengthSqr() <= 1.0E-8D) {
                continue;
            }
            Vec3 pull = delta.normalize().scale(PULL_SPEED);
            target.setDeltaMovement(target.getDeltaMovement().add(pull));
            target.hurtMarked = true;
        }
    }

    private boolean canAffect(LivingEntity owner, LivingEntity target) {
        return target.isAlive()
                && target != owner
                && !target.isSpectator()
                && !(target instanceof Player player && player.isCreative());
    }

    private void createBlockList() {
        this.blockList.clear();
        BlockPos center = BlockPos.containing(position());
        AABB searchBox = getBoundingBox().inflate(BLOCK_CAPTURE_RADIUS);
        for (BlockPos pos : BlockPos.betweenClosed(
                Mth.floor(searchBox.minX), Mth.floor(searchBox.minY), Mth.floor(searchBox.minZ),
                Mth.ceil(searchBox.maxX), Mth.ceil(searchBox.maxY), Mth.ceil(searchBox.maxZ))) {
            BlockState state = this.level().getBlockState(pos);
            float hardness = state.getDestroySpeed(this.level(), pos);
            if (state.isAir() || hardness < 0.0F || hardness > 1000.0F
                    || state.getCollisionShape(this.level(), pos).isEmpty()) {
                continue;
            }
            this.blockList.add(pos.immutable());
        }
        this.blockList.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));
    }

    private void doBlackholeThings() {
        pullTrackedEarthBlocks();
        if (!this.maxSizeReached && !this.blockList.isEmpty() && this.random.nextInt(10) == 0) {
            dislodgeNextBlockGroup();
        }
    }

    private void dislodgeNextBlockGroup() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        EarthBlocksEntity entity = null;
        while (entity == null && !this.blockList.isEmpty()) {
            BlockPos center = this.blockList.remove(0);
            if (this.level().getBlockState(center).isAir()) {
                continue;
            }
            int size = EARTH_BLOCK_GROUP_MIN + this.random.nextInt(EARTH_BLOCK_GROUP_VARIANCE);
            List<BlockPos> group = collectDislodgeGroup(center, size);
            if (group.isEmpty()) {
                continue;
            }
            entity = ModEntityTypes.EARTH_BLOCKS.get().create(serverLevel);
            if (entity != null) {
                BlockPos origin = boundingOrigin(group);
                entity.configureFromBlocks(origin, group);
                entity.setMovementEnabled(true);
                entity.setDeltaMovement(0.0D, 0.1D, 0.0D);
                for (BlockPos pos : group) {
                    this.level().removeBlock(pos, false);
                }
                serverLevel.addFreshEntity(entity);
                this.affectedEarthBlockIds.add(entity.getId());
                this.blockList.removeIf(group::contains);
            }
        }
    }

    private List<BlockPos> collectDislodgeGroup(BlockPos center, int size) {
        List<BlockPos> group = new ArrayList<>();
        int min = -size / 2;
        int max = size / 2 + (size % 2);
        for (int y = min; y < max; y++) {
            for (int z = min; z < max; z++) {
                for (int x = min; x < max; x++) {
                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = this.level().getBlockState(pos);
                    float hardness = state.getDestroySpeed(this.level(), pos);
                    if (!state.isAir() && state.getFluidState().isEmpty() && hardness >= 0.0F) {
                        group.add(pos.immutable());
                        if (group.size() >= MAX_SATELLITE_BLOCKS) {
                            return group;
                        }
                    }
                }
            }
        }
        return group;
    }

    private BlockPos boundingOrigin(List<BlockPos> positions) {
        int minX = positions.get(0).getX();
        int minY = positions.get(0).getY();
        int minZ = positions.get(0).getZ();
        for (BlockPos pos : positions) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
        }
        return new BlockPos(minX, minY, minZ);
    }

    private void pullTrackedEarthBlocks() {
        Vec3 center = centerPosition();
        Iterator<Integer> iterator = this.affectedEarthBlockIds.iterator();
        while (iterator.hasNext()) {
            int id = iterator.next();
            Entity entity = this.level().getEntity(id);
            if (!(entity instanceof EarthBlocksEntity earthBlocks) || !earthBlocks.isAlive()) {
                iterator.remove();
                continue;
            }
            Vec3 earthCenter = earthBlocks.getBoundingBox().getCenter();
            Vec3 delta = center.subtract(earthCenter);
            if (delta.lengthSqr() > 1.0E-8D) {
                earthBlocks.setDeltaMovement(earthBlocks.getDeltaMovement().add(delta.normalize().scale(PULL_SPEED)));
                earthBlocks.hurtMarked = true;
            }
            if (this.getBoundingBox().inflate(1.0D).intersects(earthBlocks.getBoundingBox())) {
                absorbEarthBlocks(earthBlocks);
                iterator.remove();
            }
        }
    }

    private void absorbEarthBlocks(EarthBlocksEntity earthBlocks) {
        List<BlockState> states = earthBlocks.copyBlockStates();
        ensureAirBlocks();
        Iterator<BlockPos> airIterator = this.airBlocks.iterator();
        int stateIndex = 0;
        while (airIterator.hasNext() && stateIndex < states.size()) {
            BlockPos pos = airIterator.next();
            if (this.level().getBlockState(pos).isAir()) {
                this.level().setBlock(pos, states.get(stateIndex++), 3);
            }
            airIterator.remove();
        }
        earthBlocks.clearBlocksAndDiscard();
        maybeMarkMaxSizeReached();
    }

    private void ensureAirBlocks() {
        if (!this.airBlocks.isEmpty()) {
            return;
        }
        Vec3 center = centerPosition();
        double grow = Math.max(maxRadius() - activeRadius(), 0.0D);
        AABB searchBox = getBoundingBox().inflate(grow);
        for (BlockPos pos : BlockPos.betweenClosed(
                Mth.floor(searchBox.minX), Mth.floor(searchBox.minY), Mth.floor(searchBox.minZ),
                Mth.ceil(searchBox.maxX), Mth.ceil(searchBox.maxY), Mth.ceil(searchBox.maxZ))) {
            if (this.level().getBlockState(pos).isAir()) {
                this.airBlocks.add(pos.immutable());
            }
        }
        this.airBlocks.sort(Comparator.comparingDouble(pos -> pos.distToCenterSqr(center.x(), center.y(), center.z())));
    }

    private void maybeMarkMaxSizeReached() {
        if (this.maxSizeReached) {
            return;
        }
        Vec3 center = centerPosition();
        this.airBlocks.removeIf(pos -> !this.level().getBlockState(pos).isAir());
        if (this.airBlocks.isEmpty()
                || this.airBlocks.get(0).distToCenterSqr(center.x(), center.y(), center.z()) >= (double)activeRadius() * activeRadius()) {
            this.maxSizeReached = true;
            this.dropTime = DROP_DELAY_TICKS;
        }
    }

    private void collideWithNearbyEntities(LivingEntity owner) {
        Vec3 center = centerPosition();
        DamageSource source = damageSources().flyIntoWall();
        for (Entity entity : this.level().getEntities(this, this.getBoundingBox().inflate(1.0D),
                target -> target.isAlive() && target != owner && !(target instanceof EarthBlocksEntity))) {
            if (!(entity instanceof LivingEntity) || entity.distanceToSqr(center) <= activeRadius() * activeRadius()) {
                entity.invulnerableTime = 0;
                entity.hurt(source, 10.0F);
                if (entity instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, COLLISION_SLOW_TICKS, 3, false, false));
                }
            }
        }
    }

    private void dropSatellite(LivingEntity owner) {
        List<BlockState> states = captureCoreBlocks();
        if (!states.isEmpty()) {
            ChibakuSatelliteEntity.spawnFromStates(owner, position(), states);
        }
        cleanupAndDiscard();
    }

    private List<BlockState> captureCoreBlocks() {
        List<BlockState> states = new ArrayList<>();
        AABB searchBox = getBoundingBox().inflate(1.0D);
        for (BlockPos pos : BlockPos.betweenClosed(
                Mth.floor(searchBox.minX), Mth.floor(searchBox.minY), Mth.floor(searchBox.minZ),
                Mth.ceil(searchBox.maxX), Mth.ceil(searchBox.maxY), Mth.ceil(searchBox.maxZ))) {
            BlockState state = this.level().getBlockState(pos);
            float hardness = state.getDestroySpeed(this.level(), pos);
            if (state.isAir() || hardness < 0.0F || hardness > 1000.0F) {
                continue;
            }
            states.add(state);
            this.level().removeBlock(pos, false);
            if (states.size() >= MAX_SATELLITE_BLOCKS) {
                break;
            }
        }
        return states;
    }

    private Vec3 centerPosition() {
        return getBoundingBox().getCenter();
    }

    private double maxRadius() {
        AABB box = getBoundingBox();
        double x = box.getXsize() * 0.5D;
        double y = box.getYsize() * 0.5D;
        double z = box.getZsize() * 0.5D;
        return Math.sqrt(x * x + y * y + z * z);
    }

    private void spawnParticles() {
        if (!(this.level() instanceof ServerLevel serverLevel) || this.tickCount % 3 != 0) {
            return;
        }
        serverLevel.sendParticles(
                ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0x80101020, 16, 20, 0xF0, -1, 4),
                getX(),
                getY(),
                getZ(),
                Mth.clamp((int)(activeRadius() * 6.0F), 12, 120),
                activeRadius() * 0.4D,
                activeRadius() * 0.4D,
                activeRadius() * 0.4D,
                0.02D);
    }

    private void cleanupAndDiscard() {
        discard();
    }

    private void updateBoundingBox() {
        double radius = activeRadius();
        setBoundingBox(new AABB(getX() - radius, getY() - radius, getZ() - radius,
                getX() + radius, getY() + radius, getZ() + radius));
    }
}
