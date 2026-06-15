package net.narutomod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.narutomod.item.ByakuganHandler;
import net.narutomod.network.ByakuganViewSyncMessage;
import net.narutomod.registry.ModItems;

public final class ClientByakuganViewState {
    private static boolean active;
    private static float fov = ByakuganHandler.DEFAULT_FOV;
    private static float ninjaLevel;

    private ClientByakuganViewState() {
    }

    public static void apply(ByakuganViewSyncMessage message) {
        active = message.active();
        fov = message.active() ? message.fov() : ByakuganHandler.DEFAULT_FOV;
        ninjaLevel = message.active() ? message.ninjaLevel() : 0.0F;
    }

    public static boolean isActiveForLocalPlayer() {
        LocalPlayer player = Minecraft.getInstance().player;
        return active && player != null && player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.BYAKUGANHELMET.get());
    }

    public static float fov() {
        return fov;
    }

    public static float ninjaLevel() {
        return ninjaLevel;
    }
}
