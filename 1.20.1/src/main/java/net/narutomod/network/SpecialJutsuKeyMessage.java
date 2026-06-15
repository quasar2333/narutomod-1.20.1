package net.narutomod.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.narutomod.NarutomodModVariables;
import net.narutomod.entity.BijuManager;
import net.narutomod.item.AmaterasuHandler;
import net.narutomod.item.ByakuganHandler;
import net.narutomod.item.ObitoKamuiHandler;
import net.narutomod.item.RinneganSpecialJutsuHandler;
import net.narutomod.procedure.ProcedureSync;

public record SpecialJutsuKeyMessage(int key, boolean pressed) {
    public static void encode(SpecialJutsuKeyMessage message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.key);
        buffer.writeBoolean(message.pressed);
    }

    public static SpecialJutsuKeyMessage decode(FriendlyByteBuf buffer) {
        return new SpecialJutsuKeyMessage(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(SpecialJutsuKeyMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || (message.key < 1 || message.key > 3)) {
                return;
            }
            if (message.key == 2) {
                ProcedureSync.EntityNBTTag.setAndSync(sender, NarutomodModVariables.JUTSU_KEY_2_PRESSED, message.pressed);
            }
            if (!ObitoKamuiHandler.handleSpecialJutsuKey(sender, message.key, message.pressed)
                    && !AmaterasuHandler.handleSpecialJutsuKey(sender, message.key, message.pressed)
                    && !ByakuganHandler.handleSpecialJutsuKey(sender, message.key, message.pressed)
                    && !RinneganSpecialJutsuHandler.handleSpecialJutsuKey(sender, message.key, message.pressed)) {
                BijuManager.handleSpecialJutsuKey(sender, message.key, message.pressed);
            }
            if (message.key == 2) {
                ProcedureSync.EntityNBTTag.setAndSync(sender, NarutomodModVariables.CTRL_PRESSED, false);
            }
        });
        context.setPacketHandled(true);
    }
}
