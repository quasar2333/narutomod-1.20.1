package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.PurpleDragonModel;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.PurpleDragonEntity;

public final class PurpleDragonRenderer extends EntityRenderer<PurpleDragonEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/dragon_purple.png");

    private final PurpleDragonModel model;

    public PurpleDragonRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        this.model = new PurpleDragonModel(context.bakeLayer(PurpleDragonModel.LAYER_LOCATION));
    }

    @Override
    public boolean shouldRender(PurpleDragonEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(PurpleDragonEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        float scale = entity.getDragonScale();
        float age = entity.tickCount + partialTick;
        float alpha = entity.getWaitProgress(partialTick) * 0.4F;

        poseStack.pushPose();
        poseStack.translate(0.0D, scale, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot()) - 180.0F));
        poseStack.scale(scale, scale, scale);

        VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.translucentEmissiveNoCull(TEXTURE));
        this.model.setupAnim(entity, 0.0F, 0.0F, age, 0.0F, 0.0F);
        this.model.renderToBuffer(
            poseStack,
            consumer,
            LightTexture.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY,
            1.0F,
            1.0F,
            1.0F,
            alpha
        );

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public ResourceLocation getTextureLocation(PurpleDragonEntity entity) {
        return TEXTURE;
    }
}
