package net.narutomod.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.narutomod.registry.ModMenuTypes;

public final class RasenganScrollMenu extends AbstractContainerMenu {
    public RasenganScrollMenu(int containerId, Inventory inventory) {
        super(ModMenuTypes.RASENGAN_SCROLL.get(), containerId);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
