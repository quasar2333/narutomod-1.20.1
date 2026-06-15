package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.SusanooSkeletonModel;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.SusanooSkeletonEntity;

public final class SusanooSkeletonRenderer extends MobRenderer<SusanooSkeletonEntity, SusanooSkeletonModel> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/susanooskeleton.png");
    private static final ResourceLocation FLAME_TEXTURE = NarutomodMod.location("textures/gas256.png");
    private static final float MODEL_SCALE = 1.8F;

    public SusanooSkeletonRenderer(EntityRendererProvider.Context context) {
        super(context, new SusanooSkeletonModel(context.bakeLayer(SusanooSkeletonModel.LAYER_LOCATION)), 1.5F);
        addLayer(new FlameLayer(this));
    }

    @Override
    public void render(SusanooSkeletonEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    protected void scale(SusanooSkeletonEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
        poseStack.translate(0.0F, 1.5F - MODEL_SCALE * 1.5F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(SusanooSkeletonEntity entity) {
        return TEXTURE;
    }

    private static final class FlameLayer extends RenderLayer<SusanooSkeletonEntity, SusanooSkeletonModel> {
        private static final float MAX_ALPHA = 0.5F;

        private FlameLayer(RenderLayerParent<SusanooSkeletonEntity, SusanooSkeletonModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, SusanooSkeletonEntity entity,
                float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            int color = entity.getFlameColor();
            float red = (float)(color >> 16 & 0xFF) / 255.0F;
            float green = (float)(color >> 8 & 0xFF) / 255.0F;
            float blue = (float)(color & 0xFF) / 255.0F;
            float alpha = MAX_ALPHA * Math.min(ageInTicks / 60.0F, 1.0F);
            VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.scrollingTranslucent(FLAME_TEXTURE, 0.0F, 0.01F));
            poseStack.pushPose();
            poseStack.scale(0.99F, 0.99F, 0.99F);
            getParentModel().renderFlameToBuffer(poseStack, consumer, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, red, green, blue, alpha);
            poseStack.popPose();
        }
    }
}
