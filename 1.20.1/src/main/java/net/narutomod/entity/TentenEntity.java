package net.narutomod.entity;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.narutomod.item.NinjaToolItem;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModBlocks;
import net.narutomod.registry.ModItems;

public final class TentenEntity extends NinjaMobEntity implements RangedAttackMob, Merchant {
    private static final double RANGED_MIN_DISTANCE_SQR = 16.0D;
    private Player tradingPlayer;
    private final LegacyMerchantOfferTiers offerTiers =
            new LegacyMerchantOfferTiers(TentenEntity::createTentenTier0Offers, TentenEntity::createTentenTier1Offers);
    private final LegacyMerchantVillageBehavior villageBehavior = new LegacyMerchantVillageBehavior();

    public TentenEntity(EntityType<? extends TentenEntity> entityType, Level level) {
        super(entityType, level);
        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        this.villageBehavior.configureNavigation(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return NinjaMobEntity.createAttributes(10.0D, 0.0D, 0.5D, 3.0D, 48.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RangedAttackGoal(this, 1.0D, 15, 15.0F) {
            @Override
            public boolean canUse() {
                LivingEntity target = TentenEntity.this.getTarget();
                return target != null
                        && TentenEntity.this.distanceToSqr(target) >= RANGED_MIN_DISTANCE_SQR
                        && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                LivingEntity target = TentenEntity.this.getTarget();
                return target != null
                        && TentenEntity.this.distanceToSqr(target) >= RANGED_MIN_DISTANCE_SQR
                        && super.canContinueToUse();
            }
        });
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
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Skeleton.class, false));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Zombie.class, false));
    }

    @Override
    public void tick() {
        super.tick();
        this.villageBehavior.tickHomeRestriction(this);
        tickLegacyTrading();
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target);
        syncLegacyMainHand(target);
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
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        this.swing(InteractionHand.MAIN_HAND);
        if (this.level().isClientSide) {
            return;
        }
        ThrownNinjaToolEntity projectile = ModEntityTypes.ENTITYBULLETSHURIKEN.get().create(this.level());
        if (projectile == null) {
            return;
        }
        projectile.configure(this, (NinjaToolItem) ModItems.SHURIKEN.get(), false);
        projectile.setBaseDamage(5.0F);
        projectile.setKnockbackStrength(1);
        projectile.moveTo(this.getX(), this.getEyeY() - 0.1D, this.getZ(), this.getYRot(), this.getXRot());
        double dx = target.getX() - projectile.getX();
        double dy = target.getY(0.3333333333333333D) - projectile.getY();
        double dz = target.getZ() - projectile.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        projectile.shoot(dx, dy + horizontal * 0.2D, dz, 1.6F, 0.0F);
        this.level().addFreshEntity(projectile);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ARROW_SHOOT,
                SoundSource.NEUTRAL, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.5F + 1.0F) + 0.5F);
    }

    private void syncLegacyMainHand(@Nullable LivingEntity target) {
        this.setItemSlot(EquipmentSlot.MAINHAND, target == null ? ItemStack.EMPTY : new ItemStack(ModItems.KUNAI.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
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
    protected SoundEvent getAmbientSound() {
        return this.tradingPlayer != null ? SoundEvents.VILLAGER_TRADE : null;
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

    private static MerchantOffers createTentenTier0Offers() {
        MerchantOffers result = new MerchantOffers();
        result.add(new MerchantOffer(new ItemStack(Items.EMERALD), new ItemStack(ModItems.SHURIKEN.get(), 24), 1, 0, 0.0F));
        result.add(new MerchantOffer(new ItemStack(Items.EMERALD), new ItemStack(ModItems.KUNAI.get(), 3), 1, 0, 0.0F));
        result.add(new MerchantOffer(new ItemStack(Items.EMERALD), new ItemStack(ModBlocks.EXPLOSIVE_TAG_ITEM.get(), 3), 1, 0, 0.0F));
        return result;
    }

    private static MerchantOffers createTentenTier1Offers() {
        MerchantOffers result = new MerchantOffers();
        result.add(new MerchantOffer(new ItemStack(Items.EMERALD), new ItemStack(ModItems.KUNAI_EXPLOSIVE.get(), 2), 1, 0, 0.0F));
        result.add(new MerchantOffer(new ItemStack(Items.EMERALD, 3), new ItemStack(ModItems.CHOKUTO.get()), 1, 0, 0.0F));
        return result;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        this.spawnAtLocation(new ItemStack(ModItems.KUNAI.get(), 1 + this.getRandom().nextInt(6)));
    }

    @Override
    public float getVoicePitch() {
        return (this.getRandom().nextFloat() - this.getRandom().nextFloat()) * 0.2F + 3.0F;
    }

    private void tickLegacyTrading() {
        if (!this.level().isClientSide
                && !this.offerTiers.tickTrading(this, this.tradingPlayer)
                && this.tradingPlayer != null) {
            this.setTradingPlayer(null);
        }
    }
}
