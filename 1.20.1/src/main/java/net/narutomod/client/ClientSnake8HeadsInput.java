package net.narutomod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.narutomod.NarutomodMod;
import net.narutomod.entity.Snake8HeadsEntity;
import net.narutomod.network.NetworkHandler;
import net.narutomod.network.Snake8HeadsAttackMessage;

@Mod.EventBusSubscriber(modid = NarutomodMod.MODID, value = Dist.CLIENT)
public final class ClientSnake8HeadsInput {
    private ClientSnake8HeadsInput() {
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        if (!(minecraft.player.getVehicle() instanceof Snake8HeadsEntity)) {
            return;
        }
        if (minecraft.hitResult != null && minecraft.hitResult.getType() != HitResult.Type.MISS) {
            return;
        }
        NetworkHandler.sendToServer(new Snake8HeadsAttackMessage());
    }
}
