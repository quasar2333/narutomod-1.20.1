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
import net.narutomod.entity.TenseiganOrbEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class TenseiganOrbRenderer extends EntityRenderer<TenseiganOrbEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/white_orb.png");
    private static final float RED = 0.592F;
    private static final float GREEN = 0.984F;
    private static final float BLUE = 0.91F;

    public TenseiganOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.1F;
    }

    @Override
    public void render(TenseiganOrbEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float warmup = Mth.clamp((entity.tickCount + partialTick) / 10.0F, 0.0F, 1.0F);
        warmup *= warmup;
        float scale = entity.getOrbScale();
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.5D * scale, 0.0D);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(scale, scale, scale);

        VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.translucentEmissiveNoCull(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        if (warmup > 0.5F) {
            quad(pose.pose(), pose.normal(), consumer, 0.6F, warmup, 0.25F);
        }
        quad(pose.pose(), pose.normal(), consumer, 0.5F, warmup, 1.0F);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public ResourceLocation getTextureLocation(TenseiganOrbEntity entity) {
        return TEXTURE;
    }

    private static void quad(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer, float halfSize, float colorScale, float alpha) {
        vertex(matrix, normal, consumer, -halfSize, -halfSize, 0.0F, 0.0F, 1.0F, colorScale, alpha);
        vertex(matrix, normal, consumer, halfSize, -halfSize, 0.0F, 1.0F, 1.0F, colorScale, alpha);
        vertex(matrix, normal, consumer, halfSize, halfSize, 0.0F, 1.0F, 0.0F, colorScale, alpha);
        vertex(matrix, normal, consumer, -halfSize, halfSize, 0.0F, 0.0F, 0.0F, colorScale, alpha);
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
            float colorScale,
            float alpha) {
        consumer.vertex(matrix, x, y, z)
                .color(RED * colorScale, GREEN * colorScale, BLUE * colorScale, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
