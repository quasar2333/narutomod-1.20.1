package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.DojutsuHelmetSnugModel;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.PortingDummyEntity;

public final class PortingDummyRenderer extends EntityRenderer<PortingDummyEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/sharinganhelmet.png");
    private final DojutsuHelmetSnugModel<?> helmetModel;

    public PortingDummyRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.25F;
        this.helmetModel = new DojutsuHelmetSnugModel<>(context.bakeLayer(DojutsuHelmetSnugModel.LAYER_LOCATION));
    }

    @Override
    public void render(PortingDummyEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 1.5D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        poseStack.scale(-1.0F, -1.0F, 1.0F);

        this.helmetModel.headwearHidden = false;
        this.helmetModel.highlightHidden = false;
        this.helmetModel.foreheadHidden = false;
        this.helmetModel.head.setRotation(0.0F, 0.0F, 0.0F);
        this.helmetModel.hat.copyFrom(this.helmetModel.head);

        VertexConsumer baseConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE));
        this.helmetModel.renderToBuffer(poseStack, baseConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        VertexConsumer shineConsumer = bufferSource.getBuffer(NarutoRenderTypes.translucentEmissiveNoCull(TEXTURE));
        this.helmetModel.renderHighlight(
            poseStack,
            shineConsumer,
            LightTexture.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY,
            1.0F,
            1.0F,
            1.0F,
            1.0F
        );
        this.helmetModel.renderForehead(
            poseStack,
            shineConsumer,
            LightTexture.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY,
            1.0F,
            1.0F,
            1.0F,
            1.0F
        );

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(PortingDummyEntity entity) {
        return TEXTURE;
    }
}
