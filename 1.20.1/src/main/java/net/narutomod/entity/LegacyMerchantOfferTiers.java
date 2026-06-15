package net.narutomod.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

final class LegacyMerchantOfferTiers {
    private static final int RECIPE_RESET_TICKS = 6000;
    private static final String REPUTATION_TAG = "NarutomodLegacyMerchantReputation";
    private static final String TIER_0_OFFERS_TAG = "OffersTier0";
    private static final String TIER_1_OFFERS_TAG = "OffersTier1";
    private static final String OFFER_MAX_USES_TAG = "maxUses";

    private final Supplier<MerchantOffers> tier0Factory;
    private final Supplier<MerchantOffers> tier1Factory;
    private final Set<UUID> hostilePlayers = new HashSet<>();
    private MerchantOffers tier0Offers;
    private MerchantOffers tier1Offers;
    private int recipeResetTime;
    private int tradeSoundCooldown;

    LegacyMerchantOfferTiers(Supplier<MerchantOffers> tier0Factory, Supplier<MerchantOffers> tier1Factory) {
        this.tier0Factory = tier0Factory;
        this.tier1Factory = tier1Factory;
    }

    MerchantOffers getOffers(@Nullable Player player) {
        return tradeTier(player) == 0 ? tier0Offers() : tier1Offers();
    }

    void overrideOffers(@Nullable Player player, MerchantOffers offers) {
        if (tradeTier(player) == 0) {
            this.tier0Offers = offers;
        } else {
            this.tier1Offers = offers;
        }
    }

    void notifyTrade(@Nullable Player player, MerchantOffer offer) {
        offer.increaseUses();
        if (offer.isOutOfStock() && this.recipeResetTime <= 0) {
            this.recipeResetTime = RECIPE_RESET_TICKS;
        }
        adjustReputation(player, 1);
    }

    boolean canTradeWith(Player player) {
        return !this.hostilePlayers.contains(player.getUUID());
    }

    static boolean isVillageTargetPlayer(Player player) {
        return reputation(player) <= -15;
    }

    boolean canOpenTradeScreen(Player player, InteractionHand hand, @Nullable Player tradingPlayer) {
        return tradingPlayer == null
                && player.getItemInHand(hand).isEmpty()
                && !player.isShiftKeyDown()
                && canTradeWith(player);
    }

    boolean tickTrading(Mob merchant, @Nullable Player tradingPlayer) {
        if (this.tradeSoundCooldown > 0) {
            --this.tradeSoundCooldown;
        }
        if (tradingPlayer == null && this.recipeResetTime > 0 && --this.recipeResetTime <= 0) {
            restockOutOfStockOffers();
        }
        if (tradingPlayer == null) {
            return false;
        }
        if (!merchant.isAlive()
                || merchant.isInWater()
                || !merchant.onGround()
                || merchant.hasImpulse
                || !tradingPlayer.isAlive()
                || tradingPlayer.containerMenu == tradingPlayer.inventoryMenu
                || merchant.distanceToSqr(tradingPlayer) > 16.0D) {
            return false;
        }
        merchant.getNavigation().stop();
        merchant.getLookControl().setLookAt(tradingPlayer, 30.0F, 30.0F);
        return true;
    }

    void notifyTradeUpdated(Mob merchant, ItemStack stack, float volume, float pitch) {
        if (!merchant.level().isClientSide && this.tradeSoundCooldown <= 0) {
            this.tradeSoundCooldown = 20;
            merchant.playSound(stack.isEmpty() ? SoundEvents.VILLAGER_NO : SoundEvents.VILLAGER_YES, volume, pitch);
        }
    }

    void markHostile(Player player) {
        this.hostilePlayers.add(player.getUUID());
        adjustReputation(player, -1);
    }

    void penalizeDeath(@Nullable Player player) {
        adjustReputation(player, -5);
    }

    private static void adjustReputation(@Nullable Player player, int delta) {
        if (player != null) {
            CompoundTag data = player.getPersistentData();
            data.putInt(REPUTATION_TAG, Mth.clamp(data.getInt(REPUTATION_TAG) + delta, -30, 30));
        }
    }

    void addAdditionalSaveData(CompoundTag tag) {
        if (this.tier0Offers != null) {
            tag.put(TIER_0_OFFERS_TAG, this.tier0Offers.createTag());
        }
        if (this.tier1Offers != null) {
            tag.put(TIER_1_OFFERS_TAG, this.tier1Offers.createTag());
        }
    }

    void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains(TIER_0_OFFERS_TAG)) {
            this.tier0Offers = new MerchantOffers(tag.getCompound(TIER_0_OFFERS_TAG));
        }
        if (tag.contains(TIER_1_OFFERS_TAG)) {
            this.tier1Offers = new MerchantOffers(tag.getCompound(TIER_1_OFFERS_TAG));
        }
    }

    private MerchantOffers tier0Offers() {
        if (this.tier0Offers == null) {
            this.tier0Offers = this.tier0Factory.get();
        }
        return this.tier0Offers;
    }

    private MerchantOffers tier1Offers() {
        if (this.tier1Offers == null) {
            this.tier1Offers = this.tier1Factory.get();
        }
        return this.tier1Offers;
    }

    private void restockOutOfStockOffers() {
        restockOutOfStockOffers(this.tier0Offers);
        restockOutOfStockOffers(this.tier1Offers);
    }

    private static void restockOutOfStockOffers(@Nullable MerchantOffers offers) {
        if (offers == null) {
            return;
        }
        for (int i = 0; i < offers.size(); i++) {
            MerchantOffer offer = offers.get(i);
            if (offer.isOutOfStock()) {
                CompoundTag tag = offer.createTag();
                tag.putInt(OFFER_MAX_USES_TAG, offer.getMaxUses() + 1);
                offers.set(i, new MerchantOffer(tag));
            }
        }
    }

    private static int tradeTier(@Nullable Player player) {
        return Mth.clamp(reputation(player) / 3, 0, 1);
    }

    private static int reputation(@Nullable Player player) {
        return player == null ? 0 : player.getPersistentData().getInt(REPUTATION_TAG);
    }
}
