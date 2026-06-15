package net.narutomod.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.narutomod.item.MedicalScrollItem;
import net.narutomod.menu.MedicalScrollMenu;

public record MedicalScrollActivateMessage() {
    public static void encode(MedicalScrollActivateMessage message, FriendlyByteBuf buffer) {
    }

    public static MedicalScrollActivateMessage decode(FriendlyByteBuf buffer) {
        return new MedicalScrollActivateMessage();
    }

    public static void handle(MedicalScrollActivateMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || !(sender.containerMenu instanceof MedicalScrollMenu menu)) {
                return;
            }
            MedicalScrollItem.activate(menu, sender);
        });
        context.setPacketHandled(true);
    }
}
