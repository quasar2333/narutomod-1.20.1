package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.KirinModel;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.KirinEntity;

public final class KirinRenderer extends EntityRenderer<KirinEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/dragon_lightning.png");
    private static final ResourceLocation CHARGE_TEXTURE = NarutomodMod.location("textures/electric_armor.png");

    private final KirinModel model;

    public KirinRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        this.model = new KirinModel(context.bakeLayer(KirinModel.LAYER_LOCATION));
    }

    @Override
    public boolean shouldRender(KirinEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(KirinEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float age = entity.tickCount + partialTick;
        float fade = Math.min(age / (float)KirinEntity.WAIT_TICKS, 1.0F);
        float yaw = -Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

        poseStack.pushPose();
        poseStack.translate(0.0F, KirinEntity.SCALE, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch - 180.0F));
        poseStack.scale(KirinEntity.SCALE, KirinEntity.SCALE, KirinEntity.SCALE);

        this.model.setupAnim(entity, 0.0F, 0.0F, age, 0.0F, 0.0F);

        this.model.setFaceDetailsVisible(true);
        VertexConsumer bodyConsumer = bufferSource.getBuffer(NarutoRenderTypes.energyAdditive(TEXTURE));
        this.model.renderToBuffer(
            poseStack,
            bodyConsumer,
            LightTexture.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY,
            1.0F,
            1.0F,
            1.0F,
            fade * 0.5F
        );

        this.model.setFaceDetailsVisible(false);
        poseStack.pushPose();
        poseStack.scale(1.05F, 1.05F, 1.05F);
        VertexConsumer chargeConsumer = bufferSource.getBuffer(NarutoRenderTypes.scrollingTranslucent(CHARGE_TEXTURE, 0.01F, 0.01F));
        this.model.renderToBuffer(
            poseStack,
            chargeConsumer,
            LightTexture.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY,
            1.0F,
            1.0F,
            1.0F,
            fade * 0.5F
        );
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.scale(1.1F, 1.1F, 1.1F);
        VertexConsumer auraConsumer = bufferSource.getBuffer(NarutoRenderTypes.translucentEmissiveNoCull(TEXTURE));
        this.model.renderToBuffer(
            poseStack,
            auraConsumer,
            LightTexture.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY,
            0.0F,
            0.0F,
            1.0F,
            fade * 0.3F
        );
        poseStack.popPose();
        this.model.setFaceDetailsVisible(true);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(KirinEntity entity) {
        return TEXTURE;
    }
}
