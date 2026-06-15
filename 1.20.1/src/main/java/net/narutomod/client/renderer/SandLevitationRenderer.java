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
import net.narutomod.client.model.SandLevitationModel;
import net.narutomod.entity.SandBulletEntity;
import net.narutomod.entity.SandLevitationEntity;

public final class SandLevitationRenderer extends EntityRenderer<SandLevitationEntity> {
    private static final float CLOUD_SCALE = 2.0F;
    private static final int WAIT_TIME = 40;
    private static final ResourceLocation IRON_TEXTURE = NarutomodMod.location("textures/gray_dark.png");
    private static final ResourceLocation SAND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/sand.png");

    private final SandLevitationModel model;

    public SandLevitationRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F * CLOUD_SCALE;
        this.model = new SandLevitationModel(context.bakeLayer(SandLevitationModel.LAYER_LOCATION));
    }

    @Override
    public void render(SandLevitationEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        if (entity.isReturning()) {
            return;
        }
        float age = entity.tickCount + partialTick;
        float scale = Mth.clamp(age / (WAIT_TIME / CLOUD_SCALE), 0.0F, CLOUD_SCALE);
        if (scale <= 0.0F) {
            return;
        }
        ResourceLocation texture = getTextureLocation(entity);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.scale(scale, scale, scale);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
        this.model.setupAnim(entity, 0.0F, 0.0F, age, 0.0F, 0.0F);
        this.model.renderToBuffer(
            poseStack,
            consumer,
            packedLight,
            OverlayTexture.NO_OVERLAY,
            1.0F,
            1.0F,
            1.0F,
            1.0F
        );

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SandLevitationEntity entity) {
        return entity.getColorForRender() == SandBulletEntity.DEFAULT_COLOR ? IRON_TEXTURE : SAND_TEXTURE;
    }
}
