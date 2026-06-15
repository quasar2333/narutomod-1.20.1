package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.ChibakuTenseiBallModel;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.ChibakuTenseiBallEntity;
import org.joml.Quaternionf;

public final class ChibakuTenseiBallRenderer extends EntityRenderer<ChibakuTenseiBallEntity> {
    private static final ResourceLocation BLANK_TEXTURE = NarutomodMod.location("textures/blank.png");
    private static final ResourceLocation TRUTH_SEEKER_TEXTURE = NarutomodMod.location("textures/truthhseekerball.png");
    private static final float ROTATION_AXIS = 0.70710677F;

    private final ChibakuTenseiBallModel model;

    public ChibakuTenseiBallRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new ChibakuTenseiBallModel(context.bakeLayer(ChibakuTenseiBallModel.LAYER_LOCATION));
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(ChibakuTenseiBallEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        float scale = Math.max(entity.getBallScale(), 0.01F);
        VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.translucentEmissiveNoCull(getTextureLocation(entity)));
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.125D * scale, 0.0D);
        poseStack.scale(scale, scale, scale);
        float rotation = (entity.tickCount + partialTick) * 10.0F * ((float)Math.PI / 180.0F);
        poseStack.mulPose(new Quaternionf().rotationAxis(rotation, ROTATION_AXIS, ROTATION_AXIS, 0.0F));
        this.model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, 0.0F);
        this.model.renderToBuffer(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public ResourceLocation getTextureLocation(ChibakuTenseiBallEntity entity) {
        return entity.getBallScale() > ChibakuTenseiBallEntity.MAX_SCALE * 0.4F
                ? BLANK_TEXTURE
                : TRUTH_SEEKER_TEXTURE;
    }
}
