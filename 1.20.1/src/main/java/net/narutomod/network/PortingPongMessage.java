package net.narutomod.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.narutomod.NarutomodMod;

public record PortingPongMessage(long nonce) {
    public static void encode(PortingPongMessage message, FriendlyByteBuf buffer) {
        buffer.writeLong(message.nonce);
    }

    public static PortingPongMessage decode(FriendlyByteBuf buffer) {
        return new PortingPongMessage(buffer.readLong());
    }

    public static void handle(PortingPongMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> NarutomodMod.LOGGER.info("Received porting pong {}", message.nonce));
        context.setPacketHandled(true);
    }
}
