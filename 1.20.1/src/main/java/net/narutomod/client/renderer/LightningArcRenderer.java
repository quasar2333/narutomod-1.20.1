package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.Random;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.narutomod.NarutomodMod;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.LightningArcEntity;
import org.joml.Matrix4f;

public final class LightningArcRenderer extends EntityRenderer<LightningArcEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/white_square.png");
    private static final double SEGMENT_OFFSET = 0.1D;

    public LightningArcRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(LightningArcEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        Vec3 line = entity.getEndVec().subtract(entity.position());
        if (line.lengthSqr() <= 1.0E-8D) {
            return;
        }
        poseStack.pushPose();
        float yaw = (float)(Mth.atan2(line.x(), line.z()) * Mth.RAD_TO_DEG);
        float pitch = (float)(-Mth.atan2(line.y(), Math.sqrt(line.x() * line.x() + line.z() * line.z())) * Mth.RAD_TO_DEG);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        double thickness = entity.getThickness();
        thickness = thickness == 0.0D ? Math.max(line.length() * 0.004D, 0.0005D) : thickness;
        VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.colorAdditiveTriangleStrip());
        Random random = new Random((long) entity.getId() * 31L + entity.tickCount);
        renderSection(
                poseStack.last().pose(),
                consumer,
                random,
                new Vec3(0.0D, 0.0D, 0.0D),
                new Vec3(0.0D, 0.0D, line.length()),
                thickness,
                entity.getColor(),
                0,
                Math.max(entity.getMaxRecursiveDepth(), 0),
                false);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public ResourceLocation getTextureLocation(LightningArcEntity entity) {
        return TEXTURE;
    }

    private void renderSection(
            Matrix4f matrix,
            VertexConsumer consumer,
            Random random,
            Vec3 from,
            Vec3 to,
            double thickness,
            int color,
            int recursiveDepth,
            int maxRecursiveDepth,
            boolean branch) {
        if (recursiveDepth >= maxRecursiveDepth) {
            emitBolt(matrix, consumer, from, to, thickness, color, branch);
            return;
        }
        Vec3 mid = to.subtract(from).scale(0.5D);
        double offset = mid.length() * SEGMENT_OFFSET;
        mid = mid.add(random.nextGaussian() * offset, random.nextGaussian() * offset, random.nextGaussian() * offset);
        Vec3 middle = from.add(mid);
        renderSection(matrix, consumer, random, from, middle, thickness, color, recursiveDepth + 1, maxRecursiveDepth, branch);
        renderSection(matrix, consumer, random, middle, to, thickness, color, recursiveDepth + 1, maxRecursiveDepth, branch);
        if (random.nextInt(5) == 0) {
            renderSection(matrix, consumer, random, middle, middle.add(mid.scale(0.8D)), thickness * 0.6D, color, recursiveDepth + 1, maxRecursiveDepth, true);
        }
    }

    private static void emitBolt(Matrix4f matrix, VertexConsumer consumer, Vec3 from, Vec3 to, double thickness, int color, boolean branch) {
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        for (int i = 1; i <= 3; i++) {
            if (!branch || i >= 2) {
                double width = thickness * i;
                int alpha = i == 3 ? 0x20 : i == 2 ? 0x80 : 0xF0;
                int r = i == 1 ? 255 : red;
                int g = i == 1 ? 255 : green;
                int b = i == 1 ? 255 : blue;
                vertex(matrix, consumer, from.x() - width, from.y() - width, from.z(), r, g, b, alpha);
                vertex(matrix, consumer, to.x() - width, to.y() - width, to.z(), r, g, b, alpha);
                vertex(matrix, consumer, from.x() - width, from.y() + width, from.z(), r, g, b, alpha);
                vertex(matrix, consumer, to.x() - width, to.y() + width, to.z(), r, g, b, alpha);
                vertex(matrix, consumer, from.x() + width, from.y() + width, from.z(), r, g, b, alpha);
                vertex(matrix, consumer, to.x() + width, to.y() + width, to.z(), r, g, b, alpha);
                vertex(matrix, consumer, from.x() + width, from.y() - width, from.z(), r, g, b, alpha);
                vertex(matrix, consumer, to.x() + width, to.y() - width, to.z(), r, g, b, alpha);
                vertex(matrix, consumer, from.x() - width, from.y() - width, from.z(), r, g, b, alpha);
                vertex(matrix, consumer, to.x() - width, to.y() - width, to.z(), r, g, b, alpha);
            }
        }
    }

    private static void vertex(Matrix4f matrix, VertexConsumer consumer, double x, double y, double z, int red, int green, int blue, int alpha) {
        consumer.vertex(matrix, (float) x, (float) y, (float) z).color(red, green, blue, alpha).endVertex();
    }
}
