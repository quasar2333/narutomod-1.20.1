package net.narutomod.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;
import net.narutomod.Chakra;
import net.narutomod.NarutomodMod;
import net.narutomod.event.SpecialEvent;
import net.narutomod.item.EightGatesItem;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModItems;
import net.narutomod.registry.ModSounds;
import net.narutomod.world.VillagePoiHelper;

@Mod.EventBusSubscriber(modid = NarutomodMod.MODID)
public final class MightGuyEntity extends NinjaMobEntity implements RangedAttackMob {
    private static final EntityDataAccessor<Boolean> SWINGING_ARMS =
            SynchedEntityData.defineId(MightGuyEntity.class, EntityDataSerializers.BOOLEAN);
    public static final String VILLAGE_REPUTATION_TAG = "NarutomodMightGuyVillageReputation";
    private static final String GATE_COOLDOWN_TAG = "GateCooldown";
    private static final String CLOSE_GATES_COUNTDOWN_TAG = "CloseGatesCountdown";
    private static final String SIEGE_OBJECTIVE_NAME = "siege_kills";
    private static final String SIEGE_ACTIVE_TAG = "VillageSiegeQuestActive";
    private static final String SIEGE_TRACKING_STARTED_TAG = "VillageSiegeTrackingStarted";
    private static final String SIEGE_CUSTOMER_UUID_TAG = "VillageSiegeCustomer";
    private static final String SIEGE_CUSTOMER_NAME_TAG = "VillageSiegeCustomerName";
    private static final String SIEGE_START_TIME_TAG = "VillageSiegeStartTime";
    private static final String SIEGE_CENTER_X_TAG = "VillageSiegeCenterX";
    private static final String SIEGE_CENTER_Y_TAG = "VillageSiegeCenterY";
    private static final String SIEGE_CENTER_Z_TAG = "VillageSiegeCenterZ";
    private static final String SIEGE_RADIUS_TAG = "VillageSiegeRadius";
    private static final String SIEGE_STARTING_VILLAGERS_TAG = "VillageSiegeStartingVillagers";
    private static final String SIEGE_CUSTOMER_KILLS_TAG = "VillageSiegeCustomerKills";
    private static final String SIEGE_GUY_KILLS_TAG = "VillageSiegeGuyKills";
    private static final int EIGHT_GATES_FULL_XP = 2760;
    private static final int GATE_CLOSE_TICKS = 100;
    private static final int GATE_REOPEN_COOLDOWN = 600;
    private static final double HIRUDORA_MIN_DISTANCE_SQR = 36.0D;
    private static final int DEFAULT_SIEGE_RADIUS = 32;
    private static final int MAX_SIEGE_RADIUS = 64;
    private static final double NATURAL_DUPLICATE_SEARCH_RADIUS = 512.0D;
    private static final long NATURAL_PLAYER_TICK_SPAWN_INTERVAL = 24000L;
    private static final int SIEGE_SPAWN_INTERVAL = 100;
    private static final int SCOREBOARD_SIDEBAR_SLOT = 1;
    private static final String NINJA_ADVANCEMENT = "narutomod:ninjaachievement";

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            getDisplayName(),
            BossEvent.BossBarColor.GREEN,
            BossEvent.BossBarOverlay.PROGRESS);
    private int closeGatesCountdown;
    private int gateCooldown = 100;
    private boolean villageSiegeQuestActive;
    private boolean villageSiegeTrackingStarted;
    @Nullable
    private UUID villageSiegeCustomerId;
    private String villageSiegeCustomerName = "";
    private long villageSiegeStartTime;
    private BlockPos villageSiegeCenter = BlockPos.ZERO;
    private int villageSiegeRadius = DEFAULT_SIEGE_RADIUS;
    private int siegeStartingVillagers;
    private int customerKillCount;
    private int guyKillCount;
    private boolean legacyVillageHome;
    private long nextLegacyVillageHomeCheckTick;

    public MightGuyEntity(EntityType<? extends MightGuyEntity> entityType, Level level) {
        super(entityType, level);
        equipEightGates();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return NinjaMobEntity.createAttributes(50.0D, 100.0D, 0.5D, 10.0D, 50.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SWINGING_ARMS, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RangedAttackGoal(this, 1.2D, 200, 30.0F) {
            @Override
            public boolean canUse() {
                LivingEntity target = MightGuyEntity.this.getTarget();
                return target != null
                        && !MightGuyEntity.this.hasLegacyVillageHome()
                        && MightGuyEntity.this.getGateOpened() >= 7.0F
                        && MightGuyEntity.this.distanceToSqr(target) >= HIRUDORA_MIN_DISTANCE_SQR
                        && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                LivingEntity target = MightGuyEntity.this.getTarget();
                return target != null
                        && !MightGuyEntity.this.hasLegacyVillageHome()
                        && MightGuyEntity.this.getGateOpened() >= 7.0F
                        && MightGuyEntity.this.distanceToSqr(target) >= HIRUDORA_MIN_DISTANCE_SQR
                        && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Zombie.class, false));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Creeper.class, false));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Skeleton.class, false));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData, dataTag);
        equipEightGates();
        return result;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (this.tickCount == 1) {
            this.level().playSound(null, getX(), getY(), getZ(), ModSounds.SOUND_HOWL_YOUTH.get(),
                    getSoundSource(), 4.0F, 1.0F);
        }
        if (this.tickCount % 100 == 0) {
            this.addEffect(new MobEffectInstance(MobEffects.JUMP, 101, 5, false, false));
            if (this.getHealth() < this.getMaxHealth()) {
                this.heal(5.0F);
            }
        }
        updateGateState();
        updateVillageSiegeQuest();
        clampVelocity(1.0D);
        this.bossEvent.setProgress(Mth.clamp(this.getHealth() / this.getMaxHealth(), 0.0F, 1.0F));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.FALL)) {
            return false;
        }
        Entity attacker = source.getEntity();
        if (!this.level().isClientSide
                && attacker instanceof ServerPlayer player
                && this.level() instanceof ServerLevel serverLevel
                && VillagePoiHelper.findQuestContext(serverLevel, blockPosition()).isPresent()) {
            addVillageReputation(player, -3);
        }
        if (attacker instanceof LivingEntity living && !source.is(DamageTypes.FELL_OUT_OF_WORLD)
                && ProcedureUtils.isEntityInFOV(this, living)) {
            amount *= this.getRandom().nextFloat() * 0.2F;
            this.swing(InteractionHand.OFF_HAND);
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        this.swing(InteractionHand.MAIN_HAND);
        if ((int) getGateOpened() == 6 && !hasLegacyVillageHome()) {
            return AsakujakuFireballEntity.spawnBurst(this) > 0;
        }
        boolean hurt = super.doHurtTarget(target);
        if (hurt) {
            double height = Math.max(target.getBoundingBox().getYsize(), 0.1D);
            target.setDeltaMovement(target.getDeltaMovement().add(0.0D,
                    getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.02D / height,
                    0.0D));
            target.hasImpulse = true;
        }
        return hurt;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        boolean canUseSpecialAttack = !hasLegacyVillageHome() && getGateOpened() >= 7.0F;
        setSwingingArms(canUseSpecialAttack);
        if (!this.level().isClientSide && canUseSpecialAttack) {
            HirudoraEntity.spawnFrom(this);
        }
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        return entity instanceof SakuraHarunoEntity
                || entity instanceof IrukaSenseiEntity
                || entity instanceof TentenEntity
                || entity instanceof MightGuyEntity
                || super.isAlliedTo(entity);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(GATE_COOLDOWN_TAG, this.gateCooldown);
        tag.putInt(CLOSE_GATES_COUNTDOWN_TAG, this.closeGatesCountdown);
        tag.putBoolean(SIEGE_ACTIVE_TAG, this.villageSiegeQuestActive);
        tag.putBoolean(SIEGE_TRACKING_STARTED_TAG, this.villageSiegeTrackingStarted);
        if (this.villageSiegeCustomerId != null) {
            tag.putUUID(SIEGE_CUSTOMER_UUID_TAG, this.villageSiegeCustomerId);
        }
        tag.putString(SIEGE_CUSTOMER_NAME_TAG, this.villageSiegeCustomerName);
        tag.putLong(SIEGE_START_TIME_TAG, this.villageSiegeStartTime);
        tag.putInt(SIEGE_CENTER_X_TAG, this.villageSiegeCenter.getX());
        tag.putInt(SIEGE_CENTER_Y_TAG, this.villageSiegeCenter.getY());
        tag.putInt(SIEGE_CENTER_Z_TAG, this.villageSiegeCenter.getZ());
        tag.putInt(SIEGE_RADIUS_TAG, this.villageSiegeRadius);
        tag.putInt(SIEGE_STARTING_VILLAGERS_TAG, this.siegeStartingVillagers);
        tag.putInt(SIEGE_CUSTOMER_KILLS_TAG, this.customerKillCount);
        tag.putInt(SIEGE_GUY_KILLS_TAG, this.guyKillCount);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.gateCooldown = tag.getInt(GATE_COOLDOWN_TAG);
        this.closeGatesCountdown = tag.getInt(CLOSE_GATES_COUNTDOWN_TAG);
        this.villageSiegeQuestActive = tag.getBoolean(SIEGE_ACTIVE_TAG);
        this.villageSiegeTrackingStarted = tag.getBoolean(SIEGE_TRACKING_STARTED_TAG);
        this.villageSiegeCustomerId = tag.hasUUID(SIEGE_CUSTOMER_UUID_TAG) ? tag.getUUID(SIEGE_CUSTOMER_UUID_TAG) : null;
        this.villageSiegeCustomerName = tag.getString(SIEGE_CUSTOMER_NAME_TAG);
        this.villageSiegeStartTime = tag.getLong(SIEGE_START_TIME_TAG);
        this.villageSiegeCenter = new BlockPos(
                tag.getInt(SIEGE_CENTER_X_TAG),
                tag.getInt(SIEGE_CENTER_Y_TAG),
                tag.getInt(SIEGE_CENTER_Z_TAG));
        this.villageSiegeRadius = Mth.clamp(tag.getInt(SIEGE_RADIUS_TAG), DEFAULT_SIEGE_RADIUS, MAX_SIEGE_RADIUS);
        this.siegeStartingVillagers = tag.getInt(SIEGE_STARTING_VILLAGERS_TAG);
        this.customerKillCount = tag.getInt(SIEGE_CUSTOMER_KILLS_TAG);
        this.guyKillCount = tag.getInt(SIEGE_GUY_KILLS_TAG);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return super.mobInteract(player, hand);
        }
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || !(this.level() instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        if (this.villageSiegeQuestActive) {
            sendVillageSiegeStatus(serverPlayer);
            return InteractionResult.SUCCESS;
        }
        if (villageReputation(serverPlayer) < 0) {
            serverPlayer.displayClientMessage(Component.literal("Might Guy village reputation is too low."), true);
            return InteractionResult.SUCCESS;
        }
        VillagePoiHelper.Context village = VillagePoiHelper.findQuestContext(serverLevel, blockPosition()).orElse(null);
        if (village == null) {
            return super.mobInteract(player, hand);
        }
        long startTime = nextVillageSiegeStart(serverLevel);
        int siegeRadius = Mth.clamp(village.radius() + 5, DEFAULT_SIEGE_RADIUS, MAX_SIEGE_RADIUS);
        boolean scheduled = SpecialEvent.setVillageSiegeEvent(
                serverLevel,
                village.center().getX(),
                village.center().getY(),
                village.center().getZ(),
                startTime,
                siegeRadius,
                null,
                SIEGE_SPAWN_INTERVAL);
        if (!scheduled) {
            serverPlayer.displayClientMessage(Component.literal("Could not schedule Might Guy village defense."), true);
            return InteractionResult.SUCCESS;
        }
        this.villageSiegeQuestActive = true;
        this.villageSiegeTrackingStarted = false;
        this.villageSiegeCustomerId = serverPlayer.getUUID();
        this.villageSiegeCustomerName = serverPlayer.getScoreboardName();
        this.villageSiegeStartTime = startTime;
        this.villageSiegeCenter = village.center();
        this.villageSiegeRadius = siegeRadius;
        this.siegeStartingVillagers = village.villagerCount();
        this.customerKillCount = 0;
        this.guyKillCount = 0;
        broadcastGuyMessage(serverLevel, "chattext.mightguy.interact1");
        return InteractionResult.SUCCESS;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ILLUSIONER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GENERIC_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GENERIC_DEATH;
    }

    public float getGateOpened() {
        ItemStack stack = this.getMainHandItem();
        return stack.getItem() instanceof EightGatesItem eightGatesItem ? eightGatesItem.getGateOpened(stack) : 0.0F;
    }

    public void setSwingingArms(boolean swingingArms) {
        this.entityData.set(SWINGING_ARMS, swingingArms);
    }

    public boolean isSwingingArms() {
        return this.entityData.get(SWINGING_ARMS);
    }

    private void updateVillageSiegeQuest() {
        if (!this.villageSiegeQuestActive || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ServerPlayer customer = this.villageSiegeCustomerId == null
                ? null
                : serverLevel.getServer().getPlayerList().getPlayer(this.villageSiegeCustomerId);
        if (customer == null || !customer.isAlive()) {
            if (this.villageSiegeTrackingStarted) {
                finishVillageSiegeQuest(serverLevel, customer, false);
            } else {
                clearVillageSiegeQuest();
            }
            return;
        }
        if (!this.villageSiegeTrackingStarted && serverLevel.getGameTime() >= this.villageSiegeStartTime) {
            startTrackingCustomer(serverLevel, customer);
        }
        if (this.villageSiegeTrackingStarted
                && serverLevel.getGameTime() > this.villageSiegeStartTime + 20L
                && isLegacyDaytime(serverLevel)) {
            finishVillageSiegeQuest(serverLevel, customer, true);
        }
    }

    private void startTrackingCustomer(ServerLevel level, ServerPlayer customer) {
        this.villageSiegeTrackingStarted = true;
        this.customerKillCount = 0;
        this.guyKillCount = 0;
        this.siegeStartingVillagers = VillagePoiHelper.countVillagers(level, this.villageSiegeCenter, this.villageSiegeRadius);

        Scoreboard scoreboard = level.getScoreboard();
        Objective objective = scoreboard.getObjective(SIEGE_OBJECTIVE_NAME);
        if (objective == null) {
            objective = scoreboard.addObjective(
                    SIEGE_OBJECTIVE_NAME,
                    ObjectiveCriteria.DUMMY,
                    Component.literal(SIEGE_OBJECTIVE_NAME),
                    ObjectiveCriteria.RenderType.INTEGER);
        }
        scoreboard.getOrCreatePlayerScore(this.getScoreboardName(), objective).setScore(0);
        scoreboard.getOrCreatePlayerScore(customer.getScoreboardName(), objective).setScore(0);
        scoreboard.setDisplayObjective(SCOREBOARD_SIDEBAR_SLOT, objective);
        broadcastGuyMessage(level, "chattext.mightguy.interact2");
    }

    private void finishVillageSiegeQuest(ServerLevel level, @Nullable ServerPlayer customer, boolean allowReward) {
        int villagersKilled = Math.max(0,
                this.siegeStartingVillagers - VillagePoiHelper.countVillagers(level, this.villageSiegeCenter, this.villageSiegeRadius));
        boolean success = allowReward
                && customer != null
                && customer.isAlive()
                && villagersKilled <= 0
                && this.customerKillCount >= this.guyKillCount / 2;

        if (success) {
            broadcastGuyMessage(level, "chattext.mightguy.interact3");
            addVillageReputation(customer, 3);
            ItemHandlerHelper.giveItemToPlayer(customer, new ItemStack(ModItems.EIGHTGATES.get()));
            ProcedureUtils.grantAdvancement(customer, "narutomod:openedgates", true);
        } else {
            broadcastGuyMessage(level, "chattext.mightguy.interact4");
            if (customer != null) {
                addVillageReputation(customer, -3);
            }
        }
        if (customer != null) {
            customer.displayClientMessage(Component.literal("Villagers killed: " + villagersKilled
                    + ", your kills: " + this.customerKillCount
                    + ", Might Guy's kills: " + this.guyKillCount
                    + ", reputation: " + villageReputation(customer)), false);
        }
        removeSiegeScoreboard(level);
        clearVillageSiegeQuest();
    }

    private void clearVillageSiegeQuest() {
        this.villageSiegeQuestActive = false;
        this.villageSiegeTrackingStarted = false;
        this.villageSiegeCustomerId = null;
        this.villageSiegeCustomerName = "";
        this.villageSiegeStartTime = 0L;
        this.villageSiegeCenter = BlockPos.ZERO;
        this.villageSiegeRadius = DEFAULT_SIEGE_RADIUS;
        this.siegeStartingVillagers = 0;
        this.customerKillCount = 0;
        this.guyKillCount = 0;
    }

    private void recordGuyKill() {
        if (!this.villageSiegeTrackingStarted) {
            return;
        }
        this.guyKillCount++;
        updateSiegeScore(this.getScoreboardName(), this.guyKillCount);
    }

    private void recordCustomerKill(ServerPlayer player) {
        if (!this.villageSiegeTrackingStarted || !player.getUUID().equals(this.villageSiegeCustomerId)) {
            return;
        }
        this.customerKillCount++;
        updateSiegeScore(player.getScoreboardName(), this.customerKillCount);
    }

    private void updateSiegeScore(String name, int value) {
        Objective objective = this.level().getScoreboard().getObjective(SIEGE_OBJECTIVE_NAME);
        if (objective != null) {
            this.level().getScoreboard().getOrCreatePlayerScore(name, objective).setScore(value);
        }
    }

    private void sendVillageSiegeStatus(ServerPlayer player) {
        String owner = this.villageSiegeCustomerName.isEmpty() ? "unknown" : this.villageSiegeCustomerName;
        player.displayClientMessage(Component.literal("Might Guy village defense: customer=" + owner
                + ", tracking=" + this.villageSiegeTrackingStarted
                + ", start=" + this.villageSiegeStartTime
                + ", center=" + this.villageSiegeCenter.getX() + "," + this.villageSiegeCenter.getY() + "," + this.villageSiegeCenter.getZ()
                + ", villagers_start=" + this.siegeStartingVillagers
                + ", your_kills=" + this.customerKillCount
                + ", might_guy_kills=" + this.guyKillCount
                + ", reputation=" + villageReputation(player)), false);
    }

    private static int villageReputation(ServerPlayer player) {
        return player.getPersistentData().getInt(VILLAGE_REPUTATION_TAG);
    }

    private static void addVillageReputation(ServerPlayer player, int delta) {
        int reputation = Mth.clamp(villageReputation(player) + delta, -30, 30);
        player.getPersistentData().putInt(VILLAGE_REPUTATION_TAG, reputation);
    }

    private static long nextVillageSiegeStart(ServerLevel level) {
        if (!isLegacyDaytime(level)) {
            return level.getGameTime() + 20L;
        }
        long dayTime = Math.floorMod(level.getDayTime(), 24000L);
        long ticksUntilNight = dayTime < 18000L ? 18000L - dayTime : 24000L - dayTime + 18000L;
        return level.getGameTime() + ticksUntilNight;
    }

    private static boolean isLegacyDaytime(ServerLevel level) {
        long dayTime = Math.floorMod(level.getDayTime(), 24000L);
        return dayTime < 13000L || dayTime > 23000L;
    }

    private static void broadcastGuyMessage(ServerLevel level, String translationKey) {
        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.translatable("entity.narutomod.mightguy").withStyle(ChatFormatting.GREEN)
                        .append(Component.literal(": ").withStyle(ChatFormatting.WHITE))
                        .append(Component.translatable(translationKey).withStyle(ChatFormatting.WHITE)),
                false);
    }

    private static void removeSiegeScoreboard(ServerLevel level) {
        Objective objective = level.getScoreboard().getObjective(SIEGE_OBJECTIVE_NAME);
        if (objective != null) {
            level.getScoreboard().removeObjective(objective);
        }
    }

    public static NaturalVillageSpawnResult tryNaturalVillageSpawn(ServerPlayer player, boolean force) {
        ServerLevel level = player.serverLevel();
        if (!force && level.getServer().overworld().getGameTime() % NATURAL_PLAYER_TICK_SPAWN_INTERVAL != 0L) {
            return NaturalVillageSpawnResult.skipped("cadence", player.blockPosition());
        }
        if (!force && !ProcedureUtils.advancementAchieved(player, NINJA_ADVANCEMENT)) {
            return NaturalVillageSpawnResult.skipped("missing_ninjaachievement", player.blockPosition());
        }
        if (!force && !VillagePoiHelper.isSavannaOrTaiga(level, player.blockPosition())) {
            return NaturalVillageSpawnResult.skipped("wrong_biome", player.blockPosition());
        }
        VillagePoiHelper.Context village = VillagePoiHelper.findNaturalMightGuyContext(level, player.blockPosition()).orElse(null);
        if (village == null) {
            return NaturalVillageSpawnResult.skipped("no_large_poi_village", player.blockPosition());
        }
        int nearbyGuys = level.getEntitiesOfClass(
                MightGuyEntity.class,
                new AABB(village.center()).inflate(NATURAL_DUPLICATE_SEARCH_RADIUS, 256.0D, NATURAL_DUPLICATE_SEARCH_RADIUS),
                MightGuyEntity::isAlive).size();
        if (nearbyGuys > 0) {
            return NaturalVillageSpawnResult.skipped("nearby_might_guy", village.center(), village, null, nearbyGuys);
        }
        BlockPos spawnPos = VillagePoiHelper.findNaturalSpawnPos(level, village, level.random).orElse(null);
        if (spawnPos == null) {
            return NaturalVillageSpawnResult.skipped("no_surface_spawn", village.center(), village, null, nearbyGuys);
        }
        MightGuyEntity guy = ModEntityTypes.MIGHTGUY.get().create(level);
        if (guy == null) {
            return NaturalVillageSpawnResult.skipped("create_failed", village.center(), village, spawnPos, nearbyGuys);
        }
        guy.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                level.random.nextFloat() * 360.0F, 0.0F);
        guy.setPersistenceRequired();
        guy.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null, null);
        boolean spawned = level.addFreshEntity(guy);
        return new NaturalVillageSpawnResult(spawned, spawned ? "spawned" : "add_failed", village.center(), spawnPos,
                village.villagerCount(), village.meetingPoiCount(), village.villagePoiCount(), nearbyGuys);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            tryNaturalVillageSpawn(player, false);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        Entity killer = event.getSource().getEntity();
        if (killer instanceof MightGuyEntity guy) {
            guy.recordGuyKill();
        } else if (killer instanceof ServerPlayer player) {
            recordPlayerKillForVillageSiege(player);
        }
    }

    private static void recordPlayerKillForVillageSiege(ServerPlayer player) {
        for (MightGuyEntity guy : player.serverLevel().getEntitiesOfClass(
                MightGuyEntity.class,
                player.getBoundingBox().inflate(MAX_SIEGE_RADIUS + 16.0D),
                MightGuyEntity::isActiveVillageSiegeFor)) {
            guy.recordCustomerKill(player);
        }
    }

    private boolean isActiveVillageSiegeFor() {
        return this.villageSiegeQuestActive && this.villageSiegeCustomerId != null;
    }

    private boolean hasLegacyVillageHome() {
        if (this.villageSiegeQuestActive) {
            return true;
        }
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return this.legacyVillageHome;
        }
        long gameTime = serverLevel.getGameTime();
        if (gameTime >= this.nextLegacyVillageHomeCheckTick) {
            this.nextLegacyVillageHomeCheckTick = gameTime + 70L + this.getRandom().nextInt(50);
            this.legacyVillageHome = VillagePoiHelper.findQuestContext(serverLevel, blockPosition()).isPresent();
        }
        return this.legacyVillageHome;
    }

    public record NaturalVillageSpawnResult(boolean spawned, String reason, BlockPos villageCenter, BlockPos spawnPos,
            int villagerCount, long meetingPoiCount, long villagePoiCount, int nearbyMightGuys) {
        private static NaturalVillageSpawnResult skipped(String reason, BlockPos pos) {
            return skipped(reason, pos, null, null, 0);
        }

        private static NaturalVillageSpawnResult skipped(String reason, BlockPos pos,
                @Nullable VillagePoiHelper.Context village, @Nullable BlockPos spawnPos, int nearbyMightGuys) {
            return new NaturalVillageSpawnResult(false, reason, pos, spawnPos,
                    village == null ? 0 : village.villagerCount(),
                    village == null ? 0L : village.meetingPoiCount(),
                    village == null ? 0L : village.villagePoiCount(),
                    nearbyMightGuys);
        }
    }

    private void updateGateState() {
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            this.setSwingingArms(!hasLegacyVillageHome()
                    && getGateOpened() >= 7.0F
                    && distanceToSqr(target) >= HIRUDORA_MIN_DISTANCE_SQR);
            if (this.gateCooldown <= 0) {
                float desiredGate = desiredGateFor(target);
                if (desiredGate >= 1.0F) {
                    openGate(desiredGate);
                }
            } else {
                this.gateCooldown--;
            }
            return;
        }

        this.setSwingingArms(false);
        if (this.closeGatesCountdown-- <= 0) {
            closeGates();
        }
        if (this.gateCooldown > 0) {
            this.gateCooldown--;
        }
    }

    private float desiredGateFor(LivingEntity target) {
        float desired = this.getHealth() < this.getMaxHealth() * 0.5F ? 3.5F : 0.0F;
        float targetAttack = (float) ProcedureUtils.getModifiedAttackDamage(target);
        if (targetAttack > 10.0D || target.getMaxHealth() >= 50.0F) {
            double chakraLevel = Chakra.getLevel(target);
            desired = Math.max(desired, (float) Math.sqrt((targetAttack + chakraLevel) * (target.getMaxHealth() + chakraLevel)) / 25.0F);
        }
        return Math.min(desired, 7.0F);
    }

    private void openGate(float gate) {
        ItemStack stack = this.getMainHandItem();
        if (!(stack.getItem() instanceof EightGatesItem)) {
            equipEightGates();
            stack = this.getMainHandItem();
        }
        float currentGate = getGateOpened();
        if (this.getHealth() < this.getMaxHealth() * 0.9F) {
            if (gate > 4.0F && currentGate < 4.0F) {
                gate = 3.5F;
            } else if (this.getHealth() < 4.0F && currentGate >= 4.0F) {
                closeGates();
                return;
            }
        }
        float nextGate = Math.min(gate, 7.0F);
        if (currentGate < nextGate && stack.getItem() instanceof EightGatesItem) {
            stack.getOrCreateTag().putFloat(EightGatesItem.GATE_OPENED_TAG, nextGate);
            this.setShiftKeyDown(true);
        }
        this.closeGatesCountdown = GATE_CLOSE_TICKS;
    }

    private void closeGates() {
        ItemStack stack = this.getMainHandItem();
        if (stack.getItem() instanceof EightGatesItem && getGateOpened() > 0.0F) {
            stack.getOrCreateTag().putFloat(EightGatesItem.GATE_OPENED_TAG, 0.0F);
            this.gateCooldown = GATE_REOPEN_COOLDOWN;
        }
        this.setShiftKeyDown(false);
    }

    private void equipEightGates() {
        ItemStack stack = new ItemStack(ModItems.EIGHTGATES.get());
        if (stack.getItem() instanceof EightGatesItem eightGatesItem) {
            eightGatesItem.bindOwner(stack, this);
            eightGatesItem.setBattleXP(stack, EIGHT_GATES_FULL_XP);
        }
        this.setItemSlot(EquipmentSlot.MAINHAND, stack);
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    private void clampVelocity(double maxSpeed) {
        Vec3 movement = getDeltaMovement();
        if (movement.length() > maxSpeed) {
            Vec3 clamped = movement.scale(maxSpeed / movement.length());
            ProcedureUtils.setVelocity(this, clamped.x(), clamped.y(), clamped.z());
        }
    }
}
