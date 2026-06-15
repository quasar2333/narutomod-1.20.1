package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.MightGuyModel;
import net.narutomod.entity.MightGuyEntity;

public final class MightGuyRenderer extends MobRenderer<MightGuyEntity, MightGuyModel> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/might_guy.png");
    private static final float LEGACY_GATE_OVERLAY_THRESHOLD = 3.0F;
    private static final float LEGACY_GATE_OVERLAY_RED = 0.75F;
    private static final float LEGACY_GATE_OVERLAY_ALPHA = 0.69F;

    public MightGuyRenderer(EntityRendererProvider.Context context) {
        super(context, new MightGuyModel(context.bakeLayer(MightGuyModel.LAYER_LOCATION)), 0.5F);
        this.addLayer(new GateOverlayLayer(this));
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(MightGuyEntity entity) {
        return TEXTURE;
    }

    private static final class GateOverlayLayer extends RenderLayer<MightGuyEntity, MightGuyModel> {
        private GateOverlayLayer(RenderLayerParent<MightGuyEntity, MightGuyModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, MightGuyEntity entity,
                float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
                float headPitch) {
            if (entity.getGateOpened() < LEGACY_GATE_OVERLAY_THRESHOLD || entity.isInvisible()) {
                return;
            }
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE));
            this.getParentModel().renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                    LEGACY_GATE_OVERLAY_RED, 0.0F, 0.0F, LEGACY_GATE_OVERLAY_ALPHA);
        }
    }
}
