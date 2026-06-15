package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.narutomod.client.model.SpikeModel;
import net.narutomod.client.render.NarutoRenderTypes;

abstract class LegacySpikeModelRenderer<T extends Entity> extends EntityRenderer<T> {
    private final SpikeModel<T> model;

    LegacySpikeModelRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new SpikeModel<>(context.bakeLayer(SpikeModel.LAYER_LOCATION));
        this.shadowRadius = 0.1F;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float scale = getScale(entity);
        if (scale <= 0.0F) {
            return;
        }
        int color = getColor(entity);
        float alpha = alpha(color);
        ResourceLocation texture = getTextureLocation(entity);
        int light = alpha < 1.0F ? LightTexture.FULL_BRIGHT : packedLight;
        VertexConsumer consumer = bufferSource.getBuffer(alpha < 1.0F
                ? NarutoRenderTypes.translucentEmissiveNoCull(texture)
                : RenderType.entityCutoutNoCull(texture));

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.rotLerp(partialTick, entity.xRotO, entity.getXRot()) - 180.0F));
        poseStack.scale(scale, scale, scale);
        this.model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, 0.0F);
        this.model.renderToBuffer(poseStack, consumer, light, OverlayTexture.NO_OVERLAY,
                red(color), green(color), blue(color), alpha);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, light);
    }

    protected abstract float getScale(T entity);

    protected int getColor(T entity) {
        return 0xFFFFFFFF;
    }

    private static float red(int color) {
        return ((color >> 16) & 0xFF) / 255.0F;
    }

    private static float green(int color) {
        return ((color >> 8) & 0xFF) / 255.0F;
    }

    private static float blue(int color) {
        return (color & 0xFF) / 255.0F;
    }

    private static float alpha(int color) {
        return ((color >> 24) & 0xFF) / 255.0F;
    }
}
