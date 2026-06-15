package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.KikaichuBeetleModel;
import net.narutomod.entity.BugSwarmEntity;

public final class BugSwarmRenderer extends EntityRenderer<BugSwarmEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/beetle.png");
    private static final float GOLDEN_ANGLE = 2.3999631F;
    private static final int MAX_RENDERED_BEETLES = 96;

    private final KikaichuBeetleModel model;

    public BugSwarmRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        this.model = new KikaichuBeetleModel(context.bakeLayer(KikaichuBeetleModel.LAYER_LOCATION));
    }

    @Override
    public void render(BugSwarmEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        float age = entity.tickCount + partialTick;
        int bugCount = entity.getBugCount();
        int renderCount = Mth.clamp(bugCount, 1, MAX_RENDERED_BEETLES);
        float radius = (float)entity.getSwarmRadius();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE));

        for (int i = 0; i < renderCount; i++) {
            float phase = i * GOLDEN_ANGLE + age * (entity.isReturning() ? -0.28F : 0.28F);
            float yNorm = -1.0F + 2.0F * (i + 0.5F) / renderCount;
            float ring = Mth.sqrt(Math.max(0.0F, 1.0F - yNorm * yNorm));
            float seed = entity.getId() * 31.0F + i * 17.0F;
            float spread = 0.72F + pseudo(seed) * 0.28F;
            float x = Mth.cos(phase) * ring * radius * spread;
            float y = yNorm * radius * 0.45F + Mth.sin(age * 0.7F + i) * 0.06F;
            float z = Mth.sin(phase) * ring * radius * spread;
            float beetleScale = 0.085F + pseudo(seed + 7.0F) * 0.025F;

            poseStack.pushPose();
            poseStack.translate(x, y + 0.03125D, z);
            poseStack.mulPose(Axis.YP.rotationDegrees(-phase * Mth.RAD_TO_DEG));
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F + Mth.sin(age * 0.3F + i) * 9.0F));
            poseStack.scale(beetleScale, beetleScale, beetleScale);
            this.model.setupAnim(entity, age + i * 0.37F, 0.55F, age * 1.8F + i, 0.0F, 0.0F);
            this.model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                    1.0F, 1.0F, 1.0F, 1.0F);
            poseStack.popPose();
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(BugSwarmEntity entity) {
        return TEXTURE;
    }

    private static float pseudo(float value) {
        float hashed = Mth.sin(value * 12.9898F) * 43758.547F;
        return hashed - Mth.floor(hashed);
    }
}
