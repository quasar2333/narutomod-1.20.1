package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.TwoTailsModel;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.TailedBeastEntity;

public final class TwoTailsRenderer extends MobRenderer<TailedBeastEntity, TwoTailsModel> {
    private static final ResourceLocation FLAME_TEXTURE = NarutomodMod.location("textures/twotailsflames.png");
    private static final float SHADOW_SCALE = TailedBeastEntity.Variant.TWO.modelScale() * 0.5F;

    public TwoTailsRenderer(EntityRendererProvider.Context context) {
        super(context, new TwoTailsModel(context.bakeLayer(TwoTailsModel.LAYER_LOCATION)), SHADOW_SCALE);
        addLayer(new FlameLayer(this));
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

    private static final class FlameLayer extends RenderLayer<TailedBeastEntity, TwoTailsModel> {
        private FlameLayer(RenderLayerParent<TailedBeastEntity, TwoTailsModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                TailedBeastEntity entity, float limbSwing, float limbSwingAmount, float partialTick,
                float ageInTicks, float netHeadYaw, float headPitch) {
            VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.scrollingTranslucent(
                    FLAME_TEXTURE,
                    0.0F,
                    0.01F
            ));
            getParentModel().renderFlameToBuffer(poseStack, consumer, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 0.9F);
        }
    }
}
