package net.narutomod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.narutomod.NarutomodMod;
import net.narutomod.NarutomodModVariables;
import net.narutomod.item.ObitoKamuiHandler;
import net.narutomod.procedure.ProcedureUtils;

@Mod.EventBusSubscriber(modid = NarutomodMod.MODID, value = Dist.CLIENT)
public final class KamuiFovEvents {
    private static final double TELEPORT_RANGE = 100.0D;
    private static final double BASE_FOV = 70.0D;
    private static final double DISTANCE_FOV_SCALE = 15.0D;
    private static final double MIN_FOV = 1.0D;
    private static final double MAX_FOV = 110.0D;

    private KamuiFovEvents() {
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (!event.usedConfiguredFov()) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !isKamuiTeleporting(player)) {
            return;
        }
        HitResult hit = ProcedureUtils.objectEntityLookingAt(player, TELEPORT_RANGE, 0.0D, true, false, target -> target != player);
        if (hit.getType() == HitResult.Type.MISS) {
            return;
        }
        double distance = Math.max(player.distanceToSqr(hit.getLocation()), 0.0001D);
        event.setFOV(kamuiFov(Math.sqrt(distance)));
    }

    public static double kamuiFov(double distance) {
        return Mth.clamp(BASE_FOV - Math.log(Math.max(distance, 0.01D)) * DISTANCE_FOV_SCALE, MIN_FOV, MAX_FOV);
    }

    private static boolean isKamuiTeleporting(LocalPlayer player) {
        return NarutomodModVariables.getOptional(player)
                .map(variables -> variables.getBoolean(ObitoKamuiHandler.KAMUI_TELEPORT_TAG))
                .orElse(false);
    }
}
