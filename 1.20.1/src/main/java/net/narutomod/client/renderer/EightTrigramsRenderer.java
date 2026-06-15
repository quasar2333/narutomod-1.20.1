package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.EightTrigramsModel;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.EightTrigramsEntity;

public final class EightTrigramsRenderer extends EntityRenderer<EightTrigramsEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/eight_trigrams.png");

    private final EightTrigramsModel model;

    public EightTrigramsRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        this.model = new EightTrigramsModel(context.bakeLayer(EightTrigramsModel.LAYER_LOCATION));
    }

    @Override
    public void render(EightTrigramsEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.1D, -0.4D);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));

        VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.translucentEmissiveNoCull(TEXTURE));
        this.model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, 0.0F);
        this.model.renderToBuffer(
                poseStack,
                consumer,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                1.0F,
                1.0F,
                1.0F,
                1.0F);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public ResourceLocation getTextureLocation(EightTrigramsEntity entity) {
        return TEXTURE;
    }
}
