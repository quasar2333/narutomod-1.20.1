package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.client.model.SevenTailsModel;
import net.narutomod.entity.TailedBeastEntity;

public final class SevenTailsRenderer extends MobRenderer<TailedBeastEntity, SevenTailsModel> {
    private static final float SHADOW_SCALE = TailedBeastEntity.Variant.SEVEN.modelScale() * 0.5F;

    public SevenTailsRenderer(EntityRendererProvider.Context context) {
        super(context, new SevenTailsModel(context.bakeLayer(SevenTailsModel.LAYER_LOCATION)), SHADOW_SCALE);
    }

    @Override
    protected void scale(TailedBeastEntity entity, PoseStack poseStack, float partialTick) {
        float modelScale = entity.getModelScale();
        poseStack.translate(0.0F, 1.5F - 1.5F * modelScale, 0.0F);
        poseStack.scale(modelScale, modelScale, modelScale);
        this.shadowRadius = modelScale * 0.5F;
    }

    @Override
    public ResourceLocation getTextureLocation(TailedBeastEntity entity) {
        return entity.getVariant().texture();
    }
}
