package net.narutomod.network;

import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.narutomod.client.ClientPlayerVariablesSync;

public record PlayerVariablesSyncMessage(int entityId, CompoundTag variables) {
    public static void encode(PlayerVariablesSyncMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.entityId);
        buffer.writeNbt(message.variables);
    }

    public static PlayerVariablesSyncMessage decode(FriendlyByteBuf buffer) {
        int entityId = buffer.readInt();
        CompoundTag tag = buffer.readNbt();
        return new PlayerVariablesSyncMessage(entityId, tag == null ? new CompoundTag() : tag);
    }

    public static void handle(PlayerVariablesSyncMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPlayerVariablesSync.apply(message)));
        context.setPacketHandled(true);
    }

    public static void sendTo(ServerPlayer player, int entityId, CompoundTag variables) {
        NetworkHandler.sendToPlayer(player, new PlayerVariablesSyncMessage(entityId, variables));
    }

    public static void sendTrackingAndSelf(ServerPlayer player, CompoundTag variables) {
        NetworkHandler.sendToTrackingAndSelf(player, new PlayerVariablesSyncMessage(player.getId(), variables));
    }
}
