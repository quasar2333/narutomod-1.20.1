package net.narutomod.entity;

import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.event.SpecialEvent;
import net.narutomod.procedure.ProcedureBasicNinjaSkills;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModItems;
import net.narutomod.registry.ModSounds;

public class NinjaMobEntity extends Monster {
    private static final EntityDataAccessor<ItemStack> LEGACY_ZABUZA_STORED_MAIN_HAND =
            SynchedEntityData.defineId(NinjaMobEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> LEGACY_KISAME_STORED_MAIN_HAND =
            SynchedEntityData.defineId(NinjaMobEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> LEGACY_KISAME_FUSED =
            SynchedEntityData.defineId(NinjaMobEntity.class, EntityDataSerializers.BOOLEAN);
    private static final UUID LEGACY_KISAME_FUSED_HEALTH_MODIFIER_ID =
            UUID.fromString("c10f46b4-6a5a-4a67-a507-82a5da8c8d24");
    private static final AttributeModifier LEGACY_KISAME_FUSED_HEALTH_MODIFIER = new AttributeModifier(
            LEGACY_KISAME_FUSED_HEALTH_MODIFIER_ID,
            "kisame.fused.health",
            100.0D,
            AttributeModifier.Operation.ADDITION);
    private static final double LEGACY_ZABUZA_INITIAL_CHAKRA = 5000.0D;
    private static final double LEGACY_ZABUZA_MIST_CHAKRA = 100.0D;
    private static final double LEGACY_ZABUZA_WATER_DRAGON_CHAKRA = 200.0D;
    private static final int LEGACY_ZABUZA_MIST_COOLDOWN_TICKS = 1200;
    private static final int LEGACY_ZABUZA_WATER_DRAGON_COOLDOWN_TICKS = 300;
    private static final int LEGACY_ZABUZA_WATER_DRAGON_STAND_STILL_TICKS = 60;
    private static final double LEGACY_ZABUZA_WATER_DRAGON_RANGE = 12.0D;
    private static final double LEGACY_ZABUZA_WATER_DRAGON_RANGE_SQR = 144.0D;
    private static final double LEGACY_ZABUZA_WATER_DRAGON_RANGE_TOLERANCE = 2.0D;
    private static final double LEGACY_ZABUZA_WATER_DRAGON_APPROACH_SPEED = 1.0D;
    private static final float LEGACY_ZABUZA_WATER_DRAGON_STRAFE_SPEED = 1.25F;
    private static final int LEGACY_ZABUZA_WATER_DRAGON_SETUP_GRACE_TICKS = 100;
    private static final int LEGACY_ZABUZA_CALL_HELP_COOLDOWN_TICKS = 40;
    private static final float LEGACY_ZABUZA_HELP_HEALTH_THRESHOLD = 0.4F;
    private static final float LEGACY_ZABUZA_REENGAGE_HEALTH_THRESHOLD = 0.6F;
    private static final float LEGACY_ZABUZA_AVOID_DISTANCE = 10.0F;
    private static final double LEGACY_ZABUZA_AVOID_SPEED = 1.25D;
    private static final double LEGACY_ZABUZA_HELP_VERTICAL_RANGE = 8.0D;
    private static final int LEGACY_ZABUZA_WATER_CLONE_COOLDOWN_TICKS = 100;
    private static final int LEGACY_ZABUZA_WATER_CLONE_INITIAL_OFFSET_TICKS = 40;
    private static final double LEGACY_ZABUZA_WATER_CLONE_CHAKRA = 500.0D;
    private static final double LEGACY_ZABUZA_WATER_CLONE_MAX_HEALTH = 10.0D;
    private static final double LEGACY_ZABUZA_WATER_CLONE_ATTACK_DAMAGE = 1.0D;
    private static final int LEGACY_ZABUZA_WATER_PRISON_COOLDOWN_TICKS = 400;
    private static final int LEGACY_ZABUZA_WATER_PRISON_INITIAL_OFFSET_TICKS = 40;
    private static final int LEGACY_ZABUZA_WATER_PRISON_DURATION_TICKS = 300;
    private static final double LEGACY_ZABUZA_WATER_PRISON_CHAKRA = 200.0D;
    private static final double LEGACY_ZABUZA_WATER_PRISON_MIN_DISTANCE = 2.0D;
    private static final int LEGACY_ZABUZA_CLONE_DEATH_WATER_TICKS = 10;
    private static final int LEGACY_ZABUZA_NINJA_LEVEL = 80;
    private static final int LEGACY_KISAME_NINJA_LEVEL = 100;
    private static final double LEGACY_ZABUZA_MAX_HEALTH = 82.0D;
    private static final double LEGACY_ZABUZA_ARMOR = 100.0D;
    private static final double LEGACY_ZABUZA_MOVEMENT_SPEED = 0.5D;
    private static final double LEGACY_ZABUZA_ATTACK_DAMAGE = 10.0D;
    private static final double LEGACY_ZABUZA_FOLLOW_RANGE = 48.0D;
    private static final double LEGACY_KISAME_MAX_HEALTH = 100.0D;
    private static final double LEGACY_KISAME_ARMOR = 100.0D;
    private static final double LEGACY_KISAME_MOVEMENT_SPEED = 0.5D;
    private static final double LEGACY_KISAME_ATTACK_DAMAGE = 10.0D;
    private static final double LEGACY_KISAME_FOLLOW_RANGE = 48.0D;
    private static final double LEGACY_KISAME_INITIAL_CHAKRA = 10000.0D;
    private static final double LEGACY_KISAME_WATER_SHARK_CHAKRA = 75.0D;
    private static final int LEGACY_KISAME_WATER_SHARK_COOLDOWN_TICKS = 80;
    private static final int LEGACY_KISAME_WATER_SHARK_STAND_STILL_TICKS = 80;
    private static final double LEGACY_KISAME_WATER_SHARK_RANGE = 15.0D;
    private static final double LEGACY_KISAME_WATER_SHARK_RANGE_TOLERANCE = 2.0D;
    private static final double LEGACY_KISAME_WATER_SHARK_APPROACH_SPEED = 1.0D;
    private static final float LEGACY_KISAME_WATER_SHARK_STRAFE_SPEED = 1.25F;
    private static final int LEGACY_KISAME_WATER_SHARK_SETUP_GRACE_TICKS = 100;
    private static final int LEGACY_KISAME_WATER_CLONE_COOLDOWN_TICKS = 100;
    private static final double LEGACY_KISAME_WATER_CLONE_CHAKRA = 500.0D;
    private static final int LEGACY_KISAME_WATER_CLONE_MAX_COUNT = 2;
    private static final double LEGACY_KISAME_WATER_CLONE_MAX_HEALTH = 10.0D;
    private static final double LEGACY_KISAME_WATER_CLONE_ATTACK_DAMAGE = 1.0D;
    private static final int LEGACY_KISAME_CLONE_DEATH_WATER_TICKS = 10;
    private static final int LEGACY_KISAME_WATER_PRISON_COOLDOWN_TICKS = 600;
    private static final int LEGACY_KISAME_WATER_PRISON_DURATION_TICKS = 300;
    private static final double LEGACY_KISAME_WATER_PRISON_CHAKRA = 200.0D;
    private static final double LEGACY_KISAME_WATER_SHOCKWAVE_CHAKRA = 600.0D;
    private static final float LEGACY_KISAME_WATER_DOME_RADIUS = 20.0F;
    private static final int LEGACY_KISAME_FUSED_EXIT_PEACEFUL_TICKS = 200;
    private static final double LEGACY_KISAME_MELEE_SPEED = 1.2D;
    private static final double LEGACY_KISAME_MELEE_REACH_BASE = 5.3D;
    private static final double LEGACY_KISAME_LEAP_STRENGTH = 1.0D;
    private static final double LEGACY_KISAME_LEAP_MIN_DISTANCE = 3.0D;
    private static final double LEGACY_KISAME_LEAP_MAX_DISTANCE = 12.0D;
    private static final double LEGACY_ZABUZA_MELEE_SPEED = 1.5D;
    private static final double LEGACY_ZABUZA_MELEE_REACH_BASE = 5.3D;
    private static final double LEGACY_ZABUZA_LEAP_STRENGTH = 1.0D;
    private static final double LEGACY_ZABUZA_LEAP_MIN_DISTANCE = 3.0D;
    private static final double LEGACY_ZABUZA_LEAP_MAX_DISTANCE = 12.0D;
    private static final int LEGACY_ZABUZA_BLOCKING_COOLDOWN_TICKS = 20;
    private static final int LEGACY_KISAME_BLOCKING_COOLDOWN_TICKS = 30;
    private static final int LEGACY_KISAME_BLOCKING_DURATION_TICKS = 20;

    private final ServerBossEvent legacyZabuzaBossEvent = new ServerBossEvent(
            getDisplayName(),
            BossEvent.BossBarColor.BLUE,
            BossEvent.BossBarOverlay.PROGRESS);
    private final ServerBossEvent legacyKisameBossEvent = new ServerBossEvent(
            getDisplayName(),
            BossEvent.BossBarColor.BLUE,
            BossEvent.BossBarOverlay.PROGRESS);
    @Nullable
    private HakuEntity legacyHakuPartner;
    @Nullable
    private LivingEntity legacyZabuzaAvoidTarget;
    private ItemStack legacyZabuzaStoredMainHand = ItemStack.EMPTY;
    private ItemStack legacyKisameStoredMainHand = ItemStack.EMPTY;
    @Nullable
    private UUID legacyZabuzaOriginalUuid;
    @Nullable
    private NinjaMobEntity legacyZabuzaOriginal;
    @Nullable
    private UUID legacyKisameOriginalUuid;
    @Nullable
    private NinjaMobEntity legacyKisameOriginal;
    private double legacyZabuzaChakra = LEGACY_ZABUZA_INITIAL_CHAKRA;
    private double legacyKisameChakra = LEGACY_KISAME_INITIAL_CHAKRA;
    private int legacyZabuzaMistLastUsed = -LEGACY_ZABUZA_MIST_COOLDOWN_TICKS;
    private int legacyZabuzaWaterDragonLastUsed;
    private int legacyZabuzaCloneLastUsed =
            -LEGACY_ZABUZA_WATER_CLONE_COOLDOWN_TICKS + LEGACY_ZABUZA_WATER_CLONE_INITIAL_OFFSET_TICKS;
    private int legacyKisameCloneLastUsed = -LEGACY_KISAME_WATER_CLONE_COOLDOWN_TICKS;
    private int legacyZabuzaWaterPrisonLastUsed =
            -LEGACY_ZABUZA_WATER_PRISON_COOLDOWN_TICKS + LEGACY_ZABUZA_WATER_PRISON_INITIAL_OFFSET_TICKS;
    private int legacyKisameWaterPrisonLastUsed = -LEGACY_KISAME_WATER_PRISON_COOLDOWN_TICKS;
    private int legacyStandStillTicks;
    private int legacyZabuzaPeacefulTicks;
    private int legacyKisamePeacefulTicks;
    private int legacyZabuzaLastCallForHelp;
    private int legacyZabuzaLastBlockTime;
    private int legacyKisameLastBlockTime;
    private int legacyZabuzaCloneCount;
    private int legacyKisameCloneCount;
    private boolean legacyZabuzaCloneCounted;
    private boolean legacyKisameCloneCounted;
    private boolean legacyZabuzaCloneCleanedUp;
    private boolean legacyKisameCloneCleanedUp;
    private boolean legacyZabuzaCloneWasTrapping;
    private boolean legacyKisameCloneWasTrapping;

    public NinjaMobEntity(EntityType<? extends NinjaMobEntity> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.setMaxUpStep(8.0F);
        this.setCustomName(entityType.getDescription());
        this.setCustomNameVisible(true);
        this.xpReward = entityType == ModEntityTypes.WHITEZETSU.get()
                ? 25
                : entityType == ModEntityTypes.ZABUZA_MOMOCHI.get()
                        ? LEGACY_ZABUZA_NINJA_LEVEL
                        : entityType == ModEntityTypes.KISAME_HOSHIGAKI.get() ? LEGACY_KISAME_NINJA_LEVEL : 0;
        equipLegacyNamedNpcLoadout(entityType);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAttributes(10.0D, 0.0D, 0.3D, 3.0D, 48.0D);
    }

    public static AttributeSupplier.Builder createAttributes(double maxHealth, double armor, double movementSpeed, double attackDamage, double followRange) {
        return Mob.createMobAttributes()
                .add(Attributes.ARMOR, armor)
                .add(Attributes.ATTACK_DAMAGE, attackDamage)
                .add(Attributes.FOLLOW_RANGE, followRange)
                .add(Attributes.MAX_HEALTH, maxHealth)
                .add(Attributes.MOVEMENT_SPEED, movementSpeed);
    }

    public static AttributeSupplier.Builder createLegacyZabuzaAttributes() {
        return createAttributes(
                LEGACY_ZABUZA_MAX_HEALTH,
                LEGACY_ZABUZA_ARMOR,
                LEGACY_ZABUZA_MOVEMENT_SPEED,
                LEGACY_ZABUZA_ATTACK_DAMAGE,
                LEGACY_ZABUZA_FOLLOW_RANGE);
    }

    public static AttributeSupplier.Builder createLegacyKisameAttributes() {
        return createAttributes(
                LEGACY_KISAME_MAX_HEALTH,
                LEGACY_KISAME_ARMOR,
                LEGACY_KISAME_MOVEMENT_SPEED,
                LEGACY_KISAME_ATTACK_DAMAGE,
                LEGACY_KISAME_FOLLOW_RANGE);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(LEGACY_ZABUZA_STORED_MAIN_HAND, ItemStack.EMPTY);
        this.entityData.define(LEGACY_KISAME_STORED_MAIN_HAND, ItemStack.EMPTY);
        this.entityData.define(LEGACY_KISAME_FUSED, false);
    }

    public ItemStack getLegacyZabuzaStoredMainHandForRender() {
        return this.entityData.get(LEGACY_ZABUZA_STORED_MAIN_HAND);
    }

    public ItemStack getLegacyKisameStoredMainHandForRender() {
        return this.entityData.get(LEGACY_KISAME_STORED_MAIN_HAND);
    }

    public boolean isLegacyKisameFusedForRender() {
        return this.entityData.get(LEGACY_KISAME_FUSED);
    }

    private void equipLegacyNamedNpcLoadout(EntityType<? extends NinjaMobEntity> entityType) {
        if (entityType == ModEntityTypes.ZABUZA_MOMOCHI.get()) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.ZABUZA_SWORD.get()));
            this.setDropChance(EquipmentSlot.MAINHAND, 1.0F);
        } else if (entityType == ModEntityTypes.KISAME_HOSHIGAKI.get()) {
            setLegacyKisameStoredMainHand(new ItemStack(ModItems.SAMEHADA.get()));
            this.setDropChance(EquipmentSlot.MAINHAND, 1.0F);
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(
                this,
                LivingEntity.class,
                living -> living == NinjaMobEntity.this.legacyZabuzaAvoidTarget,
                LEGACY_ZABUZA_AVOID_DISTANCE,
                LEGACY_ZABUZA_AVOID_SPEED,
                LEGACY_ZABUZA_AVOID_SPEED,
                living -> true) {
            @Override
            public boolean canUse() {
                return NinjaMobEntity.this.hasLegacyZabuzaAvoidTarget() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return NinjaMobEntity.this.hasLegacyZabuzaAvoidTarget() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(1, new LegacyZabuzaWaterDragonGoal());
        this.goalSelector.addGoal(1, new LegacyKisameWaterSharkGoal());
        this.goalSelector.addGoal(2, new LegacyZabuzaLeapGoal());
        this.goalSelector.addGoal(2, new LegacyKisameLeapGoal());
        this.goalSelector.addGoal(3, new LegacyZabuzaMeleeAttackGoal());
        this.goalSelector.addGoal(3, new LegacyKisameMeleeAttackGoal());
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2D, false) {
            @Override
            public boolean canUse() {
                return !NinjaMobEntity.this.hasLegacyDedicatedMeleeGoal()
                        && !NinjaMobEntity.this.usesLegacyMerchantBaseGoals()
                        && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !NinjaMobEntity.this.hasLegacyDedicatedMeleeGoal()
                        && !NinjaMobEntity.this.usesLegacyMerchantBaseGoals()
                        && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D) {
            @Override
            public boolean canUse() {
                return !NinjaMobEntity.this.usesLegacyMerchantBaseGoals() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !NinjaMobEntity.this.usesLegacyMerchantBaseGoals() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return !NinjaMobEntity.this.usesLegacyMerchantBaseGoals() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !NinjaMobEntity.this.usesLegacyMerchantBaseGoals() && super.canContinueToUse();
            }
        });
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true, false) {
            @Override
            public boolean canUse() {
                return NinjaMobEntity.this.canUseLegacyNamedNpcPlayerTargetGoal() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return NinjaMobEntity.this.canUseLegacyNamedNpcPlayerTargetGoal() && super.canContinueToUse();
            }
        });
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return !NinjaMobEntity.this.shouldSuppressLegacyZabuzaTargetGoals() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !NinjaMobEntity.this.shouldSuppressLegacyZabuzaTargetGoals() && super.canContinueToUse();
            }
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            if (this.tickCount % 200 == 1) {
                this.addEffect(new MobEffectInstance(ModEffects.FEATHER_FALLING.get(), 201, 1, false, false));
            }
            if (tickLegacyZabuzaCloneLifecycle()) {
                return;
            }
            if (tickLegacyKisameCloneLifecycle()) {
                return;
            }
            if (tickLegacyZabuzaCloneWaterPrisonHold()) {
                return;
            }
            if (tickLegacyKisameCloneWaterPrisonHold()) {
                return;
            }
            tickLegacyZabuzaBlocking();
            tickLegacyKisameBlocking();
            syncLegacyTargetDrivenEquipment();
            syncLegacyZabuzaHakuPartner();
            tickLegacyZabuzaLowHealthSupport();
            tickLegacyZabuzaMist();
            tickLegacyKisameFusedForm();
            tickLegacyZabuzaWaterPrison();
            tickLegacyKisameWaterPrison();
            tickLegacyZabuzaWaterClone();
            tickLegacyKisameWaterClone();
            tickLegacyZabuzaChakraRecovery();
            tickLegacyKisameChakraRecovery();
            tickLegacyStandStill();
            trackLegacyZabuzaBossPlayers();
            trackLegacyKisameBossPlayers();
            ProcedureBasicNinjaSkills.apply(this);
        }
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target);
        syncLegacyTargetDrivenEquipment();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        startLegacyZabuzaBlock();
        startLegacyKisameBlock(source);
        return super.hurt(source, amount);
    }

    @Override
    protected void blockUsingShield(LivingEntity attacker) {
        super.blockUsingShield(attacker);
        if (getType() == ModEntityTypes.KISAME_HOSHIGAKI.get()
                && !isLegacyKisameClone()
                && attacker.getType() != ModEntityTypes.KISAME_HOSHIGAKI.get()) {
            attacker.hurt(damageSources().thorns(this), 2.0F + getRandom().nextFloat() * 8.0F);
        }
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEFINED;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return player.isCreative();
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        return isLegacyNamedNpcAlly(entity) || super.isAlliedTo(entity);
    }

    private boolean isLegacyNamedNpcAlly(Entity entity) {
        EntityType<?> self = getType();
        EntityType<?> other = entity.getType();
        return isLegacyZabuzaTeam(self) && isLegacyZabuzaTeam(other)
                || isLegacyItachiTeam(self) && isLegacyItachiTeam(other);
    }

    private static boolean isLegacyZabuzaTeam(EntityType<?> entityType) {
        return entityType == ModEntityTypes.ZABUZA_MOMOCHI.get()
                || entityType == ModEntityTypes.HAKU.get();
    }

    private boolean hasLegacyZabuzaAvoidTarget() {
        return getType() == ModEntityTypes.ZABUZA_MOMOCHI.get()
                && this.legacyZabuzaAvoidTarget != null
                && this.legacyZabuzaAvoidTarget.isAlive();
    }

    private boolean isLegacyZabuzaClone() {
        return getType() == ModEntityTypes.ZABUZA_MOMOCHI.get()
                && (this.legacyZabuzaOriginal != null || this.legacyZabuzaOriginalUuid != null);
    }

    private boolean isLegacyKisameClone() {
        return getType() == ModEntityTypes.KISAME_HOSHIGAKI.get()
                && (this.legacyKisameOriginal != null || this.legacyKisameOriginalUuid != null);
    }

    private boolean isLegacyKisameFused() {
        return getType() == ModEntityTypes.KISAME_HOSHIGAKI.get() && isLegacyKisameFusedForRender();
    }

    private void setLegacyKisameFused(boolean fused) {
        if (getType() != ModEntityTypes.KISAME_HOSHIGAKI.get()) {
            return;
        }
        this.entityData.set(LEGACY_KISAME_FUSED, fused);
        AttributeInstance maxHealth = getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        if (fused) {
            if (maxHealth.getModifier(LEGACY_KISAME_FUSED_HEALTH_MODIFIER_ID) == null) {
                maxHealth.addTransientModifier(LEGACY_KISAME_FUSED_HEALTH_MODIFIER);
            }
        } else {
            maxHealth.removeModifier(LEGACY_KISAME_FUSED_HEALTH_MODIFIER_ID);
            if (getHealth() > getMaxHealth()) {
                setHealth(getMaxHealth());
            }
        }
    }

    private boolean shouldSuppressLegacyZabuzaTargetGoals() {
        return hasLegacyZabuzaAvoidTarget();
    }

    private boolean canUseLegacyNamedNpcPlayerTargetGoal() {
        EntityType<?> self = getType();
        return self == ModEntityTypes.KISAME_HOSHIGAKI.get()
                || self == ModEntityTypes.ZABUZA_MOMOCHI.get() && !shouldSuppressLegacyZabuzaTargetGoals();
    }

    private boolean hasLegacyDedicatedMeleeGoal() {
        EntityType<?> self = getType();
        return self == ModEntityTypes.ZABUZA_MOMOCHI.get() || self == ModEntityTypes.KISAME_HOSHIGAKI.get();
    }

    protected boolean usesLegacyMerchantBaseGoals() {
        return false;
    }

    private static boolean isLegacyItachiTeam(EntityType<?> entityType) {
        return entityType == ModEntityTypes.ITACHI.get()
                || entityType == ModEntityTypes.KISAME_HOSHIGAKI.get();
    }

    private void syncLegacyTargetDrivenEquipment() {
        if (getType() == ModEntityTypes.HAKU.get()) {
            syncLegacyHakuMainHand();
        } else if (getType() == ModEntityTypes.ZABUZA_MOMOCHI.get()) {
            syncLegacyZabuzaMainHand();
        } else if (getType() == ModEntityTypes.KISAME_HOSHIGAKI.get()) {
            syncLegacyKisameMainHand();
        }
    }

    private void startLegacyZabuzaBlock() {
        if (getType() != ModEntityTypes.ZABUZA_MOMOCHI.get()
                || isNoAi()
                || getMainHandItem().isEmpty()
                || isUsingItem()) {
            return;
        }
        startUsingItem(InteractionHand.MAIN_HAND);
        this.legacyZabuzaLastBlockTime = this.tickCount;
    }

    private void tickLegacyZabuzaBlocking() {
        if (getType() != ModEntityTypes.ZABUZA_MOMOCHI.get() || !isUsingItem()) {
            return;
        }
        if (this.tickCount > this.legacyZabuzaLastBlockTime + LEGACY_ZABUZA_BLOCKING_COOLDOWN_TICKS) {
            stopUsingItem();
        }
    }

    private void startLegacyKisameBlock(DamageSource source) {
        if (getType() != ModEntityTypes.KISAME_HOSHIGAKI.get()
                || isNoAi()
                || source.getEntity() == null
                || source.is(DamageTypes.THORNS)
                || getMainHandItem().isEmpty()
                || isUsingItem()
                || this.tickCount <= this.legacyKisameLastBlockTime + LEGACY_KISAME_BLOCKING_COOLDOWN_TICKS) {
            return;
        }
        startUsingItem(InteractionHand.MAIN_HAND);
        this.legacyKisameLastBlockTime = this.tickCount;
    }

    private void tickLegacyKisameBlocking() {
        if (getType() != ModEntityTypes.KISAME_HOSHIGAKI.get() || !isUsingItem()) {
            return;
        }
        if (this.tickCount > this.legacyKisameLastBlockTime + LEGACY_KISAME_BLOCKING_DURATION_TICKS) {
            stopUsingItem();
        }
    }

    private void syncLegacyZabuzaMainHand() {
        if (isUsingItem()) {
            return;
        }
        ItemStack mainHand = getItemBySlot(EquipmentSlot.MAINHAND);
        if (!ProcedureUtils.isWeapon(mainHand) && !ProcedureUtils.isWeapon(this.legacyZabuzaStoredMainHand)) {
            return;
        }
        boolean shouldHoldWeapon = shouldLegacyNamedNpcHoldWeapon();
        if (shouldHoldWeapon && mainHand.isEmpty() && !this.legacyZabuzaStoredMainHand.isEmpty()) {
            swapLegacyZabuzaMainHandWithStorage();
        } else if (!shouldHoldWeapon && !mainHand.isEmpty()) {
            swapLegacyZabuzaMainHandWithStorage();
        }
    }

    private void syncLegacyKisameMainHand() {
        if (isUsingItem()) {
            return;
        }
        ItemStack mainHand = getItemBySlot(EquipmentSlot.MAINHAND);
        if (!ProcedureUtils.isWeapon(mainHand) && !ProcedureUtils.isWeapon(this.legacyKisameStoredMainHand)) {
            return;
        }
        boolean shouldHoldWeapon = shouldLegacyNamedNpcHoldWeapon();
        if (shouldHoldWeapon && mainHand.isEmpty() && !this.legacyKisameStoredMainHand.isEmpty()) {
            swapLegacyKisameMainHandWithStorage();
        } else if (!shouldHoldWeapon && !mainHand.isEmpty()) {
            swapLegacyKisameMainHandWithStorage();
        }
    }

    private boolean shouldLegacyNamedNpcHoldWeapon() {
        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) {
            return true;
        }
        LivingEntity attacker = getLastHurtByMob();
        if (attacker != null && attacker.isAlive()) {
            return true;
        }
        LivingEntity lastHurt = getLastHurtMob();
        return lastHurt != null && lastHurt.isAlive() && this.tickCount <= getLastHurtMobTimestamp() + 100;
    }

    private void swapLegacyZabuzaMainHandWithStorage() {
        ItemStack previousMainHand = getItemBySlot(EquipmentSlot.MAINHAND);
        setItemSlot(EquipmentSlot.MAINHAND, this.legacyZabuzaStoredMainHand);
        setLegacyZabuzaStoredMainHand(previousMainHand);
    }

    private void hideLegacyZabuzaMainHand() {
        if (!getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
            swapLegacyZabuzaMainHandWithStorage();
        }
    }

    private void setLegacyZabuzaStoredMainHand(ItemStack stack) {
        this.legacyZabuzaStoredMainHand = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        this.entityData.set(LEGACY_ZABUZA_STORED_MAIN_HAND, this.legacyZabuzaStoredMainHand.copy());
    }

    private void swapLegacyKisameMainHandWithStorage() {
        ItemStack previousMainHand = getItemBySlot(EquipmentSlot.MAINHAND);
        setItemSlot(EquipmentSlot.MAINHAND, this.legacyKisameStoredMainHand);
        setLegacyKisameStoredMainHand(previousMainHand);
    }

    private void hideLegacyKisameMainHand() {
        if (!getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
            swapLegacyKisameMainHandWithStorage();
        }
    }

    private void setLegacyKisameStoredMainHand(ItemStack stack) {
        this.legacyKisameStoredMainHand = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        this.entityData.set(LEGACY_KISAME_STORED_MAIN_HAND, this.legacyKisameStoredMainHand.copy());
    }

    private void syncLegacyHakuMainHand() {
        LivingEntity target = getTarget();
        boolean hasCombatTarget = target != null && target.isAlive();
        ItemStack current = getItemBySlot(EquipmentSlot.MAINHAND);
        if (hasCombatTarget) {
            if (!current.is(ModItems.ICE_SENBON.get())) {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.ICE_SENBON.get()));
            }
        } else if (!current.isEmpty()) {
            setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }

    private void syncLegacyZabuzaHakuPartner() {
        if (getType() != ModEntityTypes.ZABUZA_MOMOCHI.get()
                || isLegacyZabuzaClone()
                || this.tickCount <= 20
                || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (this.legacyHakuPartner != null) {
            if (this.legacyHakuPartner.isAlive()) {
                this.legacyHakuPartner.setLeader(this);
            }
            return;
        }
        this.legacyHakuPartner = serverLevel.getEntitiesOfClass(
                        HakuEntity.class,
                        this.getBoundingBox().inflate(128.0D, 32.0D, 128.0D),
                        HakuEntity::isAlive)
                .stream()
                .min((left, right) -> Double.compare(left.distanceToSqr(this), right.distanceToSqr(this)))
                .orElse(null);
        if (this.legacyHakuPartner == null) {
            this.legacyHakuPartner = ModEntityTypes.HAKU.get().create(serverLevel);
            if (this.legacyHakuPartner == null) {
                return;
            }
            this.legacyHakuPartner.moveTo(
                    getX() + (this.random.nextBoolean() ? 3.0D : -3.0D),
                    getY(),
                    getZ() + (this.random.nextBoolean() ? 3.0D : -3.0D),
                    getYRot(),
                    0.0F);
            this.legacyHakuPartner.setLeader(this);
            serverLevel.addFreshEntity(this.legacyHakuPartner);
        } else {
            this.legacyHakuPartner.setLeader(this);
        }
    }

    private void tickLegacyZabuzaMist() {
        if (getType() != ModEntityTypes.ZABUZA_MOMOCHI.get() || isLegacyZabuzaClone()) {
            return;
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()
                || this.tickCount <= this.legacyZabuzaMistLastUsed + LEGACY_ZABUZA_MIST_COOLDOWN_TICKS
                || !consumeLegacyZabuzaChakra(LEGACY_ZABUZA_MIST_CHAKRA)) {
            return;
        }
        if (SuitonMistEntity.spawnFrom(this)) {
            this.legacyZabuzaMistLastUsed = this.tickCount;
        }
    }

    private boolean tryLegacyZabuzaWaterDragon(LivingEntity target) {
        if (getType() != ModEntityTypes.ZABUZA_MOMOCHI.get()) {
            return false;
        }
        if (target == null || !target.isAlive() || distanceToSqr(target) > LEGACY_ZABUZA_WATER_DRAGON_RANGE_SQR) {
            return false;
        }
        if (this.tickCount <= this.legacyZabuzaWaterDragonLastUsed + LEGACY_ZABUZA_WATER_DRAGON_COOLDOWN_TICKS
                || !consumeLegacyZabuzaChakra(LEGACY_ZABUZA_WATER_DRAGON_CHAKRA)) {
            return false;
        }
        if (WaterDragonEntity.spawnFrom(this, 1.0F)) {
            this.legacyZabuzaWaterDragonLastUsed = this.tickCount;
            this.legacyStandStillTicks = LEGACY_ZABUZA_WATER_DRAGON_STAND_STILL_TICKS;
            return true;
        }
        return false;
    }

    private boolean consumeLegacyZabuzaChakra(double amount) {
        if (this.legacyZabuzaChakra < amount) {
            return false;
        }
        this.legacyZabuzaChakra -= amount;
        return true;
    }

    private boolean tryLegacyKisameWaterShark(LivingEntity target) {
        if (getType() != ModEntityTypes.KISAME_HOSHIGAKI.get() || target == null || !target.isAlive()) {
            return false;
        }
        double maxRange = LEGACY_KISAME_WATER_SHARK_RANGE + LEGACY_KISAME_WATER_SHARK_RANGE_TOLERANCE;
        if (distanceToSqr(target) > maxRange * maxRange) {
            return false;
        }
        float power = isLegacyKisameFused() ? 5.0F : 1.0F + getRandom().nextFloat();
        if (!consumeLegacyKisameChakra(LEGACY_KISAME_WATER_SHARK_CHAKRA * power)) {
            return false;
        }
        if (WaterSharkEntity.spawnFrom(this, power)) {
            this.legacyStandStillTicks = LEGACY_KISAME_WATER_SHARK_STAND_STILL_TICKS;
            return true;
        }
        return false;
    }

    private boolean consumeLegacyKisameChakra(double amount) {
        if (this.legacyKisameChakra < amount) {
            return false;
        }
        this.legacyKisameChakra -= amount;
        return true;
    }

    private void tickLegacyZabuzaLowHealthSupport() {
        if (getType() != ModEntityTypes.ZABUZA_MOMOCHI.get() || isLegacyZabuzaClone()) {
            return;
        }
        if (this.legacyZabuzaAvoidTarget != null
                && (!this.legacyZabuzaAvoidTarget.isAlive()
                || getHealth() > getMaxHealth() * LEGACY_ZABUZA_REENGAGE_HEALTH_THRESHOLD)) {
            this.legacyZabuzaAvoidTarget = null;
        }
        if (hasLegacyZabuzaAvoidTarget() && getTarget() != null) {
            setTarget(null);
        }

        LivingEntity attacker = getLastHurtByMob();
        if (attacker == null
                || !attacker.isAlive()
                || isAlliedTo(attacker)
                || getHealth() > getMaxHealth() * LEGACY_ZABUZA_HELP_HEALTH_THRESHOLD
                || this.tickCount <= this.legacyZabuzaLastCallForHelp + LEGACY_ZABUZA_CALL_HELP_COOLDOWN_TICKS) {
            return;
        }

        this.legacyZabuzaAvoidTarget = attacker;
        setTarget(null);
        callLegacyZabuzaHelp(attacker);
        this.legacyZabuzaLastCallForHelp = this.tickCount;
    }

    private void tickLegacyZabuzaWaterClone() {
        if (getType() != ModEntityTypes.ZABUZA_MOMOCHI.get() || isLegacyZabuzaClone()) {
            return;
        }
        LivingEntity target = getTarget();
        if (target == null
                || !target.isAlive()
                || this.legacyZabuzaCloneCount >= 1
                || this.tickCount <= this.legacyZabuzaCloneLastUsed + LEGACY_ZABUZA_WATER_CLONE_COOLDOWN_TICKS
                || !consumeLegacyZabuzaChakra(LEGACY_ZABUZA_WATER_CLONE_CHAKRA)) {
            return;
        }
        if (spawnLegacyZabuzaWaterClone()) {
            this.legacyZabuzaCloneLastUsed = this.tickCount;
        }
    }

    private void tickLegacyKisameWaterClone() {
        if (getType() != ModEntityTypes.KISAME_HOSHIGAKI.get()
                || isLegacyKisameClone()
                || isLegacyKisameFused()) {
            return;
        }
        LivingEntity target = getTarget();
        if (target == null
                || !target.isAlive()
                || this.legacyKisameCloneCount >= LEGACY_KISAME_WATER_CLONE_MAX_COUNT
                || this.tickCount <= this.legacyKisameCloneLastUsed + LEGACY_KISAME_WATER_CLONE_COOLDOWN_TICKS
                || !consumeLegacyKisameChakra(LEGACY_KISAME_WATER_CLONE_CHAKRA)) {
            return;
        }
        if (spawnLegacyKisameWaterClone()) {
            this.legacyKisameCloneLastUsed = this.tickCount;
        }
    }

    private boolean spawnLegacyZabuzaWaterClone() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        NinjaMobEntity clone = ModEntityTypes.ZABUZA_MOMOCHI.get().create(serverLevel);
        if (clone == null) {
            return false;
        }
        clone.configureLegacyZabuzaClone(this);
        if (!serverLevel.addFreshEntity(clone)) {
            return false;
        }
        this.legacyZabuzaCloneCount++;
        clone.legacyZabuzaCloneCounted = true;
        this.level().playSound(null, getX(), getY(), getZ(),
                ModSounds.SOUND_KAGEBUNSHIN.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        return true;
    }

    private boolean spawnLegacyKisameWaterClone() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        NinjaMobEntity clone = ModEntityTypes.KISAME_HOSHIGAKI.get().create(serverLevel);
        if (clone == null) {
            return false;
        }
        clone.configureLegacyKisameClone(this);
        if (!serverLevel.addFreshEntity(clone)) {
            return false;
        }
        this.legacyKisameCloneCount++;
        clone.legacyKisameCloneCounted = true;
        this.level().playSound(null, getX(), getY(), getZ(),
                ModSounds.SOUND_KAGEBUNSHIN.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        return true;
    }

    private void configureLegacyZabuzaClone(NinjaMobEntity original) {
        this.legacyZabuzaOriginal = original;
        this.legacyZabuzaOriginalUuid = original.getUUID();
        moveTo(original.getX(), original.getY(), original.getZ(), original.getYRot(), original.getXRot());
        configureLegacyZabuzaCloneStats();
        setTarget(original.getTarget());
    }

    private void configureLegacyZabuzaCloneStats() {
        setLegacyAttributeBaseValue(Attributes.MAX_HEALTH, LEGACY_ZABUZA_WATER_CLONE_MAX_HEALTH);
        setLegacyAttributeBaseValue(Attributes.ATTACK_DAMAGE, LEGACY_ZABUZA_WATER_CLONE_ATTACK_DAMAGE);
        setHealth((float)LEGACY_ZABUZA_WATER_CLONE_MAX_HEALTH);
        this.xpReward = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setDropChance(slot, 0.0F);
        }
    }

    private void configureLegacyKisameClone(NinjaMobEntity original) {
        this.legacyKisameOriginal = original;
        this.legacyKisameOriginalUuid = original.getUUID();
        moveTo(original.getX(), original.getY(), original.getZ(), original.getYRot(), original.getXRot());
        configureLegacyKisameCloneStats();
        setTarget(original.getTarget());
    }

    private void configureLegacyKisameCloneStats() {
        setLegacyAttributeBaseValue(Attributes.MAX_HEALTH, LEGACY_KISAME_WATER_CLONE_MAX_HEALTH);
        setLegacyAttributeBaseValue(Attributes.ATTACK_DAMAGE, LEGACY_KISAME_WATER_CLONE_ATTACK_DAMAGE);
        setHealth((float)LEGACY_KISAME_WATER_CLONE_MAX_HEALTH);
        this.xpReward = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setDropChance(slot, 0.0F);
        }
    }

    private void setLegacyAttributeBaseValue(Attribute attribute, double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private boolean tickLegacyZabuzaCloneLifecycle() {
        if (!isLegacyZabuzaClone()) {
            return false;
        }
        NinjaMobEntity original = getLegacyZabuzaOriginal(true);
        if (original == null || !original.isAlive()) {
            discard();
            return true;
        }
        LivingEntity originalTarget = original.getTarget();
        if ((originalTarget == null || !originalTarget.isAlive()) && !original.hasLegacyZabuzaAvoidTarget()) {
            discard();
            return true;
        }
        syncLegacyZabuzaCloneTarget(original);
        return false;
    }

    private boolean tickLegacyKisameCloneLifecycle() {
        if (!isLegacyKisameClone()) {
            return false;
        }
        NinjaMobEntity original = getLegacyKisameOriginal(true);
        if (original == null || !original.isAlive()) {
            discard();
            return true;
        }
        LivingEntity originalTarget = original.getTarget();
        if (originalTarget == null || !originalTarget.isAlive()) {
            discard();
            return true;
        }
        if (original.isLegacyKisameFused()) {
            discard();
            return true;
        }
        syncLegacyKisameCloneTarget(original);
        return false;
    }

    private boolean tickLegacyZabuzaCloneWaterPrisonHold() {
        if (!isLegacyZabuzaClone()) {
            return false;
        }
        if (WaterPrisonEntity.isEntityTrapping(this)) {
            if (!isNoAi()) {
                setNoAi(true);
            }
            this.legacyZabuzaCloneWasTrapping = true;
            getNavigation().stop();
            setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
            this.hurtMarked = true;
            return true;
        }
        if (this.legacyZabuzaCloneWasTrapping) {
            setNoAi(false);
            this.legacyZabuzaCloneWasTrapping = false;
        }
        return false;
    }

    private boolean tickLegacyKisameCloneWaterPrisonHold() {
        if (!isLegacyKisameClone()) {
            return false;
        }
        if (WaterPrisonEntity.isEntityTrapping(this)) {
            if (!isNoAi()) {
                setNoAi(true);
            }
            this.legacyKisameCloneWasTrapping = true;
            getNavigation().stop();
            setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
            this.hurtMarked = true;
            return true;
        }
        if (this.legacyKisameCloneWasTrapping) {
            setNoAi(false);
            this.legacyKisameCloneWasTrapping = false;
        }
        return false;
    }

    @Nullable
    private NinjaMobEntity getLegacyZabuzaOriginal(boolean countIfResolved) {
        if (this.legacyZabuzaOriginal != null && this.legacyZabuzaOriginal.isAlive()) {
            return this.legacyZabuzaOriginal;
        }
        if (this.legacyZabuzaOriginalUuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(this.legacyZabuzaOriginalUuid);
        if (!(entity instanceof NinjaMobEntity ninja)
                || ninja.getType() != ModEntityTypes.ZABUZA_MOMOCHI.get()
                || ninja.isLegacyZabuzaClone()) {
            return null;
        }
        this.legacyZabuzaOriginal = ninja;
        if (countIfResolved && !this.legacyZabuzaCloneCounted) {
            ninja.legacyZabuzaCloneCount++;
            this.legacyZabuzaCloneCounted = true;
        }
        return ninja;
    }

    private void syncLegacyZabuzaCloneTarget(NinjaMobEntity original) {
        LivingEntity target = original.getTarget();
        if (!isValidLegacyCloneTarget(target)) {
            target = original.legacyZabuzaAvoidTarget;
        }
        if (!isValidLegacyCloneTarget(target)) {
            target = original.getLastHurtByMob();
            if (target != null && original.tickCount - original.getLastHurtByMobTimestamp() >= 200) {
                target = null;
            }
        }
        if (!isValidLegacyCloneTarget(target)) {
            target = original.getLastHurtMob();
            if (target != null && original.tickCount - original.getLastHurtMobTimestamp() >= 200) {
                target = null;
            }
        }
        if (isValidLegacyCloneTarget(target)) {
            setTarget(target);
        } else if (getTarget() != null && !getTarget().isAlive()) {
            setTarget(null);
        }
    }

    @Nullable
    private NinjaMobEntity getLegacyKisameOriginal(boolean countIfResolved) {
        if (this.legacyKisameOriginal != null && this.legacyKisameOriginal.isAlive()) {
            return this.legacyKisameOriginal;
        }
        if (this.legacyKisameOriginalUuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(this.legacyKisameOriginalUuid);
        if (!(entity instanceof NinjaMobEntity ninja)
                || ninja.getType() != ModEntityTypes.KISAME_HOSHIGAKI.get()
                || ninja.isLegacyKisameClone()) {
            return null;
        }
        this.legacyKisameOriginal = ninja;
        if (countIfResolved && !this.legacyKisameCloneCounted) {
            ninja.legacyKisameCloneCount++;
            this.legacyKisameCloneCounted = true;
        }
        return ninja;
    }

    private void syncLegacyKisameCloneTarget(NinjaMobEntity original) {
        LivingEntity target = original.getLastHurtByMob();
        if (!isValidLegacyCloneTarget(target)) {
            target = original.getLastHurtMob();
            if (target != null && original.tickCount - original.getLastHurtMobTimestamp() >= 200) {
                target = null;
            }
        }
        if (!isValidLegacyCloneTarget(target)) {
            target = original.getTarget();
        }
        if (isValidLegacyCloneTarget(target)) {
            setTarget(target);
        } else if (getTarget() != null && !getTarget().isAlive()) {
            setTarget(null);
        }
    }

    private boolean isValidLegacyCloneTarget(@Nullable LivingEntity target) {
        return target != null && target.isAlive() && !isAlliedTo(target);
    }

    private void tickLegacyZabuzaWaterPrison() {
        if (!isLegacyZabuzaClone()) {
            return;
        }
        LivingEntity target = getTarget();
        if (target == null
                || !target.isAlive()
                || WaterPrisonEntity.isEntityTrapped(target)
                || distanceTo(target) < LEGACY_ZABUZA_WATER_PRISON_MIN_DISTANCE
                || this.tickCount <= this.legacyZabuzaWaterPrisonLastUsed + LEGACY_ZABUZA_WATER_PRISON_COOLDOWN_TICKS
                || this.legacyZabuzaChakra < LEGACY_ZABUZA_WATER_PRISON_CHAKRA) {
            return;
        }
        getLookControl().setLookAt(target, 90.0F, 30.0F);
        if (WaterPrisonEntity.spawnFrom(this, target, LEGACY_ZABUZA_WATER_PRISON_DURATION_TICKS)) {
            hideLegacyZabuzaMainHand();
            consumeLegacyZabuzaChakra(LEGACY_ZABUZA_WATER_PRISON_CHAKRA);
            this.legacyZabuzaWaterPrisonLastUsed = this.tickCount;
            setNoAi(true);
            this.legacyZabuzaCloneWasTrapping = true;
        }
    }

    private void tickLegacyKisameWaterPrison() {
        if (!isLegacyKisameClone()) {
            return;
        }
        LivingEntity target = getTarget();
        if (target == null
                || !target.isAlive()
                || WaterPrisonEntity.isEntityTrapped(target)
                || this.tickCount <= this.legacyKisameWaterPrisonLastUsed + LEGACY_KISAME_WATER_PRISON_COOLDOWN_TICKS
                || this.legacyKisameChakra < LEGACY_KISAME_WATER_PRISON_CHAKRA) {
            return;
        }
        getLookControl().setLookAt(target, 90.0F, 30.0F);
        if (WaterPrisonEntity.spawnFrom(this, target, LEGACY_KISAME_WATER_PRISON_DURATION_TICKS)) {
            hideLegacyKisameMainHand();
            consumeLegacyKisameChakra(LEGACY_KISAME_WATER_PRISON_CHAKRA);
            this.legacyKisameWaterPrisonLastUsed = this.tickCount;
            setNoAi(true);
            this.legacyKisameCloneWasTrapping = true;
        }
    }

    private void tickLegacyKisameFusedForm() {
        if (getType() != ModEntityTypes.KISAME_HOSHIGAKI.get() || isLegacyKisameClone()) {
            return;
        }
        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) {
            if (isLegacyKisameFused()) {
                ensureLegacyKisameWaterDome();
                return;
            }
            if (getHealth() >= getMaxHealth() / 3.0F || !consumeLegacyKisameChakra(LEGACY_KISAME_WATER_SHOCKWAVE_CHAKRA)) {
                return;
            }
            if (ensureLegacyKisameWaterDome()) {
                setLegacyKisameFused(true);
                setHealth(getMaxHealth());
            }
            return;
        }
        if (isLegacyKisameFused() && this.legacyKisamePeacefulTicks > LEGACY_KISAME_FUSED_EXIT_PEACEFUL_TICKS) {
            setTarget(null);
            deactivateLegacyKisameWaterDome();
            setLegacyKisameFused(false);
        }
    }

    private boolean ensureLegacyKisameWaterDome() {
        if (WaterShockwaveEntity.getActive(this) != null) {
            return true;
        }
        return WaterShockwaveEntity.toggleFrom(this, LEGACY_KISAME_WATER_DOME_RADIUS)
                == WaterShockwaveEntity.ToggleResult.CREATED;
    }

    private void deactivateLegacyKisameWaterDome() {
        WaterShockwaveEntity active = WaterShockwaveEntity.getActive(this);
        if (active != null) {
            active.setShouldDie();
        }
    }

    private void callLegacyZabuzaHelp(LivingEntity attacker) {
        double followRange = getAttributeValue(Attributes.FOLLOW_RANGE);
        this.level().getEntitiesOfClass(
                        NinjaMobEntity.class,
                        getBoundingBox().inflate(followRange, LEGACY_ZABUZA_HELP_VERTICAL_RANGE, followRange),
                        ninja -> ninja != this && ninja.isAlive() && isLegacyZabuzaTeam(ninja.getType()) && !ninja.isAlliedTo(attacker))
                .forEach(ninja -> ninja.setTarget(attacker));
    }

    private void tickLegacyZabuzaChakraRecovery() {
        if (getType() != ModEntityTypes.ZABUZA_MOMOCHI.get()) {
            return;
        }
        LivingEntity target = getTarget();
        LivingEntity attacker = getLastHurtByMob();
        boolean peaceful = (target == null || !target.isAlive()) && (attacker == null || !attacker.isAlive());
        if (!peaceful) {
            this.legacyZabuzaPeacefulTicks = 0;
            return;
        }
        this.legacyZabuzaPeacefulTicks++;
        if (this.legacyZabuzaPeacefulTicks % 20 == 19) {
            this.legacyZabuzaChakra = Math.min(
                    LEGACY_ZABUZA_INITIAL_CHAKRA,
                    this.legacyZabuzaChakra + LEGACY_ZABUZA_INITIAL_CHAKRA * 0.04D);
            if (getHealth() < getMaxHealth()) {
                heal(1.0F);
            }
        }
    }

    private void tickLegacyKisameChakraRecovery() {
        if (getType() != ModEntityTypes.KISAME_HOSHIGAKI.get()) {
            return;
        }
        LivingEntity target = getTarget();
        LivingEntity attacker = getLastHurtByMob();
        boolean peaceful = (target == null || !target.isAlive()) && (attacker == null || !attacker.isAlive());
        if (!peaceful) {
            this.legacyKisamePeacefulTicks = 0;
            return;
        }
        this.legacyKisamePeacefulTicks++;
        if (this.legacyKisamePeacefulTicks % 20 == 19) {
            this.legacyKisameChakra = Math.min(
                    LEGACY_KISAME_INITIAL_CHAKRA,
                    this.legacyKisameChakra + LEGACY_KISAME_INITIAL_CHAKRA * 0.04D);
            if (getHealth() < getMaxHealth()) {
                heal(1.0F);
            }
        }
    }

    private void tickLegacyStandStill() {
        if (this.legacyStandStillTicks <= 0) {
            return;
        }
        this.legacyStandStillTicks--;
        getNavigation().stop();
        setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
        this.hurtMarked = true;
    }

    private void trackLegacyZabuzaBossPlayers() {
        if (getType() != ModEntityTypes.ZABUZA_MOMOCHI.get() || isLegacyZabuzaClone()) {
            return;
        }
        Entity entity = getLastHurtByMob();
        if (!(entity instanceof ServerPlayer)) {
            entity = getTarget();
        }
        if (entity instanceof ServerPlayer player) {
            this.legacyZabuzaBossEvent.addPlayer(player);
        } else {
            this.legacyZabuzaBossEvent.removeAllPlayers();
        }
        this.legacyZabuzaBossEvent.setProgress(Math.max(0.0F, Math.min(getHealth() / getMaxHealth(), 1.0F)));
    }

    private void trackLegacyKisameBossPlayers() {
        if (getType() != ModEntityTypes.KISAME_HOSHIGAKI.get() || isLegacyKisameClone()) {
            return;
        }
        Entity entity = getLastHurtByMob();
        if (!(entity instanceof ServerPlayer)) {
            entity = getTarget();
        }
        if (entity instanceof ServerPlayer player) {
            this.legacyKisameBossEvent.addPlayer(player);
        } else {
            this.legacyKisameBossEvent.removeAllPlayers();
        }
        this.legacyKisameBossEvent.setProgress(Math.max(0.0F, Math.min(getHealth() / getMaxHealth(), 1.0F)));
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (getType() == ModEntityTypes.ZABUZA_MOMOCHI.get()
                && !isLegacyZabuzaClone()
                && (player.equals(getLastHurtByMob()) || player.equals(getTarget()))) {
            this.legacyZabuzaBossEvent.addPlayer(player);
        }
        if (getType() == ModEntityTypes.KISAME_HOSHIGAKI.get()
                && !isLegacyKisameClone()
                && (player.equals(getLastHurtByMob()) || player.equals(getTarget()))) {
            this.legacyKisameBossEvent.addPlayer(player);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (getType() == ModEntityTypes.ZABUZA_MOMOCHI.get() && !isLegacyZabuzaClone()) {
            this.legacyZabuzaBossEvent.removePlayer(player);
        }
        if (getType() == ModEntityTypes.KISAME_HOSHIGAKI.get() && !isLegacyKisameClone()) {
            this.legacyKisameBossEvent.removePlayer(player);
        }
    }

    @Override
    public void die(DamageSource source) {
        if (isLegacyZabuzaClone() || isLegacyKisameClone()) {
            spawnLegacyCloneDeathWater(isLegacyKisameClone()
                    ? LEGACY_KISAME_CLONE_DEATH_WATER_TICKS
                    : LEGACY_ZABUZA_CLONE_DEATH_WATER_TICKS);
            super.die(source);
            discard();
            return;
        }
        super.die(source);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (getType() == ModEntityTypes.ZABUZA_MOMOCHI.get()
                && !isLegacyZabuzaClone()
                && !this.legacyZabuzaStoredMainHand.isEmpty()) {
            spawnAtLocation(this.legacyZabuzaStoredMainHand.copy());
            setLegacyZabuzaStoredMainHand(ItemStack.EMPTY);
        } else if (getType() == ModEntityTypes.KISAME_HOSHIGAKI.get()
                && !isLegacyKisameClone()
                && !this.legacyKisameStoredMainHand.isEmpty()) {
            spawnAtLocation(this.legacyKisameStoredMainHand.copy());
            setLegacyKisameStoredMainHand(ItemStack.EMPTY);
        }
    }

    private void spawnLegacyCloneDeathWater(int durationTicks) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.playSound(null, getX(), getY(), getZ(),
                SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL, 1.0F, 1.0F);
        BlockPos pos = blockPosition().above();
        SpecialEvent.setBlocksEvent(
                serverLevel,
                Map.of(pos, Blocks.WATER.defaultBlockState()),
                0L,
                durationTicks,
                false,
                false);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide) {
            cleanupLegacyZabuzaCloneReference();
            cleanupLegacyKisameCloneReference();
        }
        super.remove(reason);
    }

    private void cleanupLegacyZabuzaCloneReference() {
        if (!isLegacyZabuzaClone() || this.legacyZabuzaCloneCleanedUp) {
            return;
        }
        this.legacyZabuzaCloneCleanedUp = true;
        if (!this.legacyZabuzaCloneCounted) {
            return;
        }
        NinjaMobEntity original = this.legacyZabuzaOriginal;
        if (original == null && this.legacyZabuzaOriginalUuid != null && this.level() instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(this.legacyZabuzaOriginalUuid);
            if (entity instanceof NinjaMobEntity ninja
                    && ninja.getType() == ModEntityTypes.ZABUZA_MOMOCHI.get()
                    && !ninja.isLegacyZabuzaClone()) {
                original = ninja;
            }
        }
        if (original != null && original.legacyZabuzaCloneCount > 0) {
            original.legacyZabuzaCloneCount--;
        }
        this.legacyZabuzaCloneCounted = false;
    }

    private void cleanupLegacyKisameCloneReference() {
        if (!isLegacyKisameClone() || this.legacyKisameCloneCleanedUp) {
            return;
        }
        this.legacyKisameCloneCleanedUp = true;
        if (!this.legacyKisameCloneCounted) {
            return;
        }
        NinjaMobEntity original = this.legacyKisameOriginal;
        if (original == null && this.legacyKisameOriginalUuid != null && this.level() instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(this.legacyKisameOriginalUuid);
            if (entity instanceof NinjaMobEntity ninja
                    && ninja.getType() == ModEntityTypes.KISAME_HOSHIGAKI.get()
                    && !ninja.isLegacyKisameClone()) {
                original = ninja;
            }
        }
        if (original != null && original.legacyKisameCloneCount > 0) {
            original.legacyKisameCloneCount--;
        }
        this.legacyKisameCloneCounted = false;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("LegacyZabuzaOriginal")) {
            this.legacyZabuzaOriginalUuid = tag.getUUID("LegacyZabuzaOriginal");
            configureLegacyZabuzaCloneStats();
        }
        if (tag.hasUUID("LegacyKisameOriginal")) {
            this.legacyKisameOriginalUuid = tag.getUUID("LegacyKisameOriginal");
            configureLegacyKisameCloneStats();
        }
        this.legacyZabuzaStoredMainHand = tag.contains("LegacyZabuzaStoredMainHand", 10)
                ? ItemStack.of(tag.getCompound("LegacyZabuzaStoredMainHand"))
                : ItemStack.EMPTY;
        setLegacyZabuzaStoredMainHand(this.legacyZabuzaStoredMainHand);
        if (tag.contains("LegacyKisameStoredMainHand", 10)) {
            setLegacyKisameStoredMainHand(ItemStack.of(tag.getCompound("LegacyKisameStoredMainHand")));
        } else if (getType() == ModEntityTypes.KISAME_HOSHIGAKI.get() && !getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
            setLegacyKisameStoredMainHand(ItemStack.EMPTY);
        }
        this.legacyZabuzaChakra = tag.contains("LegacyZabuzaChakra")
                ? tag.getDouble("LegacyZabuzaChakra")
                : LEGACY_ZABUZA_INITIAL_CHAKRA;
        this.legacyZabuzaMistLastUsed = tag.contains("LegacyZabuzaMistLastUsed")
                ? tag.getInt("LegacyZabuzaMistLastUsed")
                : -LEGACY_ZABUZA_MIST_COOLDOWN_TICKS;
        this.legacyZabuzaWaterDragonLastUsed = tag.getInt("LegacyZabuzaWaterDragonLastUsed");
        this.legacyZabuzaCloneLastUsed = tag.contains("LegacyZabuzaCloneLastUsed")
                ? tag.getInt("LegacyZabuzaCloneLastUsed")
                : -LEGACY_ZABUZA_WATER_CLONE_COOLDOWN_TICKS + LEGACY_ZABUZA_WATER_CLONE_INITIAL_OFFSET_TICKS;
        this.legacyZabuzaWaterPrisonLastUsed = tag.contains("LegacyZabuzaWaterPrisonLastUsed")
                ? tag.getInt("LegacyZabuzaWaterPrisonLastUsed")
                : -LEGACY_ZABUZA_WATER_PRISON_COOLDOWN_TICKS + LEGACY_ZABUZA_WATER_PRISON_INITIAL_OFFSET_TICKS;
        this.legacyStandStillTicks = tag.getInt("LegacyStandStillTicks");
        this.legacyZabuzaPeacefulTicks = tag.getInt("LegacyZabuzaPeacefulTicks");
        if (getType() == ModEntityTypes.KISAME_HOSHIGAKI.get()) {
            this.legacyKisameChakra = tag.contains("LegacyKisameChakra")
                    ? tag.getDouble("LegacyKisameChakra")
                    : LEGACY_KISAME_INITIAL_CHAKRA;
            this.legacyKisamePeacefulTicks = tag.getInt("LegacyKisamePeacefulTicks");
            this.legacyKisameCloneLastUsed = tag.contains("LegacyKisameCloneLastUsed")
                    ? tag.getInt("LegacyKisameCloneLastUsed")
                    : -LEGACY_KISAME_WATER_CLONE_COOLDOWN_TICKS;
            this.legacyKisameWaterPrisonLastUsed = tag.contains("LegacyKisameWaterPrisonLastUsed")
                    ? tag.getInt("LegacyKisameWaterPrisonLastUsed")
                    : -LEGACY_KISAME_WATER_PRISON_COOLDOWN_TICKS;
            setLegacyKisameFused(tag.getBoolean("LegacyKisameFused"));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (getType() == ModEntityTypes.ZABUZA_MOMOCHI.get()) {
            if (isLegacyZabuzaClone() && this.legacyZabuzaOriginalUuid != null) {
                tag.putUUID("LegacyZabuzaOriginal", this.legacyZabuzaOriginalUuid);
            }
            if (!this.legacyZabuzaStoredMainHand.isEmpty()) {
                tag.put("LegacyZabuzaStoredMainHand", this.legacyZabuzaStoredMainHand.save(new CompoundTag()));
            }
            tag.putDouble("LegacyZabuzaChakra", this.legacyZabuzaChakra);
            tag.putInt("LegacyZabuzaMistLastUsed", this.legacyZabuzaMistLastUsed);
            tag.putInt("LegacyZabuzaWaterDragonLastUsed", this.legacyZabuzaWaterDragonLastUsed);
            tag.putInt("LegacyZabuzaCloneLastUsed", this.legacyZabuzaCloneLastUsed);
            tag.putInt("LegacyZabuzaWaterPrisonLastUsed", this.legacyZabuzaWaterPrisonLastUsed);
            tag.putInt("LegacyStandStillTicks", this.legacyStandStillTicks);
            tag.putInt("LegacyZabuzaPeacefulTicks", this.legacyZabuzaPeacefulTicks);
        }
        if (getType() == ModEntityTypes.KISAME_HOSHIGAKI.get()) {
            if (isLegacyKisameClone() && this.legacyKisameOriginalUuid != null) {
                tag.putUUID("LegacyKisameOriginal", this.legacyKisameOriginalUuid);
            }
            if (!this.legacyKisameStoredMainHand.isEmpty()) {
                tag.put("LegacyKisameStoredMainHand", this.legacyKisameStoredMainHand.save(new CompoundTag()));
            }
            tag.putDouble("LegacyKisameChakra", this.legacyKisameChakra);
            tag.putInt("LegacyKisamePeacefulTicks", this.legacyKisamePeacefulTicks);
            tag.putInt("LegacyKisameCloneLastUsed", this.legacyKisameCloneLastUsed);
            tag.putInt("LegacyKisameWaterPrisonLastUsed", this.legacyKisameWaterPrisonLastUsed);
            tag.putBoolean("LegacyKisameFused", isLegacyKisameFused());
            tag.putInt("LegacyStandStillTicks", this.legacyStandStillTicks);
        }
    }

    @Override
    public int getMaxFallDistance() {
        return 12;
    }

    @Override
    protected float getWaterSlowDown() {
        if (getType() == ModEntityTypes.KISAME_HOSHIGAKI.get()) {
            return isLegacyKisameFused() ? 1.0F : 0.8F;
        }
        return 0.98F;
    }

    @Override
    public boolean isPushedByFluid() {
        return !isLegacyKisameFused() && super.isPushedByFluid();
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.HOSTILE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.HOSTILE_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 1.0F;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private final class LegacyZabuzaLeapGoal extends Goal {
        @Nullable
        private LivingEntity target;

        private LegacyZabuzaLeapGoal() {
            setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (getType() != ModEntityTypes.ZABUZA_MOMOCHI.get() || hasLegacyZabuzaAvoidTarget() || !onGround()) {
                return false;
            }
            this.target = getTarget();
            if (this.target == null || !this.target.isAlive()) {
                return false;
            }
            double distance = distanceTo(this.target);
            return distance >= LEGACY_ZABUZA_LEAP_MIN_DISTANCE
                    && distance <= LEGACY_ZABUZA_LEAP_MAX_DISTANCE
                    && getRandom().nextInt(5) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return !onGround();
        }

        @Override
        public void start() {
            if (this.target == null) {
                return;
            }
            double x = this.target.getX() - getX();
            double z = this.target.getZ() - getZ();
            double horizontal = Math.sqrt(x * x + z * z);
            double y = this.target.getY() - getY() + horizontal * 0.2D;
            double distance = Math.sqrt(x * x + y * y + z * z);
            if (distance >= 1.0E-4D) {
                setDeltaMovement(
                        x / distance * LEGACY_ZABUZA_LEAP_STRENGTH,
                        y / distance * LEGACY_ZABUZA_LEAP_STRENGTH,
                        z / distance * LEGACY_ZABUZA_LEAP_STRENGTH);
                hurtMarked = true;
            }
        }
    }

    private final class LegacyKisameLeapGoal extends Goal {
        @Nullable
        private LivingEntity target;

        private LegacyKisameLeapGoal() {
            setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (getType() != ModEntityTypes.KISAME_HOSHIGAKI.get() || !onGround()) {
                return false;
            }
            this.target = getTarget();
            if (this.target == null || !this.target.isAlive()) {
                return false;
            }
            double distance = distanceTo(this.target);
            return distance >= LEGACY_KISAME_LEAP_MIN_DISTANCE
                    && distance <= LEGACY_KISAME_LEAP_MAX_DISTANCE
                    && getRandom().nextInt(5) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return !onGround();
        }

        @Override
        public void start() {
            if (this.target == null) {
                return;
            }
            double x = this.target.getX() - getX();
            double z = this.target.getZ() - getZ();
            double horizontal = Math.sqrt(x * x + z * z);
            double y = this.target.getY() - getY() + horizontal * 0.2D;
            double distance = Math.sqrt(x * x + y * y + z * z);
            if (distance >= 1.0E-4D) {
                setDeltaMovement(
                        x / distance * LEGACY_KISAME_LEAP_STRENGTH,
                        y / distance * LEGACY_KISAME_LEAP_STRENGTH,
                        z / distance * LEGACY_KISAME_LEAP_STRENGTH);
                hurtMarked = true;
            }
        }
    }

    private final class LegacyZabuzaMeleeAttackGoal extends MeleeAttackGoal {
        private LegacyZabuzaMeleeAttackGoal() {
            super(NinjaMobEntity.this, LEGACY_ZABUZA_MELEE_SPEED, true);
        }

        @Override
        public boolean canUse() {
            return getType() == ModEntityTypes.ZABUZA_MOMOCHI.get()
                    && !hasLegacyZabuzaAvoidTarget()
                    && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return getType() == ModEntityTypes.ZABUZA_MOMOCHI.get()
                    && !hasLegacyZabuzaAvoidTarget()
                    && super.canContinueToUse();
        }

        @Override
        protected double getAttackReachSqr(LivingEntity target) {
            return LEGACY_ZABUZA_MELEE_REACH_BASE + target.getBbWidth();
        }
    }

    private final class LegacyKisameMeleeAttackGoal extends MeleeAttackGoal {
        private LegacyKisameMeleeAttackGoal() {
            super(NinjaMobEntity.this, LEGACY_KISAME_MELEE_SPEED, true);
        }

        @Override
        public boolean canUse() {
            return getType() == ModEntityTypes.KISAME_HOSHIGAKI.get() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return getType() == ModEntityTypes.KISAME_HOSHIGAKI.get() && super.canContinueToUse();
        }

        @Override
        protected double getAttackReachSqr(LivingEntity target) {
            return LEGACY_KISAME_MELEE_REACH_BASE + target.getBbWidth();
        }
    }

    private final class LegacyKisameWaterSharkGoal extends Goal {
        @Nullable
        private Vec3 targetPos;
        private int attackTime = LEGACY_KISAME_WATER_SHARK_COOLDOWN_TICKS;
        private boolean strafingBackwards;
        private int strafingTime = -1;

        private LegacyKisameWaterSharkGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (getType() != ModEntityTypes.KISAME_HOSHIGAKI.get()) {
                return false;
            }
            this.attackTime--;
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive() || this.attackTime > 0) {
                return false;
            }
            this.attackTime = 0;
            this.targetPos = findLegacyWaterSharkSetupPos(target);
            return this.targetPos != null;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return getType() == ModEntityTypes.KISAME_HOSHIGAKI.get()
                    && this.targetPos != null
                    && target != null
                    && target.isAlive()
                    && --this.attackTime >= -LEGACY_KISAME_WATER_SHARK_SETUP_GRACE_TICKS;
        }

        @Override
        public void start() {
            double x = this.targetPos.x - getX();
            double z = this.targetPos.z - getZ();
            double horizontal = Math.sqrt(x * x + z * z);
            double y = this.targetPos.y - getY() + horizontal * 0.2D;
            double distance = Math.sqrt(x * x + y * y + z * z);
            if (distance >= 1.0E-4D) {
                setDeltaMovement(x / distance, y / distance, z / distance);
                hurtMarked = true;
            }
        }

        @Override
        public void stop() {
            this.targetPos = null;
            this.strafingTime = -1;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive()) {
                this.targetPos = null;
                return;
            }

            double distance = distanceTo(target);
            if (distance < LEGACY_KISAME_WATER_SHARK_RANGE - LEGACY_KISAME_WATER_SHARK_RANGE_TOLERANCE) {
                getNavigation().stop();
                this.strafingTime++;
            } else if (distance > LEGACY_KISAME_WATER_SHARK_RANGE + LEGACY_KISAME_WATER_SHARK_RANGE_TOLERANCE) {
                getNavigation().moveTo(target, LEGACY_KISAME_WATER_SHARK_APPROACH_SPEED);
                this.strafingTime = -1;
            } else {
                float distanceFactor = Mth.clamp((float)(distance / LEGACY_KISAME_WATER_SHARK_RANGE), 0.1F, 1.0F);
                tryLegacyKisameWaterShark(target);
                this.attackTime = Mth.floor(distanceFactor * (float)LEGACY_KISAME_WATER_SHARK_COOLDOWN_TICKS);
                this.targetPos = null;
                return;
            }

            if (this.strafingTime >= 20) {
                if (getRandom().nextFloat() < 0.3F) {
                    this.strafingBackwards = !this.strafingBackwards;
                }
                this.strafingTime = 0;
            }
            if (this.strafingTime > -1) {
                if (distance > LEGACY_KISAME_WATER_SHARK_RANGE + LEGACY_KISAME_WATER_SHARK_RANGE_TOLERANCE) {
                    this.strafingBackwards = false;
                } else if (distance < LEGACY_KISAME_WATER_SHARK_RANGE - LEGACY_KISAME_WATER_SHARK_RANGE_TOLERANCE) {
                    this.strafingBackwards = true;
                }
                getMoveControl().strafe(
                        this.strafingBackwards ? -LEGACY_KISAME_WATER_SHARK_STRAFE_SPEED : LEGACY_KISAME_WATER_SHARK_STRAFE_SPEED,
                        0.0F);
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
        }

        @Nullable
        private Vec3 findLegacyWaterSharkSetupPos(LivingEntity target) {
            Vec3 away = position().subtract(target.position());
            if (away.lengthSqr() < 1.0E-4D) {
                away = getLookAngle().multiply(-1.0D, 0.0D, -1.0D);
            }
            if (away.lengthSqr() < 1.0E-4D) {
                return null;
            }

            Vec3 candidate = away.normalize().scale(LEGACY_KISAME_WATER_SHARK_RANGE).add(position());
            for (double y = candidate.y - 3.0D; y < candidate.y + 7.0D; y += 1.0D) {
                BlockPos pos = BlockPos.containing(candidate.x, y, candidate.z);
                if (canStandAtLegacyWaterSharkPos(pos)) {
                    return Vec3.atLowerCornerOf(pos);
                }
            }
            return null;
        }

        private boolean canStandAtLegacyWaterSharkPos(BlockPos pos) {
            return level().getBlockState(pos.below()).entityCanStandOn(level(), pos.below(), NinjaMobEntity.this)
                    && level().getBlockState(pos).getCollisionShape(level(), pos).isEmpty()
                    && level().getBlockState(pos.above()).getCollisionShape(level(), pos.above()).isEmpty();
        }
    }

    private final class LegacyZabuzaWaterDragonGoal extends Goal {
        @Nullable
        private Vec3 targetPos;
        private int attackTime = LEGACY_ZABUZA_WATER_DRAGON_COOLDOWN_TICKS;
        private boolean strafingBackwards;
        private int strafingTime = -1;

        private LegacyZabuzaWaterDragonGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (getType() != ModEntityTypes.ZABUZA_MOMOCHI.get() || hasLegacyZabuzaAvoidTarget()) {
                return false;
            }
            this.attackTime--;
            LivingEntity target = getTarget();
            if (target == null
                    || !target.isAlive()
                    || this.attackTime > 0
                    || NinjaMobEntity.this.tickCount <= NinjaMobEntity.this.legacyZabuzaWaterDragonLastUsed + LEGACY_ZABUZA_WATER_DRAGON_COOLDOWN_TICKS) {
                return false;
            }
            this.attackTime = 0;
            this.targetPos = findLegacyWaterDragonSetupPos(target);
            return this.targetPos != null;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return this.targetPos != null
                    && target != null
                    && target.isAlive()
                    && !hasLegacyZabuzaAvoidTarget()
                    && --this.attackTime >= -LEGACY_ZABUZA_WATER_DRAGON_SETUP_GRACE_TICKS;
        }

        @Override
        public void start() {
            double x = this.targetPos.x - getX();
            double z = this.targetPos.z - getZ();
            double horizontal = Math.sqrt(x * x + z * z);
            double y = this.targetPos.y - getY() + horizontal * 0.2D;
            double distance = Math.sqrt(x * x + y * y + z * z);
            if (distance >= 1.0E-4D) {
                setDeltaMovement(x / distance, y / distance, z / distance);
                hurtMarked = true;
            }
        }

        @Override
        public void stop() {
            this.targetPos = null;
            this.strafingTime = -1;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive()) {
                this.targetPos = null;
                return;
            }

            double distance = distanceTo(target);
            if (distance < LEGACY_ZABUZA_WATER_DRAGON_RANGE - LEGACY_ZABUZA_WATER_DRAGON_RANGE_TOLERANCE) {
                getNavigation().stop();
                this.strafingTime++;
            } else if (distance > LEGACY_ZABUZA_WATER_DRAGON_RANGE + LEGACY_ZABUZA_WATER_DRAGON_RANGE_TOLERANCE) {
                getNavigation().moveTo(target, LEGACY_ZABUZA_WATER_DRAGON_APPROACH_SPEED);
                this.strafingTime = -1;
            } else {
                float distanceFactor = Mth.clamp((float)(distance / LEGACY_ZABUZA_WATER_DRAGON_RANGE), 0.1F, 1.0F);
                tryLegacyZabuzaWaterDragon(target);
                this.attackTime = Mth.floor(distanceFactor * (float)LEGACY_ZABUZA_WATER_DRAGON_COOLDOWN_TICKS);
                this.targetPos = null;
                return;
            }

            if (this.strafingTime >= 20) {
                if (getRandom().nextFloat() < 0.3F) {
                    this.strafingBackwards = !this.strafingBackwards;
                }
                this.strafingTime = 0;
            }
            if (this.strafingTime > -1) {
                if (distance > LEGACY_ZABUZA_WATER_DRAGON_RANGE + LEGACY_ZABUZA_WATER_DRAGON_RANGE_TOLERANCE) {
                    this.strafingBackwards = false;
                } else if (distance < LEGACY_ZABUZA_WATER_DRAGON_RANGE - LEGACY_ZABUZA_WATER_DRAGON_RANGE_TOLERANCE) {
                    this.strafingBackwards = true;
                }
                getMoveControl().strafe(
                        this.strafingBackwards ? -LEGACY_ZABUZA_WATER_DRAGON_STRAFE_SPEED : LEGACY_ZABUZA_WATER_DRAGON_STRAFE_SPEED,
                        0.0F);
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
        }

        @Nullable
        private Vec3 findLegacyWaterDragonSetupPos(LivingEntity target) {
            Vec3 away = position().subtract(target.position());
            if (away.lengthSqr() < 1.0E-4D) {
                away = getLookAngle().multiply(-1.0D, 0.0D, -1.0D);
            }
            if (away.lengthSqr() < 1.0E-4D) {
                return null;
            }

            Vec3 candidate = away.normalize().scale(LEGACY_ZABUZA_WATER_DRAGON_RANGE).add(position());
            for (double y = candidate.y - 3.0D; y < candidate.y + 7.0D; y += 1.0D) {
                BlockPos pos = BlockPos.containing(candidate.x, y, candidate.z);
                if (canStandAtLegacyWaterDragonPos(pos)) {
                    return Vec3.atLowerCornerOf(pos);
                }
            }
            return null;
        }

        private boolean canStandAtLegacyWaterDragonPos(BlockPos pos) {
            return level().getBlockState(pos.below()).entityCanStandOn(level(), pos.below(), NinjaMobEntity.this)
                    && level().getBlockState(pos).getCollisionShape(level(), pos).isEmpty()
                    && level().getBlockState(pos.above()).getCollisionShape(level(), pos.above()).isEmpty();
        }
    }
}
