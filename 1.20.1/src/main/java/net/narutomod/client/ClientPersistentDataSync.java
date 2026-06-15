package net.narutomod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.narutomod.network.EntityPersistentDataSyncMessage;
import net.narutomod.procedure.ProcedureSync;

public final class ClientPersistentDataSync {
    private ClientPersistentDataSync() {
    }

    public static void apply(EntityPersistentDataSyncMessage message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        Entity entity = minecraft.level.getEntity(message.entityId());
        if (entity == null && minecraft.player != null && minecraft.player.getId() == message.entityId()) {
            entity = minecraft.player;
        }
        ProcedureSync.applyPersistentData(
                entity,
                message.tagName(),
                message.valueType(),
                message.intValue(),
                message.doubleValue(),
                message.booleanValue());
    }
}
