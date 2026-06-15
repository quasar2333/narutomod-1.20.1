package net.narutomod.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.narutomod.entity.ChidoriEntity;
import net.narutomod.entity.ChidoriSpearEntity;

public record ChidoriHandPositionMessage(int entityId, double x, double y, double z) {
    public static void encode(ChidoriHandPositionMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.entityId);
        buffer.writeDouble(message.x);
        buffer.writeDouble(message.y);
        buffer.writeDouble(message.z);
    }

    public static ChidoriHandPositionMessage decode(FriendlyByteBuf buffer) {
        return new ChidoriHandPositionMessage(buffer.readInt(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public static void handle(ChidoriHandPositionMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            Entity entity = sender.serverLevel().getEntity(message.entityId);
            Vec3 handPosition = new Vec3(message.x, message.y, message.z);
            if (entity instanceof ChidoriEntity chidori) {
                chidori.acceptClientHandPosition(sender, handPosition);
            } else if (entity instanceof ChidoriSpearEntity spear) {
                spear.acceptClientHandPosition(sender, handPosition);
            }
        });
        context.setPacketHandled(true);
    }
}
