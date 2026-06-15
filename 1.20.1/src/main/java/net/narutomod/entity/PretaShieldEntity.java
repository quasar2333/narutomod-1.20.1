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
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.Chakra;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModItems;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class PretaShieldEntity extends Entity {
    public static final String ENTITY_ID_KEY = "RinneganPretaShieldEntityId";
    private static final float MAX_HEALTH = 10.0F;
    private static final double MOVE_ACCELERATION = 0.06D;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(PretaShieldEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(PretaShieldEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerUuid;

    public PretaShieldEntity(EntityType<? extends PretaShieldEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
        this.blocksBuilding = true;
    }

    public static boolean spawnFrom(Player owner) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        PretaShieldEntity active = findActive(serverLevel, owner);
        if (active != null) {
            owner.startRiding(active, true);
            return false;
        }
        PretaShieldEntity entity = ModEntityTypes.PRETASHIELDENTITY.get().create(serverLevel);
        if (entity == null) {
            return false;
        }
        entity.configure(owner);
        boolean added = serverLevel.addFreshEntity(entity);
        if (added) {
            owner.getPersistentData().putInt(ENTITY_ID_KEY, entity.getId());
            owner.startRiding(entity, true);
            serverLevel.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                    ModSounds.SOUND_BULLET_IMPACT.get(), SoundSource.PLAYERS, 0.6F, 0.5F);
        }
        return added;
    }

    @Nullable
    public static PretaShieldEntity findActive(ServerLevel level, Player owner) {
        int id = owner.getPersistentData().getInt(ENTITY_ID_KEY);
        if (id > 0 && level.getEntity(id) instanceof PretaShieldEntity entity && entity.isOwnedBy(owner) && entity.isAlive()) {
            return entity;
        }
        for (PretaShieldEntity entity : level.getEntitiesOfClass(PretaShieldEntity.class, owner.getBoundingBox().inflate(96.0D))) {
            if (entity.isOwnedBy(owner) && entity.isAlive()) {
                owner.getPersistentData().putInt(ENTITY_ID_KEY, entity.getId());
                return entity;
            }
        }
        owner.getPersistentData().remove(ENTITY_ID_KEY);
        return null;
    }

    public void configure(Player owner) {
        setOwner(owner);
        setHealthValue(MAX_HEALTH);
        moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
        setDeltaMovement(Vec3.ZERO);
    }

    public boolean isOwnedBy(Entity owner) {
        if (owner.getUUID().equals(this.ownerUuid)) {
            return true;
        }
        int ownerId = this.entityData.get(OWNER_ID);
        return ownerId >= 0 && ownerId == owner.getId();
    }

    public float getRenderScale(float partialTick) {
        return Mth.clamp(((float)this.tickCount + partialTick) / 30.0F, 0.0F, 1.0F);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(HEALTH, MAX_HEALTH);
    }

    @Override
    public void tick() {
        this.fallDistance = 0.0F;
        super.tick();
        if (this.level().isClientSide) {
            return;
        }

        Player owner = getOwner();
        if (owner == null || !owner.isAlive() || !hasRequiredHelmet(owner)) {
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

        if (this.tickCount % 5 == 0) {
            absorbCollidingEntities(owner);
        }
        spawnIdleParticles(owner);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (amount <= 0.0F || this.level().isClientSide || isIgnoredDamage(source) || isOwnerDamage(source)) {
            return false;
        }

        Player owner = getOwner();
        if (isJutsuDamage(source)) {
            absorbJutsuDamage(source, amount, owner);
            return false;
        }

        boolean fullyAbsorbed = absorbEntityChakra(source.getEntity(), amount);
        if (fullyAbsorbed) {
            return false;
        }
        setHealthValue(getHealthValue() - amount);
        spawnAbsorbParticles(hitPosition(source), 30);
        markHurt();
        if (getHealthValue() <= 0.0F) {
            discard();
        }
        return true;
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
        double range = 96.0D * getViewScale();
        return distance < range * range;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(1.0D);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        setHealthValue(tag.contains("Health") ? tag.getFloat("Health") : MAX_HEALTH);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putFloat("Health", getHealthValue());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void remove(RemovalReason reason) {
        Player owner = getOwner();
        if (owner != null && owner.getPersistentData().getInt(ENTITY_ID_KEY) == getId()) {
            owner.getPersistentData().remove(ENTITY_ID_KEY);
        }
        super.remove(reason);
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

    private void absorbJutsuDamage(DamageSource source, float amount, @Nullable Player owner) {
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity living && attacker != owner) {
            weakenEntity(living, amount);
        }
        Entity direct = source.getDirectEntity();
        if (direct != null && direct != this && !(direct instanceof LivingEntity)) {
            direct.discard();
        }
        if (owner != null) {
            rechargeOwner(owner, amount);
        }
        spawnAbsorbParticles(hitPosition(source), 80);
        markHurt();
    }

    private boolean absorbEntityChakra(@Nullable Entity entity, float amount) {
        Player owner = getOwner();
        if (!(entity instanceof LivingEntity living) || living == owner || !living.isAlive()) {
            return false;
        }
        weakenEntity(living, amount);
        Chakra.pathway(living).consume(amount);
        if (owner != null) {
            rechargeOwner(owner, amount);
        }
        Vec3 hit = living.position().add(0.0D, living.getBbHeight() * 0.5D, 0.0D);
        spawnAbsorbParticles(hit, 50);
        return false;
    }

    private void weakenEntity(LivingEntity entity, float amount) {
        int duration = 400;
        int amplifier = Mth.ceil(amount / 4.0F);
        MobEffectInstance current = entity.getEffect(MobEffects.WEAKNESS);
        if (current != null && amount > 0.1F) {
            duration = current.getDuration() + 60;
            amplifier += current.getAmplifier();
        }
        entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, amplifier, false, true));
        entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 300, 4, false, true));
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 4, false, true));
    }

    private void rechargeOwner(Player owner, float amount) {
        if (amount > 0.0F) {
            Chakra.pathway(owner).consume(-amount, true);
            owner.heal(amount * 0.5F);
        }
    }

    private void absorbCollidingEntities(Player owner) {
        AABB box = getBoundingBox().inflate(0.2D);
        for (Entity entity : this.level().getEntities(this, box, target -> target.isAlive()
                && target != owner
                && target.getRootVehicle() != getRootVehicle())) {
            absorbEntityChakra(entity, 20.0F);
        }
    }

    private void spawnIdleParticles(Player owner) {
        if (!(this.level() instanceof ServerLevel serverLevel) || this.tickCount % 10 != 0) {
            return;
        }
        serverLevel.sendParticles(
                ModParticleTypes.options(NarutoParticleKind.SUSPENDED_COLORED, 0x806AD1FF, 10, 3),
                getX(),
                getY() + 1.1D,
                getZ(),
                24,
                0.7D,
                1.0D,
                0.7D,
                0.0D);
    }

    private void spawnAbsorbParticles(Vec3 pos, int count) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.sendParticles(
                ModParticleTypes.options(NarutoParticleKind.SUSPENDED_COLORED, 0xC06AD1FF, 16, 3),
                pos.x(),
                pos.y(),
                pos.z(),
                count,
                0.25D,
                0.35D,
                0.25D,
                0.03D);
        serverLevel.playSound(null, pos.x(), pos.y(), pos.z(),
                ModSounds.SOUND_BULLET_IMPACT.get(), SoundSource.NEUTRAL, 0.4F, 0.6F);
    }

    private Vec3 hitPosition(DamageSource source) {
        Entity direct = source.getDirectEntity();
        Entity attacker = source.getEntity();
        Entity hit = direct != null ? direct : attacker;
        return hit != null
                ? hit.position().add(0.0D, hit.getBbHeight() * 0.5D, 0.0D)
                : position().add(0.0D, getBbHeight() * 0.5D, 0.0D);
    }

    private boolean isJutsuDamage(DamageSource source) {
        return ModDamageTypes.isNinjutsu(source) || source.is(ModDamageTypes.SENJUTSU);
    }

    private boolean isIgnoredDamage(DamageSource source) {
        return source.is(DamageTypes.CACTUS)
                || source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.FALL)
                || source.is(DamageTypes.IN_WALL)
                || source.is(DamageTypes.STARVE)
                || source.is(DamageTypes.WITHER);
    }

    private boolean isOwnerDamage(DamageSource source) {
        Player owner = getOwner();
        if (owner == null) {
            return false;
        }
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        return attacker == owner
                || direct == owner
                || attacker != null && attacker.getRootVehicle() == owner.getRootVehicle()
                || direct != null && direct.getRootVehicle() == owner.getRootVehicle();
    }

    private boolean hasRequiredHelmet(Player owner) {
        ItemStack head = owner.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        return head.is(ModItems.RINNEGANHELMET.get())
                || head.is(ModItems.TENSEIGANHELMET.get())
                || head.is(ModItems.BYAKURINNESHARINGANHELMET.get());
    }

    private void setOwner(Player owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
    }

    @Nullable
    private Player getOwner() {
        int ownerId = this.entityData.get(OWNER_ID);
        if (ownerId >= 0 && this.level().getEntity(ownerId) instanceof Player player) {
            return player;
        }
        if (this.ownerUuid != null && this.level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(this.ownerUuid) instanceof Player player) {
            setOwner(player);
            return player;
        }
        return null;
    }

    private float getHealthValue() {
        return this.entityData.get(HEALTH);
    }

    private void setHealthValue(float health) {
        this.entityData.set(HEALTH, Mth.clamp(health, 0.0F, MAX_HEALTH));
    }
}
