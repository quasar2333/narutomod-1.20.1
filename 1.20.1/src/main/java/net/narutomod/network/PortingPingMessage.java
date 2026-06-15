package net.narutomod.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.narutomod.NarutomodMod;

public record PortingPingMessage(long nonce) {
    public static void encode(PortingPingMessage message, FriendlyByteBuf buffer) {
        buffer.writeLong(message.nonce);
    }

    public static PortingPingMessage decode(FriendlyByteBuf buffer) {
        return new PortingPingMessage(buffer.readLong());
    }

    public static void handle(PortingPingMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }

            NarutomodMod.LOGGER.info("Received porting ping {} from {}", message.nonce, sender.getGameProfile().getName());
            NetworkHandler.sendToPlayer(sender, new PortingPongMessage(message.nonce));
        });
        context.setPacketHandled(true);
    }
}
