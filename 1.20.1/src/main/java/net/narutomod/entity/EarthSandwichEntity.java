package net.narutomod.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class EarthSandwichEntity extends Entity {
    private static final int MOVE_DURATION = 100;
    private static final double MOVE_SPEED = 0.15D;

    private final List<Integer> leftMovingBlockIds = new ArrayList<>();
    private final List<Integer> rightMovingBlockIds = new ArrayList<>();
    private int leftWallId = -1;
    private int rightWallId = -1;
    private int moveTick = -1;
    private float snappedYaw;
    private float power;
    @Nullable
    private UUID ownerUuid;

    public EarthSandwichEntity(EntityType<? extends EarthSandwichEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public void configure(LivingEntity owner, Entity target, float power) {
        this.ownerUuid = owner.getUUID();
        this.power = power;
        this.snappedYaw = snapYaw(owner.getYRot());
        moveTo(target.getX(), target.getY(), target.getZ(), this.snappedYaw, 0.0F);
        spawnWalls(owner, target, power);
    }

    public static boolean hasTarget(LivingEntity owner) {
        return targetLookedAt(owner) != null;
    }

    public static boolean canSpawnFrom(LivingEntity owner, float power) {
        Entity target = targetLookedAt(owner);
        return target != null && canCreateWalls(owner, target, power);
    }

    public static boolean spawnFrom(LivingEntity owner, float power) {
        Entity target = targetLookedAt(owner);
        if (target == null || !(owner.level() instanceof ServerLevel level)) {
            return false;
        }
        EarthSandwichEntity entity = ModEntityTypes.EARTH_SANDWICH.get().create(level);
        if (entity == null) {
            return false;
        }
        entity.configure(owner, target, power);
        if (entity.leftWallId < 0 || entity.rightWallId < 0) {
            entity.discard();
            return false;
        }
        level.addFreshEntity(entity);
        level.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                power >= 8.0F ? ModSounds.SOUND_SANDO_NO_JUTSU.get() : ModSounds.SOUND_JUTSU.get(),
                SoundSource.NEUTRAL,
                power >= 8.0F ? 5.0F : 1.0F,
                1.0F);
        return true;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        EarthWallEntity leftWall = wallById(this.leftWallId);
        EarthWallEntity rightWall = wallById(this.rightWallId);
        if (this.moveTick < 0) {
            if (leftWall == null || rightWall == null) {
                cleanupAndDiscard();
                return;
            }
            if (leftWall.isDone() && rightWall.isDone()) {
                beginMove(leftWall, rightWall);
            }
            return;
        }

        int elapsed = this.tickCount - this.moveTick;
        if (elapsed > MOVE_DURATION) {
            cleanupAndDiscard();
            return;
        }
        Vec3 leftMotion = directionMotion(this.snappedYaw + 90.0F);
        Vec3 rightMotion = directionMotion(this.snappedYaw - 90.0F);
        moveFallingBlocks(this.leftMovingBlockIds, leftMotion);
        moveFallingBlocks(this.rightMovingBlockIds, rightMotion);
        breakObstacles(this.leftMovingBlockIds, leftMotion);
        breakObstacles(this.rightMovingBlockIds, rightMotion);
        damageEntities(this.leftMovingBlockIds, leftMotion, this.leftMovingBlockIds.size());
        damageEntities(this.rightMovingBlockIds, rightMotion, this.rightMovingBlockIds.size());
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && reason.shouldDestroy()) {
            cleanupMovingBlocks();
        }
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.leftWallId = tag.getInt("LeftWallId");
        this.rightWallId = tag.getInt("RightWallId");
        this.moveTick = tag.contains("MoveTick") ? tag.getInt("MoveTick") : -1;
        this.snappedYaw = tag.getFloat("SnappedYaw");
        this.power = tag.getFloat("Power");
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("LeftWallId", this.leftWallId);
        tag.putInt("RightWallId", this.rightWallId);
        tag.putInt("MoveTick", this.moveTick);
        tag.putFloat("SnappedYaw", this.snappedYaw);
        tag.putFloat("Power", this.power);
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Nullable
    private static Entity targetLookedAt(LivingEntity owner) {
        HitResult hit = ProcedureUtils.objectEntityLookingAt(owner, 30.0D, 0.0D, false, false, target -> target != owner);
        return hit instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;
    }

    private static boolean canCreateWalls(LivingEntity owner, Entity target, float power) {
        if (!(owner.level() instanceof ServerLevel level)) {
            return false;
        }
        float yaw = snapYaw(owner.getYRot());
        Vec3[] centers = wallCenters(target.position(), yaw, power);
        for (Vec3 center : centers) {
            EarthWallEntity wall = ModEntityTypes.ENTITYEARTHWALL.get().create(level);
            if (wall == null) {
                return false;
            }
            wall.configure(center, yaw + 90.0F, power, power, power * 0.6D, false);
            if (!wall.hasBuildPlan()) {
                return false;
            }
        }
        return true;
    }

    private void spawnWalls(LivingEntity owner, Entity target, float power) {
        if (!(owner.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3[] centers = wallCenters(target.position(), this.snappedYaw, power);
        EarthWallEntity left = createWall(level, centers[0], this.snappedYaw + 90.0F, power);
        EarthWallEntity right = createWall(level, centers[1], this.snappedYaw + 90.0F, power);
        if (left != null) {
            level.addFreshEntity(left);
            this.leftWallId = left.getId();
        }
        if (right != null) {
            level.addFreshEntity(right);
            this.rightWallId = right.getId();
        }
    }

    @Nullable
    private EarthWallEntity createWall(ServerLevel level, Vec3 center, float yaw, float power) {
        EarthWallEntity wall = ModEntityTypes.ENTITYEARTHWALL.get().create(level);
        if (wall == null) {
            return null;
        }
        wall.configure(center, yaw, power, power, power * 0.6D, false);
        return wall.hasBuildPlan() ? wall : null;
    }

    private static Vec3[] wallCenters(Vec3 target, float yaw, float width) {
        Vec3 left = Vec3.directionFromRotation(0.0F, yaw - 90.0F).scale(width);
        Vec3 right = Vec3.directionFromRotation(0.0F, yaw + 90.0F).scale(width);
        return new Vec3[] {target.add(left), target.add(right)};
    }

    private static float snapYaw(float yaw) {
        return Mth.wrapDegrees(net.minecraft.core.Direction.fromYRot(yaw).toYRot());
    }

    @Nullable
    private EarthWallEntity wallById(int id) {
        if (id < 0) {
            return null;
        }
        Entity entity = this.level().getEntity(id);
        return entity instanceof EarthWallEntity wall ? wall : null;
    }

    private void beginMove(EarthWallEntity leftWall, EarthWallEntity rightWall) {
        this.moveTick = this.tickCount;
        collectFallingBlocks(leftWall, this.leftMovingBlockIds);
        collectFallingBlocks(rightWall, this.rightMovingBlockIds);
        leftWall.releasePlacedBlocksToController();
        rightWall.releasePlacedBlocksToController();
        this.leftWallId = -1;
        this.rightWallId = -1;
    }

    private void collectFallingBlocks(EarthWallEntity wall, List<Integer> output) {
        output.clear();
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (BlockPos pos : wall.getPlacedBlocks()) {
            BlockState state = this.level().getBlockState(pos);
            if (!state.isAir()) {
                FallingBlockEntity falling = FallingBlockEntity.fall(serverLevel, pos, state);
                falling.setNoGravity(true);
                falling.setDeltaMovement(Vec3.ZERO);
                falling.hasImpulse = true;
                output.add(falling.getId());
            }
        }
    }

    private static Vec3 directionMotion(float yaw) {
        Vec3 vector = Vec3.directionFromRotation(0.0F, yaw);
        if (vector.lengthSqr() <= 1.0E-8D) {
            return Vec3.ZERO;
        }
        return vector.normalize().scale(MOVE_SPEED);
    }

    private void moveFallingBlocks(List<Integer> ids, Vec3 motion) {
        for (FallingBlockEntity block : fallingBlocks(ids)) {
            block.setNoGravity(true);
            block.setDeltaMovement(motion);
            block.hasImpulse = true;
            block.hurtMarked = true;
        }
    }

    private void breakObstacles(List<Integer> ids, Vec3 motion) {
        float destroyHardness = Mth.clamp(Mth.sqrt((float)Math.max(ids.size(), 1)), 0.5F, 50.0F);
        for (FallingBlockEntity block : fallingBlocks(ids)) {
            AABB swept = block.getBoundingBox().expandTowards(motion).inflate(0.05D);
            for (BlockPos mutablePos : BlockPos.betweenClosed(
                    Mth.floor(swept.minX), Mth.floor(swept.minY), Mth.floor(swept.minZ),
                    Mth.floor(swept.maxX), Mth.floor(swept.maxY), Mth.floor(swept.maxZ))) {
                BlockPos pos = mutablePos.immutable();
                BlockState state = this.level().getBlockState(pos);
                if (!state.isAir() && state.getDestroySpeed(this.level(), pos) >= 0.0F) {
                    ProcedureUtils.breakBlockAndDropWithChance(this.level(), pos, destroyHardness, 1.0F, 0.1F, false);
                }
            }
        }
    }

    private void damageEntities(List<Integer> ids, Vec3 motion, int mass) {
        LivingEntity owner = getOwner();
        float damage = Math.max((float)(motion.length() * mass * 4.0D), 2.0F);
        List<Integer> damaged = new ArrayList<>();
        for (FallingBlockEntity block : fallingBlocks(ids)) {
            AABB box = block.getBoundingBox().expandTowards(motion).inflate(0.05D);
            for (Entity target : this.level().getEntities(this, box, entity -> canDamage(entity, owner))) {
                if (damaged.contains(target.getId())) {
                    continue;
                }
                damaged.add(target.getId());
                if (target instanceof LivingEntity living) {
                    living.invulnerableTime = 0;
                }
                target.hurt(ModDamageTypes.ninjutsu(this.level(), this, owner), damage);
                target.push(motion.x() * 0.6D, 0.0D, motion.z() * 0.6D);
                target.hurtMarked = true;
            }
        }
    }

    private boolean canDamage(Entity target, @Nullable LivingEntity owner) {
        return target.isAlive()
                && target != this
                && !(target instanceof FallingBlockEntity)
                && !(target instanceof EarthWallEntity)
                && !(target instanceof EarthSandwichEntity)
                && (owner == null || target != owner && target.getRootVehicle() != owner.getRootVehicle());
    }

    @Nullable
    private LivingEntity getOwner() {
        if (this.ownerUuid != null && this.level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(this.ownerUuid) instanceof LivingEntity owner) {
            return owner;
        }
        return null;
    }

    private void cleanupAndDiscard() {
        cleanupMovingBlocks();
        discard();
    }

    private void cleanupMovingBlocks() {
        releaseFallingBlocks(this.leftMovingBlockIds);
        releaseFallingBlocks(this.rightMovingBlockIds);
        EarthWallEntity leftWall = wallById(this.leftWallId);
        EarthWallEntity rightWall = wallById(this.rightWallId);
        if (leftWall != null) {
            leftWall.releasePlacedBlocksToController();
        }
        if (rightWall != null) {
            rightWall.releasePlacedBlocksToController();
        }
        this.leftWallId = -1;
        this.rightWallId = -1;
    }

    private List<FallingBlockEntity> fallingBlocks(List<Integer> ids) {
        ids.removeIf(id -> !(this.level().getEntity(id) instanceof FallingBlockEntity));
        List<FallingBlockEntity> result = new ArrayList<>();
        for (Integer id : ids) {
            Entity entity = this.level().getEntity(id);
            if (entity instanceof FallingBlockEntity falling) {
                result.add(falling);
            }
        }
        return result;
    }

    private void releaseFallingBlocks(List<Integer> ids) {
        for (FallingBlockEntity block : fallingBlocks(ids)) {
            block.setNoGravity(false);
            block.hasImpulse = true;
            block.hurtMarked = true;
        }
        ids.clear();
    }
}
