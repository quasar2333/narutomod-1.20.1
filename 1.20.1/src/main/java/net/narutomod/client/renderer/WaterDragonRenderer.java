package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.WaterDragonModel;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.WaterDragonEntity;

public final class WaterDragonRenderer extends EntityRenderer<WaterDragonEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/dragon_blue.png");
    private static final ResourceLocation GAS_TEXTURE = NarutomodMod.location("textures/gas256.png");

    private final WaterDragonModel model;

    public WaterDragonRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.1F;
        this.model = new WaterDragonModel(context.bakeLayer(WaterDragonModel.LAYER_LOCATION));
    }

    @Override
    public boolean shouldRender(WaterDragonEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(WaterDragonEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        float scale = entity.getScale();
        float age = entity.tickCount + partialTick;

        poseStack.pushPose();
        poseStack.translate(0.0D, scale, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot()) - 180.0F));
        poseStack.scale(scale, scale, scale);

        this.model.setupAnim(entity, 0.0F, 0.0F, age, 0.0F, 0.0F);
        this.model.setFaceDetailsVisible(false);
        poseStack.pushPose();
        poseStack.scale(0.99F, 0.99F, 0.99F);
        VertexConsumer gasConsumer = bufferSource.getBuffer(NarutoRenderTypes.scrollingTranslucent(GAS_TEXTURE, 0.0F, 0.01F));
        this.model.renderToBuffer(
            poseStack,
            gasConsumer,
            LightTexture.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY,
            0.04F,
            0.325F,
            0.733F,
            1.0F
        );
        poseStack.popPose();

        this.model.setFaceDetailsVisible(true);
        VertexConsumer bodyConsumer = bufferSource.getBuffer(NarutoRenderTypes.translucentEmissiveNoCull(TEXTURE));
        this.model.renderToBuffer(
            poseStack,
            bodyConsumer,
            LightTexture.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY,
            1.0F,
            1.0F,
            1.0F,
            0.8F
        );
        poseStack.popPose();

        this.shadowRadius = 0.1F * scale;
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public ResourceLocation getTextureLocation(WaterDragonEntity entity) {
        return TEXTURE;
    }
}
