package net.narutomod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class SuitonStreamModel {
    private static final float UNIT = 1.0F / 16.0F;
    private static final float TEXTURE_WIDTH = 32.0F;
    private static final float TEXTURE_HEIGHT = 1024.0F;
    private static final float HALF_WIDTH = 4.0F * UNIT;
    private static final float ORIGIN_Y = -16.0F * UNIT;

    public void render(PoseStack poseStack, VertexConsumer consumer, float length) {
        int lengthUnits = (int)(16.0F * length);
        if (lengthUnits <= 0) {
            return;
        }

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        renderBox(
                matrix,
                normal,
                consumer,
                -HALF_WIDTH,
                ORIGIN_Y,
                -HALF_WIDTH,
                HALF_WIDTH,
                ORIGIN_Y + lengthUnits * UNIT,
                HALF_WIDTH,
                lengthUnits);
    }

    private static void renderBox(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            int lengthUnits) {
        float u0 = 0.0F;
        float v0 = 0.0F;
        float uDepth = 8.0F;
        float uWidth = 8.0F;
        float vDepth = 8.0F;
        float vLength = lengthUnits;

        face(matrix, normal, consumer, x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1,
                uvX(u0), uvY(v0 + vDepth), uvX(u0 + uDepth), uvY(v0 + vDepth + vLength),
                -1.0F, 0.0F, 0.0F);
        face(matrix, normal, consumer, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0,
                uvX(u0 + uDepth + uWidth), uvY(v0 + vDepth),
                uvX(u0 + uDepth + uWidth + uDepth), uvY(v0 + vDepth + vLength),
                1.0F, 0.0F, 0.0F);
        face(matrix, normal, consumer, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1,
                uvX(u0 + uDepth), uvY(v0), uvX(u0 + uDepth + uWidth), uvY(v0 + vDepth),
                0.0F, 1.0F, 0.0F);
        face(matrix, normal, consumer, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0,
                uvX(u0 + uDepth + uWidth), uvY(v0),
                uvX(u0 + uDepth + uWidth + uWidth), uvY(v0 + vDepth),
                0.0F, -1.0F, 0.0F);
        face(matrix, normal, consumer, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0,
                uvX(u0 + uDepth), uvY(v0 + vDepth),
                uvX(u0 + uDepth + uWidth), uvY(v0 + vDepth + vLength),
                0.0F, 0.0F, -1.0F);
        face(matrix, normal, consumer, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1,
                uvX(u0 + uDepth + uWidth + uDepth), uvY(v0 + vDepth),
                uvX(u0 + uDepth + uWidth + uDepth + uWidth), uvY(v0 + vDepth + vLength),
                0.0F, 0.0F, 1.0F);
    }

    private static float uvX(float value) {
        return value / TEXTURE_WIDTH;
    }

    private static float uvY(float value) {
        return value / TEXTURE_HEIGHT;
    }

    private static void face(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float u0, float v0, float u1, float v1,
            float normalX, float normalY, float normalZ) {
        vertex(matrix, normal, consumer, x0, y0, z0, u0, v1, normalX, normalY, normalZ);
        vertex(matrix, normal, consumer, x1, y1, z1, u1, v1, normalX, normalY, normalZ);
        vertex(matrix, normal, consumer, x2, y2, z2, u1, v0, normalX, normalY, normalZ);
        vertex(matrix, normal, consumer, x3, y3, z3, u0, v0, normalX, normalY, normalZ);
    }

    private static void vertex(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer, float x, float y, float z,
            float u, float v, float normalX, float normalY, float normalZ) {
        consumer.vertex(matrix, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, normalX, normalY, normalZ)
                .endVertex();
    }
}
