package net.narutomod.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class SphereMesh {
    private static final int SLICES = 32;
    private static final int STACKS = 32;

    private SphereMesh() {
    }

    public static void renderUnitSphere(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer, int red, int green, int blue, int alpha) {
        for (int stack = 0; stack < STACKS; stack++) {
            float v0 = (float)stack / (float)STACKS;
            float v1 = (float)(stack + 1) / (float)STACKS;
            double theta0 = -Math.PI * 0.5D + Math.PI * v0;
            double theta1 = -Math.PI * 0.5D + Math.PI * v1;
            for (int slice = 0; slice < SLICES; slice++) {
                float u0 = (float)slice / (float)SLICES;
                float u1 = (float)(slice + 1) / (float)SLICES;
                double phi0 = Math.PI * 2.0D * u0;
                double phi1 = Math.PI * 2.0D * u1;
                vertex(matrix, normal, consumer, theta0, phi0, u0, v0, red, green, blue, alpha);
                vertex(matrix, normal, consumer, theta0, phi1, u1, v0, red, green, blue, alpha);
                vertex(matrix, normal, consumer, theta1, phi1, u1, v1, red, green, blue, alpha);
                vertex(matrix, normal, consumer, theta1, phi0, u0, v1, red, green, blue, alpha);
            }
        }
    }

    public static void renderDoubleSidedLegacySphere(PoseStack poseStack, VertexConsumer consumer, float radius, int red, int green, int blue, int alpha) {
        poseStack.pushPose();
        poseStack.scale(radius, radius, radius);
        PoseStack.Pose pose = poseStack.last();
        renderUnitSphere(pose.pose(), pose.normal(), consumer, red, green, blue, alpha);
        poseStack.popPose();
    }

    private static void vertex(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer,
                               double theta, double phi, float u, float v,
                               int red, int green, int blue, int alpha) {
        float y = (float)Math.sin(theta);
        float radius = (float)Math.cos(theta);
        float x = radius * (float)Math.cos(phi);
        float z = radius * (float)Math.sin(phi);
        consumer.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, x, y, z)
                .endVertex();
    }
}
