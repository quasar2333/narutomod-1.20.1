package net.narutomod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class BuddhaArmModel {
    private static final float TEXTURE_SIZE = 16.0F;
    private static final float BOX_WIDTH_UNITS = 4.0F;
    private static final float BOX_DEPTH_UNITS = 4.0F;

    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, float width, float height, float length,
            float legacyLengthUnits, int alpha) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        float x = width * 0.5F;
        float y = height * 0.5F;
        float z = length * 0.5F;
        float vSideMax = uv(BOX_DEPTH_UNITS + legacyLengthUnits);

        face(matrix, normal, consumer, -x, -y, -z, -x, -y, z, -x, y, z, -x, y, -z,
                uv(0.0F), uv(BOX_DEPTH_UNITS), uv(BOX_DEPTH_UNITS), vSideMax, -1.0F, 0.0F, 0.0F, alpha);
        face(matrix, normal, consumer, x, -y, z, x, -y, -z, x, y, -z, x, y, z,
                uv(BOX_DEPTH_UNITS + BOX_WIDTH_UNITS), uv(BOX_DEPTH_UNITS),
                uv(BOX_DEPTH_UNITS + BOX_WIDTH_UNITS + BOX_DEPTH_UNITS), vSideMax, 1.0F, 0.0F, 0.0F, alpha);
        face(matrix, normal, consumer, -x, y, -z, -x, y, z, x, y, z, x, y, -z,
                uv(BOX_DEPTH_UNITS), uv(0.0F), uv(BOX_DEPTH_UNITS + BOX_WIDTH_UNITS),
                uv(BOX_DEPTH_UNITS), 0.0F, 1.0F, 0.0F, alpha);
        face(matrix, normal, consumer, -x, -y, z, -x, -y, -z, x, -y, -z, x, -y, z,
                uv(BOX_DEPTH_UNITS + BOX_WIDTH_UNITS), uv(0.0F),
                uv(BOX_DEPTH_UNITS + BOX_WIDTH_UNITS + BOX_WIDTH_UNITS), uv(BOX_DEPTH_UNITS),
                0.0F, -1.0F, 0.0F, alpha);
        face(matrix, normal, consumer, x, -y, -z, -x, -y, -z, -x, y, -z, x, y, -z,
                uv(BOX_DEPTH_UNITS), uv(BOX_DEPTH_UNITS),
                uv(BOX_DEPTH_UNITS + BOX_WIDTH_UNITS), vSideMax, 0.0F, 0.0F, -1.0F, alpha);
        face(matrix, normal, consumer, -x, -y, z, x, -y, z, x, y, z, -x, y, z,
                uv(BOX_DEPTH_UNITS + BOX_WIDTH_UNITS + BOX_DEPTH_UNITS), uv(BOX_DEPTH_UNITS),
                uv(BOX_DEPTH_UNITS + BOX_WIDTH_UNITS + BOX_DEPTH_UNITS + BOX_WIDTH_UNITS), vSideMax,
                0.0F, 0.0F, 1.0F, alpha);
    }

    private static float uv(float value) {
        return value / TEXTURE_SIZE;
    }

    private static void face(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float u0, float v0, float u1, float v1,
            float normalX, float normalY, float normalZ, int alpha) {
        vertex(matrix, normal, consumer, x0, y0, z0, u0, v1, normalX, normalY, normalZ, alpha);
        vertex(matrix, normal, consumer, x1, y1, z1, u1, v1, normalX, normalY, normalZ, alpha);
        vertex(matrix, normal, consumer, x2, y2, z2, u1, v0, normalX, normalY, normalZ, alpha);
        vertex(matrix, normal, consumer, x3, y3, z3, u0, v0, normalX, normalY, normalZ, alpha);
    }

    private static void vertex(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer, float x, float y, float z,
            float u, float v, float normalX, float normalY, float normalZ, int alpha) {
        consumer.vertex(matrix, x, y, z)
                .color(255, 255, 255, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, normalX, normalY, normalZ)
                .endVertex();
    }
}
