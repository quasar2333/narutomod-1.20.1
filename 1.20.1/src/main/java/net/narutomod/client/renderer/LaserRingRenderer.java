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
import net.minecraft.util.Mth;
import net.narutomod.NarutomodMod;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.LaserRingEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class LaserRingRenderer extends EntityRenderer<LaserRingEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/ring_lightning.png");

    public LaserRingRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.1F;
    }

    @Override
    public void render(LaserRingEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float age = entity.tickCount + partialTick;
        float scale = (1.0F - age % 5.0F / 5.0F) * 3.0F;
        int alpha = (int)(255.0F * Mth.clamp(age / 30.0F, 0.0F, 1.0F));
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.5D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(9.0F * age));
        poseStack.scale(scale, scale, scale);

        VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.translucentEmissiveNoCull(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        quad(pose.pose(), pose.normal(), consumer, alpha);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public ResourceLocation getTextureLocation(LaserRingEntity entity) {
        return TEXTURE;
    }

    private static void quad(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer, int alpha) {
        vertex(matrix, normal, consumer, -0.5F, -0.5F, 0.0F, 0.0F, 1.0F, alpha);
        vertex(matrix, normal, consumer, 0.5F, -0.5F, 0.0F, 1.0F, 1.0F, alpha);
        vertex(matrix, normal, consumer, 0.5F, 0.5F, 0.0F, 1.0F, 0.0F, alpha);
        vertex(matrix, normal, consumer, -0.5F, 0.5F, 0.0F, 0.0F, 0.0F, alpha);
    }

    private static void vertex(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer, float x, float y, float z, float u, float v, int alpha) {
        consumer.vertex(matrix, x, y, z)
                .color(255, 255, 255, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
