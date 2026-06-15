package net.narutomod.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.narutomod.registry.ModMenuTypes;

public final class MedicalScrollMenu extends AbstractContainerMenu {
    public static final int PRIMARY_SLOT = 0;
    public static final int SECONDARY_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    private static final int INTERNAL_SLOT_COUNT = 3;
    private final Container internal = new SimpleContainer(INTERNAL_SLOT_COUNT);

    public MedicalScrollMenu(int containerId, Inventory inventory) {
        super(ModMenuTypes.MEDICAL_SCROLL.get(), containerId);
        addSlot(new Slot(internal, PRIMARY_SLOT, 22, 21));
        addSlot(new Slot(internal, SECONDARY_SLOT, 138, 21));
        addSlot(new OutputSlot(internal, OUTPUT_SLOT, 78, 22));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 88 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 146));
        }
    }

    public ItemStack input(int slotIndex) {
        return getSlot(slotIndex).getItem();
    }

    public boolean outputEmpty() {
        return getSlot(OUTPUT_SLOT).getItem().isEmpty();
    }

    public void setOutput(ItemStack stack) {
        getSlot(OUTPUT_SLOT).set(stack);
        getSlot(OUTPUT_SLOT).setChanged();
    }

    public void consumeInputs() {
        shrinkSlot(PRIMARY_SLOT);
        shrinkSlot(SECONDARY_SLOT);
    }

    private void shrinkSlot(int slotIndex) {
        Slot slot = getSlot(slotIndex);
        slot.getItem().shrink(1);
        slot.setChanged();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack current = slot.getItem();
        moved = current.copy();
        if (index < INTERNAL_SLOT_COUNT) {
            if (!moveItemStackTo(current, INTERNAL_SLOT_COUNT, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(current, PRIMARY_SLOT, OUTPUT_SLOT, false)) {
            return ItemStack.EMPTY;
        }

        if (current.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return moved;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        clearContainer(player, internal);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    private static final class OutputSlot extends Slot {
        private OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
