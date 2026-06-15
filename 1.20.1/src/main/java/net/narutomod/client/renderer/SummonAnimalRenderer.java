package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.function.Function;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.entity.AbstractSummonAnimalEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class SummonAnimalRenderer<T extends AbstractSummonAnimalEntity> extends EntityRenderer<T> {
    private final Function<T, ResourceLocation> textureResolver;

    public SummonAnimalRenderer(EntityRendererProvider.Context context, Function<T, ResourceLocation> textureResolver) {
        super(context);
        this.textureResolver = textureResolver;
        this.shadowRadius = 0.5F;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (!entity.isVisibleSummon()) {
            return;
        }
        float scale = entity.getSummonScale();
        float width = (float)(entity.baseRenderWidth() * scale);
        float height = (float)(entity.baseRenderHeight() * scale);
        float depth = (float)(entity.baseRenderDepth() * scale);
        this.shadowRadius = Math.max(width, depth) * 0.5F;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(getTextureLocation(entity)));
        PoseStack.Pose pose = poseStack.last();
        box(pose.pose(), pose.normal(), consumer, width, height, depth, 255);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return this.textureResolver.apply(entity);
    }

    private static void box(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer, float width, float height, float depth, int alpha) {
        float hw = width * 0.5F;
        float hd = depth * 0.5F;
        float y0 = 0.0F;
        float y1 = height;
        face(matrix, normal, consumer, -hw, y0, -hd, hw, y0, -hd, hw, y1, -hd, -hw, y1, -hd, alpha);
        face(matrix, normal, consumer, hw, y0, hd, -hw, y0, hd, -hw, y1, hd, hw, y1, hd, alpha);
        face(matrix, normal, consumer, -hw, y0, hd, -hw, y0, -hd, -hw, y1, -hd, -hw, y1, hd, alpha);
        face(matrix, normal, consumer, hw, y0, -hd, hw, y0, hd, hw, y1, hd, hw, y1, -hd, alpha);
        face(matrix, normal, consumer, -hw, y1, -hd, hw, y1, -hd, hw, y1, hd, -hw, y1, hd, alpha);
        face(matrix, normal, consumer, -hw, y0, hd, hw, y0, hd, hw, y0, -hd, -hw, y0, -hd, alpha);
    }

    private static void face(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             int alpha) {
        vertex(matrix, normal, consumer, x0, y0, z0, 0.0F, 1.0F, alpha);
        vertex(matrix, normal, consumer, x1, y1, z1, 1.0F, 1.0F, alpha);
        vertex(matrix, normal, consumer, x2, y2, z2, 1.0F, 0.0F, alpha);
        vertex(matrix, normal, consumer, x3, y3, z3, 0.0F, 0.0F, alpha);
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
