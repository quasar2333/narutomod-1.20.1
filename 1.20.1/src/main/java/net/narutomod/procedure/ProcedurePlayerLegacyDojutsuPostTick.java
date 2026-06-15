package net.narutomod.procedure;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.narutomod.NarutomodModVariables;
import net.narutomod.PlayerTracker;
import net.narutomod.entity.BijuManager;
import net.narutomod.item.IntonItem;
import net.narutomod.item.JutsuItem;
import net.narutomod.item.ObitoMangekyoHelmetItem;
import net.narutomod.item.SusanooPowerIncreaseHandler;
import net.narutomod.registry.ModItems;

public final class ProcedurePlayerLegacyDojutsuPostTick {
    private ProcedurePlayerLegacyDojutsuPostTick() {
    }

    public static void apply(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || player.tickCount % 20 != 0) {
            return;
        }

        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (isSharinganHead(head) && ObitoMangekyoHelmetItem.isBlinded(head)) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 1200, 0, false, false));
        }
        cleanupInvalidSusanoo(serverPlayer, head);
        if (!applyRecentDojutsuDeathPenaltyIfNeeded(serverPlayer)) {
            grantCatchupOrRandomAwakeningIfNeeded(serverPlayer);
        }
    }

    private static boolean isSharinganHead(ItemStack stack) {
        return stack.is(ModItems.SHARINGANHELMET.get())
                || stack.is(ModItems.MANGEKYOSHARINGANHELMET.get())
                || stack.is(ModItems.MANGEKYOSHARINGANOBITOHELMET.get())
                || stack.is(ModItems.MANGEKYOSHARINGANETERNALHELMET.get());
    }

    private static void cleanupInvalidSusanoo(ServerPlayer player, ItemStack head) {
        if (!SusanooPowerIncreaseHandler.isActivated(player)) {
            return;
        }

        if (SusanooPowerIncreaseHandler.shouldAutoCloseLegacyBodyTick(player)) {
            SusanooPowerIncreaseHandler.deactivate(player, true);
            return;
        }

        if (hasAnyDojutsu(player)) {
            if (!SusanooPowerIncreaseHandler.isMangekyoHead(head)) {
                SusanooPowerIncreaseHandler.deactivate(player, true);
            }
        } else if (lacksLegacyAdvancedOrJinchuriki(player)) {
            SusanooPowerIncreaseHandler.deactivate(player, true);
        }
    }

    private static boolean applyRecentDojutsuDeathPenaltyIfNeeded(ServerPlayer player) {
        if (hasAnyDojutsu(player) || !lacksLegacyAdvancedOrJinchuriki(player)) {
            return false;
        }
        long mostRecentWornDojutsuTime = NarutomodModVariables.get(player).getLong(NarutomodModVariables.MOST_RECENT_WORN_DOJUTSU_TIME);
        if (PlayerTracker.Deaths.mostRecentTime(player) >= mostRecentWornDojutsuTime) {
            return false;
        }
        if (!player.isCreative()) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 1200, 0, false, false));
        }
        return true;
    }

    private static void grantCatchupOrRandomAwakeningIfNeeded(ServerPlayer player) {
        if (hasAnyDojutsu(player)
                || !lacksLegacyAdvancedOrJinchuriki(player)
                || NarutomodModVariables.getBattleExperience(player) < 300.0D) {
            return;
        }

        ItemStack stack = deterministicCatchupStack(player);
        if (stack.isEmpty()) {
            stack = randomAwakeningStack(player);
        }
        giveLegacyCatchupStack(player, stack);
    }

    private static void giveLegacyCatchupStack(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        if (isDojutsuCatchupStack(stack)) {
            ProcedureUtils.setOriginalOwner(player, stack);
            NarutomodModVariables.get(player).putLong(NarutomodModVariables.MOST_RECENT_WORN_DOJUTSU_TIME, player.level().getGameTime());
            NarutomodModVariables.sync(player);
        } else if (stack.getItem() instanceof JutsuItem) {
            stack.getOrCreateTag().putBoolean(JutsuItem.AFFINITY_TAG, true);
        }
        ItemHandlerHelper.giveItemToPlayer(player, stack);
    }

    private static ItemStack deterministicCatchupStack(ServerPlayer player) {
        if (ProcedureUtils.advancementAchieved(player, "narutomod:byakuganopened")) {
            return new ItemStack(ModItems.BYAKUGANHELMET.get());
        }
        if (ProcedureUtils.advancementAchieved(player, "narutomod:sharinganopened")) {
            grantGenjutsu(player);
            return new ItemStack(ModItems.SHARINGANHELMET.get());
        }
        if (ProcedureUtils.advancementAchieved(player, "narutomod:kekkei_tota_awakened")) {
            return new ItemStack(ModItems.JINTON.get());
        }
        if (ProcedureUtils.advancementAchieved(player, "narutomod:shikotsumyaku_acquired")) {
            return new ItemStack(ModItems.SHIKOTSUMYAKU.get());
        }
        if (ProcedureUtils.advancementAchieved(player, "narutomod:futton_acquired")) {
            return new ItemStack(ModItems.FUTTON.get());
        }
        if (ProcedureUtils.advancementAchieved(player, "narutomod:ranton_acquired")) {
            return new ItemStack(ModItems.RANTON.get());
        }
        if (ProcedureUtils.advancementAchieved(player, "narutomod:yooton_acquired")) {
            return new ItemStack(ModItems.YOOTON.get());
        }
        if (ProcedureUtils.advancementAchieved(player, "narutomod:shakuton_acquired")) {
            return new ItemStack(ModItems.SHAKUTON.get());
        }
        if (ProcedureUtils.advancementAchieved(player, "narutomod:hyoton_acquired")) {
            return new ItemStack(ModItems.HYOTON.get());
        }
        if (ProcedureUtils.advancementAchieved(player, "narutomod:jiton_acquired")) {
            return new ItemStack(ModItems.JITON.get());
        }
        if (ProcedureUtils.advancementAchieved(player, "narutomod:bakuton_acquired")) {
            return new ItemStack(ModItems.BAKUTON.get());
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack randomAwakeningStack(ServerPlayer player) {
        if (player.experienceLevel < 10 || player.getRandom().nextDouble() > 0.001D) {
            return ItemStack.EMPTY;
        }

        double rngbase = 105.0D + (BijuManager.availableTailedBeasts(player.getServer()) > 0 ? 5.0D : 0.0D);
        if (player.getRandom().nextDouble() <= 10.0D / rngbase) {
            grantAwakeningAdvancement(player, "narutomod:byakuganopened");
            return new ItemStack(ModItems.BYAKUGANHELMET.get());
        } else if (player.getRandom().nextDouble() <= 10.0D / (rngbase - 10.0D)) {
            grantGenjutsu(player);
            grantAwakeningAdvancement(player, "narutomod:sharinganopened");
            return new ItemStack(ModItems.SHARINGANHELMET.get());
        } else if (player.getRandom().nextDouble() <= 10.0D / (rngbase - 20.0D)) {
            grantAwakeningAdvancement(player, "narutomod:shikotsumyaku_acquired");
            return new ItemStack(ModItems.SHIKOTSUMYAKU.get());
        } else if (hasAnyItem(player, ModItems.DOTON.get(), ModItems.KATON.get())
                && player.getRandom().nextDouble() <= (10.0D / (rngbase - 30.0D)) / 0.4D) {
            grantAwakeningAdvancement(player, "narutomod:yooton_acquired");
            giveAffinityIfMissing(player, ModItems.KATON.get());
            giveAffinityIfMissing(player, ModItems.DOTON.get());
            return new ItemStack(ModItems.YOOTON.get());
        } else if (hasAnyItem(player, ModItems.FUTON.get(), ModItems.KATON.get())
                && player.getRandom().nextDouble() <= (10.0D / (rngbase - 40.0D)) / 0.4D) {
            grantAwakeningAdvancement(player, "narutomod:shakuton_acquired");
            giveAffinityIfMissing(player, ModItems.FUTON.get());
            giveAffinityIfMissing(player, ModItems.KATON.get());
            return new ItemStack(ModItems.SHAKUTON.get());
        } else if (hasAnyItem(player, ModItems.FUTON.get(), ModItems.SUITON.get())
                && player.getRandom().nextDouble() <= (10.0D / (rngbase - 50.0D)) / 0.4D) {
            grantAwakeningAdvancement(player, "narutomod:hyoton_acquired");
            giveAffinityIfMissing(player, ModItems.FUTON.get());
            giveAffinityIfMissing(player, ModItems.SUITON.get());
            return new ItemStack(ModItems.HYOTON.get());
        } else if (hasAnyItem(player, ModItems.FUTON.get(), ModItems.DOTON.get())
                && player.getRandom().nextDouble() <= (10.0D / (rngbase - 60.0D)) / 0.4D) {
            grantAwakeningAdvancement(player, "narutomod:jiton_acquired");
            giveAffinityIfMissing(player, ModItems.FUTON.get());
            giveAffinityIfMissing(player, ModItems.DOTON.get());
            return new ItemStack(ModItems.JITON.get());
        } else if (hasAnyItem(player, ModItems.RAITON.get(), ModItems.DOTON.get())
                && player.getRandom().nextDouble() <= (10.0D / (rngbase - 70.0D)) / 0.4D) {
            grantAwakeningAdvancement(player, "narutomod:bakuton_acquired");
            giveAffinityIfMissing(player, ModItems.DOTON.get());
            giveAffinityIfMissing(player, ModItems.RAITON.get());
            return new ItemStack(ModItems.BAKUTON.get());
        } else if (hasAnyItem(player, ModItems.RAITON.get(), ModItems.SUITON.get())
                && player.getRandom().nextDouble() <= (10.0D / (rngbase - 80.0D)) / 0.4D) {
            grantAwakeningAdvancement(player, "narutomod:ranton_acquired");
            giveAffinityIfMissing(player, ModItems.RAITON.get());
            giveAffinityIfMissing(player, ModItems.SUITON.get());
            return new ItemStack(ModItems.RANTON.get());
        } else if (hasAnyItem(player, ModItems.SUITON.get(), ModItems.KATON.get())
                && player.getRandom().nextDouble() <= (10.0D / (rngbase - 90.0D)) / 0.4D) {
            grantAwakeningAdvancement(player, "narutomod:futton_acquired");
            giveAffinityIfMissing(player, ModItems.SUITON.get());
            giveAffinityIfMissing(player, ModItems.KATON.get());
            return new ItemStack(ModItems.FUTTON.get());
        } else if (hasAnyItem(player, ModItems.KATON.get(), ModItems.DOTON.get(), ModItems.FUTON.get())
                && player.getRandom().nextDouble() <= (5.0D / (rngbase - 100.0D)) / 0.6D) {
            grantAwakeningAdvancement(player, "narutomod:kekkei_tota_awakened");
            giveAffinityIfMissing(player, ModItems.KATON.get());
            giveAffinityIfMissing(player, ModItems.DOTON.get());
            giveAffinityIfMissing(player, ModItems.FUTON.get());
            return new ItemStack(ModItems.JINTON.get());
        }

        if (rngbase > 105.0D) {
            tryLegacyRandomJinchurikiGrant(player);
        }
        return ItemStack.EMPTY;
    }

    private static void grantAwakeningAdvancement(ServerPlayer player, String advancement) {
        ProcedureUtils.grantAdvancement(player, advancement, true);
    }

    private static void giveAffinityIfMissing(Player player, Item item) {
        if (ProcedureUtils.hasItemInInventory(player, item)) {
            return;
        }
        ItemStack stack = new ItemStack(item);
        if (stack.getItem() instanceof JutsuItem) {
            stack.getOrCreateTag().putBoolean(JutsuItem.AFFINITY_TAG, true);
        }
        ItemHandlerHelper.giveItemToPlayer(player, stack);
    }

    private static boolean hasAnyItem(Player player, Item... items) {
        for (Item item : items) {
            if (ProcedureUtils.hasItemInInventory(player, item)) {
                return true;
            }
        }
        return false;
    }

    private static void tryLegacyRandomJinchurikiGrant(ServerPlayer player) {
        int tails = BijuManager.getRandomAvailableTailedBeast(player.getServer());
        if (tails > 0 && BijuManager.setPlayerAsJinchuriki(player, tails)) {
            player.getServer().getPlayerList().broadcastSystemMessage(
                    Component.translatable(
                            "chattext.biju.playerisjinchuriki",
                            player.getScoreboardName(),
                            Component.translatable("entity.narutomod." + BijuManager.displayName(tails))),
                    false);
        }
    }

    private static void grantGenjutsu(ServerPlayer player) {
        ItemStack stack = ProcedureUtils.getMatchingItemStack(player, ModItems.INTON.get());
        if (stack == null) {
            stack = new ItemStack(ModItems.INTON.get());
            JutsuItem.setOwner(stack, player);
            ItemHandlerHelper.giveItemToPlayer(player, stack);
        }
        if (stack.getItem() instanceof IntonItem intonItem) {
            intonItem.enableJutsu(stack, IntonItem.GENJUTSU, true);
        }
    }

    private static boolean isDojutsuCatchupStack(ItemStack stack) {
        Item item = stack.getItem();
        return item == ModItems.BYAKUGANHELMET.get() || item == ModItems.SHARINGANHELMET.get();
    }

    private static boolean hasAnyDojutsu(Player player) {
        return ProcedureUtils.hasItemInInventory(player, ModItems.BYAKUGANHELMET.get())
                || ProcedureUtils.hasItemInInventory(player, ModItems.BYAKURINNESHARINGANHELMET.get())
                || ProcedureUtils.hasItemInInventory(player, ModItems.SHARINGANHELMET.get())
                || ProcedureUtils.hasItemInInventory(player, ModItems.MANGEKYOSHARINGANHELMET.get())
                || ProcedureUtils.hasItemInInventory(player, ModItems.MANGEKYOSHARINGANOBITOHELMET.get())
                || ProcedureUtils.hasItemInInventory(player, ModItems.MANGEKYOSHARINGANETERNALHELMET.get())
                || ProcedureUtils.hasItemInInventory(player, ModItems.RINNEGANHELMET.get())
                || ProcedureUtils.hasItemInInventory(player, ModItems.TENSEIGANHELMET.get());
    }

    private static boolean lacksLegacyAdvancedOrJinchuriki(Player player) {
        return !ProcedureUtils.hasItemInInventory(player, ModItems.YOOTON.get())
                && !ProcedureUtils.hasItemInInventory(player, ModItems.RANTON.get())
                && !ProcedureUtils.hasItemInInventory(player, ModItems.HYOTON.get())
                && !ProcedureUtils.hasItemInInventory(player, ModItems.JITON.get())
                && !ProcedureUtils.hasItemInInventory(player, ModItems.SHAKUTON.get())
                && !ProcedureUtils.hasItemInInventory(player, ModItems.BAKUTON.get())
                && !ProcedureUtils.hasItemInInventory(player, ModItems.JINTON.get())
                && !ProcedureUtils.hasItemInInventory(player, ModItems.FUTTON.get())
                && !ProcedureUtils.hasItemInInventory(player, ModItems.SHIKOTSUMYAKU.get())
                && !BijuManager.isJinchuriki(player);
    }
}
