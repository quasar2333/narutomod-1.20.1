package net.narutomod.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.narutomod.item.JutsuLearning;
import net.narutomod.item.NinjutsuItem;
import net.narutomod.menu.RasenganScrollMenu;
import net.narutomod.registry.ModItems;

public record RasenganScrollLearnMessage() {
    public static void encode(RasenganScrollLearnMessage message, FriendlyByteBuf buffer) {
    }

    public static RasenganScrollLearnMessage decode(FriendlyByteBuf buffer) {
        return new RasenganScrollLearnMessage();
    }

    public static void handle(RasenganScrollLearnMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || !(sender.containerMenu instanceof RasenganScrollMenu)) {
                return;
            }
            learnRasengan(sender);
        });
        context.setPacketHandled(true);
    }

    private static void learnRasengan(ServerPlayer player) {
        if (JutsuLearning.learn(player, ModItems.NINJUTSU.get(), NinjutsuItem.RASENGAN, Component.literal("Rasengan"))) {
            player.closeContainer();
        }
    }
}
