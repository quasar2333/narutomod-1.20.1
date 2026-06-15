package net.narutomod.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.narutomod.client.ClientCameraShakeEvents;

public record CameraShakeMessage(int duration, float scale) {
    public static void encode(CameraShakeMessage message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.duration);
        buffer.writeFloat(message.scale);
    }

    public static CameraShakeMessage decode(FriendlyByteBuf buffer) {
        return new CameraShakeMessage(buffer.readVarInt(), buffer.readFloat());
    }

    public static void handle(CameraShakeMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientCameraShakeEvents.apply(message)));
        context.setPacketHandled(true);
    }
}
