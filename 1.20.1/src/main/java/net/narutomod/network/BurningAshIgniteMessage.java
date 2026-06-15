package net.narutomod.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

public record BurningAshIgniteMessage(int entityId, int seconds) {
    private static final double MAX_DISTANCE_SQR = 64.0D * 64.0D;

    public static void encode(BurningAshIgniteMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.entityId);
        buffer.writeInt(message.seconds);
    }

    public static BurningAshIgniteMessage decode(FriendlyByteBuf buffer) {
        return new BurningAshIgniteMessage(buffer.readInt(), buffer.readInt());
    }

    public static void handle(BurningAshIgniteMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            Entity target = sender.serverLevel().getEntity(message.entityId);
            if (target instanceof LivingEntity living && living.isAlive() && sender.distanceToSqr(living) <= MAX_DISTANCE_SQR) {
                int ticks = Math.max(message.seconds, 0) * 20;
                living.setRemainingFireTicks(Math.max(living.getRemainingFireTicks(), ticks));
            }
        });
        context.setPacketHandled(true);
    }
}
