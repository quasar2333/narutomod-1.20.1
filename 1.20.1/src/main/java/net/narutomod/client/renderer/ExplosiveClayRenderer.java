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
import net.narutomod.client.model.ClayC1Model;
import net.narutomod.client.model.ClayC2Model;
import net.narutomod.client.model.ClayC3Model;
import net.narutomod.entity.ExplosiveClayEntity;

public final class ExplosiveClayRenderer extends EntityRenderer<ExplosiveClayEntity> {
    private static final ResourceLocation C1_TEXTURE = NarutomodMod.location("textures/vex1.png");
    private static final ResourceLocation C2_TEXTURE = NarutomodMod.location("textures/phantom1.png");
    private static final ResourceLocation C3_TEXTURE = NarutomodMod.location("textures/c3.png");

    private final ClayC1Model c1Model;
    private final ClayC2Model c2Model;
    private final ClayC3Model c3Model;

    public ExplosiveClayRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.35F;
        this.c1Model = new ClayC1Model(context.bakeLayer(ClayC1Model.LAYER_LOCATION));
        this.c2Model = new ClayC2Model(context.bakeLayer(ClayC2Model.LAYER_LOCATION));
        this.c3Model = new ClayC3Model(context.bakeLayer(ClayC3Model.LAYER_LOCATION));
    }

    @Override
    public void render(ExplosiveClayEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        float age = entity.tickCount + partialTick;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        switch (entity.tier()) {
            case C1 -> renderC1(entity, age, partialTick, poseStack, bufferSource, packedLight);
            case C2 -> renderC2(entity, age, partialTick, poseStack, bufferSource, packedLight);
            case C3 -> renderC3(entity, age, partialTick, poseStack, bufferSource, packedLight);
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ExplosiveClayEntity entity) {
        return switch (entity.tier()) {
            case C1 -> C1_TEXTURE;
            case C2 -> C2_TEXTURE;
            case C3 -> C3_TEXTURE;
        };
    }

    private void renderC1(ExplosiveClayEntity entity, float age, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        float scale = entity.getRenderScale(partialTick);
        poseStack.scale(scale, scale, scale);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(C1_TEXTURE));
        this.c1Model.setupAnim(entity, 0.0F, 0.0F, age, 0.0F, 0.0F);
        this.c1Model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
        this.shadowRadius = 0.3F * scale;
    }

    private void renderC2(ExplosiveClayEntity entity, float age, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        float scale = entity.getRenderScale(partialTick);
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0.0D, 1.3125D, 0.1875D);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(C2_TEXTURE));
        this.c2Model.setupAnim(entity, 0.0F, 0.0F, age, 0.0F, 0.0F);
        this.c2Model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
        this.shadowRadius = 0.75F * scale;
    }

    private void renderC3(ExplosiveClayEntity entity, float age, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        float scale = entity.getRenderScale(partialTick);
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0.0D, 0.1D * (scale - 1.0F), 0.0D);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(C3_TEXTURE));
        this.c3Model.setupAnim(entity, 0.0F, 0.0F, age, 0.0F, 0.0F);
        this.c3Model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
        this.shadowRadius = 0.5F * Mth.clamp(scale, 0.5F, 8.0F);
    }
}
