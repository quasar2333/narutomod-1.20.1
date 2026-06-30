package net.narutomod.entity;

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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.Chakra;
import net.narutomod.PlayerTracker;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class HakkeshoKeitenEntity extends Entity {
    public static final double CHAKRA_COST_PER_TICK = 4.0D;
    private static final int MATURE_TICKS = 10;
    private static final float MIN_SCALE = 1.0F;
    private static final float MAX_SCALE = 6.0F;
    private static final double MOVE_ACCELERATION = 0.05D;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(HakkeshoKeitenEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(HakkeshoKeitenEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUuid;
    private float dimensionsScale = MIN_SCALE;

    public HakkeshoKeitenEntity(EntityType<? extends HakkeshoKeitenEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
        this.blocksBuilding = true;
    }

    public static boolean spawnFrom(Player owner) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        HakkeshoKeitenEntity active = findActive(serverLevel, owner);
        if (active != null) {
            owner.startRiding(active, true);
            return false;
        }
        HakkeshoKeitenEntity entity = ModEntityTypes.HAKKESHOKEITENENTITY.get().create(serverLevel);
        if (entity == null) {
            return false;
        }
        entity.configure(owner);
        boolean added = serverLevel.addFreshEntity(entity);
        if (added) {
            owner.startRiding(entity, true);
            serverLevel.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                    ModSounds.SOUND_HAKKESHOKAITEN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return added;
    }

    public static boolean hasActiveFor(Player owner) {
        return owner.level() instanceof ServerLevel serverLevel && findActive(serverLevel, owner) != null;
    }

    public static int releaseOwnedBy(Player owner) {
        if (owner.getVehicle() instanceof HakkeshoKeitenEntity ridden && ridden.isOwnedBy(owner)) {
            return release(ridden, owner);
        }
        if (owner.level() instanceof ServerLevel serverLevel) {
            HakkeshoKeitenEntity active = findActive(serverLevel, owner);
            if (active != null) {
                return release(active, owner);
            }
        }
        return 0;
    }

    private static int release(HakkeshoKeitenEntity entity, Player owner) {
        int activeTicks = Math.max(entity.tickCount, 1);
        if (owner.getVehicle() == entity) {
            owner.stopRiding();
        }
        entity.ejectPassengers();
        entity.discard();
        return activeTicks;
    }

    @Nullable
    private static HakkeshoKeitenEntity findActive(ServerLevel level, Player owner) {
        for (HakkeshoKeitenEntity entity : level.getEntitiesOfClass(HakkeshoKeitenEntity.class, owner.getBoundingBox().inflate(96.0D))) {
            if (entity.isOwnedBy(owner) && entity.isAlive()) {
                return entity;
            }
        }
        return null;
    }

    private void configure(Player owner) {
        setOwner(owner);
        setShieldScale((float)PlayerTracker.getNinjaLevel(owner) * 0.02F);
        moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(SCALE, MIN_SCALE);
    }

    @Override
    public void tick() {
        this.fallDistance = 0.0F;
        super.tick();
        if (this.level().isClientSide) {
            return;
        }

        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive() || !burnChakra(owner)) {
            discard();
            return;
        }

        if (getControllingPassenger() instanceof Player rider && isOwnedBy(rider)) {
            tickControlled(rider);
        } else if (!isVehicle() && this.tickCount > 5) {
            discard();
            return;
        } else {
            setDeltaMovement(getDeltaMovement().multiply(0.65D, 0.0D, 0.65D));
        }

        if (getMaturity(0.0F) >= 0.9F) {
            breakBlocks(owner);
            ProcedureUtils.purgeHarmfulEffects(owner);
            collideNearby(owner);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        LivingEntity owner = getOwner();
        if (attacker instanceof LivingEntity living
                && attacker != this
                && attacker != owner
                && (owner == null || attacker.getRootVehicle() != owner.getRootVehicle())) {
            living.invulnerableTime = 0;
            living.hurt(source, amount);
        }
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && isOwnedBy(player)) {
            player.startRiding(this, true);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && isOwnedBy(passenger);
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof LivingEntity living ? living : null;
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.35D;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(this.dimensionsScale);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SCALE.equals(key)) {
            this.dimensionsScale = getShieldScale();
            refreshDimensions();
        }
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
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 128.0D * getViewScale();
        return distance < range * range;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(getShieldScale());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        setShieldScale(tag.contains("Scale") ? tag.getFloat("Scale") : MIN_SCALE);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putFloat("Scale", getShieldScale());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public float getRenderShellScale(float partialTick) {
        return getMaturity(partialTick) * getShieldScale() * 3.0F;
    }

    private float getMaturity(float partialTick) {
        return Mth.clamp(((float)this.tickCount + partialTick) / (float)MATURE_TICKS, 0.0F, 1.0F);
    }

    private void tickControlled(Player rider) {
        setYRot(rider.getYRot());
        setXRot(rider.getXRot());
        yRotO = getYRot();
        xRotO = getXRot();
        rider.fallDistance = 0.0F;
        Vec3 motion = getDeltaMovement();
        Vec3 acceleration = controlAcceleration(rider);
        setDeltaMovement(motion.x() * 0.65D + acceleration.x(), 0.0D, motion.z() * 0.65D + acceleration.z());
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().multiply(0.85D, 0.0D, 0.85D));
    }

    private Vec3 controlAcceleration(Player rider) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, rider.getYRot());
        forward = new Vec3(forward.x(), 0.0D, forward.z());
        if (forward.lengthSqr() < 1.0E-8D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        forward = forward.normalize();
        Vec3 right = new Vec3(-forward.z(), 0.0D, forward.x());
        return forward.scale(rider.zza * MOVE_ACCELERATION)
                .add(right.scale(rider.xxa * MOVE_ACCELERATION));
    }

    private boolean burnChakra(LivingEntity owner) {
        if (owner instanceof Player player && player.isCreative()) {
            return true;
        }
        return Chakra.pathway(owner).consume(CHAKRA_COST_PER_TICK);
    }

    private void collideNearby(LivingEntity owner) {
        AABB hitBox = getBoundingBox().inflate(0.2D);
        for (Entity target : this.level().getEntities(this, hitBox, entity -> entity.isAlive()
                && entity != owner
                && entity.getRootVehicle() != getRootVehicle())) {
            ProcedureUtils.pushEntity(this, target, 60.0D, 1.0F);
            target.invulnerableTime = 0;
            float damage = owner instanceof Player player ? (float)PlayerTracker.getNinjaLevel(player) / 4.0F + 10.0F : 10.0F;
            target.hurt(ModDamageTypes.ninjutsu(this.level(), this, owner), damage);
        }
    }

    private void breakBlocks(LivingEntity owner) {
        AABB box = getBoundingBox().inflate(0.0D, 1.0D, 0.0D);
        int minX = Mth.floor(box.minX);
        int minY = Mth.floor(box.minY);
        int minZ = Mth.floor(box.minZ);
        int maxX = Mth.floor(box.maxX);
        int maxY = Mth.floor(box.maxY);
        int maxZ = Mth.floor(box.maxZ);
        boolean canBreakSolid = owner instanceof Player player && PlayerTracker.getNinjaLevel(player) >= 70.0D;
        for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            BlockState state = this.level().getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            if (!state.getFluidState().isEmpty()) {
                this.level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            } else if (canBreakSolid) {
                ProcedureUtils.breakBlockAndDropWithChance(this.level(), pos, 5.0F, 1.0F, 0.3F);
            }
        }
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

    private boolean isOwnedBy(Entity entity) {
        return this.ownerUuid != null && this.ownerUuid.equals(entity.getUUID())
                || this.entityData.get(OWNER_ID) == entity.getId();
    }

    private void setShieldScale(float scale) {
        this.dimensionsScale = Mth.clamp(scale, MIN_SCALE, MAX_SCALE);
        this.entityData.set(SCALE, this.dimensionsScale);
        refreshDimensions();
    }

    public float getShieldScale() {
        return this.entityData.get(SCALE);
    }
}
