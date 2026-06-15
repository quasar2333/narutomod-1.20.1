package net.narutomod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class LegacyLongCubeModel {
    private static final float UNIT = 1.0F / 16.0F;
    private static final float TEXTURE_SIZE = 32.0F;
    private static final float ORIGIN_Y_UNITS = -16.0F;
    private static final int OPAQUE = 255;
    private static final int ALPHA_03 = 77;
    private static final LayerSpec[] JINTON_LAYERS = {
            new LayerSpec(1.0F, 255, 255, 255, OPAQUE),
            new LayerSpec(2.0F, 255, 255, 255, ALPHA_03),
            new LayerSpec(3.0F, 255, 255, 255, ALPHA_03),
            new LayerSpec(4.0F, 255, 255, 255, ALPHA_03),
            new LayerSpec(8.0F, 255, 255, 255, ALPHA_03)
    };
    private static final LayerSpec[] SEKIZO_LAYERS = {
            new LayerSpec(1.0F, 0, 0, 0, ALPHA_03),
            new LayerSpec(2.0F, 0, 0, 0, ALPHA_03),
            new LayerSpec(3.0F, 0, 0, 0, ALPHA_03),
            new LayerSpec(4.0F, 0, 0, 0, ALPHA_03),
            new LayerSpec(8.0F, 255, 255, 255, ALPHA_03)
    };

    public void renderJintonBeam(PoseStack poseStack, VertexConsumer consumer, float modelLength, float modelScale) {
        renderLayers(poseStack, consumer, modelLength, modelScale, 1.0F, JINTON_LAYERS);
    }

    public void renderSekizo(PoseStack poseStack, VertexConsumer consumer, float modelLength, float modelScale) {
        renderLayers(poseStack, consumer, modelLength, modelScale, 0.0F, SEKIZO_LAYERS);
    }

    private static void renderLayers(PoseStack poseStack, VertexConsumer consumer, float modelLength, float modelScale,
            float extraYOffset, LayerSpec[] layers) {
        int lengthUnits = Math.max((int)(16.0F * modelLength), 1);
        poseStack.pushPose();
        poseStack.translate(0.0D, (modelScale - 1.0F) * 1.5F + extraYOffset, 0.0D);
        poseStack.scale(modelScale, modelScale, modelScale);
        for (LayerSpec layer : layers) {
            renderLayer(poseStack, consumer, layer, lengthUnits);
        }
        poseStack.popPose();
    }

    private static void renderLayer(PoseStack poseStack, VertexConsumer consumer, LayerSpec layer, int lengthUnits) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        float half = layer.widthUnits * 0.5F * UNIT;
        float y0 = ORIGIN_Y_UNITS * UNIT;
        float y1 = (ORIGIN_Y_UNITS + lengthUnits) * UNIT;
        renderBox(matrix, normal, consumer, -half, y0, -half, half, y1, half, layer, lengthUnits);
    }

    private static void renderBox(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            LayerSpec layer, int lengthUnits) {
        float u0 = 0.0F;
        float v0 = 0.0F;
        float uDepth = layer.widthUnits;
        float uWidth = layer.widthUnits;
        float vDepth = layer.widthUnits;
        float vLength = lengthUnits;

        face(matrix, normal, consumer, x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1,
                uv(u0), uv(v0 + vDepth), uv(u0 + uDepth), uv(v0 + vDepth + vLength), -1.0F, 0.0F, 0.0F, layer);
        face(matrix, normal, consumer, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0,
                uv(u0 + uDepth + uWidth), uv(v0 + vDepth),
                uv(u0 + uDepth + uWidth + uDepth), uv(v0 + vDepth + vLength), 1.0F, 0.0F, 0.0F, layer);
        face(matrix, normal, consumer, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1,
                uv(u0 + uDepth), uv(v0), uv(u0 + uDepth + uWidth), uv(v0 + vDepth), 0.0F, 1.0F, 0.0F, layer);
        face(matrix, normal, consumer, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0,
                uv(u0 + uDepth + uWidth), uv(v0),
                uv(u0 + uDepth + uWidth + uWidth), uv(v0 + vDepth), 0.0F, -1.0F, 0.0F, layer);
        face(matrix, normal, consumer, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0,
                uv(u0 + uDepth), uv(v0 + vDepth),
                uv(u0 + uDepth + uWidth), uv(v0 + vDepth + vLength), 0.0F, 0.0F, -1.0F, layer);
        face(matrix, normal, consumer, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1,
                uv(u0 + uDepth + uWidth + uDepth), uv(v0 + vDepth),
                uv(u0 + uDepth + uWidth + uDepth + uWidth), uv(v0 + vDepth + vLength), 0.0F, 0.0F, 1.0F, layer);
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
            float normalX, float normalY, float normalZ, LayerSpec layer) {
        vertex(matrix, normal, consumer, x0, y0, z0, u0, v1, normalX, normalY, normalZ, layer);
        vertex(matrix, normal, consumer, x1, y1, z1, u1, v1, normalX, normalY, normalZ, layer);
        vertex(matrix, normal, consumer, x2, y2, z2, u1, v0, normalX, normalY, normalZ, layer);
        vertex(matrix, normal, consumer, x3, y3, z3, u0, v0, normalX, normalY, normalZ, layer);
    }

    private static void vertex(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer, float x, float y, float z,
            float u, float v, float normalX, float normalY, float normalZ, LayerSpec layer) {
        consumer.vertex(matrix, x, y, z)
                .color(layer.red, layer.green, layer.blue, layer.alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, normalX, normalY, normalZ)
                .endVertex();
    }

    private static final class LayerSpec {
        private final float widthUnits;
        private final int red;
        private final int green;
        private final int blue;
        private final int alpha;

        private LayerSpec(float widthUnits, int red, int green, int blue, int alpha) {
            this.widthUnits = widthUnits;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
        }
    }
}
