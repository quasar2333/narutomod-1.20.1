package net.narutomod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.narutomod.network.NetworkHandler;
import net.narutomod.network.PortingPingMessage;

public final class PortingMarkerItem extends Item {
    public PortingMarkerItem() {
        super(new Item.Properties());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            NetworkHandler.sendToServer(new PortingPingMessage(level.getGameTime()));
        } else {
            player.displayClientMessage(Component.literal("Narutomod porting ping sent"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
