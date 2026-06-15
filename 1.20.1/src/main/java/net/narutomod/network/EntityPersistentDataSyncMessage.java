package net.narutomod.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.narutomod.client.ClientPersistentDataSync;
import net.narutomod.procedure.ProcedureSync;

public record EntityPersistentDataSyncMessage(
        int entityId,
        String tagName,
        ValueType valueType,
        int intValue,
        double doubleValue,
        boolean booleanValue) {

    public enum ValueType {
        REMOVE,
        INTEGER,
        DOUBLE,
        BOOLEAN
    }

    public static EntityPersistentDataSyncMessage remove(Entity entity, String tagName) {
        return new EntityPersistentDataSyncMessage(entity.getId(), tagName, ValueType.REMOVE, 0, 0.0D, false);
    }

    public static EntityPersistentDataSyncMessage of(Entity entity, String tagName, int value) {
        return new EntityPersistentDataSyncMessage(entity.getId(), tagName, ValueType.INTEGER, value, 0.0D, false);
    }

    public static EntityPersistentDataSyncMessage of(Entity entity, String tagName, double value) {
        return new EntityPersistentDataSyncMessage(entity.getId(), tagName, ValueType.DOUBLE, 0, value, false);
    }

    public static EntityPersistentDataSyncMessage of(Entity entity, String tagName, boolean value) {
        return new EntityPersistentDataSyncMessage(entity.getId(), tagName, ValueType.BOOLEAN, 0, 0.0D, value);
    }

    public static void encode(EntityPersistentDataSyncMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.entityId);
        buffer.writeUtf(message.tagName);
        buffer.writeEnum(message.valueType);
        buffer.writeInt(message.intValue);
        buffer.writeDouble(message.doubleValue);
        buffer.writeBoolean(message.booleanValue);
    }

    public static EntityPersistentDataSyncMessage decode(FriendlyByteBuf buffer) {
        return new EntityPersistentDataSyncMessage(
                buffer.readInt(),
                buffer.readUtf(),
                buffer.readEnum(ValueType.class),
                buffer.readInt(),
                buffer.readDouble(),
                buffer.readBoolean());
    }

    public static void handle(EntityPersistentDataSyncMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                Entity entity = sender.serverLevel().getEntity(message.entityId);
                if (entity != null) {
                    ProcedureSync.applyPersistentData(
                            entity,
                            message.tagName,
                            message.valueType,
                            message.intValue,
                            message.doubleValue,
                            message.booleanValue);
                }
            } else {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPersistentDataSync.apply(message));
            }
        });
        context.setPacketHandled(true);
    }

    public static void sendTo(ServerPlayer player, EntityPersistentDataSyncMessage message) {
        NetworkHandler.sendToPlayer(player, message);
    }

    public static void sendTrackingAndSelf(Entity entity, EntityPersistentDataSyncMessage message) {
        NetworkHandler.sendToTrackingAndSelf(entity, message);
    }

    public static void sendToServer(EntityPersistentDataSyncMessage message) {
        NetworkHandler.sendToServer(message);
    }
}
