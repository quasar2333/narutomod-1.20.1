package net.narutomod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.narutomod.PlayerTracker;
import net.narutomod.procedure.ProcedureUtils;

public final class JutsuLearning {
    private JutsuLearning() {
    }

    public static boolean learn(ServerPlayer player, Item targetItem, JutsuItem.JutsuDefinition definition, Component displayName) {
        if (!player.isCreative() && !PlayerTracker.isNinja(player)) {
            player.displayClientMessage(Component.literal("You need ninja experience to learn ")
                    .append(displayName)
                    .append(Component.literal(".")), true);
            return false;
        }

        ItemStack stack = findOwnedOrUnboundStack(player, targetItem);
        if (stack.isEmpty()) {
            stack = new ItemStack(targetItem);
            if (!(stack.getItem() instanceof JutsuItem jutsuItem)) {
                player.displayClientMessage(Component.literal("This scroll cannot bind to a jutsu item yet."), true);
                return false;
            }
            JutsuItem.setOwner(stack, player);
            unlock(jutsuItem, stack, definition);
            ItemHandlerHelper.giveItemToPlayer(player, stack);
        } else if (stack.getItem() instanceof JutsuItem jutsuItem) {
            JutsuItem.setOwnerIfMissing(stack, player);
            unlock(jutsuItem, stack, definition);
        } else {
            player.displayClientMessage(Component.literal("This scroll target is not a jutsu item."), true);
            return false;
        }

        ProcedureUtils.grantAdvancement(player, "narutomod:learned_1st_jutsu", true);
        player.displayClientMessage(Component.literal("Learned ").append(displayName).append(Component.literal(".")), true);
        return true;
    }

    private static void unlock(JutsuItem item, ItemStack stack, JutsuItem.JutsuDefinition definition) {
        item.enableJutsu(stack, definition, true);
        if (item instanceof NinjutsuItem && definition.index() == NinjutsuItem.RASENGAN.index()
                && definition.translationKey().equals(NinjutsuItem.RASENGAN.translationKey())) {
            NinjutsuItem.setRasenganLearned(stack, true);
        }
    }

    private static ItemStack findOwnedOrUnboundStack(ServerPlayer player, Item targetItem) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack candidate = player.getInventory().getItem(i);
            if (!candidate.is(targetItem)) {
                continue;
            }
            if (!(candidate.getItem() instanceof JutsuItem) || JutsuItem.isOwnedByOrUnbound(player, candidate)) {
                return candidate;
            }
        }
        return ItemStack.EMPTY;
    }
}
