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
import net.narutomod.NarutomodMod;
import net.narutomod.entity.KagutsuchiFireballEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class KagutsuchiFireballRenderer extends EntityRenderer<KagutsuchiFireballEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/black_fireball.png");

    public KagutsuchiFireballRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(KagutsuchiFireballEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        float scale = entity.getRenderScale();
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.1D * scale, 0.0D);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(scale, scale, scale);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        vertex(matrix, normal, consumer, -0.5F, -0.25F, 0.0F, 0.0F, 1.0F);
        vertex(matrix, normal, consumer, 0.5F, -0.25F, 0.0F, 1.0F, 1.0F);
        vertex(matrix, normal, consumer, 0.5F, 0.75F, 0.0F, 1.0F, 0.0F);
        vertex(matrix, normal, consumer, -0.5F, 0.75F, 0.0F, 0.0F, 0.0F);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(KagutsuchiFireballEntity entity) {
        return TEXTURE;
    }

    private static void vertex(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer, float x, float y, float z, float u, float v) {
        consumer.vertex(matrix, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
