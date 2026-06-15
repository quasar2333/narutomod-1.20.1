package net.narutomod.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.narutomod.client.ClientChakraHudEvents;

public record ChakraWarningMessage(int ticks) {
    public static void encode(ChakraWarningMessage message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.ticks);
    }

    public static ChakraWarningMessage decode(FriendlyByteBuf buffer) {
        return new ChakraWarningMessage(buffer.readVarInt());
    }

    public static void handle(ChakraWarningMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientChakraHudEvents.warn(message.ticks())));
        context.setPacketHandled(true);
    }
}
