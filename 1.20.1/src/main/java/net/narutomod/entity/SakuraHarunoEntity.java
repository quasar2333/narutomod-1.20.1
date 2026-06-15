package net.narutomod.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.narutomod.item.IryoJutsuItem;
import net.narutomod.item.JutsuItem;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModItems;

public final class SakuraHarunoEntity extends NinjaMobEntity implements Merchant {
    private static final double HEAL_DISTANCE_SQR = 4.0D;
    private static final float HEAL_START_RATIO = 0.4F;
    private static final float HEAL_STOP_RATIO = 0.9F;
    private static final float HEAL_AMOUNT = 4.0F;

    private final List<LivingEntity> healableEntities = new ArrayList<>();
    private Player tradingPlayer;
    private final LegacyMerchantOfferTiers offerTiers =
            new LegacyMerchantOfferTiers(SakuraHarunoEntity::createSakuraTier0Offers, SakuraHarunoEntity::createSakuraTier1Offers);
    private final LegacyMerchantVillageBehavior villageBehavior = new LegacyMerchantVillageBehavior();
    @Nullable
    private LivingEntity healingTarget;

    public SakuraHarunoEntity(EntityType<? extends SakuraHarunoEntity> entityType, Level level) {
        super(entityType, level);
        syncLegacyHealingMainHand();
        this.villageBehavior.configureNavigation(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return NinjaMobEntity.createAttributes(60.0D, 0.0D, 0.5D, 10.0D, 48.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new HealNearbyGoal(this, 1.2D));
        this.goalSelector.addGoal(2, new LegacyNinjaLeapAtTargetGoal(this, 1.0F));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(9, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(10, new MoveTowardsRestrictionGoal(this, 0.8D));
        this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 3.0F));
        this.goalSelector.addGoal(12, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(13, new LookAtPlayerGoal(this, LivingEntity.class, 8.0F));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Mob.class, 10, false, false,
                target -> LegacyMerchantVillageBehavior.isDefendVillageTarget(this, target)));
        this.targetSelector.addGoal(1, LegacyMerchantVillageBehavior.targetPlayerGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Zombie.class, false) {
            @Override
            public boolean canUse() {
                return SakuraHarunoEntity.this.hasRestriction() && super.canUse();
            }
        });
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Creeper.class, false) {
            @Override
            public boolean canUse() {
                return SakuraHarunoEntity.this.hasRestriction() && super.canUse();
            }
        });
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData, dataTag);
        syncLegacyHealingMainHand();
        return result;
    }

    @Override
    public void tick() {
        super.tick();
        syncLegacyHealingMainHand();
        if (!this.level().isClientSide) {
            this.villageBehavior.tickHomeRestriction(this);
            tickLegacyTrading();
            if (this.tickCount % 20 == 0) {
                if (this.getHealth() < this.getMaxHealth()) {
                    this.heal(10.0F);
                }
                this.addEffect(new MobEffectInstance(ModEffects.CHAKRA_ENHANCED_STRENGTH.get(), 21, 10, false, false));
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!source.is(DamageTypes.FELL_OUT_OF_WORLD) && amount >= this.getHealth()) {
            amount = Math.max(this.getHealth() - 1.0F, 0.0F);
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && source.getEntity() instanceof Player player) {
            this.offerTiers.markHostile(player);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        if (source.getEntity() instanceof Player player) {
            this.offerTiers.penalizeDeath(player);
        }
        super.die(source);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt) {
            ProcedureUtils.pushEntity(this, target, 10.0D, 1.5F);
        }
        return hurt;
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        return isKonohaAlly(entity) || this.healableEntities.contains(entity) || super.isAlliedTo(entity);
    }

    @Override
    protected boolean usesLegacyMerchantBaseGoals() {
        return true;
    }

    @Override
    public float getVoicePitch() {
        return (this.getRandom().nextFloat() - this.getRandom().nextFloat()) * 0.2F + 2.4F;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.isAlive() && this.offerTiers.canOpenTradeScreen(player, hand, this.tradingPlayer)) {
            if (!this.level().isClientSide) {
                this.setTradingPlayer(player);
                this.openTradingScreen(player, this.getDisplayName(), 1);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        this.tradingPlayer = player;
    }

    @Nullable
    @Override
    public Player getTradingPlayer() {
        return this.tradingPlayer;
    }

    @Override
    public MerchantOffers getOffers() {
        return this.offerTiers.getOffers(this.tradingPlayer);
    }

    @Override
    public void overrideOffers(MerchantOffers offers) {
        this.offerTiers.overrideOffers(this.tradingPlayer, offers);
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        this.offerTiers.notifyTrade(this.tradingPlayer, offer);
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
        this.offerTiers.notifyTradeUpdated(this, stack, this.getSoundVolume(), this.getVoicePitch());
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xp) {
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }

    @Override
    public boolean isClientSide() {
        return this.level().isClientSide;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        this.offerTiers.addAdditionalSaveData(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.offerTiers.readAdditionalSaveData(tag);
        syncLegacyHealingMainHand();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GENERIC_HURT;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.tradingPlayer != null ? SoundEvents.VILLAGER_TRADE : null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GENERIC_DEATH;
    }

    @Nullable
    public LivingEntity getHealingTarget() {
        return this.healingTarget;
    }

    public void addHealableEntity(LivingEntity entity) {
        if (!this.healableEntities.contains(entity)) {
            this.healableEntities.add(entity);
        }
    }

    private void setHealingTarget(@Nullable LivingEntity target) {
        this.healingTarget = target;
        syncLegacyHealingMainHand();
    }

    private void tickLegacyTrading() {
        if (!this.offerTiers.tickTrading(this, this.tradingPlayer) && this.tradingPlayer != null) {
            this.setTradingPlayer(null);
        }
    }

    private boolean canHealEntity(LivingEntity entity) {
        return this.healableEntities.contains(entity)
                || isKonohaAlly(entity)
                || entity instanceof Villager;
    }

    private boolean canStartHealing(LivingEntity entity) {
        return entity != this
                && entity.isAlive()
                && entity.getHealth() < entity.getMaxHealth() * HEAL_START_RATIO
                && canHealEntity(entity)
                && isInHealFollowRange(entity);
    }

    private static boolean shouldContinueHealing(LivingEntity entity) {
        return entity.isAlive() && entity.getHealth() < entity.getMaxHealth() * HEAL_STOP_RATIO;
    }

    private boolean isInHealFollowRange(LivingEntity entity) {
        double range = this.getAttributeValue(Attributes.FOLLOW_RANGE);
        return this.distanceToSqr(entity) <= range * range;
    }

    private static boolean isKonohaAlly(Entity entity) {
        return entity instanceof SakuraHarunoEntity
                || entity instanceof IrukaSenseiEntity
                || entity instanceof TentenEntity
                || entity instanceof MightGuyEntity;
    }

    private static MerchantOffers createSakuraTier0Offers() {
        MerchantOffers result = new MerchantOffers();
        result.add(new MerchantOffer(new ItemStack(Items.EMERALD), new ItemStack(Items.BAKED_POTATO, 3), 1, 0, 0.0F));
        result.add(new MerchantOffer(new ItemStack(Items.EMERALD), new ItemStack(ModItems.MILITARY_RATIONS_PILL.get(), 2), 1, 0, 0.0F));
        return result;
    }

    private static MerchantOffers createSakuraTier1Offers() {
        MerchantOffers result = new MerchantOffers();
        result.add(new MerchantOffer(new ItemStack(Items.EMERALD, 5), new ItemStack(ModItems.SCROLL_HEALING.get()), 1, 0, 0.0F));
        result.add(new MerchantOffer(new ItemStack(Items.EMERALD, 5), new ItemStack(ModItems.MILITARY_RATIONS_PILL_GOLD.get()), 1, 0, 0.0F));
        return result;
    }

    private void syncLegacyHealingMainHand() {
        boolean shouldHoldHealing = this.healingTarget != null;
        ItemStack mainHand = this.getItemBySlot(EquipmentSlot.MAINHAND);
        boolean holdingHealing = mainHand.is(ModItems.IRYO_JUTSU.get());
        if (shouldHoldHealing && !holdingHealing) {
            this.stopUsingItem();
            this.setItemSlot(EquipmentSlot.MAINHAND, createHealingJutsuStack());
        } else if (!shouldHoldHealing && holdingHealing) {
            this.stopUsingItem();
            this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    private ItemStack createHealingJutsuStack() {
        ItemStack stack = new ItemStack(ModItems.IRYO_JUTSU.get());
        if (stack.getItem() instanceof IryoJutsuItem iryoJutsuItem) {
            JutsuItem.setOwner(stack, this);
            stack.getOrCreateTag().putInt(JutsuItem.JUTSU_INDEX_TAG, IryoJutsuItem.HEALING.index());
            iryoJutsuItem.enableJutsu(stack, IryoJutsuItem.HEALING, true);
            iryoJutsuItem.setJutsuXp(stack, IryoJutsuItem.HEALING, iryoJutsuItem.getRequiredXp(stack, IryoJutsuItem.HEALING));
        }
        return stack;
    }

    private static final class HealNearbyGoal extends Goal {
        private final SakuraHarunoEntity healer;
        private final double speed;
        @Nullable
        private LivingEntity target;
        @Nullable
        private Vec3 lastTargetPosition;
        private int navigationDelay;
        private int healCooldown;

        private HealNearbyGoal(SakuraHarunoEntity healer, double speed) {
            this.healer = healer;
            this.speed = speed;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity livingEntity = this.healer.getHealingTarget();
            if (livingEntity == null) {
                if (!this.healer.hasRestriction() || this.healer.getRandom().nextInt(40) != 0) {
                    return false;
                }
                double radius = this.healer.getRestrictRadius();
                AABB searchBox = new AABB(this.healer.getRestrictCenter()).inflate(radius, 8.0D, radius);
                livingEntity = this.healer.level()
                        .getEntitiesOfClass(LivingEntity.class, searchBox, this.healer::canStartHealing)
                        .stream()
                        .min(Comparator.comparingDouble(this.healer::distanceToSqr))
                        .orElse(null);
            }
            this.target = livingEntity != null && this.healer.canStartHealing(livingEntity) ? livingEntity : null;
            return this.target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.target != null
                    && this.healer.canHealEntity(this.target)
                    && shouldContinueHealing(this.target)
                    && this.healer.isInHealFollowRange(this.target);
        }

        @Override
        public void start() {
            this.navigationDelay = 0;
            this.healCooldown = 0;
            this.lastTargetPosition = null;
            this.healer.setHealingTarget(this.target);
            if (this.target != null) {
                this.healer.getNavigation().moveTo(this.target, this.speed);
            }
        }

        @Override
        public void stop() {
            this.target = null;
            this.lastTargetPosition = null;
            this.healer.setHealingTarget(null);
            this.healer.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (this.target == null) {
                return;
            }
            Vec3 lookPosition = this.target.getEyePosition().subtract(0.0D, 0.3D, 0.0D);
            this.healer.getLookControl().setLookAt(lookPosition.x, lookPosition.y, lookPosition.z, 30.0F, 30.0F);
            if (--this.navigationDelay <= 0
                    && (this.lastTargetPosition == null
                            || this.target.position().distanceToSqr(this.lastTargetPosition) >= 1.0D
                            || this.healer.getRandom().nextFloat() < 0.05F)) {
                this.navigationDelay = this.healer.getRandom().nextInt(7) + 4;
                this.lastTargetPosition = this.target.position();
                double movementSpeed = this.healer.distanceToSqr(this.target) < 9.0D ? this.speed * 0.4D : this.speed;
                if (!this.healer.getNavigation().moveTo(this.target, movementSpeed)) {
                    this.navigationDelay += 15;
                }
            }
            if (this.healer.distanceToSqr(this.target) <= HEAL_DISTANCE_SQR && --this.healCooldown <= 0) {
                this.healCooldown = 10;
                this.target.heal(HEAL_AMOUNT);
                this.healer.swing(InteractionHand.MAIN_HAND);
            }
        }
    }
}
