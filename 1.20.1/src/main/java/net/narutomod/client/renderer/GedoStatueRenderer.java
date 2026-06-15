package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.GedoStatueModel;
import net.narutomod.entity.GedoStatueEntity;

public final class GedoStatueRenderer extends MobRenderer<GedoStatueEntity, GedoStatueModel> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/gedomazo.png");

    public GedoStatueRenderer(EntityRendererProvider.Context context) {
        super(context, new GedoStatueModel(context.bakeLayer(GedoStatueModel.LAYER_LOCATION)), 5.0F);
    }

    @Override
    protected void scale(GedoStatueEntity entity, PoseStack poseStack, float partialTick) {
        float scale = entity.getSummonScale();
        poseStack.translate(0.0F, 1.5F - scale * 1.5F, 0.0F);
        poseStack.scale(scale, scale, scale);
        this.shadowRadius = 0.5F * scale;
    }

    @Override
    public ResourceLocation getTextureLocation(GedoStatueEntity entity) {
        return TEXTURE;
    }
}
