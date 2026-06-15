package net.narutomod.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.Chakra;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class SealingEntity extends Entity {
    public static final double RADIUS = 8.0D;
    private static final double VALIDATION_RADIUS_SQR = 49.0D;
    private static final int LIFETIME_TICKS = 200;
    private static final int FUUIN_TOTAL_PROGRESS = 400;
    private static final int FUUIN_PROGRESS_PER_SEALER = 2;
    private static final int FUUIN_SMOKE_COLOR = 0x10B00000;
    private static final int FUUIN_SMOKE_LIFETIME = 100;
    private static final double TARGET_VERTICAL_RANGE = 8.0D;
    private static final double PASSENGER_CHAKRA_PER_SECOND = 10.0D;
    private static final String OWNER_TAG = "Owner";
    private static final String TARGET_TAG = "FuuinTarget";
    private static final String PROGRESS_TAG = "FuuinProgress";
    private static final EntityDataAccessor<Integer> OWNER_ID =
            SynchedEntityData.defineId(SealingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID =
            SynchedEntityData.defineId(SealingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FUUIN_PROGRESS =
            SynchedEntityData.defineId(SealingEntity.class, EntityDataSerializers.INT);
    private static final BlockPos[] TORCH_OFFSETS = {
            new BlockPos(-2, 1, 1),
            new BlockPos(-1, 1, 2),
            new BlockPos(1, 1, 2),
            new BlockPos(2, 1, 1),
            new BlockPos(2, 1, -1),
            new BlockPos(1, 1, -2),
            new BlockPos(-1, 1, -2),
            new BlockPos(-2, 1, -1)
    };
    private static final Vec3[] SEAT_OFFSETS = {
            new Vec3(-5.0D, 0.0D, 0.0D),
            new Vec3(0.0D, 0.0D, 5.0D),
            new Vec3(5.0D, 0.0D, 0.0D),
            new Vec3(0.0D, 0.0D, -5.0D)
    };

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private UUID targetUuid;

    public SealingEntity(EntityType<? extends SealingEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public static boolean spawnFrom(LivingEntity owner) {
        BlockHitResult hit = findPlacement(owner);
        if (hit == null || !(owner.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        return spawnAt(serverLevel, hit.getBlockPos(), owner) != null;
    }

    @Nullable
    public static SealingEntity spawnAt(ServerLevel serverLevel, BlockPos ground, @Nullable LivingEntity owner) {
        if (!canPlaceAt(serverLevel, ground)) {
            return null;
        }
        SealingEntity seal = ModEntityTypes.SEALING.get().create(serverLevel);
        if (seal == null) {
            return null;
        }
        seal.moveTo(ground.getX() + 0.5D, ground.getY() + 1.0D, ground.getZ() + 0.5D, 0.0F, 0.0F);
        if (owner != null) {
            seal.assignOwner(owner);
        }
        serverLevel.playSound(null, ground.above(), ModSounds.SOUND_JUTSU.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        if (!serverLevel.addFreshEntity(seal)) {
            return null;
        }
        return seal;
    }

    public static boolean hasValidPlacementTarget(LivingEntity owner) {
        return findPlacement(owner) != null;
    }

    public static boolean canPlaceAt(Level level, BlockPos groundCenter) {
        for (BlockPos pos : BlockPos.betweenClosed(groundCenter.offset(-6, 0, -6), groundCenter.offset(6, 0, 6))) {
            if (horizontalDistanceSqr(pos, groundCenter) >= VALIDATION_RADIUS_SQR) {
                continue;
            }
            if (!level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP)) {
                return false;
            }
            if (!level.getBlockState(pos.above(2)).isAir()) {
                return false;
            }
            if (!isTorchOrAir(level, pos.above(), groundCenter)) {
                return false;
            }
        }
        return true;
    }

    private static BlockHitResult findPlacement(LivingEntity owner) {
        BlockHitResult hit = ProcedureUtils.raytraceBlocks(owner, 10.0D);
        if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) {
            return null;
        }
        return canPlaceAt(owner.level(), hit.getBlockPos()) ? hit : null;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(TARGET_ID, -1);
        this.entityData.define(FUUIN_PROGRESS, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (this.tickCount > LIFETIME_TICKS) {
            discard();
            return;
        }
        tickPassengers();
        tickFuuin();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && !player.isPassenger()) {
            player.startRiding(this, true);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof LivingEntity && getPassengers().size() < SEAT_OFFSETS.length;
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.7D;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (!hasPassenger(passenger)) {
            return;
        }
        int index = Math.max(0, Math.min(getPassengers().indexOf(passenger), SEAT_OFFSETS.length - 1));
        Vec3 offset = SEAT_OFFSETS[index];
        moveFunction.accept(passenger,
                getX() + offset.x(),
                getY() + getPassengersRidingOffset() + passenger.getMyRidingOffset(),
                getZ() + offset.z());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.ownerUuid = tag.hasUUID(OWNER_TAG) ? tag.getUUID(OWNER_TAG) : null;
        this.targetUuid = tag.hasUUID(TARGET_TAG) ? tag.getUUID(TARGET_TAG) : null;
        this.entityData.set(FUUIN_PROGRESS, tag.getInt(PROGRESS_TAG));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID(OWNER_TAG, this.ownerUuid);
        }
        if (this.targetUuid != null) {
            tag.putUUID(TARGET_TAG, this.targetUuid);
        }
        tag.putInt(PROGRESS_TAG, getFuuinProgress());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public int getSealersCount() {
        int count = 0;
        for (Entity passenger : getPassengers()) {
            if (passenger instanceof LivingEntity) {
                count++;
            }
        }
        return count;
    }

    private void tickPassengers() {
        List<Entity> passengers = List.copyOf(getPassengers());
        for (Entity passenger : passengers) {
            if (!(passenger instanceof LivingEntity living)
                    || living.isShiftKeyDown()
                    || this.tickCount % 20 == 0 && !Chakra.pathway(living).consume(PASSENGER_CHAKRA_PER_SECOND)) {
                passenger.stopRiding();
            }
        }
    }

    private void tickFuuin() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LivingEntity target = resolveTarget(serverLevel);
        if (target == null) {
            target = findTarget(serverLevel);
            if (target != null) {
                assignTarget(target);
            }
        }
        if (target == null) {
            this.entityData.set(FUUIN_PROGRESS, 0);
            return;
        }
        if (!isValidFuuinTarget(serverLevel, target)) {
            clearTarget();
            return;
        }

        ServerPlayer recipient = resolveRecipient(serverLevel);
        if (recipient == null || BijuManager.getAssignedTail(recipient) > 0) {
            return;
        }

        int sealers = getSealersCount();
        if (sealers <= 0) {
            return;
        }

        holdFuuinTarget(target);

        int progress = Math.min(FUUIN_TOTAL_PROGRESS,
                getFuuinProgress() + sealers * FUUIN_PROGRESS_PER_SEALER);
        this.entityData.set(FUUIN_PROGRESS, progress);
        spawnFuuinFeedback(serverLevel, target, progress);
        if (this.tickCount % 20 == 0) {
            displayProgress(recipient, target, progress);
        }
        if (progress >= FUUIN_TOTAL_PROGRESS) {
            spawnFuuinCompletionFeedback(serverLevel, target);
            if (sealTargetIntoPlayer(target, recipient)) {
                discard();
            } else {
                clearTarget();
            }
        }
    }

    private void spawnFuuinFeedback(ServerLevel serverLevel, LivingEntity target, int progress) {
        if (this.tickCount % 50 == 1) {
            serverLevel.playSound(null, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    ModSounds.SOUND_KAMUISFX.get(), SoundSource.NEUTRAL, 3.0F, 1.0F);
        }
        if (this.tickCount % 5 == 0) {
            int smokeCount = Math.max(1, Math.min(32, progress * 32 / FUUIN_TOTAL_PROGRESS));
            serverLevel.sendParticles(
                    ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED,
                            FUUIN_SMOKE_COLOR,
                            100,
                            FUUIN_SMOKE_LIFETIME,
                            0xF0,
                            target.getId(),
                            4),
                    target.getX(),
                    target.getY() + target.getBbHeight() * 0.5D,
                    target.getZ(),
                    smokeCount,
                    target.getBbWidth() * 0.5D,
                    target.getBbHeight() * 0.5D,
                    target.getBbWidth() * 0.5D,
                    0.02D);
        }
        if (this.tickCount % 20 == 0) {
            serverLevel.sendParticles(
                    ModParticleTypes.options(NarutoParticleKind.SEAL_FORMULA, 80, 0, 40),
                    getX(),
                    getY() + 0.05D,
                    getZ(),
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D);
        }
    }

    private void spawnFuuinCompletionFeedback(ServerLevel serverLevel, LivingEntity target) {
        serverLevel.playSound(null, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                ModSounds.SOUND_KAMUISFX.get(), SoundSource.NEUTRAL, 3.0F, 1.0F);
        serverLevel.sendParticles(
                ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED,
                        FUUIN_SMOKE_COLOR,
                        100,
                        FUUIN_SMOKE_LIFETIME,
                        0xF0,
                        target.getId(),
                        4),
                target.getX(),
                target.getY() + target.getBbHeight() * 0.5D,
                target.getZ(),
                96,
                target.getBbWidth() * 0.5D,
                target.getBbHeight() * 0.5D,
                target.getBbWidth() * 0.5D,
                0.04D);
        serverLevel.sendParticles(ParticleTypes.POOF,
                target.getX(),
                target.getY() + target.getBbHeight() * 0.5D,
                target.getZ(),
                24,
                target.getBbWidth() * 0.4D,
                target.getBbHeight() * 0.4D,
                target.getBbWidth() * 0.4D,
                0.05D);
    }

    @Nullable
    private ServerPlayer resolveRecipient(ServerLevel serverLevel) {
        if (this.ownerUuid != null) {
            ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(this.ownerUuid);
            if (owner != null && owner.isAlive() && !owner.isSpectator()) {
                return owner;
            }
        }
        for (Entity passenger : getPassengers()) {
            if (passenger instanceof ServerPlayer player && player.isAlive() && !player.isSpectator()) {
                return player;
            }
        }
        return null;
    }

    @Nullable
    private LivingEntity resolveTarget(ServerLevel serverLevel) {
        if (this.targetUuid == null) {
            return null;
        }
        Entity entity = serverLevel.getEntity(this.targetUuid);
        return entity instanceof LivingEntity living && living.isAlive() && isValidFuuinTarget(serverLevel, living)
                ? living
                : null;
    }

    @Nullable
    private LivingEntity findTarget(ServerLevel serverLevel) {
        List<LivingEntity> candidates = new ArrayList<>();
        candidates.addAll(serverLevel.getEntitiesOfClass(
                TailedBeastEntity.class,
                getBoundingBox().inflate(RADIUS, TARGET_VERTICAL_RANGE, RADIUS),
                beast -> beast.isAlive() && isValidFuuinTarget(serverLevel, beast)));
        candidates.addAll(serverLevel.getEntitiesOfClass(
                TenTailsEntity.class,
                getBoundingBox().inflate(RADIUS, TARGET_VERTICAL_RANGE, RADIUS),
                beast -> beast.isAlive() && isValidFuuinTarget(serverLevel, beast)));
        candidates.sort(Comparator.comparingDouble(this::distanceToSqr));
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private boolean isValidFuuinTarget(ServerLevel serverLevel, LivingEntity target) {
        if (target instanceof TailedBeastEntity beast) {
            int tails = beast.getTailCount();
            return tails >= BijuManager.MIN_TAILS
                    && tails <= 9
                    && BijuManager.getJinchurikiUuid(serverLevel.getServer(), tails) == null;
        }
        return target instanceof TenTailsEntity
                && BijuManager.getJinchurikiUuid(serverLevel.getServer(), BijuManager.MAX_TAILS) == null;
    }

    private void holdFuuinTarget(LivingEntity target) {
        target.setDeltaMovement(Vec3.ZERO);
        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setTarget(null);
        }
    }

    private static boolean sealTargetIntoPlayer(LivingEntity target, ServerPlayer recipient) {
        if (target instanceof TailedBeastEntity beast) {
            return BijuManager.sealTailedBeastIntoPlayer(beast, recipient);
        }
        if (target instanceof TenTailsEntity beast) {
            return BijuManager.sealTenTailsIntoPlayer(beast, recipient);
        }
        return false;
    }

    private void assignOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
    }

    private void assignTarget(LivingEntity target) {
        this.targetUuid = target.getUUID();
        this.entityData.set(TARGET_ID, target.getId());
    }

    private void clearTarget() {
        this.targetUuid = null;
        this.entityData.set(TARGET_ID, -1);
        this.entityData.set(FUUIN_PROGRESS, 0);
    }

    public int getFuuinProgress() {
        return this.entityData.get(FUUIN_PROGRESS);
    }

    public int getOwnerEntityId() {
        return this.entityData.get(OWNER_ID);
    }

    public int getTargetEntityId() {
        return this.entityData.get(TARGET_ID);
    }

    private void displayProgress(ServerPlayer recipient, LivingEntity target, int progress) {
        Component message = Component.literal("Fuuin " + target.getDisplayName().getString()
                + " " + progress + "/" + FUUIN_TOTAL_PROGRESS);
        recipient.displayClientMessage(message, true);
        for (Entity passenger : getPassengers()) {
            if (passenger instanceof ServerPlayer player && player != recipient) {
                player.displayClientMessage(message, true);
            }
        }
    }

    private static boolean isTorchOrAir(Level level, BlockPos pos, BlockPos groundCenter) {
        if (isTorchPosition(pos, groundCenter)) {
            return level.getBlockState(pos).is(Blocks.TORCH);
        }
        return level.getBlockState(pos).isAir();
    }

    private static boolean isTorchPosition(BlockPos pos, BlockPos groundCenter) {
        for (BlockPos offset : TORCH_OFFSETS) {
            if (pos.equals(groundCenter.offset(offset))) {
                return true;
            }
        }
        return false;
    }

    private static double horizontalDistanceSqr(BlockPos pos, BlockPos center) {
        double dx = pos.getX() - center.getX();
        double dz = pos.getZ() - center.getZ();
        return dx * dx + dz * dz;
    }
}
