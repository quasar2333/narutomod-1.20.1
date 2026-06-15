package net.narutomod.client;

import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.narutomod.NarutomodMod;

@Mod.EventBusSubscriber(modid = NarutomodMod.MODID, value = Dist.CLIENT)
public final class SuitonMistFogEvents {
    private static final float EXP_HALF_VISIBILITY_SCALE = 0.85F;
    private static final float MIN_FAR_PLANE = 2.0F;

    private SuitonMistFogEvents() {
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        float density = ClientSuitonMistFogState.density();
        if (density <= 0.0F) {
            return;
        }

        float far = Mth.clamp(EXP_HALF_VISIBILITY_SCALE / density, MIN_FAR_PLANE, event.getFarPlaneDistance());
        event.setNearPlaneDistance(0.0F);
        event.setFarPlaneDistance(far);
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true);
    }
}
