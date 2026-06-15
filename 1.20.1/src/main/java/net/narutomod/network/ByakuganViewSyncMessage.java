package net.narutomod.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.narutomod.client.ClientByakuganViewState;

public record ByakuganViewSyncMessage(boolean active, float fov, float ninjaLevel) {
    public static void encode(ByakuganViewSyncMessage message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.active);
        buffer.writeFloat(message.fov);
        buffer.writeFloat(message.ninjaLevel);
    }

    public static ByakuganViewSyncMessage decode(FriendlyByteBuf buffer) {
        return new ByakuganViewSyncMessage(buffer.readBoolean(), buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(ByakuganViewSyncMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientByakuganViewState.apply(message)));
        context.setPacketHandled(true);
    }
}
