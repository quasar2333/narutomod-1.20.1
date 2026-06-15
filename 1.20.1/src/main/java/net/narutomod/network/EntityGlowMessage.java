package net.narutomod.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.narutomod.client.ClientEntityGlowState;

public record EntityGlowMessage(int entityId, boolean glow, int durationTicks) {
    public static void encode(EntityGlowMessage message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.entityId);
        buffer.writeBoolean(message.glow);
        buffer.writeVarInt(message.durationTicks);
    }

    public static EntityGlowMessage decode(FriendlyByteBuf buffer) {
        return new EntityGlowMessage(buffer.readVarInt(), buffer.readBoolean(), buffer.readVarInt());
    }

    public static void handle(EntityGlowMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientEntityGlowState.apply(message)));
        context.setPacketHandled(true);
    }
}
