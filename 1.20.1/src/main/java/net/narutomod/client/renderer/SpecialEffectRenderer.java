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
import net.narutomod.NarutomodMod;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.client.render.SphereMesh;
import net.narutomod.entity.SpecialEffectEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class SpecialEffectRenderer extends EntityRenderer<SpecialEffectEntity> {
    private static final ResourceLocation WHITE_TEXTURE = NarutomodMod.location("textures/white_square.png");
    private static final int ROTATING_LINE_COUNT = 120;

    public SpecialEffectRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(SpecialEffectEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        switch (entity.getEffectType()) {
            case ROTATING_LINES_COLOR_END -> renderRotatingLines(entity, partialTick, poseStack, bufferSource);
            case EXPANDING_SPHERES_FADE_TO_BLACK -> renderExpandingSpheres(entity, partialTick, poseStack, bufferSource);
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public ResourceLocation getTextureLocation(SpecialEffectEntity entity) {
        return WHITE_TEXTURE;
    }

    private static void renderRotatingLines(SpecialEffectEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {
        float age = entity.getAge() + partialTick;
        float progress = Mth.clamp(age / (float)entity.getLifespan(), 0.0F, 1.0F);
        int color = entity.getColor();
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        int centerAlpha = Mth.clamp((int)(255.0F * (1.0F - progress)), 0, 255);
        float radius = entity.getRadius();
        Random random = new Random(432L);
        VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.colorAdditiveTriangles());

        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 30.0F));
        for (int i = 0; i < ROTATING_LINE_COUNT; i++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360.0F + progress * 90.0F));
            float length = (random.nextFloat() + progress) * 0.5F * radius;
            float width = (random.nextFloat() + progress) * 0.12F * radius;
            PoseStack.Pose pose = poseStack.last();
            Matrix4f matrix = pose.pose();
            vertex(matrix, consumer, 0.0F, 0.0F, 0.0F, 255, 255, 255, centerAlpha);
            vertex(matrix, consumer, -0.866F * width, length, -0.5F * width, red, green, blue, 0);
            vertex(matrix, consumer, 0.866F * width, length, -0.5F * width, red, green, blue, 0);
            vertex(matrix, consumer, 0.0F, 0.0F, 0.0F, 255, 255, 255, centerAlpha);
            vertex(matrix, consumer, 0.866F * width, length, -0.5F * width, red, green, blue, 0);
            vertex(matrix, consumer, 0.0F, length, width, red, green, blue, 0);
            vertex(matrix, consumer, 0.0F, 0.0F, 0.0F, 255, 255, 255, centerAlpha);
            vertex(matrix, consumer, 0.0F, length, width, red, green, blue, 0);
            vertex(matrix, consumer, -0.866F * width, length, -0.5F * width, red, green, blue, 0);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void renderExpandingSpheres(SpecialEffectEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {
        float age = entity.getAge() + partialTick;
        float maxScaleAge = Math.max(entity.getRadius(), 1.0F);
        int lifespan = entity.getLifespan();
        float fade = age > 0.6F * lifespan
                ? Mth.clamp(1.0F - (age - 0.6F * lifespan) / (0.4F * lifespan), 0.0F, 1.0F)
                : 1.0F;
        VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.translucentEmissiveNoCull(WHITE_TEXTURE));
        int shells = Math.min(Mth.ceil(age), 256);
        for (int i = 0; i < shells; i++) {
            if (!(age <= maxScaleAge || i > age - maxScaleAge)) {
                continue;
            }
            float shellAge = age - i;
            if (shellAge <= 0.0F) {
                continue;
            }
            float scale = shellAge * 0.7F;
            int gray = Mth.clamp((int)((1.0F - 0.05F * i) * 255.0F), 0, 255);
            int alpha = Mth.clamp((int)(0.101F * fade * 255.0F), 0, 255);
            poseStack.pushPose();
            poseStack.scale(scale, scale, scale);
            PoseStack.Pose pose = poseStack.last();
            Matrix4f matrix = pose.pose();
            Matrix3f normal = pose.normal();
            SphereMesh.renderUnitSphere(matrix, normal, consumer, gray, gray, gray, alpha);
            poseStack.popPose();
        }
    }

    private static void vertex(Matrix4f matrix, VertexConsumer consumer, float x, float y, float z, int red, int green, int blue, int alpha) {
        consumer.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .endVertex();
    }
}
