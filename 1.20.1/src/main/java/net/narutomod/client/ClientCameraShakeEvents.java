package net.narutomod.client;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.narutomod.NarutomodMod;
import net.narutomod.network.CameraShakeMessage;

@Mod.EventBusSubscriber(modid = NarutomodMod.MODID, value = Dist.CLIENT)
public final class ClientCameraShakeEvents {
    private static int shakeDuration;
    private static float shakeScale;

    private ClientCameraShakeEvents() {
    }

    public static void apply(CameraShakeMessage message) {
        shakeDuration = Math.max(0, message.duration());
        shakeScale = Math.max(0.0F, message.scale());
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (shakeDuration <= 0 || shakeScale <= 0.0F) {
            return;
        }
        event.setYaw(event.getYaw() + randomOffset());
        event.setPitch(event.getPitch() + randomOffset());
        event.setRoll(event.getRoll() + randomOffset());
        shakeDuration--;
    }

    private static float randomOffset() {
        return (ThreadLocalRandom.current().nextFloat() - 0.5F) * shakeScale;
    }
}
