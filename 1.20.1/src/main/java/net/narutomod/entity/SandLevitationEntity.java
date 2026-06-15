package net.narutomod.entity;

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
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.Chakra;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModParticleTypes;

public final class SandLevitationEntity extends Entity {
    public static final String ENTITY_ID_KEY = "JitonSandLevitationEntityIdKey";
    public static final int DEFAULT_COLOR = SandBulletEntity.DEFAULT_COLOR;
    public static final double CHAKRA_COST_PER_TICK = 0.25D;
    private static final int WAIT_TIME = 40;
    private static final int RETURN_TIME = 40;
    private static final int MAX_IDLE_TIME = 200;
    private static final double WALK_ACCELERATION = 0.04D;
    private static final double SPRINT_ACCELERATION = 0.1D;
    private static final EntityDataAccessor<Integer> SUMMONER_ID = SynchedEntityData.defineId(SandLevitationEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(SandLevitationEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FIRST_RIDDEN = SynchedEntityData.defineId(SandLevitationEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RETURNING = SynchedEntityData.defineId(SandLevitationEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID summonerUuid;
    private int returnTicks;

    public SandLevitationEntity(EntityType<? extends SandLevitationEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.blocksBuilding = true;
    }

    public static boolean spawnFrom(Player owner, int color) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        SandLevitationEntity active = findActive(serverLevel, owner);
        if (active != null) {
            if (!owner.isPassenger() && !active.isReturning()) {
                owner.startRiding(active, true);
                active.setFirstRidden(true);
            }
            return false;
        }
        SandLevitationEntity entity = ModEntityTypes.SAND_LEVITATION.get().create(owner.level());
        if (entity == null) {
            return false;
        }
        entity.configure(owner, color);
        boolean added = owner.level().addFreshEntity(entity);
        if (added) {
            owner.getPersistentData().putInt(ENTITY_ID_KEY, entity.getId());
            owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                    SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 0.8F, owner.getRandom().nextFloat() * 0.4F + 0.8F);
        }
        return added;
    }

    @Nullable
    public static SandLevitationEntity findActive(ServerLevel level, Player owner) {
        int id = owner.getPersistentData().getInt(ENTITY_ID_KEY);
        if (id > 0 && level.getEntity(id) instanceof SandLevitationEntity entity && entity.isOwnedBy(owner) && entity.isAlive()) {
            return entity;
        }
        for (SandLevitationEntity entity : level.getEntitiesOfClass(SandLevitationEntity.class, owner.getBoundingBox().inflate(96.0D))) {
            if (entity.isOwnedBy(owner) && entity.isAlive()) {
                owner.getPersistentData().putInt(ENTITY_ID_KEY, entity.getId());
                return entity;
            }
        }
        owner.getPersistentData().remove(ENTITY_ID_KEY);
        return null;
    }

    public void configure(Player owner, int color) {
        setSummoner(owner);
        setColor(color);
        Vec3 look = owner.getLookAngle();
        Vec3 horizontal = new Vec3(look.x(), 0.0D, look.z());
        if (horizontal.lengthSqr() <= 1.0E-8D) {
            horizontal = Vec3.directionFromRotation(0.0F, owner.getYRot());
            horizontal = new Vec3(horizontal.x(), 0.0D, horizontal.z());
        }
        if (horizontal.lengthSqr() <= 1.0E-8D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        }
        Vec3 pos = owner.position().add(horizontal.normalize().scale(2.0D));
        moveTo(pos.x(), pos.y(), pos.z(), owner.getYRot(), 0.0F);
        setDeltaMovement(Vec3.ZERO);
    }

    public boolean isOwnedBy(Entity owner) {
        if (owner.getUUID().equals(this.summonerUuid)) {
            return true;
        }
        int ownerId = this.entityData.get(SUMMONER_ID);
        return ownerId >= 0 && ownerId == owner.getId();
    }

    public boolean isReturning() {
        return this.entityData.get(RETURNING);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(SUMMONER_ID, -1);
        this.entityData.define(COLOR, DEFAULT_COLOR);
        this.entityData.define(FIRST_RIDDEN, false);
        this.entityData.define(RETURNING, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        Player summoner = getSummoner();
        if (summoner == null || !summoner.isAlive()) {
            discard();
            return;
        }
        if (this.tickCount > MAX_IDLE_TIME && !isFirstRidden()) {
            startReturning();
        }
        if (isReturning()) {
            tickReturning(summoner);
            return;
        }
        tickSandCloud(summoner);
        if (!isFirstRidden() && this.tickCount >= WAIT_TIME && !summoner.isPassenger()) {
            if (summoner.startRiding(this, true)) {
                setFirstRidden(true);
            }
        }
        if (isSummonerControlling(summoner)) {
            tickControlled(summoner);
        } else if (isFirstRidden()) {
            startReturning();
        } else {
            setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && isOwnedBy(player) && !isReturning()) {
            player.startRiding(this, true);
            setFirstRidden(true);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public boolean isPickable() {
        return !isReturning();
    }

    @Override
    public boolean canBeCollidedWith() {
        return !isReturning();
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Player && getPassengers().size() < 3 && !isReturning();
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof LivingEntity livingEntity ? livingEntity : null;
    }

    @Override
    public double getPassengersRidingOffset() {
        return getBbHeight() + 0.35D;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (!hasPassenger(passenger)) {
            return;
        }
        Vec3[] offsets = {
                new Vec3(0.3D, 0.0D, 0.0D),
                new Vec3(-0.5D, 0.0D, 0.4D),
                new Vec3(-0.5D, 0.0D, -0.4D)
        };
        int index = Mth.clamp(getPassengers().indexOf(passenger), 0, offsets.length - 1);
        Vec3 rotated = offsets[index].yRot(-getYRot() * Mth.DEG_TO_RAD - ((float)Math.PI / 2.0F));
        moveFunction.accept(passenger,
                getX() + rotated.x(),
                getY() + getPassengersRidingOffset() + passenger.getMyRidingOffset(),
                getZ() + rotated.z());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Summoner")) {
            this.summonerUuid = tag.getUUID("Summoner");
        }
        setColor(tag.contains("Color") ? tag.getInt("Color") : DEFAULT_COLOR);
        setFirstRidden(tag.getBoolean("FirstRidden"));
        setReturning(tag.getBoolean("Returning"));
        this.returnTicks = tag.getInt("ReturnTicks");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.summonerUuid != null) {
            tag.putUUID("Summoner", this.summonerUuid);
        }
        tag.putInt("Color", getColor());
        tag.putBoolean("FirstRidden", isFirstRidden());
        tag.putBoolean("Returning", isReturning());
        tag.putInt("ReturnTicks", this.returnTicks);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void remove(RemovalReason reason) {
        Player summoner = getSummoner();
        if (summoner != null && summoner.getPersistentData().getInt(ENTITY_ID_KEY) == getId()) {
            summoner.getPersistentData().remove(ENTITY_ID_KEY);
        }
        super.remove(reason);
    }

    private void tickControlled(Player rider) {
        if (!rider.isCreative() && !Chakra.pathway(rider).consume(CHAKRA_COST_PER_TICK)) {
            rider.stopRiding();
            startReturning();
            return;
        }
        setYRot(rider.getYRot());
        setXRot(0.0F);
        yRotO = getYRot();
        xRotO = getXRot();
        rider.fallDistance = 0.0F;
        setDeltaMovement(getDeltaMovement().scale(0.9D).add(controlAcceleration(rider)));
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(0.96D));
        if (onGround()) {
            setDeltaMovement(getDeltaMovement().add(0.0D, 0.02D, 0.0D));
        }
    }

    private Vec3 controlAcceleration(Player rider) {
        float forwardInput = rider.zza;
        float strafeInput = rider.xxa;
        double speed = rider.isSprinting() ? SPRINT_ACCELERATION : WALK_ACCELERATION;
        Vec3 look = rider.getLookAngle();
        Vec3 forward = new Vec3(look.x(), 0.0D, look.z());
        if (forward.lengthSqr() <= 1.0E-8D) {
            forward = Vec3.directionFromRotation(0.0F, rider.getYRot());
            forward = new Vec3(forward.x(), 0.0D, forward.z());
        }
        if (forward.lengthSqr() <= 1.0E-8D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        forward = forward.normalize();
        Vec3 right = new Vec3(-forward.z(), 0.0D, forward.x());
        double up = forwardInput > 0.0F ? -rider.getXRot() / 45.0D : 0.0D;
        return forward.scale(forwardInput * speed)
                .add(right.scale(strafeInput * speed))
                .add(0.0D, up * speed, 0.0D);
    }

    private void tickSandCloud(Player summoner) {
        spawnCloudParticles();
        if (!isFirstRidden() && this.tickCount <= WAIT_TIME) {
            spawnPathParticles(gourdMouthPosition(summoner), position());
        }
        if (this.tickCount % 10 == 0) {
            this.level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 0.18F, this.random.nextFloat() * 0.4F + 0.8F);
        }
    }

    private void tickReturning(Player summoner) {
        ejectPassengers();
        this.returnTicks++;
        Vec3 mouth = gourdMouthPosition(summoner);
        Vec3 delta = mouth.subtract(position());
        if (delta.lengthSqr() > 0.04D) {
            Vec3 step = delta.normalize().scale(Math.min(0.45D, delta.length()));
            setDeltaMovement(step);
            move(MoverType.SELF, step);
        }
        spawnPathParticles(position(), mouth);
        if (this.returnTicks >= RETURN_TIME || position().distanceToSqr(mouth) < 0.36D) {
            discard();
        }
    }

    private void startReturning() {
        if (!isReturning()) {
            setReturning(true);
            this.returnTicks = 0;
            setDeltaMovement(Vec3.ZERO);
        }
    }

    private boolean isSummonerControlling(Player summoner) {
        return getControllingPassenger() == summoner;
    }

    private void spawnCloudParticles() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        double scale = Mth.clamp(this.tickCount / (double)(WAIT_TIME / 2), 0.2D, 2.0D);
        serverLevel.sendParticles(
                ModParticleTypes.options(NarutoParticleKind.SUSPENDED_COLORED, getColor(), 20, 3),
                getX(),
                getY() + 0.15D,
                getZ(),
                90,
                0.55D * scale,
                0.12D * scale,
                0.55D * scale,
                0.0D);
    }

    private void spawnPathParticles(Vec3 from, Vec3 to) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 delta = to.subtract(from);
        int steps = Mth.clamp((int)(delta.length() * 3.0D), 3, 18);
        for (int i = 0; i <= steps; i++) {
            Vec3 point = from.add(delta.scale(i / (double)steps));
            serverLevel.sendParticles(
                    ModParticleTypes.options(NarutoParticleKind.SUSPENDED_COLORED, getColor(), 12, 3),
                    point.x(),
                    point.y(),
                    point.z(),
                    4,
                    0.08D,
                    0.08D,
                    0.08D,
                    0.0D);
        }
    }

    private Vec3 gourdMouthPosition(LivingEntity owner) {
        Vec3 look = owner.getLookAngle();
        Vec3 right = new Vec3(-look.z(), 0.0D, look.x());
        if (right.lengthSqr() <= 1.0E-8D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 safeLook = look.lengthSqr() <= 1.0E-8D ? new Vec3(0.0D, 0.0D, 1.0D) : look.normalize();
        return owner.position()
                .add(right.scale(-0.35D))
                .add(0.0D, owner.getBbHeight() * 0.75D, 0.0D)
                .subtract(safeLook.scale(0.25D));
    }

    private void setSummoner(Player owner) {
        this.summonerUuid = owner.getUUID();
        this.entityData.set(SUMMONER_ID, owner.getId());
    }

    @Nullable
    private Player getSummoner() {
        int summonerId = this.entityData.get(SUMMONER_ID);
        if (summonerId >= 0 && this.level().getEntity(summonerId) instanceof Player player) {
            return player;
        }
        if (this.summonerUuid != null && this.level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(this.summonerUuid) instanceof Player player) {
            setSummoner(player);
            return player;
        }
        return null;
    }

    private int getColor() {
        return this.entityData.get(COLOR);
    }

    public int getColorForRender() {
        return getColor();
    }

    private void setColor(int color) {
        this.entityData.set(COLOR, color);
    }

    private boolean isFirstRidden() {
        return this.entityData.get(FIRST_RIDDEN);
    }

    private void setFirstRidden(boolean firstRidden) {
        this.entityData.set(FIRST_RIDDEN, firstRidden);
    }

    private void setReturning(boolean returning) {
        this.entityData.set(RETURNING, returning);
    }
}
