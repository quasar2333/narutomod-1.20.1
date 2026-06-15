package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.RasenshurikenModel;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.RasenshurikenEntity;

public final class RasenshurikenRenderer extends EntityRenderer<RasenshurikenEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/rasenshuriken.png");

    private final RasenshurikenModel model;

    public RasenshurikenRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        this.model = new RasenshurikenModel(context.bakeLayer(RasenshurikenModel.LAYER_LOCATION));
    }

    @Override
    public void render(RasenshurikenEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        float scale = entity.getCurrentScale();
        float age = entity.tickCount + partialTick;

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.25D * scale, 0.0D);
        poseStack.scale(scale, scale, scale);
        this.model.setupAnim(entity, 0.0F, 0.0F, age, 0.0F, 0.0F);

        int color = entity.getBallColor();
        int impactTicks = entity.getImpactTicks();
        float red = impactTicks == 0 ? ((color >> 16) & 0xFF) / 255.0F : 1.0F;
        float green = impactTicks == 0 ? ((color >> 8) & 0xFF) / 255.0F : 1.0F;
        float blue = impactTicks == 0 ? (color & 0xFF) / 255.0F : 1.0F;
        float alpha = impactTicks == 0 ? ((color >> 24) & 0xFF) / 255.0F : 0.2F;
        RenderType ballRenderType = impactTicks == 0 && alpha > 0.8F
            ? NarutoRenderTypes.translucentEmissiveNoCull(TEXTURE)
            : NarutoRenderTypes.energyAdditive(TEXTURE);
        VertexConsumer ballConsumer = bufferSource.getBuffer(ballRenderType);
        this.model.renderBall(
            poseStack,
            ballConsumer,
            LightTexture.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY,
            red,
            green,
            blue,
            alpha
        );

        if (impactTicks == 0) {
            poseStack.pushPose();
            float flapScale = RasenshurikenModel.flapScale(age);
            poseStack.scale(flapScale, flapScale, flapScale);
            VertexConsumer flapConsumer = bufferSource.getBuffer(NarutoRenderTypes.energyAdditive(TEXTURE));
            this.model.renderFlaps(
                poseStack,
                flapConsumer,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                1.0F,
                1.0F,
                1.0F,
                1.0F
            );
            poseStack.popPose();
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public ResourceLocation getTextureLocation(RasenshurikenEntity entity) {
        return TEXTURE;
    }
}
