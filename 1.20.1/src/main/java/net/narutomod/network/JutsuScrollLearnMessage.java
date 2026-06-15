package net.narutomod.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.narutomod.item.JutsuLearning;
import net.narutomod.item.JutsuScrollDefinition;
import net.narutomod.menu.JutsuScrollMenu;

public record JutsuScrollLearnMessage(String definitionId) {
    public static void encode(JutsuScrollLearnMessage message, FriendlyByteBuf buffer) {
        buffer.writeUtf(message.definitionId);
    }

    public static JutsuScrollLearnMessage decode(FriendlyByteBuf buffer) {
        return new JutsuScrollLearnMessage(buffer.readUtf());
    }

    public static void handle(JutsuScrollLearnMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || !(sender.containerMenu instanceof JutsuScrollMenu menu)) {
                return;
            }
            JutsuScrollDefinition definition = menu.definition();
            if (!definition.id().equals(message.definitionId())) {
                return;
            }
            if (JutsuLearning.learn(sender, definition.targetItem(), definition.jutsuDefinition(), Component.translatable(definition.jutsuNameKey()))) {
                sender.closeContainer();
            }
        });
        context.setPacketHandled(true);
    }
}
