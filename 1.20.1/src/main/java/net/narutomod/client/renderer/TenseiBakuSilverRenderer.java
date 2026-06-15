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
import net.narutomod.entity.TenseiBakuSilverEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class TenseiBakuSilverRenderer extends EntityRenderer<TenseiBakuSilverEntity> {
    private static final ResourceLocation RING_TEXTURE = NarutomodMod.location("textures/ring_green.png");
    private static final ResourceLocation ORB_TEXTURE = NarutomodMod.location("textures/white_orb.png");

    public TenseiBakuSilverRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.1F;
    }

    @Override
    public void render(TenseiBakuSilverEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float age = entity.tickCount + partialTick;
        float alpha = age / (float) TenseiBakuSilverEntity.GROW_TIME;
        if (alpha > 1.0F) {
            alpha = Math.max(1.0F - (alpha - 1.0F) * 0.5F, 0.0F);
        }

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.5D, 0.0D);
        renderRing(entity, partialTick, age, alpha, poseStack, bufferSource);
        renderOrb(entity, age, poseStack, bufferSource);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public ResourceLocation getTextureLocation(TenseiBakuSilverEntity entity) {
        return RING_TEXTURE;
    }

    private void renderRing(
            TenseiBakuSilverEntity entity,
            float partialTick,
            float age,
            float alpha,
            PoseStack poseStack,
            MultiBufferSource bufferSource) {
        poseStack.pushPose();
        poseStack.scale(3.0F, 3.0F, 3.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(9.0F * age));
        VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.translucentEmissiveNoCull(RING_TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        quad(pose.pose(), pose.normal(), consumer, 0.5F, 1.0F, 1.0F, 1.0F, alpha);
        poseStack.popPose();
    }

    private void renderOrb(TenseiBakuSilverEntity entity, float age, PoseStack poseStack, MultiBufferSource bufferSource) {
        float orbAlpha = Mth.sqrt(1.0F - Mth.clamp(age / (float) TenseiBakuSilverEntity.GROW_TIME, 0.0F, 1.0F));
        if (orbAlpha <= 0.0F) {
            return;
        }
        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(9.0F * age));
        VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.translucentEmissiveNoCull(ORB_TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        quad(pose.pose(), pose.normal(), consumer, 0.5F, 0.0F, 0.0F, 0.0F, orbAlpha);
        poseStack.popPose();
    }

    private static void quad(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer, float halfSize, float red, float green, float blue, float alpha) {
        vertex(matrix, normal, consumer, -halfSize, -halfSize, 0.0F, 0.0F, 1.0F, red, green, blue, alpha);
        vertex(matrix, normal, consumer, halfSize, -halfSize, 0.0F, 1.0F, 1.0F, red, green, blue, alpha);
        vertex(matrix, normal, consumer, halfSize, halfSize, 0.0F, 1.0F, 0.0F, red, green, blue, alpha);
        vertex(matrix, normal, consumer, -halfSize, halfSize, 0.0F, 0.0F, 0.0F, red, green, blue, alpha);
    }

    private static void vertex(
            Matrix4f matrix,
            Matrix3f normal,
            VertexConsumer consumer,
            float x,
            float y,
            float z,
            float u,
            float v,
            float red,
            float green,
            float blue,
            float alpha) {
        consumer.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
