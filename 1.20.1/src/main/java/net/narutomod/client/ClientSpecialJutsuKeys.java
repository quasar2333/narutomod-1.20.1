package net.narutomod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.narutomod.NarutomodMod;
import net.narutomod.network.NetworkHandler;
import net.narutomod.network.PowerIncreaseKeyMessage;
import net.narutomod.network.SpecialJutsuKeyMessage;
import org.lwjgl.glfw.GLFW;

public final class ClientSpecialJutsuKeys {
    private static final KeyMapping SPECIAL_JUTSU_1 = new KeyMapping(
            "key.mcreator.specialjutsu1",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.mcreator.category");
    private static final KeyMapping SPECIAL_JUTSU_2 = new KeyMapping(
            "key.mcreator.specialjutsu2",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F,
            "key.mcreator.category");
    private static final KeyMapping SPECIAL_JUTSU_3 = new KeyMapping(
            "key.mcreator.specialjutsu3",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "key.mcreator.category");
    private static final KeyMapping POWER_INCREASE = new KeyMapping(
            "key.mcreator.powerincrease",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UP,
            "key.mcreator.category");

    private static boolean wasSpecialJutsu1Down;
    private static boolean wasSpecialJutsu2Down;
    private static boolean wasSpecialJutsu3Down;
    private static boolean wasPowerIncreaseDown;

    private ClientSpecialJutsuKeys() {
    }

    @Mod.EventBusSubscriber(modid = NarutomodMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBusEvents {
        private ModBusEvents() {
        }

        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(SPECIAL_JUTSU_1);
            event.register(SPECIAL_JUTSU_2);
            event.register(SPECIAL_JUTSU_3);
            event.register(POWER_INCREASE);
        }
    }

    @Mod.EventBusSubscriber(modid = NarutomodMod.MODID, value = Dist.CLIENT)
    public static final class ForgeBusEvents {
        private ForgeBusEvents() {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            boolean canSend = minecraft.player != null && minecraft.screen == null;
            boolean specialJutsu1Down = canSend && SPECIAL_JUTSU_1.isDown();
            boolean specialJutsu2Down = canSend && SPECIAL_JUTSU_2.isDown();
            boolean specialJutsu3Down = canSend && SPECIAL_JUTSU_3.isDown();
            boolean powerIncreaseDown = canSend && POWER_INCREASE.isDown();

            if (specialJutsu1Down != wasSpecialJutsu1Down) {
                NetworkHandler.sendToServer(new SpecialJutsuKeyMessage(1, specialJutsu1Down));
            }
            if (specialJutsu2Down != wasSpecialJutsu2Down) {
                NetworkHandler.sendToServer(new SpecialJutsuKeyMessage(2, specialJutsu2Down));
            }
            if (specialJutsu3Down) {
                NetworkHandler.sendToServer(new SpecialJutsuKeyMessage(3, true));
            } else if (wasSpecialJutsu3Down) {
                NetworkHandler.sendToServer(new SpecialJutsuKeyMessage(3, false));
            }
            if (powerIncreaseDown != wasPowerIncreaseDown) {
                NetworkHandler.sendToServer(new PowerIncreaseKeyMessage(powerIncreaseDown));
            }

            wasSpecialJutsu1Down = specialJutsu1Down;
            wasSpecialJutsu2Down = specialJutsu2Down;
            wasSpecialJutsu3Down = specialJutsu3Down;
            wasPowerIncreaseDown = powerIncreaseDown;
        }
    }
}
