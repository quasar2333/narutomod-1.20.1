package net.narutomod.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.narutomod.entity.BijuManager;

public final class PowerIncreaseKeyHandler {
    private PowerIncreaseKeyHandler() {
    }

    public static boolean handle(ServerPlayer player, boolean pressed) {
        if (player.isSpectator() || !player.isAlive()) {
            return false;
        }
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof JutsuItem jutsuItem) {
            if (!pressed) {
                jutsuItem.switchToNextUsableJutsuAndNotify(mainHand, player);
            }
            return true;
        }

        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof JutsuItem jutsuItem) {
            if (!pressed) {
                jutsuItem.switchToNextUsableJutsuAndNotify(offhand, player);
            }
            return true;
        }

        if (ByakuganHandler.handlePowerIncreaseKey(player, pressed)) {
            return true;
        }

        if (SusanooPowerIncreaseHandler.handlePowerIncreaseKey(player, pressed)) {
            return true;
        }

        if (RinneganSpecialJutsuHandler.handlePowerIncreaseKey(player, pressed)) {
            return true;
        }

        if (isWearingBijuCloakFullSet(player)) {
            if (!pressed) {
                BijuManager.increaseCloakLevel(player);
            }
            return true;
        }
        return false;
    }

    public static boolean isWearingBijuCloakFullSet(ServerPlayer player) {
        return BijuCloakItem.isBijuCloak(player.getItemBySlot(EquipmentSlot.HEAD))
                && BijuCloakItem.isBijuCloak(player.getItemBySlot(EquipmentSlot.CHEST))
                && BijuCloakItem.isBijuCloak(player.getItemBySlot(EquipmentSlot.LEGS));
    }
}
