package net.narutomod.client.renderer;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

final class PlayerSkinTextures {
    private PlayerSkinTextures() {
    }

    static ResourceLocation textureOrDefault(LivingEntity entity, ResourceLocation fallback) {
        if (entity instanceof AbstractClientPlayer player) {
            return player.getSkinTextureLocation();
        }
        return fallback;
    }

    static boolean isSlimModel(LivingEntity entity) {
        return entity instanceof AbstractClientPlayer player && "slim".equals(player.getModelName());
    }
}
