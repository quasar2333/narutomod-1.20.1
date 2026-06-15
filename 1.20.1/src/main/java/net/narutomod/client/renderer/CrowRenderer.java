package net.narutomod.client.renderer;

import net.minecraft.client.renderer.entity.BatRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ambient.Bat;
import net.narutomod.NarutomodMod;
import net.narutomod.entity.CrowEntity;

public final class CrowRenderer extends BatRenderer {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/crow.png");

    public CrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static EntityRendererProvider<CrowEntity> provider() {
        return context -> {
            @SuppressWarnings("unchecked")
            EntityRenderer<CrowEntity> renderer = (EntityRenderer<CrowEntity>) (EntityRenderer<?>) new CrowRenderer(context);
            return renderer;
        };
    }

    @Override
    public ResourceLocation getTextureLocation(Bat entity) {
        return TEXTURE;
    }
}
