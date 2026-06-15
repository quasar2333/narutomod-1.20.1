package net.narutomod.entity;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModItems;

public final class SusanooWingedEntity extends AbstractSusanooEntity {
    public static final float MODEL_SCALE = 8.0F;
    public static final float WIDTH = MODEL_SCALE * 0.8F;
    public static final float HEIGHT = MODEL_SCALE * 2.0F;
    private static final int DEFAULT_LIFESPAN = Integer.MAX_VALUE;
    private static final int WING_ANIMATION_TICKS = 60;
    private static final EntityDataAccessor<Float> WING_SWING = SynchedEntityData.defineId(SusanooWingedEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> SWINGING_ARMS = SynchedEntityData.defineId(SusanooWingedEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHOW_SWORD = SynchedEntityData.defineId(SusanooWingedEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> MOTION_X = SynchedEntityData.defineId(SusanooWingedEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> MOTION_Z = SynchedEntityData.defineId(SusanooWingedEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEAD_YAW = SynchedEntityData.defineId(SusanooWingedEntity.class, EntityDataSerializers.FLOAT);

    private int wingSwingTicks;
    private boolean wingExtending;
    private boolean wingDetracting;
    private int lifeSpan = DEFAULT_LIFESPAN;
    private boolean grantedKagutsuchi;
    private boolean grantedKamuiShuriken;

    public SusanooWingedEntity(EntityType<? extends SusanooWingedEntity> entityType, Level level) {
        super(entityType, level);
        this.chakraUsage = 70.0D;
        this.setMaxUpStep(HEIGHT / 3.0F);
    }

    @Nullable
    public static SusanooWingedEntity spawnFrom(Player owner) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        SusanooWingedEntity entity = ModEntityTypes.SUSANOOWINGED.get().create(serverLevel);
        if (entity == null) {
            return null;
        }
        entity.configure(owner);
        serverLevel.addFreshEntity(entity);
        owner.startRiding(entity, true);
        return entity;
    }

    @Override
    public float entityModelScale() {
        return MODEL_SCALE;
    }

    public float getWingSwingProgress() {
        return this.entityData.get(WING_SWING);
    }

    public boolean isSwingingArms() {
        return this.entityData.get(SWINGING_ARMS);
    }

    public void setSwingingArms(boolean swingingArms) {
        this.entityData.set(SWINGING_ARMS, swingingArms);
    }

    public Vec3 getSyncedMotionXZ() {
        return new Vec3(this.entityData.get(MOTION_X), 0.0D, this.entityData.get(MOTION_Z));
    }

    public float getSyncedHeadYaw() {
        return this.entityData.get(HEAD_YAW);
    }

    @Override
    public boolean shouldShowSword() {
        return this.entityData.get(SHOW_SWORD);
    }

    @Override
    public void setShowSword(boolean show) {
        this.entityData.set(SHOW_SWORD, show);
        setAttributeBaseValue(Attributes.ATTACK_DAMAGE, baseAttackDamage() * (show ? 2.2D : 1.0D));
    }

    @Override
    public double getPassengersRidingOffset() {
        return 14.0D;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed(WIDTH, HEIGHT);
    }

    @Override
    public void tick() {
        double previousX = getX();
        double previousZ = getZ();
        super.tick();
        if (this.level().isClientSide || isRemoved()) {
            return;
        }
        LivingEntity owner = getOwner();
        if (owner != null) {
            syncHeldWeapons(owner);
        }
        LivingEntity rider = getControllingPassenger();
        if (rider != null && isOwnedBy(rider)) {
            tickWingFlight(rider);
        } else if (onGround()) {
            detractWings();
        }
        updateWingSwing();
        setMotionXZ((float)(getX() - previousX), (float)(getZ() - previousZ), getYHeadRot());
        if (this.lifeSpan-- <= 0) {
            discard();
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof LivingEntity living && ownerHoldingKagutsuchi() && !isAlliedTo(living)) {
            living.addEffect(new MobEffectInstance(ModEffects.AMATERASUFLAME.get(), 200, 2, false, false));
        }
        return hurt;
    }

    @Override
    protected void doPush(Entity entity) {
        if (!this.level().isClientSide
                && entity instanceof LivingEntity living
                && living != getOwner()
                && !isAlliedTo(living)
                && ownerHoldingKagutsuchi()) {
            living.addEffect(new MobEffectInstance(ModEffects.AMATERASUFLAME.get(), 200, 2, false, false));
        }
        super.doPush(entity);
    }

    @Override
    public void remove(RemovalReason reason) {
        setSwingingArms(false);
        LivingEntity owner = getOwner();
        if (!this.level().isClientSide && owner instanceof Player player) {
            removeGrantedWeapons(player);
        }
        super.remove(reason);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(WING_SWING, 0.0F);
        this.entityData.define(SWINGING_ARMS, false);
        this.entityData.define(SHOW_SWORD, false);
        this.entityData.define(MOTION_X, 0.0F);
        this.entityData.define(MOTION_Z, 0.0F);
        this.entityData.define(HEAD_YAW, 0.0F);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(WING_SWING, tag.getFloat("WingSwing"));
        this.entityData.set(SWINGING_ARMS, tag.getBoolean("SwingingArms"));
        this.entityData.set(SHOW_SWORD, tag.getBoolean("ShowSword"));
        this.entityData.set(MOTION_X, tag.getFloat("MotionX"));
        this.entityData.set(MOTION_Z, tag.getFloat("MotionZ"));
        this.entityData.set(HEAD_YAW, tag.getFloat("HeadYaw"));
        this.wingSwingTicks = tag.getInt("WingSwingTicks");
        this.wingExtending = tag.getBoolean("WingExtending");
        this.wingDetracting = tag.getBoolean("WingDetracting");
        this.lifeSpan = tag.contains("LifeSpan") ? tag.getInt("LifeSpan") : DEFAULT_LIFESPAN;
        this.grantedKagutsuchi = tag.getBoolean("GrantedKagutsuchi");
        this.grantedKamuiShuriken = tag.getBoolean("GrantedKamuiShuriken");
        refreshDimensions();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("WingSwing", getWingSwingProgress());
        tag.putBoolean("SwingingArms", isSwingingArms());
        tag.putBoolean("ShowSword", shouldShowSword());
        tag.putFloat("MotionX", this.entityData.get(MOTION_X));
        tag.putFloat("MotionZ", this.entityData.get(MOTION_Z));
        tag.putFloat("HeadYaw", this.entityData.get(HEAD_YAW));
        tag.putInt("WingSwingTicks", this.wingSwingTicks);
        tag.putBoolean("WingExtending", this.wingExtending);
        tag.putBoolean("WingDetracting", this.wingDetracting);
        tag.putInt("LifeSpan", this.lifeSpan);
        tag.putBoolean("GrantedKagutsuchi", this.grantedKagutsuchi);
        tag.putBoolean("GrantedKamuiShuriken", this.grantedKamuiShuriken);
    }

    public void setLifeSpan(int ticks) {
        this.lifeSpan = ticks;
    }

    public boolean createMagatama(float size) {
        setSwingingArms(true);
        setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        return YasakaMagatamaEntity.spawnFrom(this, getFlameColor(), size, false);
    }

    public boolean launchMagatama(Vec3 direction, float size) {
        setSwingingArms(false);
        return YasakaMagatamaEntity.spawnFrom(this, getFlameColor(), size, true, direction);
    }

    private void configure(Player owner) {
        this.chakraUsage = 70.0D;
        configureFromOwner(owner);
        setAttributeBaseValue(Attributes.MAX_HEALTH, Math.max(getMaxHealth() * 44.0D, 100.0D));
        setAttributeBaseValue(Attributes.ATTACK_DAMAGE, baseAttackDamage());
        setAttributeBaseValue(Attributes.MOVEMENT_SPEED, getAttributeValue(Attributes.MOVEMENT_SPEED) + 0.5D);
        setHealth(getMaxHealth());
        refreshDimensions();
        grantWeapons(owner);
    }

    private double baseAttackDamage() {
        return this.ownerBattleXp * 0.005D;
    }

    private void tickWingFlight(LivingEntity rider) {
        boolean shouldFly = !onGround() || rider.getXRot() < -4.0F || rider.zza > 0.0F && rider.getXRot() < 0.0F;
        if (shouldFly) {
            extendWings();
        } else {
            detractWings();
        }
        if (rider.zza > 0.0F && (!onGround() || rider.getXRot() < 0.0F)) {
            double lift = Mth.clamp(-rider.getXRot() / 45.0D, -0.45D, 0.55D);
            Vec3 motion = getDeltaMovement();
            setDeltaMovement(motion.x(), Mth.clamp(motion.y() * 0.6D + lift * 0.08D, -0.25D, 0.25D), motion.z());
            move(MoverType.SELF, new Vec3(0.0D, getDeltaMovement().y(), 0.0D));
        }
    }

    private void extendWings() {
        this.wingDetracting = false;
        this.wingExtending = true;
    }

    private void detractWings() {
        this.wingExtending = false;
        this.wingDetracting = true;
    }

    private void updateWingSwing() {
        if (this.wingExtending) {
            this.wingSwingTicks++;
            if (this.wingSwingTicks >= WING_ANIMATION_TICKS) {
                this.wingSwingTicks = WING_ANIMATION_TICKS;
                this.wingExtending = false;
            }
        }
        if (this.wingDetracting) {
            this.wingSwingTicks--;
            if (this.wingSwingTicks <= 0) {
                this.wingSwingTicks = 0;
                this.wingDetracting = false;
            }
        }
        this.entityData.set(WING_SWING, (float)this.wingSwingTicks / (float)WING_ANIMATION_TICKS);
    }

    private void setMotionXZ(float x, float z, float headYaw) {
        this.entityData.set(MOTION_X, x);
        this.entityData.set(MOTION_Z, z);
        this.entityData.set(HEAD_YAW, headYaw);
    }

    private void syncHeldWeapons(LivingEntity owner) {
        ItemStack mainHand = owner.getMainHandItem();
        if (mainHand.is(ModItems.KAGUTSUCHISWORDRANGED.get())) {
            if (!getMainHandItem().is(ModItems.KAGUTSUCHISWORDRANGED.get())) {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.KAGUTSUCHISWORDRANGED.get()));
            }
            setShowSword(false);
        } else if (getMainHandItem().is(ModItems.KAGUTSUCHISWORDRANGED.get())) {
            setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        if (mainHand.is(ModItems.KAMUISHURIKEN.get())) {
            if (!getOffhandItem().is(ModItems.KAMUISHURIKEN.get())) {
                setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.KAMUISHURIKEN.get()));
            }
        } else if (getOffhandItem().is(ModItems.KAMUISHURIKEN.get())) {
            setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
    }

    private boolean ownerHoldingKagutsuchi() {
        LivingEntity owner = getOwner();
        return owner != null && owner.getMainHandItem().is(ModItems.KAGUTSUCHISWORDRANGED.get());
    }

    private void grantWeapons(Player player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if ((head.is(ModItems.MANGEKYOSHARINGANHELMET.get()) || head.is(ModItems.MANGEKYOSHARINGANETERNALHELMET.get()))
                && !hasItem(player, ModItems.KAGUTSUCHISWORDRANGED.get())) {
            giveOrDrop(player, new ItemStack(ModItems.KAGUTSUCHISWORDRANGED.get()));
            this.grantedKagutsuchi = true;
        }
        if ((head.is(ModItems.MANGEKYOSHARINGANOBITOHELMET.get()) || head.is(ModItems.MANGEKYOSHARINGANETERNALHELMET.get()))
                && !hasItem(player, ModItems.KAMUISHURIKEN.get())) {
            giveOrDrop(player, new ItemStack(ModItems.KAMUISHURIKEN.get()));
            this.grantedKamuiShuriken = true;
        }
    }

    private void removeGrantedWeapons(Player player) {
        if (this.grantedKagutsuchi) {
            removeOneItem(player, ModItems.KAGUTSUCHISWORDRANGED.get());
        }
        if (this.grantedKamuiShuriken) {
            removeOneItem(player, ModItems.KAMUISHURIKEN.get());
        }
        this.grantedKagutsuchi = false;
        this.grantedKamuiShuriken = false;
    }

    private static boolean hasItem(Player player, Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(item)) {
                return true;
            }
        }
        return false;
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static void removeOneItem(Player player, Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                }
                return;
            }
        }
    }
}
