package net.narutomod.network;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.narutomod.client.ClientInventoryTrackerSync;

public record InventoryTrackerSyncMessage(int playerId, int selectedSlot, Map<Integer, ItemStack> slots) {
    public InventoryTrackerSyncMessage {
        Map<Integer, ItemStack> copy = new HashMap<>();
        for (Map.Entry<Integer, ItemStack> entry : slots.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().copy());
        }
        slots = copy;
    }

    public static void encode(InventoryTrackerSyncMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.playerId);
        buffer.writeInt(message.selectedSlot);
        buffer.writeVarInt(message.slots.size());
        for (Map.Entry<Integer, ItemStack> entry : message.slots.entrySet()) {
            buffer.writeVarInt(entry.getKey());
            buffer.writeItem(entry.getValue());
        }
    }

    public static InventoryTrackerSyncMessage decode(FriendlyByteBuf buffer) {
        int playerId = buffer.readInt();
        int selectedSlot = buffer.readInt();
        int size = buffer.readVarInt();
        Map<Integer, ItemStack> slots = new HashMap<>();
        for (int index = 0; index < size; index++) {
            slots.put(buffer.readVarInt(), buffer.readItem());
        }
        return new InventoryTrackerSyncMessage(playerId, selectedSlot, slots);
    }

    public static void handle(InventoryTrackerSyncMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientInventoryTrackerSync.apply(message)));
        context.setPacketHandled(true);
    }

    public static void sendTo(ServerPlayer watcher, int playerId, int selectedSlot, Map<Integer, ItemStack> slots) {
        NetworkHandler.sendToPlayer(watcher, new InventoryTrackerSyncMessage(playerId, selectedSlot, slots));
    }

    public static void sendToTracking(ServerPlayer player, int selectedSlot, Map<Integer, ItemStack> slots) {
        NetworkHandler.sendToTracking(player, new InventoryTrackerSyncMessage(player.getId(), selectedSlot, slots));
    }
}
