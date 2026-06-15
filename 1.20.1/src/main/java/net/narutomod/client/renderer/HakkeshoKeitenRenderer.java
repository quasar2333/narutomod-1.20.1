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
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.HakkeshoKeitenModel;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.HakkeshoKeitenEntity;

public final class HakkeshoKeitenRenderer extends EntityRenderer<HakkeshoKeitenEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/electric_armor.png");
    private static final float SHELL_ALPHA = 0.3F;

    private final HakkeshoKeitenModel model;

    public HakkeshoKeitenRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        this.model = new HakkeshoKeitenModel(context.bakeLayer(HakkeshoKeitenModel.LAYER_LOCATION));
    }

    @Override
    public void render(HakkeshoKeitenEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        float scale = entity.getRenderShellScale(partialTick);
        if (scale <= 0.0F) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTick) * 30.0F));
        poseStack.scale(scale, scale, scale);

        VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.translucentEmissiveNoCull(TEXTURE));
        this.model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, 0.0F);
        this.model.renderToBuffer(
                poseStack,
                consumer,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                1.0F,
                1.0F,
                1.0F,
                SHELL_ALPHA);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public ResourceLocation getTextureLocation(HakkeshoKeitenEntity entity) {
        return TEXTURE;
    }
}
