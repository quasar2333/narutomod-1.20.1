package net.narutomod.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.narutomod.item.JutsuScrollDefinition;
import net.narutomod.registry.ModMenuTypes;

public final class JutsuScrollMenu extends AbstractContainerMenu {
    private final JutsuScrollDefinition definition;

    public JutsuScrollMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, JutsuScrollDefinition.byId(buffer.readUtf()));
    }

    public JutsuScrollMenu(int containerId, Inventory inventory, JutsuScrollDefinition definition) {
        super(ModMenuTypes.JUTSU_SCROLL.get(), containerId);
        this.definition = definition;
    }

    public JutsuScrollDefinition definition() {
        return definition;
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
