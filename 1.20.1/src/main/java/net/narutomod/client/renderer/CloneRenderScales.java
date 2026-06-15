package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;

final class CloneRenderScales {
    private static final float PLAYER_OWNER_SCALE = 0.9375F;

    private CloneRenderScales() {
    }

    static float applyOwnerPlayerScale(LivingEntity owner, PoseStack poseStack) {
        if (owner instanceof AbstractClientPlayer) {
            poseStack.scale(PLAYER_OWNER_SCALE, PLAYER_OWNER_SCALE, PLAYER_OWNER_SCALE);
            return PLAYER_OWNER_SCALE;
        }
        return 1.0F;
    }
}
