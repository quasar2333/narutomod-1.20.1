package net.narutomod.procedure;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.narutomod.NarutomodModVariables;
import net.narutomod.network.EntityPersistentDataSyncMessage;

public final class ProcedureSync {
    private ProcedureSync() {
    }

    public static void applyPersistentData(Entity entity, String tagName, EntityPersistentDataSyncMessage.ValueType type, int intValue, double doubleValue, boolean booleanValue) {
        if (entity == null) {
            return;
        }

        if (entity instanceof Player player) {
            NarutomodModVariables.PlayerVariables variables = NarutomodModVariables.get(player);
            switch (type) {
                case REMOVE -> variables.remove(tagName);
                case INTEGER -> variables.putInt(tagName, intValue);
                case DOUBLE -> variables.putDouble(tagName, doubleValue);
                case BOOLEAN -> variables.putBoolean(tagName, booleanValue);
            }
            if (player instanceof ServerPlayer serverPlayer) {
                NarutomodModVariables.sync(serverPlayer);
            }
            return;
        }

        switch (type) {
            case REMOVE -> entity.getPersistentData().remove(tagName);
            case INTEGER -> entity.getPersistentData().putInt(tagName, intValue);
            case DOUBLE -> entity.getPersistentData().putDouble(tagName, doubleValue);
            case BOOLEAN -> entity.getPersistentData().putBoolean(tagName, booleanValue);
        }
    }

    public static final class EntityNBTTag {
        private EntityNBTTag() {
        }

        public static void removeAndSync(Entity entity, String tagName) {
            applyPersistentData(entity, tagName, EntityPersistentDataSyncMessage.ValueType.REMOVE, 0, 0.0D, false);
            sendToTracking(entity, tagName);
        }

        public static void setAndSync(Entity entity, String tagName, int value) {
            applyPersistentData(entity, tagName, EntityPersistentDataSyncMessage.ValueType.INTEGER, value, 0.0D, false);
            sendToTracking(entity, tagName, value);
        }

        public static void setAndSync(Entity entity, String tagName, double value) {
            applyPersistentData(entity, tagName, EntityPersistentDataSyncMessage.ValueType.DOUBLE, 0, value, false);
            sendToTracking(entity, tagName, value);
        }

        public static void setAndSync(Entity entity, String tagName, boolean value) {
            applyPersistentData(entity, tagName, EntityPersistentDataSyncMessage.ValueType.BOOLEAN, 0, 0.0D, value);
            sendToTracking(entity, tagName, value);
        }

        public static void sendToSelf(ServerPlayer player, String tagName) {
            EntityPersistentDataSyncMessage.sendTo(player, EntityPersistentDataSyncMessage.remove(player, tagName));
        }

        public static void sendToSelf(ServerPlayer player, String tagName, int value) {
            EntityPersistentDataSyncMessage.sendTo(player, EntityPersistentDataSyncMessage.of(player, tagName, value));
        }

        public static void sendToSelf(ServerPlayer player, String tagName, double value) {
            EntityPersistentDataSyncMessage.sendTo(player, EntityPersistentDataSyncMessage.of(player, tagName, value));
        }

        public static void sendToSelf(ServerPlayer player, String tagName, boolean value) {
            EntityPersistentDataSyncMessage.sendTo(player, EntityPersistentDataSyncMessage.of(player, tagName, value));
        }

        public static void sendToTracking(Entity entity, String tagName) {
            EntityPersistentDataSyncMessage.sendTrackingAndSelf(entity, EntityPersistentDataSyncMessage.remove(entity, tagName));
        }

        public static void sendToTracking(Entity entity, String tagName, int value) {
            EntityPersistentDataSyncMessage.sendTrackingAndSelf(entity, EntityPersistentDataSyncMessage.of(entity, tagName, value));
        }

        public static void sendToTracking(Entity entity, String tagName, double value) {
            EntityPersistentDataSyncMessage.sendTrackingAndSelf(entity, EntityPersistentDataSyncMessage.of(entity, tagName, value));
        }

        public static void sendToTracking(Entity entity, String tagName, boolean value) {
            EntityPersistentDataSyncMessage.sendTrackingAndSelf(entity, EntityPersistentDataSyncMessage.of(entity, tagName, value));
        }

        public static void sendToServer(Entity entity, String tagName, int value) {
            EntityPersistentDataSyncMessage.sendToServer(EntityPersistentDataSyncMessage.of(entity, tagName, value));
        }

        public static void sendToServer(Entity entity, String tagName, double value) {
            EntityPersistentDataSyncMessage.sendToServer(EntityPersistentDataSyncMessage.of(entity, tagName, value));
        }

        public static void sendToServer(Entity entity, String tagName, boolean value) {
            EntityPersistentDataSyncMessage.sendToServer(EntityPersistentDataSyncMessage.of(entity, tagName, value));
        }
    }
}
