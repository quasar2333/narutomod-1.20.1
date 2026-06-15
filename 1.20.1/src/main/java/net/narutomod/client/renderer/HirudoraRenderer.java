package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.HirudoraTigerModel;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.HirudoraEntity;

public final class HirudoraRenderer extends EntityRenderer<HirudoraEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/hirudoratiger.png");

    private final HirudoraTigerModel model;

    public HirudoraRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.1F;
        this.model = new HirudoraTigerModel(context.bakeLayer(HirudoraTigerModel.LAYER_LOCATION));
    }

    @Override
    public void render(HirudoraEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        float scale = entity.getHirudoraScale();
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        poseStack.pushPose();
        poseStack.translate(0.0D, scale, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch - 180.0F));
        poseStack.scale(scale, scale, scale);

        this.model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, 0.0F);
        VertexConsumer headConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE));
        this.model.renderHead(
                poseStack,
                headConsumer,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                1.0F,
                1.0F,
                1.0F,
                1.0F);
        VertexConsumer eyeConsumer = bufferSource.getBuffer(NarutoRenderTypes.translucentEmissiveNoCull(TEXTURE));
        this.model.renderEyes(
                poseStack,
                eyeConsumer,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                1.0F,
                1.0F,
                1.0F,
                1.0F);
        poseStack.popPose();
        this.shadowRadius = 0.1F * scale;
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(HirudoraEntity entity) {
        return TEXTURE;
    }
}
