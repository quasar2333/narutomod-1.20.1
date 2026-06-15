package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.narutomod.NarutomodMod;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.SealingChainsEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class SealingChainsRenderer extends EntityRenderer<SealingChainsEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/chainlink_gold.png");
    private static final double LINK_SPACING = 2.5D / 16.0D;
    private static final double LINK_HALF_LENGTH = 1.5D / 16.0D;
    private static final double LINK_HALF_WIDTH = 0.7D / 16.0D;
    private static final double LINK_HALF_DEPTH = 1.2D / 16.0D;
    private static final double LINK_TWIST_RADIANS = Math.PI * 0.4722222D;
    private static final double TIP_LENGTH = 0.32D;
    private static final double TIP_RADIUS = 0.24D;

    public SealingChainsRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(SealingChainsEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        Vec3 fromWorld = interpolatedPosition(entity, partialTick);
        Vec3 line = entity.getTargetAttachVec().subtract(fromWorld);
        double length = line.length();
        if (length <= 1.0E-6D) {
            return;
        }

        float extension = getExtension(entity, partialTick);
        Vec3 from = Vec3.ZERO;
        Vec3 to = line.scale(extension);
        VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.translucentEmissiveNoCull(TEXTURE));
        renderChainLinks(poseStack.last().pose(), poseStack.last().normal(), consumer, from, to);
        renderChainTip(poseStack.last().pose(), poseStack.last().normal(), consumer, from, to);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public boolean shouldRender(SealingChainsEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(SealingChainsEntity entity) {
        return TEXTURE;
    }

    private static float getExtension(SealingChainsEntity entity, float partialTick) {
        int retractTicks = entity.getRetractTicks();
        if (retractTicks >= 0) {
            return Mth.clamp((retractTicks - partialTick) / 20.0F, 0.0F, 1.0F);
        }
        return Mth.clamp((entity.tickCount + partialTick) / 10.0F, 0.0F, 1.0F);
    }

    private static Vec3 interpolatedPosition(SealingChainsEntity entity, float partialTick) {
        return new Vec3(
                Mth.lerp(partialTick, entity.xOld, entity.getX()),
                Mth.lerp(partialTick, entity.yOld, entity.getY()),
                Mth.lerp(partialTick, entity.zOld, entity.getZ()));
    }

    private static void renderChainLinks(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer, Vec3 from, Vec3 to) {
        Vec3 axis = to.subtract(from);
        double length = axis.length();
        if (length <= 1.0E-6D) {
            return;
        }
        axis = axis.scale(1.0D / length);
        Vec3 reference = Math.abs(axis.y()) > 0.9D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = axis.cross(reference).normalize();
        Vec3 up = right.cross(axis).normalize();

        int linkCount = Math.max(0, (int)Math.ceil(length * 6.4D) - 1);
        for (int i = 0; i < linkCount; i++) {
            double centerOffset = i * LINK_SPACING + LINK_HALF_LENGTH;
            if (centerOffset - LINK_HALF_LENGTH > length) {
                break;
            }
            double twist = i * LINK_TWIST_RADIANS;
            Vec3 sideA = right.scale(Math.cos(twist)).add(up.scale(Math.sin(twist))).normalize();
            Vec3 sideB = right.scale(-Math.sin(twist)).add(up.scale(Math.cos(twist))).normalize();
            Vec3 center = from.add(axis.scale(Math.min(centerOffset, length)));
            renderLinkBox(matrix, normal, consumer, center, axis, sideA, sideB, i);
        }
    }

    private static void renderLinkBox(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer, Vec3 center,
            Vec3 axis, Vec3 sideA, Vec3 sideB, int index) {
        Vec3 forward = axis.scale(LINK_HALF_LENGTH);
        Vec3 right = sideA.scale(LINK_HALF_WIDTH);
        Vec3 up = sideB.scale(LINK_HALF_DEPTH);

        Vec3 p000 = center.subtract(forward).subtract(right).subtract(up);
        Vec3 p001 = center.subtract(forward).subtract(right).add(up);
        Vec3 p010 = center.subtract(forward).add(right).subtract(up);
        Vec3 p011 = center.subtract(forward).add(right).add(up);
        Vec3 p100 = center.add(forward).subtract(right).subtract(up);
        Vec3 p101 = center.add(forward).subtract(right).add(up);
        Vec3 p110 = center.add(forward).add(right).subtract(up);
        Vec3 p111 = center.add(forward).add(right).add(up);

        float v0 = index % 2 == 0 ? 0.0F : 0.5F;
        float v1 = v0 + 0.5F;
        quad(matrix, normal, consumer, p000, p010, p110, p100, 0.0F, v0, 0.5F, v1);
        quad(matrix, normal, consumer, p011, p001, p101, p111, 0.5F, v0, 1.0F, v1);
        quad(matrix, normal, consumer, p001, p000, p100, p101, 0.0F, v0, 0.5F, v1);
        quad(matrix, normal, consumer, p010, p011, p111, p110, 0.5F, v0, 1.0F, v1);
        quad(matrix, normal, consumer, p001, p011, p010, p000, 0.0F, 0.0F, 1.0F, 0.5F);
        quad(matrix, normal, consumer, p100, p110, p111, p101, 0.0F, 0.5F, 1.0F, 1.0F);
    }

    private static void quad(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
            float u0, float v0, float u1, float v1) {
        vertex(matrix, normal, consumer, p0, u0, v1);
        vertex(matrix, normal, consumer, p1, u1, v1);
        vertex(matrix, normal, consumer, p2, u1, v0);
        vertex(matrix, normal, consumer, p3, u0, v0);
    }

    private static void renderChainTip(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer, Vec3 from, Vec3 to) {
        Vec3 axis = to.subtract(from);
        double length = axis.length();
        if (length <= 1.0E-6D) {
            return;
        }
        axis = axis.scale(1.0D / length);
        Vec3 reference = Math.abs(axis.y()) > 0.9D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = axis.cross(reference).normalize();
        Vec3 up = right.cross(axis).normalize();
        Vec3 base = to.subtract(axis.scale(TIP_LENGTH));
        Vec3 point = to.add(axis.scale(TIP_LENGTH * 0.25D));
        diamond(matrix, normal, consumer, base, point, right);
        diamond(matrix, normal, consumer, base, point, up);
    }

    private static void diamond(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer, Vec3 base, Vec3 point, Vec3 side) {
        Vec3 center = base.add(point).scale(0.5D);
        Vec3 left = center.add(side.scale(TIP_RADIUS));
        Vec3 right = center.subtract(side.scale(TIP_RADIUS));
        vertex(matrix, normal, consumer, base, 0.0F, 1.0F);
        vertex(matrix, normal, consumer, left, 0.0F, 0.0F);
        vertex(matrix, normal, consumer, point, 1.0F, 0.0F);
        vertex(matrix, normal, consumer, right, 1.0F, 1.0F);
    }

    private static void vertex(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer, Vec3 point, float u, float v) {
        consumer.vertex(matrix, (float)point.x(), (float)point.y(), (float)point.z())
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
