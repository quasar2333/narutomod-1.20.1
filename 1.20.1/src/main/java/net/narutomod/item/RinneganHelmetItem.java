package net.narutomod.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemHandlerHelper;
import net.narutomod.NarutomodModVariables;
import net.narutomod.entity.BijuManager;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModItems;

public final class RinneganHelmetItem extends ArmorItem {
    private static final String LAST_LEGACY_TICK_TAG = "RinneganHelmetLastLegacyTick";
    private static final String CHIBAKU_TENSEI_ACTIVE_TAG = "chibakutensei_active";
    private static final String RINNESHARINGAN_ADVANCEMENT = "narutomod:rinnesharinganactivated";
    private final boolean tenseigan;

    public RinneganHelmetItem(boolean tenseigan) {
        super(ArmorMaterials.LEATHER, Type.HELMET, new Item.Properties().stacksTo(1));
        this.tenseigan = tenseigan;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!level.isClientSide && entity instanceof ServerPlayer player && player.getItemBySlot(EquipmentSlot.HEAD) == stack) {
            applyLegacyTick(player, stack);
        }
    }

    @Override
    public void onArmorTick(ItemStack stack, Level level, Player player) {
        super.onArmorTick(stack, level, player);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            applyLegacyTick(serverPlayer, stack);
        }
    }

    public static boolean applyLegacyTick(ServerPlayer player, ItemStack stack) {
        if (!RinneganSpecialJutsuHandler.isRinneganLikeHead(stack)) {
            return false;
        }
        long gameTime = player.level().getGameTime();
        CompoundTag tag = stack.getOrCreateTag();
        if (tag.contains(LAST_LEGACY_TICK_TAG) && tag.getLong(LAST_LEGACY_TICK_TAG) == gameTime) {
            return false;
        }
        tag.putLong(LAST_LEGACY_TICK_TAG, gameTime);

        player.fallDistance = 0.0F;
        NarutomodModVariables.get(player).putLong(NarutomodModVariables.MOST_RECENT_WORN_DOJUTSU_TIME, gameTime);

        boolean mainhandTenseiganChakraMode = player.getMainHandItem().is(ModItems.TENSEIGAN_CHAKRA_MODE.get());
        if (isRinnesharinganActivated(stack) && !mainhandTenseiganChakraMode) {
            applyRinnesharinganBranch(player, stack);
        } else {
            applyBaseRinneganBranch(player, stack);
        }

        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2, 4, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 210, 0, false, false));
        giveIfMissing(player, ModItems.BLACK_RECEIVER.get());

        if (player.getPersistentData().getBoolean(CHIBAKU_TENSEI_ACTIVE_TAG)) {
            player.addEffect(new MobEffectInstance(ModEffects.FLIGHT.get(), 200, 1, false, false));
        }
        return true;
    }

    public static boolean isRinnesharinganActivated(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().getBoolean(NarutomodModVariables.RINNESHARINGAN_ACTIVATED);
    }

    private static void applyRinnesharinganBranch(ServerPlayer player, ItemStack stack) {
        ProcedureUtils.purgeHarmfulEffects(player);
        player.addEffect(new MobEffectInstance(ModEffects.FLIGHT.get(), 20, 1, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 2, 1, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 2, 9, false, false));
        if (player.getHealth() > 0.0F && player.getHealth() < player.getMaxHealth()) {
            player.heal(1.0F);
        }
        if (!player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.RINNEGANBODY.get())) {
            ProcedureUtils.swapItemToSlot(player, EquipmentSlot.CHEST, new ItemStack(ModItems.RINNEGANBODY.get()));
        }
        if (!player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.RINNEGANLEGS.get())) {
            ProcedureUtils.swapItemToSlot(player, EquipmentSlot.LEGS, new ItemStack(ModItems.RINNEGANLEGS.get()));
        }
        if (!ProcedureUtils.hasItemInInventory(player, ModItems.SIX_PATH_SENJUTSU.get())) {
            ItemStack sixPath = new ItemStack(ModItems.SIX_PATH_SENJUTSU.get());
            JutsuItem.setOwnerIfMissing(sixPath, player);
            ItemHandlerHelper.giveItemToPlayer(player, sixPath);
        }
        if (isTenseiganStack(stack)) {
            giveIfMissing(player, ModItems.TENSEIGAN_CHAKRA_MODE.get());
        }
    }

    private static void applyBaseRinneganBranch(ServerPlayer player, ItemStack stack) {
        ItemStack ninjutsu = ProcedureUtils.getMatchingItemStack(player, ModItems.NINJUTSU.get());
        if (ninjutsu != null && ninjutsu.getItem() instanceof NinjutsuItem ninjutsuItem
                && !ninjutsuItem.isJutsuEnabled(ninjutsu, NinjutsuItem.LIMBO_CLONE)) {
            ninjutsuItem.enableJutsu(ninjutsu, NinjutsuItem.LIMBO_CLONE, true);
        }
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 2, 2, false, false));

        if (BijuManager.isJinchurikiOf(player, BijuManager.MAX_TAILS) && player.experienceLevel >= 180) {
            stack.getOrCreateTag().putBoolean(NarutomodModVariables.RINNESHARINGAN_ACTIVATED, true);
            ProcedureUtils.grantAdvancement(player, RINNESHARINGAN_ADVANCEMENT, true);
        } else if (isTenseiganStack(stack)
                && TenseiganChakraModeItem.canUseChakraMode(stack)
                && !ProcedureUtils.hasItemInInventory(player, ModItems.TENSEIGAN_CHAKRA_MODE.get())) {
            ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(ModItems.TENSEIGAN_CHAKRA_MODE.get()));
        }

        RinneganSpecialJutsuHandler.maintainAsuraPath(player);
    }

    private static boolean isTenseiganStack(ItemStack stack) {
        return stack.is(ModItems.TENSEIGANHELMET.get())
                || stack.getItem() instanceof RinneganHelmetItem helmet && helmet.tenseigan;
    }

    private static void giveIfMissing(ServerPlayer player, Item item) {
        if (!ProcedureUtils.hasItemInInventory(player, item)) {
            ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(item));
        }
    }
}
