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
import net.narutomod.client.model.WaterSharkModel;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.WaterSharkEntity;

public final class WaterSharkRenderer extends EntityRenderer<WaterSharkEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/shark.png");

    private final WaterSharkModel model;

    public WaterSharkRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
        this.model = new WaterSharkModel(context.bakeLayer(WaterSharkModel.LAYER_LOCATION));
    }

    @Override
    public void render(WaterSharkEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        float scale = entity.getScale();
        if (scale <= 0.0F) {
            return;
        }

        float age = entity.tickCount + partialTick;
        float swimAmount = Mth.lerp(partialTick, entity.getPrevLimbSwingAmount(), entity.getLimbSwingAmount());
        float limbSwing = entity.getLegacyLimbSwing() - entity.getLimbSwingAmount() * (1.0F - partialTick);
        float mouthOpen = entity.getMouthOpenAmount();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot()) - 180.0F));
        poseStack.scale(scale, scale, scale);

        VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.translucentEmissiveNoCull(TEXTURE));
        this.model.setupAnim(entity, limbSwing, swimAmount, age, 0.0F, mouthOpen);
        this.model.renderToBuffer(
            poseStack,
            consumer,
            LightTexture.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY,
            1.0F,
            1.0F,
            1.0F,
            1.0F
        );

        poseStack.popPose();
        this.shadowRadius = 0.5F * scale;
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public ResourceLocation getTextureLocation(WaterSharkEntity entity) {
        return TEXTURE;
    }
}
