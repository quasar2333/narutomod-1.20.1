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
import net.narutomod.entity.TenseiBakuGoldEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class TenseiBakuGoldRenderer extends EntityRenderer<TenseiBakuGoldEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/beam_gold.png");

    public TenseiBakuGoldRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.1F;
    }

    @Override
    public void render(TenseiBakuGoldEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float growth = entity.getGrowth(partialTick);
        float length = entity.getBeamLength() * growth;
        if (length <= 0.0F) {
            return;
        }
        float age = entity.tickCount + partialTick;
        float v0 = -age * 0.01F;
        float v1 = entity.getBeamLength() / 32.0F - age * 0.01F;
        float endWidthScale = 1.5F + (1.0F - growth) * 10.0F;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F - Mth.rotLerp(partialTick, entity.xRotO, entity.getXRot())));
        VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.scrollingAdditive(TEXTURE, 0.0F, 0.0F));
        PoseStack.Pose pose = poseStack.last();
        renderBeamStrip(pose.pose(), pose.normal(), consumer, 0.5F, 0.5F * endWidthScale, length, v0, v1, 0.7F, 0.7F * growth);
        if (growth > 0.98F) {
            renderBeamStrip(pose.pose(), pose.normal(), consumer, 0.6F, 0.6F * endWidthScale, length, v0, v1, 0.11F, 0.11F);
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public boolean shouldRender(TenseiBakuGoldEntity entity, net.minecraft.client.renderer.culling.Frustum camera, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(TenseiBakuGoldEntity entity) {
        return TEXTURE;
    }

    private static void renderBeamStrip(
            Matrix4f matrix,
            Matrix3f normal,
            VertexConsumer consumer,
            float startRadius,
            float endRadius,
            float length,
            float v0,
            float v1,
            float startAlpha,
            float endAlpha) {
        for (int j = 0; j < 8; j++) {
            float u0 = j / 8.0F;
            float u1 = (j + 1) / 8.0F;
            double angle0 = j * Math.PI * 2.0D / 8.0D;
            double angle1 = (j + 1) * Math.PI * 2.0D / 8.0D;
            float x0 = Mth.sin((float) angle0);
            float z0 = Mth.cos((float) angle0);
            float x1 = Mth.sin((float) angle1);
            float z1 = Mth.cos((float) angle1);
            vertex(matrix, normal, consumer, x0 * startRadius, 0.0F, z0 * startRadius, u0, v0, startAlpha);
            vertex(matrix, normal, consumer, x1 * startRadius, 0.0F, z1 * startRadius, u1, v0, startAlpha);
            vertex(matrix, normal, consumer, x1 * endRadius, length, z1 * endRadius, u1, v1, endAlpha);
            vertex(matrix, normal, consumer, x0 * endRadius, length, z0 * endRadius, u0, v1, endAlpha);
        }
    }

    private static void vertex(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer, float x, float y, float z, float u, float v, float alpha) {
        consumer.vertex(matrix, x, y, z)
                .color(1.0F, 1.0F, 1.0F, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
