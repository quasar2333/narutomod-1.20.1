package net.narutomod.item;

import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.narutomod.Chakra;
import net.narutomod.NarutomodModVariables;
import net.narutomod.entity.AbstractSusanooEntity;
import net.narutomod.entity.BijuManager;
import net.narutomod.entity.SusanooClothedEntity;
import net.narutomod.entity.SusanooSkeletonEntity;
import net.narutomod.entity.SusanooWingedEntity;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModItems;

public final class SusanooPowerIncreaseHandler {
    public static final double BASE_CHAKRA_USAGE = 500.0D;
    private static final String SUMMONED_SUSANOO_ID_TAG = "summonedSusanooID";
    private static final String SUSANOO_ACTIVATED_TAG = "susanoo_activated";

    private SusanooPowerIncreaseHandler() {
    }

    public static boolean handlePowerIncreaseKey(ServerPlayer player, boolean pressed) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (player.getVehicle() instanceof AbstractSusanooEntity susanoo && susanoo.isOwnedBy(player)) {
            if (!isSharinganHead(head)) {
                return false;
            }
            if (!pressed) {
                upgrade(player, susanoo);
            }
            return true;
        }

        if (!isMangekyoHead(head)) {
            return false;
        }

        if (!pressed) {
            activate(player);
        }
        return true;
    }

    public static boolean upgrade(ServerPlayer player) {
        if (player.getVehicle() instanceof AbstractSusanooEntity susanoo && susanoo.isOwnedBy(player)) {
            return upgrade(player, susanoo) != null;
        }
        return false;
    }

    public static boolean isActivated(Player player) {
        return NarutomodModVariables.get(player).getBoolean(SUSANOO_ACTIVATED_TAG);
    }

    public static void setActiveTicks(ServerPlayer player, double activeTicks) {
        if (isActivated(player)) {
            player.getPersistentData().putDouble("susanoo_ticks", activeTicks);
        }
    }

    public static boolean shouldAutoCloseLegacyBodyTick(ServerPlayer player) {
        if (!isActivated(player) || bypassLegacyBodyTickAutoClose(player)) {
            return false;
        }
        double activeTicks = player.getPersistentData().getDouble("susanoo_ticks");
        return activeTicks > 820.0D || !hasActiveOwnedSusanoo(player);
    }

    public static boolean hasActiveOwnedSusanoo(ServerPlayer player) {
        AbstractSusanooEntity susanoo = findActiveSusanoo(player);
        return susanoo != null && susanoo.isAlive();
    }

    public static boolean activate(ServerPlayer player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (isActivated(player)
                || hasActiveOwnedSusanoo(player)
                || !isMangekyoHead(head)
                || ObitoMangekyoHelmetItem.isBlinded(head)
                || NarutomodModVariables.getBattleExperience(player) < AbstractSusanooEntity.BXP_REQUIRED_L1) {
            return false;
        }
        if (BijuManager.getCloakLevel(player) > 0) {
            BijuManager.toggleBijuCloak(player);
        }
        if (!Chakra.pathway(player).consume(BASE_CHAKRA_USAGE)) {
            return false;
        }

        SusanooSkeletonEntity susanoo = SusanooSkeletonEntity.spawnFrom(player);
        if (susanoo == null) {
            return false;
        }
        markActivated(player, susanoo);
        return true;
    }

    public static boolean deactivate(ServerPlayer player, boolean applyPenalty) {
        AbstractSusanooEntity susanoo = findActiveSusanoo(player);
        double activeTicks = susanoo != null ? susanoo.tickCount : player.getPersistentData().getDouble("susanoo_ticks");

        if (player.getVehicle() instanceof AbstractSusanooEntity vehicle && vehicle.isOwnedBy(player)) {
            player.stopRiding();
            if (susanoo == null) {
                susanoo = vehicle;
            }
        }
        if (susanoo != null && susanoo.isAlive()) {
            susanoo.discard();
        }

        player.getPersistentData().putInt(SUMMONED_SUSANOO_ID_TAG, 0);
        player.getPersistentData().putDouble("susanoo_ticks", 0.0D);
        NarutomodModVariables.get(player).putBoolean(SUSANOO_ACTIVATED_TAG, false);
        NarutomodModVariables.sync(player);

        int cooldown = (int)(activeTicks * 0.25D);
        if (applyPenalty && cooldown > 0 && shouldApplyDeactivatePenalty(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, cooldown, 3, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, cooldown, 2, false, false));
        }
        player.addEffect(new MobEffectInstance(ModEffects.FEATHER_FALLING.get(), 60, 5, false, false));
        return susanoo != null || activeTicks > 0.0D;
    }

    @Nullable
    public static AbstractSusanooEntity upgrade(ServerPlayer player, AbstractSusanooEntity current) {
        if (!isSharinganHead(player.getItemBySlot(EquipmentSlot.HEAD))) {
            return null;
        }
        double battleXp = NarutomodModVariables.getBattleExperience(player);
        if (current instanceof SusanooSkeletonEntity) {
            if (battleXp >= AbstractSusanooEntity.BXP_REQUIRED_L2 && consumeUpgradeChakra(player)) {
                return replaceWith(player, current, SusanooStage.CLOTHED_HALF);
            }
        } else if (current instanceof SusanooClothedEntity clothed) {
            if (!clothed.hasLegs() && battleXp >= AbstractSusanooEntity.BXP_REQUIRED_L3 && consumeUpgradeChakra(player)) {
                return replaceWith(player, current, SusanooStage.CLOTHED_FULL);
            }
            if (clothed.hasLegs() && battleXp >= AbstractSusanooEntity.BXP_REQUIRED_L4 && consumeUpgradeChakra(player)) {
                return replaceWith(player, current, SusanooStage.WINGED);
            }
        }
        return null;
    }

    public static boolean isSharinganHead(ItemStack stack) {
        return stack.is(ModItems.SHARINGANHELMET.get())
                || stack.is(ModItems.MANGEKYOSHARINGANHELMET.get())
                || stack.is(ModItems.MANGEKYOSHARINGANOBITOHELMET.get())
                || stack.is(ModItems.MANGEKYOSHARINGANETERNALHELMET.get());
    }

    public static boolean isMangekyoHead(ItemStack stack) {
        return stack.is(ModItems.MANGEKYOSHARINGANHELMET.get())
                || stack.is(ModItems.MANGEKYOSHARINGANOBITOHELMET.get())
                || stack.is(ModItems.MANGEKYOSHARINGANETERNALHELMET.get());
    }

    public static String describeVehicle(Entity vehicle) {
        if (vehicle instanceof SusanooSkeletonEntity) {
            return "susanoo_skeleton";
        }
        if (vehicle instanceof SusanooClothedEntity clothed) {
            return clothed.hasLegs() ? "susanoo_clothed_full" : "susanoo_clothed_half";
        }
        if (vehicle instanceof SusanooWingedEntity) {
            return "susanoo_winged";
        }
        return vehicle == null ? "none" : vehicle.getType().builtInRegistryHolder().key().location().toString();
    }

    private static boolean consumeUpgradeChakra(ServerPlayer player) {
        return Chakra.pathway(player).consume(BASE_CHAKRA_USAGE);
    }

    @Nullable
    private static AbstractSusanooEntity findActiveSusanoo(ServerPlayer player) {
        int id = player.getPersistentData().getInt(SUMMONED_SUSANOO_ID_TAG);
        if (id > 0 && player.level().getEntity(id) instanceof AbstractSusanooEntity susanoo && susanoo.isOwnedBy(player)) {
            return susanoo;
        }
        if (player.getVehicle() instanceof AbstractSusanooEntity susanoo && susanoo.isOwnedBy(player)) {
            return susanoo;
        }
        return null;
    }

    private static boolean shouldApplyDeactivatePenalty(ServerPlayer player) {
        return !player.isCreative()
                && !ProcedureUtils.hasItemInInventory(player, ModItems.RINNEGANHELMET.get())
                && !player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.MANGEKYOSHARINGANETERNALHELMET.get());
    }

    private static boolean bypassLegacyBodyTickAutoClose(ServerPlayer player) {
        return player.isCreative() || ProcedureUtils.hasItemInInventory(player, ModItems.RINNEGANHELMET.get());
    }

    @Nullable
    private static AbstractSusanooEntity replaceWith(ServerPlayer player, AbstractSusanooEntity current, SusanooStage nextStage) {
        player.stopRiding();
        current.discard();
        AbstractSusanooEntity next = switch (nextStage) {
            case CLOTHED_HALF -> SusanooClothedEntity.spawnFrom(player, false);
            case CLOTHED_FULL -> SusanooClothedEntity.spawnFrom(player, true);
            case WINGED -> SusanooWingedEntity.spawnFrom(player);
        };
        if (next != null) {
            markActivated(player, next);
        }
        return next;
    }

    private static void markActivated(ServerPlayer player, AbstractSusanooEntity susanoo) {
        player.getPersistentData().putInt(SUMMONED_SUSANOO_ID_TAG, susanoo.getId());
        player.getPersistentData().putDouble("susanoo_ticks", 0.0D);
        NarutomodModVariables.get(player).putBoolean(SUSANOO_ACTIVATED_TAG, true);
        NarutomodModVariables.sync(player);
    }

    private enum SusanooStage {
        CLOTHED_HALF,
        CLOTHED_FULL,
        WINGED
    }
}
