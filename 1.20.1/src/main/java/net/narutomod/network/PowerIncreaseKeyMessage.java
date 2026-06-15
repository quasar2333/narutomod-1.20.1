package net.narutomod.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.narutomod.item.PowerIncreaseKeyHandler;

public record PowerIncreaseKeyMessage(boolean pressed) {
    public static void encode(PowerIncreaseKeyMessage message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.pressed);
    }

    public static PowerIncreaseKeyMessage decode(FriendlyByteBuf buffer) {
        return new PowerIncreaseKeyMessage(buffer.readBoolean());
    }

    public static void handle(PowerIncreaseKeyMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            PowerIncreaseKeyHandler.handle(sender, message.pressed);
        });
        context.setPacketHandled(true);
    }
}
