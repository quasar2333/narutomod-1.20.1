package net.narutomod.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.narutomod.entity.Buddha1000Entity;

public record WoodBuddhaAttackMessage() {
    public static void encode(WoodBuddhaAttackMessage message, FriendlyByteBuf buffer) {
    }

    public static WoodBuddhaAttackMessage decode(FriendlyByteBuf buffer) {
        return new WoodBuddhaAttackMessage();
    }

    public static void handle(WoodBuddhaAttackMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            if (sender.getVehicle() instanceof Buddha1000Entity buddha && buddha.isOwnedBy(sender)) {
                buddha.shootArms();
            }
        });
        context.setPacketHandled(true);
    }
}
