package net.narutomod.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.narutomod.entity.Snake8HeadsEntity;

public record Snake8HeadsAttackMessage() {
    private static final double LEGACY_EMPTY_CLICK_TARGET_INFLATION = 2.0D;

    public static void encode(Snake8HeadsAttackMessage message, FriendlyByteBuf buffer) {
    }

    public static Snake8HeadsAttackMessage decode(FriendlyByteBuf buffer) {
        return new Snake8HeadsAttackMessage();
    }

    public static void handle(Snake8HeadsAttackMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            if (sender.getVehicle() instanceof Snake8HeadsEntity snake && snake.isOwnedBy(sender)) {
                snake.shootLookTarget(sender, LEGACY_EMPTY_CLICK_TARGET_INFLATION);
            }
        });
        context.setPacketHandled(true);
    }
}
