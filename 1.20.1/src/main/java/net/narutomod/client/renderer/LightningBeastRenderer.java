package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.LightningBeastModel;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.LightningBeastEntity;

public final class LightningBeastRenderer extends EntityRenderer<LightningBeastEntity> {
    private static final float MODEL_SCALE = 2.0F;
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/wolf_lightning.png");
    private static final ResourceLocation CHARGE_TEXTURE = NarutomodMod.location("textures/electric_armor.png");

    private final LightningBeastModel model;

    public LightningBeastRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F * MODEL_SCALE;
        this.model = new LightningBeastModel(context.bakeLayer(LightningBeastModel.LAYER_LOCATION));
    }

    @Override
    public void render(LightningBeastEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float age = entity.tickCount + partialTick;
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        float headYaw = Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot) - bodyYaw;
        float headPitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        float limbSwing = entity.walkAnimation.position(partialTick);
        float limbSwingAmount = Math.min(entity.walkAnimation.speed(partialTick), 1.0F);
        float fadeAlpha = Math.min(age / 60.0F, 1.0F);

        poseStack.pushPose();
        poseStack.translate(0.0D, 1.5D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        poseStack.scale(-MODEL_SCALE, -MODEL_SCALE, MODEL_SCALE);
        poseStack.translate(0.0D, -1.5D, 0.0D);

        this.model.setupAnim(entity, limbSwing, limbSwingAmount, age, headYaw, headPitch);
        VertexConsumer bodyConsumer = bufferSource.getBuffer(NarutoRenderTypes.energyAdditive(TEXTURE));
        this.model.renderToBuffer(
            poseStack,
            bodyConsumer,
            LightTexture.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY,
            1.0F,
            1.0F,
            1.0F,
            fadeAlpha
        );

        if (shouldRenderCharge(entity)) {
            poseStack.pushPose();
            poseStack.scale(1.1F, 1.1F, 1.1F);
            VertexConsumer chargeConsumer = bufferSource.getBuffer(NarutoRenderTypes.scrollingAdditive(CHARGE_TEXTURE, 0.01F, 0.01F));
            this.model.renderToBuffer(
                poseStack,
                chargeConsumer,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                0.5F,
                0.5F,
                0.5F,
                0.5F
            );
            poseStack.popPose();
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LightningBeastEntity entity) {
        return TEXTURE;
    }

    private static boolean shouldRenderCharge(LightningBeastEntity entity) {
        int tickPhase = entity.tickCount % 20;
        int threshold = 10 + Math.floorMod(entity.getId() * 7 + entity.tickCount / 20, 10);
        return tickPhase <= threshold;
    }
}
