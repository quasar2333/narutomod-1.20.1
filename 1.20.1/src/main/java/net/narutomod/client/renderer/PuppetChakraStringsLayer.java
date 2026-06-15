package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.narutomod.NarutomodMod;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.AbstractPuppetEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class PuppetChakraStringsLayer<T extends AbstractPuppetEntity, M extends EntityModel<T>>
        extends RenderLayer<T, M> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/fuuin_beam.png");
    private static final int SEGMENTS = 8;
    private static final float RADIUS = 0.008F;

    public PuppetChakraStringsLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity,
            float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
            float headPitch) {
        LivingEntity owner = entity.getOwner();
        if (owner == null) {
            return;
        }

        float offset = entity.getBbHeight() * 0.1F;
        double dx = Mth.lerp(partialTick, owner.xOld, owner.getX())
                - Mth.lerp(partialTick, entity.xOld, entity.getX());
        double dy = Mth.lerp(partialTick, owner.yOld, owner.getY())
                - (Mth.lerp(partialTick, entity.yOld, entity.getY()) + offset);
        double dz = Mth.lerp(partialTick, owner.zOld, owner.getZ())
                - Mth.lerp(partialTick, entity.zOld, entity.getZ());
        double dxz = Math.sqrt(dx * dx + dz * dz);
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length <= 1.0E-6D) {
            return;
        }

        float rotY = (float) (-Math.atan2(dx, dz) * 180.0D / Math.PI);
        float rotX = (float) (-Math.atan2(dy, dxz) * 180.0D / Math.PI);
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        rotY = Mth.wrapDegrees(rotY - bodyYaw);

        VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.translucentEmissiveNoCull(TEXTURE));
        float scroll = (entity.tickCount + partialTick) * 0.01F;
        poseStack.pushPose();
        poseStack.translate(0.0D, -offset + 0.5D, 0.0D);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotY));
        poseStack.mulPose(Axis.XP.rotationDegrees(rotX - 90.0F));
        renderTube(poseStack.last().pose(), poseStack.last().normal(), consumer, (float) length, scroll);
        poseStack.popPose();
    }

    private static void renderTube(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer, float length,
            float scroll) {
        float v0 = -scroll;
        float v1 = length / 32.0F - scroll;
        for (int index = 0; index < SEGMENTS; index++) {
            float angle0 = index * Mth.TWO_PI / SEGMENTS;
            float angle1 = (index + 1) * Mth.TWO_PI / SEGMENTS;
            float u0 = index / (float) SEGMENTS;
            float u1 = (index + 1) / (float) SEGMENTS;
            float x0 = Mth.sin(angle0) * RADIUS;
            float y0 = Mth.cos(angle0) * RADIUS;
            float x1 = Mth.sin(angle1) * RADIUS;
            float y1 = Mth.cos(angle1) * RADIUS;
            vertex(matrix, normal, consumer, x0, y0, 0.0F, u0, v0);
            vertex(matrix, normal, consumer, x0, y0, length, u0, v1);
            vertex(matrix, normal, consumer, x1, y1, length, u1, v1);
            vertex(matrix, normal, consumer, x1, y1, 0.0F, u1, v0);
        }
    }

    private static void vertex(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer, float x, float y, float z,
            float u, float v) {
        consumer.vertex(matrix, x, y, z)
                .color(255, 255, 255, 128)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
