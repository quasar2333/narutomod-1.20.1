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
import net.narutomod.entity.YasakaMagatamaEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class YasakaMagatamaRenderer extends EntityRenderer<YasakaMagatamaEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/yasaka_magatama.png");

    public YasakaMagatamaRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.1F;
    }

    @Override
    public void render(YasakaMagatamaEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        float age = entity.tickCount + partialTick;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.rotLerp(partialTick, entity.xRotO, entity.getXRot())));
        if (entity.getDeltaMovement().lengthSqr() > 1.0E-6D) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(age));
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(-30.0F * age));
        float scale = entity.getMagatamaScale();
        poseStack.scale(scale, scale, scale);

        int color = entity.getColor();
        float red = ((color >>> 16) & 0xFF) / 255.0F;
        float green = ((color >>> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.translucentEmissiveNoCull(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        renderLayer(pose.pose(), pose.normal(), consumer, 0.50F, 0.50F, red, green, blue, 1.0F);
        renderLayer(pose.pose(), pose.normal(), consumer, 0.51F, 0.49F, red, green, blue, 0.5F);
        renderLayer(pose.pose(), pose.normal(), consumer, 0.49F, 0.49F, red, green, blue, 0.5F);
        renderLayer(pose.pose(), pose.normal(), consumer, 0.52F, 0.48F, red, green, blue, 0.5F);
        renderLayer(pose.pose(), pose.normal(), consumer, 0.48F, 0.48F, red, green, blue, 0.5F);
        renderLayer(pose.pose(), pose.normal(), consumer, 0.53F, 0.45F, 1.0F, 1.0F, 0.0F, 0.5F);
        renderLayer(pose.pose(), pose.normal(), consumer, 0.47F, 0.45F, 1.0F, 1.0F, 0.0F, 0.5F);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public ResourceLocation getTextureLocation(YasakaMagatamaEntity entity) {
        return TEXTURE;
    }

    private static void renderLayer(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer, float y, float halfSize,
            float red, float green, float blue, float alpha) {
        vertex(matrix, normal, consumer, -halfSize, y, -halfSize, 0.0F, 1.0F, red, green, blue, alpha);
        vertex(matrix, normal, consumer, halfSize, y, -halfSize, 1.0F, 1.0F, red, green, blue, alpha);
        vertex(matrix, normal, consumer, halfSize, y, halfSize, 1.0F, 0.0F, red, green, blue, alpha);
        vertex(matrix, normal, consumer, -halfSize, y, halfSize, 0.0F, 0.0F, red, green, blue, alpha);
    }

    private static void vertex(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer, float x, float y, float z,
            float u, float v, float red, float green, float blue, float alpha) {
        consumer.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
