package net.narutomod.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.narutomod.item.ItemOnBody;
import net.narutomod.network.InventoryTrackerSyncMessage;

public final class ClientInventoryTrackerSync {
    private static final Map<Integer, Snapshot> SNAPSHOTS = new HashMap<>();

    private ClientInventoryTrackerSync() {
    }

    public static void apply(InventoryTrackerSyncMessage message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Entity entity = minecraft.level.getEntity(message.playerId());
        if (!(entity instanceof Player)) {
            SNAPSHOTS.remove(message.playerId());
            return;
        }
        Snapshot snapshot = SNAPSHOTS.computeIfAbsent(message.playerId(), ignored -> new Snapshot());
        snapshot.selectedSlot = message.selectedSlot();
        for (Map.Entry<Integer, ItemStack> entry : message.slots().entrySet()) {
            ItemStack stack = entry.getValue();
            if (stack.isEmpty() || !(stack.getItem() instanceof ItemOnBody.Interface)) {
                snapshot.slots.remove(entry.getKey());
            } else {
                snapshot.slots.put(entry.getKey(), stack.copy());
            }
        }
        if (snapshot.slots.isEmpty()) {
            SNAPSHOTS.remove(message.playerId());
        }
    }

    public static Map<Integer, ItemStack> getSlots(Player player) {
        Snapshot snapshot = SNAPSHOTS.get(player.getId());
        return snapshot == null ? Map.of() : snapshot.slots;
    }

    public static int getSelectedSlot(Player player) {
        Snapshot snapshot = SNAPSHOTS.get(player.getId());
        return snapshot == null ? -1 : snapshot.selectedSlot;
    }

    private static final class Snapshot {
        private final Map<Integer, ItemStack> slots = new HashMap<>();
        private int selectedSlot = -1;
    }
}
