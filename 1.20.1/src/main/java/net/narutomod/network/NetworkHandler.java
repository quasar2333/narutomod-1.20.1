package net.narutomod.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.narutomod.NarutomodMod;

public final class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    private static int nextMessageId;
    private static boolean registered;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            NarutomodMod.location("main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private NetworkHandler() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }

        CHANNEL.registerMessage(nextMessageId(), PortingPingMessage.class, PortingPingMessage::encode, PortingPingMessage::decode, PortingPingMessage::handle);
        CHANNEL.registerMessage(nextMessageId(), PortingPongMessage.class, PortingPongMessage::encode, PortingPongMessage::decode, PortingPongMessage::handle);
        CHANNEL.registerMessage(nextMessageId(), PlayerVariablesSyncMessage.class, PlayerVariablesSyncMessage::encode, PlayerVariablesSyncMessage::decode, PlayerVariablesSyncMessage::handle);
        CHANNEL.registerMessage(nextMessageId(), EntityPersistentDataSyncMessage.class, EntityPersistentDataSyncMessage::encode, EntityPersistentDataSyncMessage::decode, EntityPersistentDataSyncMessage::handle);
        CHANNEL.registerMessage(nextMessageId(), BurningAshIgniteMessage.class, BurningAshIgniteMessage::encode, BurningAshIgniteMessage::decode, BurningAshIgniteMessage::handle);
        CHANNEL.registerMessage(nextMessageId(), AcidSpitCorrosionMessage.class, AcidSpitCorrosionMessage::encode, AcidSpitCorrosionMessage::decode, AcidSpitCorrosionMessage::handle);
        CHANNEL.registerMessage(nextMessageId(), RasenganHandPositionMessage.class, RasenganHandPositionMessage::encode, RasenganHandPositionMessage::decode, RasenganHandPositionMessage::handle);
        CHANNEL.registerMessage(nextMessageId(), ChidoriHandPositionMessage.class, ChidoriHandPositionMessage::encode, ChidoriHandPositionMessage::decode, ChidoriHandPositionMessage::handle);
        CHANNEL.registerMessage(nextMessageId(), JutsuScrollLearnMessage.class, JutsuScrollLearnMessage::encode, JutsuScrollLearnMessage::decode, JutsuScrollLearnMessage::handle);
        CHANNEL.registerMessage(nextMessageId(), RasenganScrollLearnMessage.class, RasenganScrollLearnMessage::encode, RasenganScrollLearnMessage::decode, RasenganScrollLearnMessage::handle);
        CHANNEL.registerMessage(nextMessageId(), MedicalScrollActivateMessage.class, MedicalScrollActivateMessage::encode, MedicalScrollActivateMessage::decode, MedicalScrollActivateMessage::handle);
        CHANNEL.registerMessage(nextMessageId(), SpecialJutsuKeyMessage.class, SpecialJutsuKeyMessage::encode, SpecialJutsuKeyMessage::decode, SpecialJutsuKeyMessage::handle);
        CHANNEL.registerMessage(nextMessageId(), PowerIncreaseKeyMessage.class, PowerIncreaseKeyMessage::encode, PowerIncreaseKeyMessage::decode, PowerIncreaseKeyMessage::handle);
        CHANNEL.registerMessage(nextMessageId(), ByakuganViewSyncMessage.class, ByakuganViewSyncMessage::encode, ByakuganViewSyncMessage::decode, ByakuganViewSyncMessage::handle);
        CHANNEL.registerMessage(nextMessageId(), SuitonMistFogMessage.class, SuitonMistFogMessage::encode, SuitonMistFogMessage::decode, SuitonMistFogMessage::handle);
        CHANNEL.registerMessage(nextMessageId(), EntityGlowMessage.class, EntityGlowMessage::encode, EntityGlowMessage::decode, EntityGlowMessage::handle);
        CHANNEL.registerMessage(nextMessageId(), WoodBuddhaAttackMessage.class, WoodBuddhaAttackMessage::encode, WoodBuddhaAttackMessage::decode, WoodBuddhaAttackMessage::handle);
        CHANNEL.registerMessage(nextMessageId(), Snake8HeadsAttackMessage.class, Snake8HeadsAttackMessage::encode, Snake8HeadsAttackMessage::decode, Snake8HeadsAttackMessage::handle);
        CHANNEL.registerMessage(nextMessageId(), InventoryTrackerSyncMessage.class, InventoryTrackerSyncMessage::encode, InventoryTrackerSyncMessage::decode, InventoryTrackerSyncMessage::handle);
        CHANNEL.registerMessage(nextMessageId(), CameraShakeMessage.class, CameraShakeMessage::encode, CameraShakeMessage::decode, CameraShakeMessage::handle);
        CHANNEL.registerMessage(nextMessageId(), ChakraWarningMessage.class, ChakraWarningMessage::encode, ChakraWarningMessage::decode, ChakraWarningMessage::handle);
        registered = true;
    }

    public static synchronized int nextMessageId() {
        return nextMessageId++;
    }

    public static void sendToServer(PortingPingMessage message) {
        CHANNEL.sendToServer(message);
    }

    public static void sendToServer(EntityPersistentDataSyncMessage message) {
        CHANNEL.sendToServer(message);
    }

    public static void sendToServer(BurningAshIgniteMessage message) {
        CHANNEL.sendToServer(message);
    }

    public static void sendToServer(AcidSpitCorrosionMessage message) {
        CHANNEL.sendToServer(message);
    }

    public static void sendToServer(RasenganHandPositionMessage message) {
        CHANNEL.sendToServer(message);
    }

    public static void sendToServer(ChidoriHandPositionMessage message) {
        CHANNEL.sendToServer(message);
    }

    public static void sendToServer(RasenganScrollLearnMessage message) {
        CHANNEL.sendToServer(message);
    }

    public static void sendToServer(JutsuScrollLearnMessage message) {
        CHANNEL.sendToServer(message);
    }

    public static void sendToServer(MedicalScrollActivateMessage message) {
        CHANNEL.sendToServer(message);
    }

    public static void sendToServer(SpecialJutsuKeyMessage message) {
        CHANNEL.sendToServer(message);
    }

    public static void sendToServer(PowerIncreaseKeyMessage message) {
        CHANNEL.sendToServer(message);
    }

    public static void sendToServer(WoodBuddhaAttackMessage message) {
        CHANNEL.sendToServer(message);
    }

    public static void sendToServer(Snake8HeadsAttackMessage message) {
        CHANNEL.sendToServer(message);
    }

    public static void sendToPlayer(ServerPlayer player, PortingPongMessage message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToPlayer(ServerPlayer player, PlayerVariablesSyncMessage message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToPlayer(ServerPlayer player, EntityPersistentDataSyncMessage message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToPlayer(ServerPlayer player, ByakuganViewSyncMessage message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToPlayer(ServerPlayer player, SuitonMistFogMessage message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToPlayer(ServerPlayer player, EntityGlowMessage message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToPlayer(ServerPlayer player, InventoryTrackerSyncMessage message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToPlayer(ServerPlayer player, ChakraWarningMessage message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToPlayersNear(ServerLevel level, double x, double y, double z, double range, CameraShakeMessage message) {
        CHANNEL.send(PacketDistributor.NEAR.with(PacketDistributor.TargetPoint.p(x, y, z, range, level.dimension())), message);
    }

    public static void sendToTracking(Entity entity, InventoryTrackerSyncMessage message) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), message);
    }

    public static void sendToTrackingAndSelf(Entity entity, PlayerVariablesSyncMessage message) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), message);
    }

    public static void sendToTrackingAndSelf(Entity entity, EntityPersistentDataSyncMessage message) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), message);
    }
}
