package net.narutomod.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;
import net.narutomod.registry.ModEffects;

public record AcidSpitCorrosionMessage(int entityId, int ticks) {
    private static final double MAX_DISTANCE_SQR = 64.0D * 64.0D;

    public static void encode(AcidSpitCorrosionMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.entityId);
        buffer.writeInt(message.ticks);
    }

    public static AcidSpitCorrosionMessage decode(FriendlyByteBuf buffer) {
        return new AcidSpitCorrosionMessage(buffer.readInt(), buffer.readInt());
    }

    public static void handle(AcidSpitCorrosionMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            Entity target = sender.serverLevel().getEntity(message.entityId);
            if (target instanceof LivingEntity living && living.isAlive() && sender.distanceToSqr(living) <= MAX_DISTANCE_SQR) {
                living.addEffect(new MobEffectInstance(ModEffects.CORROSION.get(), Math.max(message.ticks, 1), 1));
            }
        });
        context.setPacketHandled(true);
    }
}
