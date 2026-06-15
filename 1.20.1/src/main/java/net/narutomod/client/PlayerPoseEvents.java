package net.narutomod.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.narutomod.NarutomodMod;
import net.narutomod.NarutomodModVariables;
import net.narutomod.entity.BiggerMeEntity;

@Mod.EventBusSubscriber(modid = NarutomodMod.MODID, value = Dist.CLIENT)
public final class PlayerPoseEvents {
    private PlayerPoseEvents() {
    }

    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        if (event.getRenderer().getModel() instanceof HumanoidModel<?> model
                && hasForcedBowPose(event.getEntity())) {
            model.rightArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (event.getEntity().getVehicle() instanceof BiggerMeEntity) {
            event.setCanceled(true);
        }
    }

    private static boolean hasForcedBowPose(LivingEntity entity) {
        if (entity instanceof Player player) {
            return NarutomodModVariables.getOptional(player)
                    .map(variables -> variables.getBoolean(NarutomodModVariables.FORCE_BOW_POSE))
                    .orElse(false);
        }
        return entity.getPersistentData().getBoolean(NarutomodModVariables.FORCE_BOW_POSE);
    }
}
