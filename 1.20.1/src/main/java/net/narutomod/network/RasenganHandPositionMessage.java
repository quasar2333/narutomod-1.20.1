package net.narutomod.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.narutomod.entity.RasenganEntity;

public record RasenganHandPositionMessage(int entityId, double x, double y, double z) {
    public static void encode(RasenganHandPositionMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.entityId);
        buffer.writeDouble(message.x);
        buffer.writeDouble(message.y);
        buffer.writeDouble(message.z);
    }

    public static RasenganHandPositionMessage decode(FriendlyByteBuf buffer) {
        return new RasenganHandPositionMessage(buffer.readInt(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public static void handle(RasenganHandPositionMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            Entity entity = sender.serverLevel().getEntity(message.entityId);
            if (entity instanceof RasenganEntity rasengan) {
                rasengan.acceptClientHandPosition(sender, new Vec3(message.x, message.y, message.z));
            }
        });
        context.setPacketHandled(true);
    }
}
