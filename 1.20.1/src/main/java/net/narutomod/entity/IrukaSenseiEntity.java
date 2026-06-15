package net.narutomod.entity;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.narutomod.registry.ModItems;

public final class IrukaSenseiEntity extends NinjaMobEntity implements Merchant {
    private Player tradingPlayer;
    private final LegacyMerchantOfferTiers offerTiers =
            new LegacyMerchantOfferTiers(IrukaSenseiEntity::createIrukaTier0Offers, IrukaSenseiEntity::createIrukaTier1Offers);
    private final LegacyMerchantVillageBehavior villageBehavior = new LegacyMerchantVillageBehavior();

    public IrukaSenseiEntity(EntityType<? extends IrukaSenseiEntity> entityType, Level level) {
        super(entityType, level);
        syncLegacyCombatMainHand();
        this.villageBehavior.configureNavigation(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return NinjaMobEntity.createAttributes(20.0D, 0.0D, 0.5D, 5.0D, 48.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
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
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Zombie.class, false));
    }

    @Override
    public void tick() {
        super.tick();
        syncLegacyCombatMainHand();
        this.villageBehavior.tickHomeRestriction(this);
        tickLegacyTrading();
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData, dataTag);
        syncLegacyCombatMainHand();
        return result;
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        return entity instanceof TentenEntity
                || entity instanceof SakuraHarunoEntity
                || entity instanceof IrukaSenseiEntity
                || entity instanceof MightGuyEntity
                || super.isAlliedTo(entity);
    }

    @Override
    protected boolean usesLegacyMerchantBaseGoals() {
        return true;
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
    public boolean hurt(DamageSource source, float amount) {
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
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.tradingPlayer != null ? SoundEvents.VILLAGER_TRADE : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GENERIC_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GENERIC_DEATH;
    }

    private void tickLegacyTrading() {
        if (!this.level().isClientSide
                && !this.offerTiers.tickTrading(this, this.tradingPlayer)
                && this.tradingPlayer != null) {
            this.setTradingPlayer(null);
        }
    }

    private static MerchantOffers createIrukaTier0Offers() {
        MerchantOffers result = new MerchantOffers();
        result.add(new MerchantOffer(new ItemStack(Items.EMERALD, 3), new ItemStack(Items.GOLDEN_APPLE), 1, 0, 0.0F));
        result.add(new MerchantOffer(new ItemStack(Items.EMERALD), new ItemStack(ModItems.SCROLL_BODY_REPLACEMENT.get()), 1, 0, 0.0F));
        return result;
    }

    private static MerchantOffers createIrukaTier1Offers() {
        MerchantOffers result = new MerchantOffers();
        result.add(new MerchantOffer(new ItemStack(Items.EMERALD, 5), new ItemStack(ModItems.SCROLL_KAGE_BUNSHIN.get()), 1, 0, 0.0F));
        result.add(new MerchantOffer(new ItemStack(Items.EMERALD, 30), new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), 1, 0, 0.0F));
        return result;
    }

    private void syncLegacyCombatMainHand() {
        boolean shouldHoldKunai = hasLegacyCombatActivity();
        ItemStack mainHand = this.getItemBySlot(EquipmentSlot.MAINHAND);
        if (shouldHoldKunai && mainHand.isEmpty()) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.KUNAI.get()));
        } else if (!shouldHoldKunai && mainHand.is(ModItems.KUNAI.get())) {
            this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    private boolean hasLegacyCombatActivity() {
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            return true;
        }
        LivingEntity attacker = this.getLastHurtByMob();
        if (attacker != null && attacker.isAlive()) {
            return true;
        }
        LivingEntity lastHurt = this.getLastHurtMob();
        return lastHurt != null && lastHurt.isAlive() && this.tickCount <= this.getLastHurtMobTimestamp() + 100;
    }
}
