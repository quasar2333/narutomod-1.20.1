package net.narutomod.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.narutomod.client.ClientSuitonMistFogState;

public record SuitonMistFogMessage(float density, int durationTicks) {
    public static void encode(SuitonMistFogMessage message, FriendlyByteBuf buffer) {
        buffer.writeFloat(message.density);
        buffer.writeVarInt(message.durationTicks);
    }

    public static SuitonMistFogMessage decode(FriendlyByteBuf buffer) {
        return new SuitonMistFogMessage(buffer.readFloat(), buffer.readVarInt());
    }

    public static void handle(SuitonMistFogMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSuitonMistFogState.apply(message)));
        context.setPacketHandled(true);
    }
}
