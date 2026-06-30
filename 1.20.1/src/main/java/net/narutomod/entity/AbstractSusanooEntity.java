package net.narutomod.entity;

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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.Chakra;
import net.narutomod.NarutomodModVariables;
import net.narutomod.item.SusanooPowerIncreaseHandler;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModItems;
import net.narutomod.registry.ModParticleTypes;

public abstract class AbstractSusanooEntity extends PathfinderMob {
    public static final double BXP_REQUIRED_L0 = 2000.0D;
    public static final double BXP_REQUIRED_L1 = 5000.0D;
    public static final double BXP_REQUIRED_L2 = 10000.0D;
    public static final double BXP_REQUIRED_L3 = 20000.0D;
    public static final double BXP_REQUIRED_L4 = 40000.0D;
    protected static final double BASE_REACH_DISTANCE = 7.0D;
    private static final int ATTACK_COOLDOWN_TICKS = 10;
    private static final double MOTION_LIMIT = 0.05D;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(AbstractSusanooEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FLAME_COLOR = SynchedEntityData.defineId(AbstractSusanooEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;
    protected double chakraUsage = 30.0D;
    protected double chakraUsageModifier = 2.0D;
    protected double ownerBattleXp;
    private int attackCooldown;

    protected AbstractSusanooEntity(EntityType<? extends AbstractSusanooEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 5;
        this.setNoAi(true);
        this.setPersistenceRequired();
        this.setMaxUpStep(0.5F);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setDropChance(slot, 0.0F);
        }
    }

    public static AttributeSupplier.Builder createSusanooAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.1D)
                .add(ForgeMod.ENTITY_REACH.get(), BASE_REACH_DISTANCE);
    }

    public abstract float entityModelScale();

    public boolean shouldShowSword() {
        return false;
    }

    public void setShowSword(boolean show) {
    }

    public int getFlameColor() {
        return this.entityData.get(FLAME_COLOR);
    }

    public boolean isOwnedBy(Entity entity) {
        return this.ownerUuid != null && this.ownerUuid.equals(entity.getUUID())
                || this.entityData.get(OWNER_ID) == entity.getId();
    }

    @Nullable
    public LivingEntity getOwner() {
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

    protected void configureFromOwner(LivingEntity owner) {
        setOwner(owner);
        moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), 0.0F);
        setYBodyRot(owner.getYRot());
        setYHeadRot(owner.getYRot());
        if (owner instanceof Player player) {
            this.ownerBattleXp = NarutomodModVariables.getBattleExperience(player);
            setAttributeBaseValue(Attributes.MAX_HEALTH, Math.sqrt(Math.max(this.ownerBattleXp, 1.0D)));
        } else {
            this.ownerBattleXp = 4000.0D;
        }
        setAttributeBaseValue(Attributes.MOVEMENT_SPEED, 0.1D);
        setReachDistance(BASE_REACH_DISTANCE);
        copyFlameColor(owner);
        getPersistentData().putDouble("entityModelScale", entityModelScale());
        setHealth(getMaxHealth());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(FLAME_COLOR, 0x202C183D);
    }

    @Override
    public void tick() {
        this.fallDistance = 0.0F;
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        if (owner instanceof Player player) {
            if (!player.isCreative()) {
                if (!isVehicle()) {
                    if (player instanceof ServerPlayer serverPlayer) {
                        SusanooPowerIncreaseHandler.deactivate(serverPlayer, true);
                    } else {
                        discard();
                    }
                    return;
                }
                player.setShiftKeyDown(false);
                consumeChakra(owner);
            }
            if (this.tickCount % 20 == 1) {
                owner.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 22, 6, false, false));
            }
            if (player instanceof ServerPlayer serverPlayer) {
                SusanooPowerIncreaseHandler.setActiveTicks(serverPlayer, this.tickCount);
            }
        } else if (!isVehicle()) {
            discard();
            return;
        }
        syncHeldWeapons(owner);
        LivingEntity rider = getControllingPassenger();
        if (rider != null && isOwnedBy(rider)) {
            tickControlled(rider);
        } else {
            getNavigation().stop();
            setDeltaMovement(getDeltaMovement().multiply(0.65D, 1.0D, 0.65D));
        }
        clampMotion(MOTION_LIMIT);
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
        if (this.tickCount % 30 == 0) {
            this.level().playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_AMBIENT, SoundSource.NEUTRAL,
                    1.0F, getRandom().nextFloat() * 0.7F + 0.3F);
        }
        spawnFlames();
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        LivingEntity owner = getOwner();
        LivingEntity passenger = getControllingPassenger();
        LivingEntity attacker = passenger != null ? passenger : this;
        float cooledAttack = 1.0F;
        if (passenger instanceof Player player) {
            cooledAttack = player.getAttackStrengthScale(0.5F);
            player.resetAttackStrengthTicker();
        }
        ItemStack held = attacker.getMainHandItem();
        float damage = (float)(getAttributeValue(Attributes.ATTACK_DAMAGE) * cooledAttack);
        if (held.is(ModItems.TOTSUKA_SWORD.get())
                && target instanceof LivingEntity living
                && Chakra.pathway(living).getAmount() < 5.0D) {
            damage *= 2.0F + getRandom().nextFloat();
        }
        target.invulnerableTime = 0;
        boolean hurt = target.hurt(ModDamageTypes.ninjutsu(this.level(), this, owner), damage);
        if (hurt && target instanceof LivingEntity living) {
            living.knockback(Math.max(0.4D, cooledAttack * 2.5D),
                    Mth.sin(getYRot() * Mth.DEG_TO_RAD), -Mth.cos(getYRot() * Mth.DEG_TO_RAD));
            setDeltaMovement(getDeltaMovement().multiply(0.6D, 1.0D, 0.6D));
        }
        return hurt;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && isOwnedBy(player)) {
            player.startRiding(this, true);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && passenger instanceof LivingEntity living && isOwnedBy(living);
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
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (!hasPassenger(passenger)) {
            return;
        }
        moveFunction.accept(passenger, getX(), getY() + getPassengersRidingOffset() + passenger.getMyRidingOffset(), getZ());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        if (attacker != null && attacker == getControllingPassenger()) {
            return false;
        }
        if (direct != null && direct == getControllingPassenger()) {
            return false;
        }
        if (source.is(DamageTypes.ARROW)
                || source.is(DamageTypes.CACTUS)
                || source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.FALL)
                || source.is(DamageTypes.INDIRECT_MAGIC)
                || source.is(DamageTypes.MAGIC)
                || source.is(DamageTypes.WITHER)
                || source.is(ModDamageTypes.AMATERASU)
                || source.is(ModDamageTypes.NINJUTSU_FIRE)) {
            return false;
        }
        float previousHealth = getHealth();
        boolean hurt = super.hurt(source, amount);
        LivingEntity owner = getOwner();
        if (hurt && !isAlive() && owner != null) {
            float overflow = amount - previousHealth;
            if (overflow > 0.0F) {
                owner.hurt(source, overflow);
            }
        }
        return hurt;
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        return entity == getOwner()
                || entity instanceof AbstractSusanooEntity susanoo
                        && susanoo.ownerUuid != null
                        && susanoo.ownerUuid.equals(this.ownerUuid)
                || super.isAlliedTo(entity);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        this.chakraUsage = tag.contains("ChakraUsage") ? tag.getDouble("ChakraUsage") : this.chakraUsage;
        this.chakraUsageModifier = tag.contains("ChakraUsageModifier") ? tag.getDouble("ChakraUsageModifier") : this.chakraUsageModifier;
        this.ownerBattleXp = tag.getDouble("OwnerBattleXp");
        this.entityData.set(FLAME_COLOR, tag.contains("FlameColor") ? tag.getInt("FlameColor") : getFlameColor());
        getPersistentData().putDouble("entityModelScale", entityModelScale());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putDouble("ChakraUsage", this.chakraUsage);
        tag.putDouble("ChakraUsageModifier", this.chakraUsageModifier);
        tag.putDouble("OwnerBattleXp", this.ownerBattleXp);
        tag.putInt("FlameColor", getFlameColor());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void tickControlled(LivingEntity rider) {
        setYRot(rider.getYRot());
        setXRot(rider.getXRot());
        setYBodyRot(rider.getYRot());
        setYHeadRot(rider.getYHeadRot());
        yRotO = getYRot();
        xRotO = getXRot();
        rider.fallDistance = 0.0F;
        setMaxUpStep(Math.max(getBbHeight() / 3.0F, 0.6F));
        getNavigation().stop();
        Vec3 motion = getDeltaMovement();
        Vec3 acceleration = controlAcceleration(rider);
        setDeltaMovement(motion.x() * 0.65D + acceleration.x(), motion.y(), motion.z() * 0.65D + acceleration.z());
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().multiply(0.85D, 1.0D, 0.85D));
        attackLookTarget(rider);
    }

    private Vec3 controlAcceleration(LivingEntity rider) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, rider.getYRot());
        forward = new Vec3(forward.x(), 0.0D, forward.z());
        if (forward.lengthSqr() < 1.0E-8D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        forward = forward.normalize();
        Vec3 right = new Vec3(-forward.z(), 0.0D, forward.x());
        double acceleration = Math.max(getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.65D, 0.035D);
        return forward.scale(rider.zza * acceleration).add(right.scale(rider.xxa * acceleration));
    }

    private void attackLookTarget(LivingEntity rider) {
        if (!rider.swinging || this.attackCooldown > 0) {
            return;
        }
        HitResult hit = ProcedureUtils.objectEntityLookingAt(rider, getAttackReach(), 1.0D, true, false,
                target -> target instanceof LivingEntity living && living.isAlive() && target != rider && target != this && !isAlliedTo(target));
        if (hit instanceof EntityHitResult entityHit) {
            this.attackCooldown = ATTACK_COOLDOWN_TICKS;
            doHurtTarget(entityHit.getEntity());
        }
    }

    private double getAttackReach() {
        AttributeInstance reach = getAttribute(ForgeMod.ENTITY_REACH.get());
        return reach != null ? Math.max(0.0D, reach.getValue())
                : Math.max(4.0D, Math.sqrt(getBbWidth() * getBbWidth() + getBbHeight() * getBbHeight()));
    }

    private void consumeChakra(LivingEntity owner) {
        if (this.tickCount % 20 == 0 && !Chakra.pathway(owner).consume(this.chakraUsage * this.chakraUsageModifier)) {
            discard();
        }
    }

    private void spawnFlames() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int count = Mth.clamp((int)getBbHeight(), 2, 16);
        int color = getFlameColor();
        for (int i = 0; i < count; i++) {
            double x = getX() + (getRandom().nextDouble() - 0.5D) * getBbWidth();
            double y = getY() + getRandom().nextDouble() * getBbHeight();
            double z = getZ() + (getRandom().nextDouble() - 0.5D) * getBbWidth();
            serverLevel.sendParticles(
                    ModParticleTypes.options(NarutoParticleKind.FLAME_COLORED, color, Math.max((int)(getBbWidth() * 15.0F), 8)),
                    x,
                    y,
                    z,
                    1,
                    0.0D,
                    0.05D,
                    0.0D,
                    0.0D);
        }
        if (this.tickCount % 20 == 0) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY() + getBbHeight() * 0.55D, getZ(),
                    12, getBbWidth() * 0.3D, getBbHeight() * 0.2D, getBbWidth() * 0.3D, 0.01D);
        }
    }

    private void copyFlameColor(LivingEntity owner) {
        ItemStack head = owner.getItemBySlot(EquipmentSlot.HEAD);
        if (SusanooPowerIncreaseHandler.isSharinganHead(head)) {
            this.chakraUsageModifier = 1.0D;
        }
        if (head.is(ModItems.MANGEKYOSHARINGANHELMET.get()) || head.is(ModItems.MANGEKYOSHARINGANETERNALHELMET.get())) {
            this.entityData.set(FLAME_COLOR, 0x20B83DBA);
        }
    }

    private void clampMotion(double limit) {
        Vec3 motion = getDeltaMovement();
        setDeltaMovement(
                Mth.clamp(motion.x(), -limit, limit),
                Mth.clamp(motion.y(), -limit, limit),
                Mth.clamp(motion.z(), -limit, limit));
    }

    private void setOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
    }

    protected void syncHeldWeapons(LivingEntity owner) {
        setShowSword(owner.getMainHandItem().is(ModItems.CHOKUTO.get()));
    }

    protected void setReachDistance(double reach) {
        setAttributeBaseValue(ForgeMod.ENTITY_REACH.get(), reach);
    }

    protected double getReachDistance() {
        AttributeInstance instance = getAttribute(ForgeMod.ENTITY_REACH.get());
        return instance != null ? instance.getBaseValue() : BASE_REACH_DISTANCE;
    }

    protected void setAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }
}
