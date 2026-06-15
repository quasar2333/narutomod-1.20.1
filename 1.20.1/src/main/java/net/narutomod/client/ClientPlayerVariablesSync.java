package net.narutomod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.narutomod.NarutomodModVariables;
import net.narutomod.network.PlayerVariablesSyncMessage;

public final class ClientPlayerVariablesSync {
    private ClientPlayerVariablesSync() {
    }

    public static void apply(PlayerVariablesSyncMessage message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        Entity entity = minecraft.level.getEntity(message.entityId());
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            NarutomodModVariables.get(player).load(message.variables());
        }
    }
}
