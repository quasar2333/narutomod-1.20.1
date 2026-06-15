package net.narutomod.client.renderer;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.entity.AltCamViewEntity;

public final class AltCamViewRenderer extends EntityRenderer<AltCamViewEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/particle/white_square.png");

    public AltCamViewRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(AltCamViewEntity entity) {
        return TEXTURE;
    }
}
