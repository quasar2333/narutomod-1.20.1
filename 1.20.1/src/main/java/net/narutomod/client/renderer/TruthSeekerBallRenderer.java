package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.TruthSeekerBallModel;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.TruthSeekerBallEntity;
import org.joml.Quaternionf;

public final class TruthSeekerBallRenderer extends EntityRenderer<TruthSeekerBallEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/truthhseekerball.png");
    private static final float AXIS = 0.70710677F;

    private final TruthSeekerBallModel model;

    public TruthSeekerBallRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.1F;
        this.model = new TruthSeekerBallModel(context.bakeLayer(TruthSeekerBallModel.LAYER_LOCATION));
    }

    @Override
    public void render(TruthSeekerBallEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        float scale = entity.getScale();
        float age = entity.tickCount + partialTick;

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.125D * scale, 0.0D);
        poseStack.scale(scale, scale, scale);
        if (!entity.isShieldOn()) {
            poseStack.mulPose(new Quaternionf().rotationAxis(age * 60.0F * Mth.DEG_TO_RAD, AXIS, AXIS, 0.0F));
        }

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
                1.0F);
        poseStack.popPose();
        this.shadowRadius = 0.1F * scale;
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public ResourceLocation getTextureLocation(TruthSeekerBallEntity entity) {
        return TEXTURE;
    }
}
