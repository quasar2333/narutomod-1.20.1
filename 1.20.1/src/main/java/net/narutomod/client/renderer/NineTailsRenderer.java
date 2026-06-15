package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.client.model.NineTailsModel;
import net.narutomod.entity.TailedBeastEntity;

public final class NineTailsRenderer extends MobRenderer<TailedBeastEntity, NineTailsModel> {
    private static final float SHADOW_SCALE = TailedBeastEntity.Variant.NINE.modelScale() * 0.5F;

    public NineTailsRenderer(EntityRendererProvider.Context context) {
        super(context, new NineTailsModel(context.bakeLayer(NineTailsModel.LAYER_LOCATION)), SHADOW_SCALE);
    }

    @Override
    public void render(TailedBeastEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        this.model.setLegacyAlpha(entity.isKcm() ? 0.8F : 1.0F);
        try {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource,
                    entity.isKcm() ? LightTexture.FULL_BRIGHT : packedLight);
        } finally {
            this.model.setLegacyAlpha(1.0F);
        }
    }

    @Override
    protected RenderType getRenderType(TailedBeastEntity entity, boolean bodyVisible, boolean translucent, boolean glowing) {
        if (entity.isKcm() && bodyVisible) {
            return RenderType.entityTranslucent(getTextureLocation(entity));
        }
        return super.getRenderType(entity, bodyVisible, translucent, glowing);
    }

    @Override
    protected void scale(TailedBeastEntity entity, PoseStack poseStack, float partialTick) {
        float modelScale = entity.getModelScale();
        poseStack.translate(0.0F, 1.5F - 1.5F * modelScale, 0.375F * modelScale);
        poseStack.scale(modelScale, modelScale, modelScale);
        this.shadowRadius = modelScale * 0.5F;
    }

    @Override
    public ResourceLocation getTextureLocation(TailedBeastEntity entity) {
        return entity.isKcm() ? entity.getVariant().kcmTexture() : entity.getVariant().texture();
    }
}
